"""Generate minimal APK fixtures containing a hand-built binary AndroidManifest.xml (AXML).

The AXML layout is written from the AOSP ResourceTypes.h specification, independently of the
Kotlin parser under test, so the tests do not validate the parser against itself.
"""

import os
import struct
import zipfile

OUT_DIR = os.path.dirname(os.path.abspath(__file__))

RES_XML_TYPE = 0x0003
RES_STRING_POOL_TYPE = 0x0001
RES_XML_RESOURCE_MAP_TYPE = 0x0180
RES_XML_START_ELEMENT_TYPE = 0x0102
RES_XML_END_ELEMENT_TYPE = 0x0103

UTF8_FLAG = 0x0100

TYPE_REFERENCE = 0x01
TYPE_STRING = 0x03
TYPE_INT_DEC = 0x10

RES_ID_VERSION_CODE = 0x0101021B
RES_ID_VERSION_NAME = 0x0101021C


def encode_utf16_string(value):
    data = value.encode("utf-16-le")
    length = len(data) // 2
    assert length < 0x8000
    return struct.pack("<H", length) + data + b"\x00\x00"


def encode_length_utf8(length):
    if length < 0x80:
        return struct.pack("<B", length)
    return struct.pack("<BB", 0x80 | (length >> 8), length & 0xFF)


def encode_utf8_string(value):
    data = value.encode("utf-8")
    char_count = len(value.encode("utf-16-le")) // 2
    return encode_length_utf8(char_count) + encode_length_utf8(len(data)) + data + b"\x00"


def build_string_pool(strings, utf8):
    encode = encode_utf8_string if utf8 else encode_utf16_string
    offsets = []
    blob = b""
    for value in strings:
        offsets.append(len(blob))
        blob += encode(value)
    while len(blob) % 4 != 0:
        blob += b"\x00"

    header_size = 28
    strings_start = header_size + 4 * len(strings)
    size = strings_start + len(blob)
    flags = UTF8_FLAG if utf8 else 0

    chunk = struct.pack(
        "<HHIIIIII",
        RES_STRING_POOL_TYPE,
        header_size,
        size,
        len(strings),
        0,
        flags,
        strings_start,
        0,
    )
    chunk += b"".join(struct.pack("<I", offset) for offset in offsets)
    chunk += blob
    return chunk


def build_resource_map(resource_ids):
    size = 8 + 4 * len(resource_ids)
    chunk = struct.pack("<HHI", RES_XML_RESOURCE_MAP_TYPE, 8, size)
    chunk += b"".join(struct.pack("<I", value) for value in resource_ids)
    return chunk


def build_attribute(ns_index, name_index, raw_value_index, data_type, data):
    return struct.pack(
        "<iiiHBBi",
        ns_index,
        name_index,
        raw_value_index,
        8,
        0,
        data_type,
        data,
    )


def build_start_element(name_index, attributes, ns_index=-1):
    body = struct.pack(
        "<iiHHHHHH",
        ns_index,
        name_index,
        20,
        20,
        len(attributes),
        0,
        0,
        0,
    )
    body += b"".join(attributes)
    header = struct.pack("<HHIii", RES_XML_START_ELEMENT_TYPE, 16, 16 + len(body), 1, -1)
    return header + body


def build_end_element(name_index, ns_index=-1):
    body = struct.pack("<ii", ns_index, name_index)
    header = struct.pack("<HHIii", RES_XML_END_ELEMENT_TYPE, 16, 16 + len(body), 1, -1)
    return header + body


def assemble(pool_chunk, resource_ids, element_chunks):
    body = pool_chunk + build_resource_map(resource_ids) + b"".join(element_chunks)
    header = struct.pack("<HHI", RES_XML_TYPE, 8, 8 + len(body))
    return header + body


def manifest_axml(version_name, version_code, utf8=False, version_name_as_reference=False,
                  omit_version_code=False, version_code_as_string=False, element_name="manifest"):
    strings = ["versionCode", "versionName", element_name]
    resource_ids = [RES_ID_VERSION_CODE, RES_ID_VERSION_NAME]

    version_name_index = len(strings)
    strings.append(version_name)

    version_code_index = None
    if version_code_as_string:
        version_code_index = len(strings)
        strings.append(str(version_code))

    element_index = 2

    attributes = []
    if not omit_version_code:
        if version_code_as_string:
            attributes.append(build_attribute(-1, 0, version_code_index, TYPE_STRING, version_code_index))
        else:
            attributes.append(build_attribute(-1, 0, -1, TYPE_INT_DEC, version_code))

    if version_name_as_reference:
        attributes.append(build_attribute(-1, 1, -1, TYPE_REFERENCE, 0x7F040001))
    else:
        attributes.append(build_attribute(-1, 1, version_name_index, TYPE_STRING, version_name_index))

    pool = build_string_pool(strings, utf8=utf8)
    elements = [
        build_start_element(element_index, attributes),
        build_end_element(element_index),
    ]
    return assemble(pool, resource_ids, elements)


def write_apk(name, entries):
    os.makedirs(OUT_DIR, exist_ok=True)
    path = os.path.join(OUT_DIR, name)
    with zipfile.ZipFile(path, "w", zipfile.ZIP_DEFLATED) as archive:
        for entry_name, content in entries:
            archive.writestr(entry_name, content)
    return path


def main():
    write_apk("valid-utf16.apk", [
        ("res/layout/main.xml", b"dummy"),
        ("AndroidManifest.xml", manifest_axml("1.2.3", 10203)),
        ("classes.dex", b"dummy"),
    ])

    write_apk("valid-utf8.apk", [
        ("AndroidManifest.xml", manifest_axml("2.0.0-beta", 20000, utf8=True)),
    ])

    write_apk("valid-utf8-multibyte.apk", [
        ("AndroidManifest.xml", manifest_axml("1.0.0-テスト", 100, utf8=True)),
    ])

    write_apk("version-code-as-string.apk", [
        ("AndroidManifest.xml", manifest_axml("3.1.4", 30104, version_code_as_string=True)),
    ])

    write_apk("version-name-reference.apk", [
        ("AndroidManifest.xml", manifest_axml("ignored", 1, version_name_as_reference=True)),
    ])

    write_apk("missing-version-code.apk", [
        ("AndroidManifest.xml", manifest_axml("1.0.0", 1, omit_version_code=True)),
    ])

    write_apk("no-manifest-element.apk", [
        ("AndroidManifest.xml", manifest_axml("1.0.0", 1, element_name="application")),
    ])

    write_apk("path-traversal-version.apk", [
        ("AndroidManifest.xml", manifest_axml("../../../etc/evil", 40001)),
    ])

    write_apk("only-dots-version.apk", [
        ("AndroidManifest.xml", manifest_axml("../..", 40002)),
    ])

    write_apk("no-manifest.apk", [
        ("classes.dex", b"dummy"),
        ("resources.arsc", b"dummy"),
    ])

    os.makedirs(OUT_DIR, exist_ok=True)
    with open(os.path.join(OUT_DIR, "not-a-zip.apk"), "wb") as handle:
        handle.write(b"this file is definitely not a zip archive" * 4)

    write_apk("broken-manifest.apk", [
        ("AndroidManifest.xml", b"\x03\x00\x08\x00\xff\xff\xff\x7f" + b"\x00" * 8),
    ])

    for name in sorted(os.listdir(OUT_DIR)):
        print(name, os.path.getsize(os.path.join(OUT_DIR, name)))


if __name__ == "__main__":
    main()
