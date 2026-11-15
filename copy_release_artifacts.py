import os
import shutil
import zipfile
import hashlib

def sha256_file(filepath):
    h = hashlib.sha256()
    with open(filepath, 'rb') as f:
        while chunk := f.read(65536):
            h.update(chunk)
    return h.hexdigest()

def zip_dir(src_dir, zip_filepath):
    with zipfile.ZipFile(zip_filepath, 'w', zipfile.ZIP_DEFLATED) as zipf:
        for root, _, files in os.walk(src_dir):
            for file in files:
                full_path = os.path.join(root, file)
                rel_path = os.path.relpath(full_path, src_dir)
                zipf.write(full_path, rel_path)

def main():
    root = r"D:\0\000\amir-hossein-ghafari\projects\ioc-lens"
    rel_dir = os.path.join(root, "release")
    win_dir = os.path.join(rel_dir, "windows")
    desk_dir = os.path.join(rel_dir, "desktop")
    mob_dir = os.path.join(rel_dir, "mobile")

    os.makedirs(win_dir, exist_ok=True)
    os.makedirs(desk_dir, exist_ok=True)
    os.makedirs(mob_dir, exist_ok=True)

    # 1. Windows EXE
    src_exe = os.path.join(root, r"desktopApp\build\compose\binaries\main-release\exe\io.github.mrcoder20.ioclens-1.0.0.exe")
    if os.path.exists(src_exe):
        shutil.copy2(src_exe, os.path.join(win_dir, "IOCLens-1.0.0-Setup.exe"))
        shutil.copy2(src_exe, os.path.join(win_dir, "ioclens-1.0.0-setup.exe"))
        print(f"Copied EXE: {os.path.getsize(src_exe):,} bytes")

    # 2. Windows MSI
    src_msi = os.path.join(root, r"desktopApp\build\compose\binaries\main-release\msi\io.github.mrcoder20.ioclens-1.0.0.msi")
    if os.path.exists(src_msi):
        shutil.copy2(src_msi, os.path.join(win_dir, "IOCLens-1.0.0-Installer.msi"))
        shutil.copy2(src_msi, os.path.join(win_dir, "ioclens-1.0.0-installer.msi"))
        print(f"Copied MSI: {os.path.getsize(src_msi):,} bytes")

    # 3. Windows Portable ZIP
    src_app = os.path.join(root, r"desktopApp\build\compose\binaries\main-release\app")
    if os.path.exists(src_app):
        zip_temp = os.path.join(win_dir, "temp_portable.zip")
        zip_out = os.path.join(win_dir, "IOCLens-1.0.0-Portable-Windows-x64.zip")
        print("Zipping portable distributable...")
        if os.path.exists(zip_temp):
            os.remove(zip_temp)
        zip_dir(src_app, zip_temp)
        shutil.move(zip_temp, zip_out)
        shutil.copy2(zip_out, os.path.join(win_dir, "ioclens-1.0.0-portable.zip"))
        print(f"Created Portable ZIP: {os.path.getsize(zip_out):,} bytes")

    # 4. Desktop Universal JAR
    src_jar = os.path.join(root, r"desktopApp\build\compose\jars\io.github.mrcoder20.ioclens-windows-x64-1.0.0-release.jar")
    if os.path.exists(src_jar):
        shutil.copy2(src_jar, os.path.join(desk_dir, "IOCLens-1.0.0-Universal-Desktop.jar"))
        shutil.copy2(src_jar, os.path.join(desk_dir, "ioclens-v1.0.0-universal.jar"))
        print(f"Copied JAR: {os.path.getsize(src_jar):,} bytes")

    # 5. Android APK
    src_apk = os.path.join(root, r"androidApp\build\outputs\apk\release\androidApp-release.apk")
    if os.path.exists(src_apk):
        shutil.copy2(src_apk, os.path.join(mob_dir, "IOCLens-1.0.0-Release.apk"))
        shutil.copy2(src_apk, os.path.join(mob_dir, "ioclens-v1.0-release.apk"))
        print(f"Copied APK: {os.path.getsize(src_apk):,} bytes")

    # 6. Android AAB
    src_aab = os.path.join(root, r"androidApp\build\outputs\bundle\release\androidApp-release.aab")
    if os.path.exists(src_aab):
        shutil.copy2(src_aab, os.path.join(mob_dir, "IOCLens-1.0.0-Release.aab"))
        shutil.copy2(src_aab, os.path.join(mob_dir, "ioclens-v1.0-release.aab"))
        print(f"Copied AAB: {os.path.getsize(src_aab):,} bytes")

    # 7. Compute SHA-256 Checksums
    checksums = []
    for d in [win_dir, desk_dir, mob_dir]:
        for fname in sorted(os.listdir(d)):
            fpath = os.path.join(d, fname)
            if os.path.isfile(fpath):
                sha = sha256_file(fpath)
                size_mb = os.path.getsize(fpath) / (1024 * 1024)
                rel = os.path.relpath(fpath, root).replace("\\", "/")
                checksums.append(f"{sha}  {rel}  ({size_mb:.2f} MB)")

    chk_path = os.path.join(rel_dir, "CHECKSUMS.txt")
    with open(chk_path, "w", encoding="utf-8") as f:
        f.write("\n".join(checksums) + "\n")
    print(f"Wrote CHECKSUMS.txt ({len(checksums)} entries)")

if __name__ == "__main__":
    main()
