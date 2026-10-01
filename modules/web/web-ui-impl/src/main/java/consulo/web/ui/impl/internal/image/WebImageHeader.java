/*
 * Copyright 2013-2026 consulo.io
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package consulo.web.ui.impl.internal.image;

import org.jspecify.annotations.Nullable;

/**
 * Format and size of a raster image the browser shows by itself, read from the first bytes of it - the server never
 * decodes the pixels, it only has to know how big the image is before the browser has it.
 *
 * @author VISTALL
 * @since 2026-10-01
 */
record WebImageHeader(String contentType, int width, int height) {
    private static final byte[] PNG_SIGNATURE = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'};

    /**
     * @return null when the bytes are none of png, jpeg, gif, bmp, ico or webp - the formats every browser shows
     */
    static @Nullable WebImageHeader read(byte[] data) {
        WebImageHeader header = readHeader(data);
        return header == null || header.width() <= 0 || header.height() <= 0 ? null : header;
    }

    private static @Nullable WebImageHeader readHeader(byte[] data) {
        if (startsWith(data, 0, PNG_SIGNATURE) && data.length >= 24) {
            return new WebImageHeader("image/png", int32BE(data, 16), int32BE(data, 20));
        }

        if ((startsWith(data, 0, "GIF87a") || startsWith(data, 0, "GIF89a")) && data.length >= 10) {
            return new WebImageHeader("image/gif", uint16LE(data, 6), uint16LE(data, 8));
        }

        if (startsWith(data, 0, "BM") && data.length >= 26) {
            // the oldest bitmap header keeps the size in 16 bits, every later one in 32 - with a negative height
            // for an image stored top down
            if (int32LE(data, 14) == 12) {
                return new WebImageHeader("image/bmp", uint16LE(data, 18), uint16LE(data, 20));
            }
            return new WebImageHeader("image/bmp", int32LE(data, 18), Math.abs(int32LE(data, 22)));
        }

        if (data.length >= 4 && uint8(data, 0) == 0xFF && uint8(data, 1) == 0xD8) {
            return readJpeg(data);
        }

        if (startsWith(data, 0, "RIFF") && startsWith(data, 8, "WEBP") && data.length >= 30) {
            return readWebp(data);
        }

        if (data.length >= 22 && uint16LE(data, 0) == 0 && uint16LE(data, 2) == 1) {
            return readIco(data);
        }

        return null;
    }

    private static @Nullable WebImageHeader readJpeg(byte[] data) {
        int offset = 2;
        while (offset + 9 < data.length) {
            if (uint8(data, offset) != 0xFF) {
                return null;
            }

            int marker = uint8(data, offset + 1);
            if (marker == 0xFF) {
                // a fill byte in front of the marker
                offset++;
                continue;
            }

            if (marker == 0x01 || (marker >= 0xD0 && marker <= 0xD8)) {
                // markers which stand alone, without a length
                offset += 2;
                continue;
            }

            // a start of frame - every one but the huffman, arithmetic and jpeg-ls tables of the same range
            if (marker >= 0xC0 && marker <= 0xCF && marker != 0xC4 && marker != 0xC8 && marker != 0xCC) {
                return new WebImageHeader("image/jpeg", uint16BE(data, offset + 7), uint16BE(data, offset + 5));
            }

            int length = uint16BE(data, offset + 2);
            if (length < 2) {
                return null;
            }
            offset += 2 + length;
        }
        return null;
    }

    private static @Nullable WebImageHeader readWebp(byte[] data) {
        if (startsWith(data, 12, "VP8 ")) {
            // lossy: the frame header follows the three bytes of the frame tag and the start code
            return new WebImageHeader("image/webp", uint16LE(data, 26) & 0x3FFF, uint16LE(data, 28) & 0x3FFF);
        }

        if (startsWith(data, 12, "VP8L")) {
            // lossless: width and height less one, 14 bits each, right after the signature byte
            int b0 = uint8(data, 21);
            int b1 = uint8(data, 22);
            int b2 = uint8(data, 23);
            int b3 = uint8(data, 24);
            int width = 1 + (((b1 & 0x3F) << 8) | b0);
            int height = 1 + (((b3 & 0x0F) << 10) | (b2 << 2) | ((b1 & 0xC0) >> 6));
            return new WebImageHeader("image/webp", width, height);
        }

        if (startsWith(data, 12, "VP8X")) {
            // extended: the size of the canvas less one, 24 bits each
            return new WebImageHeader("image/webp", 1 + uint24LE(data, 24), 1 + uint24LE(data, 27));
        }

        return null;
    }

    private static @Nullable WebImageHeader readIco(byte[] data) {
        int count = uint16LE(data, 4);
        if (count <= 0 || data.length < 6 + count * 16) {
            return null;
        }

        // the browser shows the biggest of the images an icon holds; a size of zero stands for 256
        int width = 0;
        int height = 0;
        for (int i = 0; i < count; i++) {
            int entry = 6 + i * 16;
            int entryWidth = uint8(data, entry) == 0 ? 256 : uint8(data, entry);
            int entryHeight = uint8(data, entry + 1) == 0 ? 256 : uint8(data, entry + 1);
            if (entryWidth * entryHeight > width * height) {
                width = entryWidth;
                height = entryHeight;
            }
        }
        return new WebImageHeader("image/x-icon", width, height);
    }

    private static boolean startsWith(byte[] data, int offset, String prefix) {
        if (data.length < offset + prefix.length()) {
            return false;
        }
        for (int i = 0; i < prefix.length(); i++) {
            if (data[offset + i] != (byte) prefix.charAt(i)) {
                return false;
            }
        }
        return true;
    }

    private static boolean startsWith(byte[] data, int offset, byte[] prefix) {
        if (data.length < offset + prefix.length) {
            return false;
        }
        for (int i = 0; i < prefix.length; i++) {
            if (data[offset + i] != prefix[i]) {
                return false;
            }
        }
        return true;
    }

    private static int uint8(byte[] data, int offset) {
        return data[offset] & 0xFF;
    }

    private static int uint16LE(byte[] data, int offset) {
        return uint8(data, offset) | (uint8(data, offset + 1) << 8);
    }

    private static int uint16BE(byte[] data, int offset) {
        return (uint8(data, offset) << 8) | uint8(data, offset + 1);
    }

    private static int uint24LE(byte[] data, int offset) {
        return uint16LE(data, offset) | (uint8(data, offset + 2) << 16);
    }

    private static int int32LE(byte[] data, int offset) {
        return uint16LE(data, offset) | (uint16LE(data, offset + 2) << 16);
    }

    private static int int32BE(byte[] data, int offset) {
        return (uint16BE(data, offset) << 16) | uint16BE(data, offset + 2);
    }
}
