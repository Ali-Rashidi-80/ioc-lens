"""Unit tests for release artifact collection and checksum generation."""

from __future__ import annotations

import hashlib
import tempfile
import unittest
import zipfile
from pathlib import Path

from copy_release_artifacts import (
    collect_release_artifacts,
    discover_first_artifact,
    main,
    resolve_safe_target_path,
    sha256_file,
)


def _create_file(path: Path, content: bytes) -> Path:
    """Create parent directories and write deterministic test bytes."""
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(content)
    return path


def _seed_sample_build_tree(root: Path) -> dict[str, bytes]:
    """Populate realistic Gradle build outputs under root and return payload map."""
    samples = {
        "desktopApp/build/compose/binaries/main-release/exe/app.exe": b"exe-binary-payload",
        "desktopApp/build/compose/binaries/main-release/msi/app.msi": b"msi-binary-payload",
        "desktopApp/build/compose/binaries/main-release/deb/app.deb": b"deb-binary-payload",
        "desktopApp/build/compose/jars/universal.jar": b"jar-archive-payload",
        "androidApp/build/outputs/apk/release/app-release.apk": b"apk-archive-payload",
        "desktopApp/build/compose/binaries/main-release/app/IOCLens.cfg": b"app.main=MainKt\n",
    }
    for rel_path, content in samples.items():
        _create_file(root / rel_path, content)
    return samples


class CopyReleaseArtifactsTests(unittest.TestCase):
    """Verify multi-platform release staging, safety checks, and SHA256SUMS output."""

    def _assert_valid_checksums(
        self, out_dir: Path, expected_rel_paths: set[str]
    ) -> None:
        """Verify SHA256SUMS.txt formatting and cryptographic hashes for staged files."""
        lines = [
            line
            for line in (out_dir / "SHA256SUMS.txt")
            .read_text(encoding="utf-8")
            .splitlines()
            if line
        ]
        self.assertEqual(len(lines), len(expected_rel_paths))
        for line in lines:
            digest, rel_posix = line.split("  ", maxsplit=1)
            self.assertEqual(len(digest), 64)
            self.assertEqual(digest, sha256_file(out_dir / rel_posix))

    def test_collect_release_artifacts_and_checksums(self) -> None:
        """All configured build artifacts stage uniquely with matching SHA-256 digests."""
        with tempfile.TemporaryDirectory() as tmp_dir:
            root = Path(tmp_dir)
            out_dir = root / "dist"
            samples = _seed_sample_build_tree(root)

            staged = collect_release_artifacts(
                project_root=root,
                output_dir=out_dir,
                version="2.1.0",
                clean=True,
            )

            expected_map = {
                "windows/IOC-Lens-v2.1.0-windows-x64-setup.exe": samples[
                    "desktopApp/build/compose/binaries/main-release/exe/app.exe"
                ],
                "windows/IOC-Lens-v2.1.0-windows-x64.msi": samples[
                    "desktopApp/build/compose/binaries/main-release/msi/app.msi"
                ],
                "linux/IOC-Lens-v2.1.0-linux-amd64.deb": samples[
                    "desktopApp/build/compose/binaries/main-release/deb/app.deb"
                ],
                "desktop/IOC-Lens-v2.1.0-desktop-universal.jar": samples[
                    "desktopApp/build/compose/jars/universal.jar"
                ],
                "mobile/IOC-Lens-v2.1.0-android.apk": samples[
                    "androidApp/build/outputs/apk/release/app-release.apk"
                ],
            }
            expected_rel_paths = set(expected_map) | {
                "windows/IOC-Lens-v2.1.0-windows-x64-portable.zip"
            }
            self.assertEqual(
                expected_rel_paths,
                {p.resolve().relative_to(out_dir.resolve()).as_posix() for p in staged},
            )

            for rel_path, raw_bytes in expected_map.items():
                staged_file = out_dir / rel_path
                self.assertEqual(staged_file.read_bytes(), raw_bytes)
                self.assertEqual(
                    sha256_file(staged_file), hashlib.sha256(raw_bytes).hexdigest()
                )

            with zipfile.ZipFile(
                out_dir / "windows/IOC-Lens-v2.1.0-windows-x64-portable.zip", "r"
            ) as zf:
                self.assertIn("IOCLens.cfg", zf.namelist())

            self._assert_valid_checksums(out_dir, expected_rel_paths)

    def test_clean_removes_stale_files_and_empty_repo_succeeds(self) -> None:
        """Running main() with --clean wipes stale artifacts on an empty build tree."""
        with tempfile.TemporaryDirectory() as tmp_dir:
            root = Path(tmp_dir)
            out_dir = root / "dist"
            stale = _create_file(out_dir / "old/stale.txt", b"stale")
            self.assertTrue(stale.exists())

            rc = main(
                ["--project-root", str(root), "--output-dir", str(out_dir), "--clean"]
            )
            self.assertEqual(0, rc)
            self.assertFalse(stale.exists())
            sums = out_dir / "SHA256SUMS.txt"
            self.assertTrue(sums.is_file())
            self.assertEqual("", sums.read_text(encoding="utf-8"))

    def test_discover_first_artifact_missing_dir_and_path_traversal_guard(self) -> None:
        """Missing directories return None and escaping destination paths raises ValueError."""
        with tempfile.TemporaryDirectory() as tmp_dir:
            root = Path(tmp_dir)
            self.assertIsNone(discover_first_artifact(root / "does_not_exist", "*.apk"))
            with self.assertRaises(ValueError):
                resolve_safe_target_path(root / "dist", "../../escape", "payload.bin")


if __name__ == "__main__":
    unittest.main()
