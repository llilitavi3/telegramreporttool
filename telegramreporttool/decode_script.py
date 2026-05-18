import zlib, base64, re

def decode_and_print(file_path):
    with open(file_path, 'r', encoding='utf-8') as f:
        content = f.read()

    match = re.search(r'base64\.b64decode\("([^"]+)"\)', content)
    if not match:
        print("Could not find base64 string")
        return

    encoded_data = match.group(1)
    print(f"Encoded data length: {len(encoded_data)}")

    try:
        # Try cleaning the string (remove whitespace/newlines if any)
        encoded_data = "".join(encoded_data.split())

        missing_padding = len(encoded_data) % 4
        if missing_padding:
            encoded_data += '=' * (4 - missing_padding)

        decoded_bytes = base64.b64decode(encoded_data)
        decompressed = zlib.decompress(decoded_bytes)
        print(decompressed.decode('utf-8'))
    except Exception as e:
        print(f"Error: {e}")

decode_and_print("C:/Users/CyberAvt/AndroidStudioProjects/telegramreporttool/aaaremm/ultrareporter_enc.py")
