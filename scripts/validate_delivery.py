"""Cross-platform static validation for the KinVoice handoff."""

from __future__ import annotations

import json
import plistlib
import struct
import xml.etree.ElementTree as ET
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
IOS = ROOT / "ios"
BACKEND = ROOT / "backend"


def require(condition: bool, message: str) -> None:
    if not condition:
        raise SystemExit(f"FAILED: {message}")


def validate_ios() -> None:
    project = (IOS / "KinVoice.xcodeproj" / "project.pbxproj").read_text(encoding="utf-8")
    scheme_path = IOS / "KinVoice.xcodeproj" / "xcshareddata" / "xcschemes" / "KinVoice.xcscheme"
    require(scheme_path.exists(), "shared KinVoice scheme is missing")
    scheme = ET.parse(scheme_path).getroot()
    require(scheme.tag == "Scheme", "shared scheme XML is invalid")

    swift_files = sorted(IOS.glob("*.swift"))
    require(swift_files, "no Swift sources found")
    for source in swift_files:
        require(f"/* {source.name} in Sources */" in project, f"{source.name} is not in the Xcode Sources phase")
    root_view = (IOS / "RootTabView.swift").read_text(encoding="utf-8")
    require("#Preview" in root_view, "RootTabView.swift is missing the SwiftUI Canvas preview")
    for resource in ("Assets.xcassets", "PrivacyInfo.xcprivacy"):
        require(resource in project, f"{resource} is not referenced by the Xcode project")
    require("IPHONEOS_DEPLOYMENT_TARGET = 17.0" in project, "deployment target must be iOS 17.0")
    require("XCRemoteSwiftPackageReference" not in project, "unexpected remote Swift package dependency")
    require(
        '"CODE_SIGNING_ALLOWED[sdk=iphonesimulator*]" = NO' in project,
        "simulator builds must not require Apple Developer signing",
    )

    with (IOS / "Info.plist").open("rb") as stream:
        info = plistlib.load(stream)
    for key in (
        "APIBaseURL",
        "CFBundleIdentifier",
        "CFBundleExecutable",
        "CFBundlePackageType",
        "CFBundleShortVersionString",
        "CFBundleVersion",
        "NSMicrophoneUsageDescription",
        "NSSpeechRecognitionUsageDescription",
    ):
        require(bool(info.get(key)), f"Info.plist is missing {key}")
    require(
        info["CFBundleIdentifier"] == "$(PRODUCT_BUNDLE_IDENTIFIER)",
        "CFBundleIdentifier must inherit PRODUCT_BUNDLE_IDENTIFIER",
    )
    require("NSAppTransportSecurity" not in info, "do not add an App Transport Security exception")

    with (IOS / "PrivacyInfo.xcprivacy").open("rb") as stream:
        privacy = plistlib.load(stream)
    require(privacy.get("NSPrivacyTracking") is False, "privacy manifest must disable tracking")

    icon_manifest = json.loads((IOS / "Assets.xcassets" / "AppIcon.appiconset" / "Contents.json").read_text(encoding="utf-8"))
    icon_name = icon_manifest["images"][0]["filename"]
    icon_path = IOS / "Assets.xcassets" / "AppIcon.appiconset" / icon_name
    data = icon_path.read_bytes()
    require(data[:8] == b"\x89PNG\r\n\x1a\n", "App Icon is not a PNG")
    width, height, bit_depth, color_type = struct.unpack(">IIBB", data[16:26])
    require((width, height) == (1024, 1024), "App Icon must be 1024x1024")
    require(color_type not in {4, 6}, "App Icon must not contain an alpha channel")

    print(f"OK: shared scheme references {len(swift_files)} Swift files and required resources")
    print("OK: iOS 17 target, account-free simulator signing configuration")
    print("OK: no remote packages or ATS exception")
    print("OK: Info.plist, privacy manifest and opaque 1024px App Icon")
    if info["APIBaseURL"] == "https://api.example.com":
        print("ACTION REQUIRED: replace APIBaseURL before a TestFlight archive")


def validate_backend() -> None:
    main = (BACKEND / "app" / "main.py").read_text(encoding="utf-8")
    requirements = (BACKEND / "requirements.txt").read_text(encoding="utf-8").lower()
    for forbidden in ("replica_router", "tts_router", "chat_router", "app.models"):
        require(forbidden not in main, f"focused backend still imports {forbidden}")
    for forbidden in ("websocket-client", "pydub", "sqlalchemy", "aiosqlite"):
        require(forbidden not in requirements, f"requirements still contain {forbidden}")
    require((BACKEND / ".env.example").exists(), "backend .env.example is missing")
    print("OK: focused backend has no vivo or database runtime dependencies")


if __name__ == "__main__":
    validate_ios()
    validate_backend()
    print("STATIC DELIVERY VALIDATION PASSED")
