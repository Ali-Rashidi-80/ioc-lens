import SwiftUI
import Security
import UIKit
import UniformTypeIdentifiers
import Vision
import Shared

private final class KeychainStorageDelegate: IosSecureKeychainDelegate {
    private let service = "io.github.mrcoder20.ioclens.secrets"

    func getSecret(key: String) -> String? {
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: key,
            kSecReturnData as String: true,
            kSecMatchLimit as String: kSecMatchLimitOne
        ]
        var result: AnyObject?
        let status = SecItemCopyMatching(query as CFDictionary, &result)
        guard status == errSecSuccess, let data = result as? Data else {
            return nil
        }
        return String(data: data, encoding: .utf8)
    }

    func setSecret(key: String, value: String) {
        if value.isEmpty {
            removeSecret(key: key)
            return
        }
        guard let encoded = value.data(using: .utf8) else { return }
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: key
        ]
        let attributes: [String: Any] = [
            kSecValueData as String: encoded,
            kSecAttrAccessible as String: kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly
        ]
        let status = SecItemUpdate(query as CFDictionary, attributes as CFDictionary)
        if status == errSecItemNotFound {
            var addQuery = query
            addQuery[kSecValueData as String] = encoded
            addQuery[kSecAttrAccessible as String] = kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly
            SecItemAdd(addQuery as CFDictionary, nil)
        }
    }

    func removeSecret(key: String) {
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: key
        ]
        SecItemDelete(query as CFDictionary)
    }
}

private final class NativeVisionDecoderDelegate: IosBarcodeDecoderDelegate {
    func decodeBarcodes(imageBytes: KotlinByteArray) -> [IosDecodedBarcode] {
        let count = Int(imageBytes.size)
        guard count > 0, count <= 20 * 1024 * 1024 else { return [] }
        var buffer = [UInt8](repeating: 0, count: count)
        for index in 0..<count {
            buffer[index] = UInt8(bitPattern: imageBytes.get(index: Int32(index)))
        }
        let data = Data(buffer)
        return Self.detectBarcodes(in: data)
    }

    static func detectBarcodes(in data: Data) -> [IosDecodedBarcode] {
        let request = VNDetectBarcodesRequest()
        let handler = VNImageRequestHandler(data: data, options: [:])
        do {
            try handler.perform([request])
            let observations = request.results ?? []
            return observations.compactMap { obs in
                guard let payload = obs.payloadStringValue, !payload.isEmpty else { return nil }
                return IosDecodedBarcode(text: payload, formatHint: mapSymbologyHint(obs.symbology))
            }
        } catch {
            return []
        }
    }

    private static func mapSymbologyHint(_ symbology: VNBarcodeSymbology) -> String {
        switch symbology {
        case .qr: return "QR_CODE"
        case .dataMatrix: return "DATA_MATRIX"
        case .aztec: return "AZTEC"
        case .pdf417: return "PDF_417"
        case .code128: return "CODE_128"
        case .code39: return "CODE_39"
        default: return "OTHER"
        }
    }
}

private final class NativeFilePickerDelegate: NSObject, UIDocumentPickerDelegate, IosFilePickerDelegate {
    private var pendingCallback: (([PickedFile]) -> Void)?

    func pickFiles(allowMultiple: Bool, onResult: @escaping ([PickedFile]) -> Void) {
        DispatchQueue.main.async {
            guard let presenter = Self.topViewController() else {
                onResult([])
                return
            }
            self.pendingCallback = onResult
            let picker = UIDocumentPickerViewController(forOpeningContentTypes: [.item, .data, .content], asCopy: true)
            picker.allowsMultipleSelection = allowMultiple
            picker.delegate = self
            presenter.present(picker, animated: true)
        }
    }

    func documentPicker(_ controller: UIDocumentPickerViewController, didPickDocumentsAt urls: [URL]) {
        let callback = pendingCallback
        pendingCallback = nil
        var results: [PickedFile] = []
        for url in urls {
            if let picked = Self.copyScopedUrlToTemp(url: url, prefix: "batch_") {
                results.append(picked)
            }
        }
        callback?(results)
    }

    func documentPickerWasCancelled(_ controller: UIDocumentPickerViewController) {
        pendingCallback?([])
        pendingCallback = nil
    }

    static func copyScopedUrlToTemp(url: URL, prefix: String) -> PickedFile? {
        let needsSecurityScope = url.startAccessingSecurityScopedResource()
        defer {
            if needsSecurityScope {
                url.stopAccessingSecurityScopedResource()
            }
        }
        let maxBytes = Int64(256 * 1024 * 1024)
        let values = try? url.resourceValues(forKeys: [.fileSizeKey, .nameKey])
        if let size = values?.fileSize, Int64(size) > maxBytes {
            return nil
        }
        let safeName = (values?.name ?? url.lastPathComponent)
            .replacingOccurrences(of: "[^a-zA-Z0-9._-]", with: "_", options: .regularExpression)
        let finalName = safeName.isEmpty ? "payload.bin" : String(safeName.prefix(100))
        let dest = FileManager.default.temporaryDirectory.appendingPathComponent("\(prefix)\(UUID().uuidString).tmp")
        do {
            let input = try FileHandle(forReadingFrom: url)
            defer { try? input.close() }
            FileManager.default.createFile(atPath: dest.path, contents: nil)
            let output = try FileHandle(forWritingTo: dest)
            defer { try? output.close() }
            var totalBytes: Int64 = 0
            while true {
                let chunk = try input.read(upToCount: 64 * 1024) ?? Data()
                if chunk.isEmpty { break }
                totalBytes += Int64(chunk.count)
                if totalBytes > maxBytes {
                    try? FileManager.default.removeItem(at: dest)
                    return nil
                }
                try output.write(contentsOf: chunk)
            }
            return PickedFile(path: dest.path, name: finalName, sizeBytes: totalBytes)
        } catch {
            try? FileManager.default.removeItem(at: dest)
            return nil
        }
    }

    static func topViewController() -> UIViewController? {
        let scenes = UIApplication.shared.connectedScenes.compactMap { $0 as? UIWindowScene }
        let root = scenes.flatMap(\.windows).first(where: \.isKeyWindow)?.rootViewController
        var current = root
        while let presented = current?.presentedViewController {
            current = presented
        }
        return current
    }
}

private final class NativeCameraScannerDelegate: NSObject, UIImagePickerControllerDelegate, UINavigationControllerDelegate, IosCameraScannerDelegate {
    private var pendingCallback: (([IosDecodedBarcode]) -> Void)?

    func openNativeBarcodeScanner(onResult: @escaping ([IosDecodedBarcode]) -> Void) {
        DispatchQueue.main.async {
            guard let presenter = NativeFilePickerDelegate.topViewController(),
                  UIImagePickerController.isSourceTypeAvailable(.camera) else {
                onResult([])
                return
            }
            self.pendingCallback = onResult
            let picker = UIImagePickerController()
            picker.sourceType = .camera
            picker.delegate = self
            presenter.present(picker, animated: true)
        }
    }

    func imagePickerController(_ picker: UIImagePickerController, didFinishPickingMediaWithInfo info: [UIImagePickerController.InfoKey: Any]) {
        picker.dismiss(animated: true)
        let callback = pendingCallback
        pendingCallback = nil
        guard let image = info[.originalImage] as? UIImage,
              let data = image.jpegData(compressionQuality: 0.9) else {
            callback?([])
            return
        }
        callback?(NativeVisionDecoderDelegate.detectBarcodes(in: data))
    }

    func imagePickerControllerDidCancel(_ picker: UIImagePickerController) {
        picker.dismiss(animated: true)
        pendingCallback?([])
        pendingCallback = nil
    }
}

@main
struct iOSApp: App {
    private let keychainDelegate = KeychainStorageDelegate()
    private let filePickerDelegate = NativeFilePickerDelegate()
    private let visionDecoderDelegate = NativeVisionDecoderDelegate()
    private let cameraScannerDelegate = NativeCameraScannerDelegate()

    init() {
        IosPlatformStorageBridge.shared.keychainDelegate = keychainDelegate
        IosFilePickerBridge.shared.delegate = filePickerDelegate
        IosVisualDecoderBridge.shared.delegate = visionDecoderDelegate
        IosCameraScannerBridge.shared.delegate = cameraScannerDelegate
        IosSystemBarBridge.shared.onAppearanceChanged = { isDark in
            let style: UIUserInterfaceStyle = isDark.boolValue ? .dark : .light
            DispatchQueue.main.async {
                UIApplication.shared.connectedScenes
                    .compactMap { $0 as? UIWindowScene }
                    .flatMap(\.windows)
                    .forEach { $0.overrideUserInterfaceStyle = style }
            }
        }
        DispatchQueue.global(qos: .utility).async {
            DefaultTemporaryFileCleaner.shared.sweepStaleTempFiles(maxAgeMs: 3_600_000)
        }
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
                .onOpenURL { url in
                    if url.scheme?.lowercased() == "ioclens" {
                        let raw = url.absoluteString.replacingOccurrences(of: "ioclens://", with: "")
                        if !raw.isEmpty {
                            SharePayloadReceiver.shared.dispatch(
                                payload: SharedPayload(
                                    textOrUrl: raw,
                                    filePath: nil,
                                    fileName: nil,
                                    batchFiles: nil,
                                    isTemporary: false,
                                    errorMessage: nil
                                )
                            )
                        }
                    } else if url.isFileURL {
                        DispatchQueue.global(qos: .userInitiated).async {
                            if let copied = NativeFilePickerDelegate.copyScopedUrlToTemp(url: url, prefix: "share_") {
                                SharePayloadReceiver.shared.dispatch(
                                    payload: SharedPayload(
                                        textOrUrl: nil,
                                        filePath: copied.path,
                                        fileName: copied.name,
                                        batchFiles: [copied],
                                        isTemporary: true,
                                        errorMessage: nil
                                    )
                                )
                            }
                        }
                    }
                }
        }
    }
}