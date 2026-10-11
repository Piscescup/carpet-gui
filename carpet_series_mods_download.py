#!/usr/bin/env python3
"""Download compatible Carpet add-ons for every enabled Minecraft version.

The enabled Minecraft versions come from ``settings.json``.  Each selected
Modrinth file is installed into ``versions/<minecraft>/run/mods`` so the
corresponding Gradle run configuration can use it immediately.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import os
import re
import sys
import time
import urllib.error
import urllib.parse
import urllib.request
from dataclasses import dataclass
from pathlib import Path
from typing import Any, Iterable


ROOT = Path(__file__).resolve().parent
MODRINTH_API = "https://api.modrinth.com/v2"
USER_AGENT = "Piscescup/carpet-gui/1.0.0 (https://github.com/Piscescup/carpet-gui)"
MANIFEST_NAME = ".carpet-series-mods.json"

# These projects correspond to CarpetSeriesMods.defaultAdapters().
PROJECTS: dict[str, str] = {
    "carpet-pry-addition": "Carpet PRY Addition",
    "carpet-igny-addition": "Carpet IGNY Addition",
    "carpet-tis-addition": "Carpet TIS Addition",
    "carpet-org-addition": "Carpet Org Addition",
    "carpet-ams-addition": "Carpet AMS Addition",
    "carpet-extra-extras": "Carpet Extra Extras",
    "gca": "Gugle Carpet Addition",
    "carpet-rof-addition": "Carpet ROF Addition",
    "crystalcarpetaddition": "Crystal Carpet Addition",
}


class DownloadError(RuntimeError):
    """An expected configuration, API, or download failure."""


@dataclass(frozen=True)
class Download:
    project: str
    project_name: str
    minecraft_version: str
    version_id: str
    version_number: str
    filename: str
    url: str
    hashes: dict[str, str]
    size: int | None


def parse_args(argv: list[str] | None = None) -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description=(
            "Download Modrinth releases for the Carpet add-ons supported by "
            "Carpet GUI into each enabled version's run/mods directory."
        )
    )
    parser.add_argument(
        "--settings",
        type=Path,
        default=ROOT / "settings.json",
        help="settings.json path (default: repository settings.json)",
    )
    parser.add_argument(
        "--version",
        action="append",
        dest="versions",
        metavar="MINECRAFT_VERSION",
        help="only process this enabled Minecraft version; may be repeated",
    )
    parser.add_argument(
        "--project",
        action="append",
        choices=tuple(PROJECTS),
        dest="projects",
        help="only process this Modrinth project; may be repeated",
    )
    parser.add_argument(
        "--channel",
        choices=("any", "release", "beta", "alpha"),
        default="any",
        help="accepted release channel (default: newest compatible version)",
    )
    parser.add_argument(
        "--dry-run",
        action="store_true",
        help="resolve and print files without creating directories or downloading",
    )
    parser.add_argument(
        "--force",
        action="store_true",
        help="download again even when the existing file passes its hash check",
    )
    parser.add_argument(
        "--timeout",
        type=float,
        default=30.0,
        help="HTTP timeout in seconds (default: 30)",
    )
    args = parser.parse_args(argv)
    if args.timeout <= 0:
        parser.error("--timeout must be greater than zero")
    return args


def read_enabled_versions(settings_path: Path) -> list[str]:
    try:
        data = json.loads(settings_path.read_text(encoding="utf-8"))
    except FileNotFoundError as error:
        raise DownloadError(f"Settings file does not exist: {settings_path}") from error
    except (OSError, json.JSONDecodeError) as error:
        raise DownloadError(f"Cannot read {settings_path}: {error}") from error

    versions = data.get("versions") if isinstance(data, dict) else None
    if not isinstance(versions, list) or not versions:
        raise DownloadError(f'{settings_path} must contain a non-empty "versions" array')
    if any(not isinstance(version, str) or not version.strip() for version in versions):
        raise DownloadError(f"Every version in {settings_path} must be a non-empty string")
    if len(versions) != len(set(versions)):
        raise DownloadError(f"Duplicate Minecraft versions in {settings_path}")
    return versions


def request(url: str, timeout: float, *, attempts: int = 3) -> urllib.response.addinfourl:
    headers = {
        "User-Agent": USER_AGENT,
        "Accept": "application/json, application/octet-stream;q=0.9, */*;q=0.8",
    }
    for attempt in range(1, attempts + 1):
        try:
            return urllib.request.urlopen(
                urllib.request.Request(url, headers=headers), timeout=timeout
            )
        except urllib.error.HTTPError as error:
            retryable = error.code == 429 or 500 <= error.code < 600
            if not retryable or attempt == attempts:
                raise DownloadError(f"HTTP {error.code} for {url}") from error
            retry_after = error.headers.get("Retry-After")
            delay = float(retry_after) if retry_after and retry_after.isdigit() else attempt * 2
            time.sleep(min(delay, 30))
        except urllib.error.URLError as error:
            if attempt == attempts:
                raise DownloadError(f"Cannot reach {url}: {error.reason}") from error
            time.sleep(attempt * 2)
    raise AssertionError("unreachable")


def api_json(path: str, timeout: float) -> Any:
    with request(f"{MODRINTH_API}{path}", timeout) as response:
        try:
            return json.load(response)
        except (UnicodeDecodeError, json.JSONDecodeError) as error:
            raise DownloadError(f"Invalid JSON returned for {path}") from error


def fetch_project_versions(project: str, timeout: float) -> list[dict[str, Any]]:
    query = urllib.parse.urlencode({"loaders": json.dumps(["fabric"])})
    encoded_project = urllib.parse.quote(project, safe="")
    result = api_json(f"/project/{encoded_project}/version?{query}", timeout)
    if not isinstance(result, list):
        raise DownloadError(f"Unexpected version response for {project}")
    return [entry for entry in result if isinstance(entry, dict)]


def release_rank(version_type: str) -> int:
    return {"release": 2, "beta": 1, "alpha": 0}.get(version_type, -1)


def compatible_version(
    versions: Iterable[dict[str, Any]], minecraft_version: str, channel: str
) -> dict[str, Any] | None:
    compatible = [
        version
        for version in versions
        if minecraft_version in version.get("game_versions", [])
        and "fabric" in version.get("loaders", [])
        and (channel == "any" or version.get("version_type") == channel)
    ]
    if not compatible:
        return None
    return max(
        compatible,
        key=lambda version: (
            str(version.get("date_published", "")),
            release_rank(str(version.get("version_type", ""))),
        ),
    )


def select_jar(version: dict[str, Any], minecraft_version: str) -> dict[str, Any]:
    files = [
        file
        for file in version.get("files", [])
        if isinstance(file, dict)
        and isinstance(file.get("filename"), str)
        and file["filename"].lower().endswith(".jar")
        and "sources" not in file["filename"].lower()
    ]
    if not files:
        raise DownloadError(
            f'Modrinth version {version.get("id", "?")} contains no downloadable JAR'
        )
    # A single Modrinth version may contain one JAR per Minecraft version.
    # Prefer a filename containing the exact requested version before falling
    # back to Modrinth's primary-file marker.
    version_pattern = re.compile(
        rf"(?<![0-9]){re.escape(minecraft_version)}(?![0-9])",
        re.IGNORECASE,
    )
    matching = [file for file in files if version_pattern.search(file["filename"])]
    candidates = matching or files
    return next(
        (file for file in candidates if file.get("primary") is True), candidates[0]
    )


def make_download(
    project: str, project_name: str, minecraft_version: str, version: dict[str, Any]
) -> Download:
    file = select_jar(version, minecraft_version)
    filename = Path(file["filename"]).name
    if not filename or filename != file["filename"]:
        raise DownloadError(f"Unsafe filename returned by Modrinth: {file['filename']!r}")
    url = file.get("url")
    if not isinstance(url, str) or not url.startswith("https://"):
        raise DownloadError(f"Invalid download URL returned for {project}")
    hashes = file.get("hashes")
    return Download(
        project=project,
        project_name=project_name,
        minecraft_version=minecraft_version,
        version_id=str(version.get("id", "")),
        version_number=str(version.get("version_number", "")),
        filename=filename,
        url=url,
        hashes={str(key): str(value).lower() for key, value in hashes.items()}
        if isinstance(hashes, dict)
        else {},
        size=file.get("size") if isinstance(file.get("size"), int) else None,
    )


def preferred_hash(hashes: dict[str, str]) -> tuple[str, str] | None:
    for algorithm in ("sha512", "sha1"):
        value = hashes.get(algorithm)
        if value:
            return algorithm, value
    return None


def file_matches(path: Path, download: Download) -> bool:
    if not path.is_file():
        return False
    if download.size is not None and path.stat().st_size != download.size:
        return False
    expected = preferred_hash(download.hashes)
    if expected is None:
        return True
    algorithm, digest = expected
    checksum = hashlib.new(algorithm)
    with path.open("rb") as stream:
        for block in iter(lambda: stream.read(1024 * 1024), b""):
            checksum.update(block)
    return checksum.hexdigest().lower() == digest


def load_manifest(mods_dir: Path) -> dict[str, Any]:
    path = mods_dir / MANIFEST_NAME
    if not path.exists():
        return {"schema": 1, "projects": {}}
    try:
        data = json.loads(path.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError):
        print(f"warning: ignoring invalid manifest {path}", file=sys.stderr)
        return {"schema": 1, "projects": {}}
    projects = data.get("projects") if isinstance(data, dict) else None
    return {"schema": 1, "projects": projects if isinstance(projects, dict) else {}}


def save_manifest(mods_dir: Path, manifest: dict[str, Any]) -> None:
    path = mods_dir / MANIFEST_NAME
    temporary = path.with_name(path.name + ".tmp")
    temporary.write_text(
        json.dumps(manifest, ensure_ascii=False, indent=2) + "\n", encoding="utf-8"
    )
    os.replace(temporary, path)


def download_file(download: Download, destination: Path, timeout: float) -> None:
    temporary = destination.with_name(destination.name + ".part")
    checksum_spec = preferred_hash(download.hashes)
    checksum = hashlib.new(checksum_spec[0]) if checksum_spec else None
    downloaded = 0
    try:
        with request(download.url, timeout) as response, temporary.open("wb") as output:
            while block := response.read(1024 * 1024):
                output.write(block)
                downloaded += len(block)
                if checksum is not None:
                    checksum.update(block)
        if download.size is not None and downloaded != download.size:
            raise DownloadError(
                f"Size mismatch for {download.filename}: expected {download.size}, got {downloaded}"
            )
        if checksum_spec and checksum and checksum.hexdigest().lower() != checksum_spec[1]:
            raise DownloadError(f"Checksum mismatch for {download.filename}")
        os.replace(temporary, destination)
    finally:
        temporary.unlink(missing_ok=True)


def install(download: Download, *, timeout: float, force: bool, dry_run: bool) -> str:
    mods_dir = ROOT / "versions" / download.minecraft_version / "run" / "mods"
    destination = mods_dir / download.filename
    if dry_run:
        return f"would download {download.filename}"

    mods_dir.mkdir(parents=True, exist_ok=True)
    manifest = load_manifest(mods_dir)
    projects: dict[str, Any] = manifest["projects"]
    previous = projects.get(download.project)

    if not force and file_matches(destination, download):
        status = "already current"
    else:
        download_file(download, destination, timeout)
        status = "downloaded"

    projects[download.project] = {
        "name": download.project_name,
        "version_id": download.version_id,
        "version_number": download.version_number,
        "filename": download.filename,
        "hashes": download.hashes,
    }
    save_manifest(mods_dir, manifest)

    if isinstance(previous, dict):
        old_filename = previous.get("filename")
        if (
            isinstance(old_filename, str)
            and old_filename != download.filename
            and Path(old_filename).name == old_filename
        ):
            (mods_dir / old_filename).unlink(missing_ok=True)
    return status


def selected_values(
    requested: list[str] | None, available: Iterable[str], label: str
) -> list[str]:
    available_list = list(available)
    if not requested:
        return available_list
    unknown = sorted(set(requested) - set(available_list))
    if unknown:
        raise DownloadError(f"Unknown {label}: {', '.join(unknown)}")
    requested_set = set(requested)
    return [value for value in available_list if value in requested_set]


def run(args: argparse.Namespace) -> int:
    enabled_versions = read_enabled_versions(args.settings.resolve())
    minecraft_versions = selected_values(args.versions, enabled_versions, "enabled version")
    projects = selected_values(args.projects, PROJECTS, "project")

    for minecraft_version in minecraft_versions:
        version_dir = ROOT / "versions" / minecraft_version
        if not version_dir.is_dir():
            raise DownloadError(
                f"Enabled version directory does not exist: {version_dir}"
            )

    print(
        f"Resolving {len(projects)} Modrinth projects for "
        f"{len(minecraft_versions)} Minecraft versions..."
    )
    project_versions: dict[str, list[dict[str, Any]]] = {}
    failures = 0
    for project in projects:
        try:
            project_versions[project] = fetch_project_versions(project, args.timeout)
        except DownloadError as error:
            failures += 1
            print(f"error: {PROJECTS[project]}: {error}", file=sys.stderr)

    installed = skipped = unavailable = 0
    for minecraft_version in minecraft_versions:
        print(f"\nMinecraft {minecraft_version}")
        for project in projects:
            versions = project_versions.get(project)
            if versions is None:
                continue
            version = compatible_version(versions, minecraft_version, args.channel)
            if version is None:
                unavailable += 1
                print(f"  - {PROJECTS[project]}: no compatible Fabric version")
                continue
            try:
                selected = make_download(
                    project, PROJECTS[project], minecraft_version, version
                )
                status = install(
                    selected,
                    timeout=args.timeout,
                    force=args.force,
                    dry_run=args.dry_run,
                )
                if status == "already current":
                    skipped += 1
                else:
                    installed += 1
                print(
                    f"  - {selected.project_name}: {selected.version_number} "
                    f"({status})"
                )
            except (DownloadError, OSError) as error:
                failures += 1
                print(f"  ! {PROJECTS[project]}: {error}", file=sys.stderr)

    verb = "resolved" if args.dry_run else "downloaded"
    print(
        f"\nDone: {installed} {verb}, {skipped} already current, "
        f"{unavailable} unavailable, {failures} failed."
    )
    return 1 if failures else 0


def main(argv: list[str] | None = None) -> int:
    try:
        return run(parse_args(argv))
    except DownloadError as error:
        print(f"error: {error}", file=sys.stderr)
        return 2
    except KeyboardInterrupt:
        print("\nInterrupted.", file=sys.stderr)
        return 130


if __name__ == "__main__":
    raise SystemExit(main())
