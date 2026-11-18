"""Stage multi-platform IOC Lens build outputs into a clean release directory."""

from __future__ import annotations

import argparse
import hashlib
import shutil
import sys
import zipfile
from collections.abc import Sequence
from dataclasses import dataclass
from pathlib import Path


@dataclass(frozen=True)
class ArtifactRule:
    """Mapping from a Gradle build output glob to a normalized release filename."""

    category: str
    search_root: Path
    glob_pattern: str
    target_filename: str


def sha256_file(filepath: Path) -> str:
    """Return the lowercase hexadecimal SHA-256 digest of a regular file."""
    digest = hashlib.sha256()
    with filepath.open("rb") as handle:
        while chunk := handle.read(65536):
            digest.update(chunk)
    return digest.hexdigest()


def zip_directory(src_dir: Path, zip_filepath: Path) -> Path:
    """Compress all non-symlink files inside src_dir into zip_filepath cleanly."""
    resolved_src = src_dir.resolve()
    tmp_zip = zip_filepath.with_suffix(".zip.tmp")
    if tmp_zip.exists():
        tmp_zip.unlink()

    with zipfile.ZipFile(tmp_zip, "w", zipfile.ZIP_DEFLATED) as archive:
        for candidate in sorted(resolved_src.rglob("*")):
            if candidate.is_symlink() or not candidate.is_file():
                continue
            resolved_candidate = candidate.resolve()
            if not resolved_candidate.is_relative_to(resolved_src):
                continue
            archive.write(
                resolved_candidate, resolved_candidate.relative_to(resolved_src)
            )

    shutil.move(str(tmp_zip), str(zip_filepath))
    return zip_filepath


def resolve_safe_target_path(
    base_dir: Path, relative_subdir: str, filename: str
) -> Path:
    """Resolve and validate that destination path cannot escape base_dir."""
    resolved_base = base_dir.resolve()
    dest = (resolved_base / relative_subdir / filename).resolve()
    if not dest.is_relative_to(resolved_base):
        raise ValueError(
            f"Refusing unsafe output path outside target directory: {dest}"
        )
    return dest


def discover_first_artifact(search_root: Path, glob_pattern: str) -> Path | None:
    """Return the first regular non-symlink artifact matching glob_pattern."""
    if not search_root.exists() or not search_root.is_dir() or search_root.is_symlink():
        return None
    resolved_root = search_root.resolve()
    for candidate in sorted(resolved_root.rglob(glob_pattern)):
        if candidate.is_symlink() or not candidate.is_file():
            continue
        if candidate.resolve().is_relative_to(resolved_root):
            return candidate
    return None


def build_artifact_rules(project_root: Path, version: str) -> tuple[ArtifactRule, ...]:
    """Return ordered discovery-to-destination staging rules for IOC Lens binaries."""
    desktop_binaries = project_root / "desktopApp" / "build" / "compose" / "binaries"
    desktop_jars = project_root / "desktopApp" / "build" / "compose" / "jars"
    android_outputs = project_root / "androidApp" / "build" / "outputs"
    return (
        ArtifactRule(
            "windows",
            desktop_binaries,
            "*.exe",
            f"IOC-Lens-v{version}-windows-x64-setup.exe",
        ),
        ArtifactRule(
            "windows",
            desktop_binaries,
            "*.msi",
            f"IOC-Lens-v{version}-windows-x64.msi",
        ),
        ArtifactRule(
            "linux",
            desktop_binaries,
            "*.deb",
            f"IOC-Lens-v{version}-linux-amd64.deb",
        ),
        ArtifactRule(
            "macos",
            desktop_binaries,
            "*.dmg",
            f"IOC-Lens-v{version}-macos-arm64.dmg",
        ),
        ArtifactRule(
            "desktop",
            desktop_jars,
            "*.jar",
            f"IOC-Lens-v{version}-desktop-universal.jar",
        ),
        ArtifactRule(
            "mobile",
            android_outputs / "apk",
            "*.apk",
            f"IOC-Lens-v{version}-android.apk",
        ),
        ArtifactRule(
            "mobile",
            android_outputs / "bundle",
            "*.aab",
            f"IOC-Lens-v{version}-android.aab",
        ),
    )


def _stage_portable_zip(
    project_root: Path,
    output_dir: Path,
    version: str,
) -> Path | None:
    """Create and stage the Windows portable app ZIP archive when present."""
    portable_app_dir = (
        project_root
        / "desktopApp"
        / "build"
        / "compose"
        / "binaries"
        / "main-release"
        / "app"
    )
    if not portable_app_dir.is_dir() or portable_app_dir.is_symlink():
        return None
    zip_dest = resolve_safe_target_path(
        output_dir,
        "windows",
        f"IOC-Lens-v{version}-windows-x64-portable.zip",
    )
    zip_dest.parent.mkdir(parents=True, exist_ok=True)
    return zip_directory(portable_app_dir, zip_dest)


def collect_release_artifacts(
    project_root: Path,
    output_dir: Path,
    version: str = "1.0.0",
    clean: bool = False,
) -> list[Path]:
    """Copy discovered release artifacts and write GNU-compatible SHA256SUMS.txt."""
    resolved_root = project_root.resolve()
    resolved_out = output_dir.resolve()

    if clean and resolved_out.exists() and not resolved_out.is_symlink():
        shutil.rmtree(resolved_out)
    resolved_out.mkdir(parents=True, exist_ok=True)

    staged: list[Path] = []
    for rule in build_artifact_rules(resolved_root, version):
        src = discover_first_artifact(rule.search_root, rule.glob_pattern)
        if src is None:
            continue
        dest = resolve_safe_target_path(
            resolved_out, rule.category, rule.target_filename
        )
        dest.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(src, dest)
        staged.append(dest)

    portable_zip = _stage_portable_zip(resolved_root, resolved_out, version)
    if portable_zip is not None:
        staged.append(portable_zip)

    write_sha256sums(resolved_out, staged)
    return staged


def write_sha256sums(output_dir: Path, files: Sequence[Path]) -> Path:
    """Write standard sha256sum-verifiable checksum manifest in output_dir."""
    resolved_out = output_dir.resolve()
    checksum_path = resolved_out / "SHA256SUMS.txt"
    lines: list[str] = []
    for artifact in sorted(files, key=lambda path: path.as_posix()):
        if not artifact.is_file():
            continue
        digest = sha256_file(artifact)
        rel_posix = artifact.resolve().relative_to(resolved_out).as_posix()
        lines.append(f"{digest}  {rel_posix}")
    checksum_path.write_text(
        "\n".join(lines) + ("\n" if lines else ""), encoding="utf-8"
    )
    return checksum_path


def parse_args(argv: Sequence[str] | None = None) -> argparse.Namespace:
    """Parse CLI arguments for artifact collection."""
    default_root = Path(__file__).resolve().parent
    parser = argparse.ArgumentParser(
        description="Stage IOC Lens desktop and mobile release artifacts with SHA-256 checksums."
    )
    parser.add_argument(
        "--project-root",
        type=Path,
        default=default_root,
        help="Path to IOC Lens repository root.",
    )
    parser.add_argument(
        "--output-dir",
        type=Path,
        default=None,
        help="Destination directory for staged artifacts (default: <root>/dist).",
    )
    parser.add_argument(
        "--version",
        type=str,
        default="1.0.0",
        help="Semantic version tag used in staged filenames.",
    )
    parser.add_argument(
        "--clean",
        action="store_true",
        help="Remove existing output directory contents before copying.",
    )
    return parser.parse_args(list(argv) if argv is not None else None)


def main(argv: Sequence[str] | None = None) -> int:
    """Run release artifact staging and report checksum count."""
    args = parse_args(argv)
    output_dir = (
        args.output_dir if args.output_dir is not None else args.project_root / "dist"
    )
    staged = collect_release_artifacts(
        project_root=args.project_root,
        output_dir=output_dir,
        version=args.version,
        clean=args.clean,
    )
    print(f"Staged {len(staged)} release artifact(s) into {output_dir.resolve()}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
