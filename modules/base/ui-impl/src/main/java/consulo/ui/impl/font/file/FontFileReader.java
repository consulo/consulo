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
package consulo.ui.impl.font.file;

import java.io.IOException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

/**
 * @author VISTALL
 * @since 2026-09-30
 */
final class FontFileReader {
    private static final int SFNT_TRUETYPE = 0x00010000;
    private static final int TAG_TRUE = tag("true");
    private static final int TAG_OTTO = tag("OTTO");
    private static final int TAG_TTCF = tag("ttcf");
    private static final int TAG_WOFF = tag("wOFF");
    private static final int TAG_WOFF2 = tag("wOF2");
    private static final int TAG_NAME = tag("name");
    private static final int TAG_CMAP = tag("cmap");
    private static final Set<Integer> REQUIRED_TABLES = Set.of(TAG_NAME, TAG_CMAP);
    private static final int MAX_TABLE_LENGTH = 1 << 24;

    private static final int NAME_FAMILY = 1;
    private static final int NAME_FULL = 4;
    private static final int NAME_TYPOGRAPHIC_FAMILY = 16;

    private static final int PLATFORM_UNICODE = 0;
    private static final int PLATFORM_MACINTOSH = 1;
    private static final int PLATFORM_WINDOWS = 3;
    private static final int LANGUAGE_WINDOWS_ENGLISH = 0x0409;

    private static final Charset MAC_ROMAN = Charset.isSupported("x-MacRoman") ? Charset.forName("x-MacRoman") : StandardCharsets.ISO_8859_1;

    private FontFileReader() {
    }

    static FontFile read(byte[] data) throws IOException {
        try {
            Map<Integer, ByteBuffer> tables = readTables(ByteBuffer.wrap(data));

            ByteBuffer cmap = tables.get(TAG_CMAP);
            if (cmap == null) {
                throw new IOException("Font has no character map");
            }
            BitSet coverage = readCoverage(cmap);

            ByteBuffer name = tables.get(TAG_NAME);
            Map<Integer, String> names = name == null ? Map.of() : readNames(name);
            String family = names.getOrDefault(NAME_TYPOGRAPHIC_FAMILY, names.getOrDefault(NAME_FAMILY, ""));
            String fullName = names.getOrDefault(NAME_FULL, family);

            return new FontFile(fullName, coverage);
        }
        catch (BufferUnderflowException | IndexOutOfBoundsException | IllegalArgumentException | ArithmeticException | DataFormatException e) {
            throw new IOException("Malformed font file", e);
        }
    }

    private static Map<Integer, ByteBuffer> readTables(ByteBuffer data) throws IOException, DataFormatException {
        int signature = data.getInt(0);
        if (signature == TAG_WOFF) {
            return readWoffTables(data);
        }
        if (signature == TAG_WOFF2) {
            throw new IOException("WOFF2 fonts are not supported");
        }

        int start = 0;
        if (signature == TAG_TTCF) {
            start = data.getInt(12);
            signature = data.getInt(start);
        }
        if (signature != SFNT_TRUETYPE && signature != TAG_TRUE && signature != TAG_OTTO) {
            throw new IOException("Not a font file");
        }

        int numTables = Short.toUnsignedInt(data.getShort(start + 4));
        Map<Integer, ByteBuffer> tables = new HashMap<>();
        for (int i = 0; i < numTables; i++) {
            int record = start + 12 + i * 16;
            int tag = data.getInt(record);
            if (REQUIRED_TABLES.contains(tag)) {
                tables.put(tag, data.slice(data.getInt(record + 8), data.getInt(record + 12)));
            }
        }
        return tables;
    }

    private static Map<Integer, ByteBuffer> readWoffTables(ByteBuffer data) throws IOException, DataFormatException {
        int numTables = Short.toUnsignedInt(data.getShort(12));
        Map<Integer, ByteBuffer> tables = new HashMap<>();
        for (int i = 0; i < numTables; i++) {
            int entry = 44 + i * 20;
            int tag = data.getInt(entry);
            if (!REQUIRED_TABLES.contains(tag)) {
                continue;
            }

            int compressedLength = data.getInt(entry + 8);
            int length = data.getInt(entry + 12);
            if (length > MAX_TABLE_LENGTH) {
                throw new IOException("Font table is too large");
            }
            ByteBuffer table = data.slice(data.getInt(entry + 4), compressedLength);
            tables.put(tag, compressedLength < length ? inflate(table, length) : table);
        }
        return tables;
    }

    private static ByteBuffer inflate(ByteBuffer compressed, int length) throws DataFormatException {
        Inflater inflater = new Inflater();
        try {
            inflater.setInput(compressed);
            byte[] result = new byte[length];
            int read = 0;
            while (read < length && !inflater.finished()) {
                int count = inflater.inflate(result, read, length - read);
                if (count == 0 && (inflater.needsInput() || inflater.needsDictionary())) {
                    break;
                }
                read += count;
            }
            return ByteBuffer.wrap(result, 0, read).slice();
        }
        finally {
            inflater.end();
        }
    }

    private static Map<Integer, String> readNames(ByteBuffer table) {
        int count = Short.toUnsignedInt(table.getShort(2));
        int storage = Short.toUnsignedInt(table.getShort(4));

        Map<Integer, String> names = new HashMap<>();
        Map<Integer, Integer> ranks = new HashMap<>();
        for (int i = 0; i < count; i++) {
            int record = 6 + i * 12;
            int nameId = Short.toUnsignedInt(table.getShort(record + 6));
            if (nameId != NAME_FAMILY && nameId != NAME_FULL && nameId != NAME_TYPOGRAPHIC_FAMILY) {
                continue;
            }

            int platform = Short.toUnsignedInt(table.getShort(record));
            int rank = nameRank(platform, Short.toUnsignedInt(table.getShort(record + 2)), Short.toUnsignedInt(table.getShort(record + 4)));
            if (rank <= ranks.getOrDefault(nameId, 0)) {
                continue;
            }

            byte[] bytes = new byte[Short.toUnsignedInt(table.getShort(record + 8))];
            table.get(storage + Short.toUnsignedInt(table.getShort(record + 10)), bytes);
            String value = new String(bytes, platform == PLATFORM_MACINTOSH ? MAC_ROMAN : StandardCharsets.UTF_16BE).trim();
            if (!value.isEmpty()) {
                names.put(nameId, value);
                ranks.put(nameId, rank);
            }
        }
        return names;
    }

    private static int nameRank(int platform, int encoding, int language) {
        if (platform == PLATFORM_WINDOWS && (encoding == 0 || encoding == 1 || encoding == 10)) {
            return language == LANGUAGE_WINDOWS_ENGLISH ? 4 : 3;
        }
        if (platform == PLATFORM_UNICODE) {
            return 2;
        }
        if (platform == PLATFORM_MACINTOSH && encoding == 0 && language == 0) {
            return 1;
        }
        return 0;
    }

    private static BitSet readCoverage(ByteBuffer table) throws IOException {
        int numTables = Short.toUnsignedInt(table.getShort(2));
        int bestOffset = -1;
        int bestRank = 0;
        for (int i = 0; i < numTables; i++) {
            int record = 4 + i * 8;
            int offset = table.getInt(record + 4);
            int rank = cmapRank(
                Short.toUnsignedInt(table.getShort(record)),
                Short.toUnsignedInt(table.getShort(record + 2)),
                Short.toUnsignedInt(table.getShort(offset))
            );
            if (rank > bestRank) {
                bestRank = rank;
                bestOffset = offset;
            }
        }
        if (bestOffset < 0) {
            throw new IOException("Font has no Unicode character map");
        }

        ByteBuffer subtable = table.slice(bestOffset, table.limit() - bestOffset);
        BitSet coverage = new BitSet();
        switch (Short.toUnsignedInt(subtable.getShort(0))) {
            case 4 -> readSegmentMapping(subtable, coverage);
            case 6 -> readTrimmedMapping(subtable, coverage);
            case 12 -> readSegmentedCoverage(subtable, coverage, false);
            case 13 -> readSegmentedCoverage(subtable, coverage, true);
            default -> {
            }
        }
        return coverage;
    }

    private static int cmapRank(int platform, int encoding, int format) {
        boolean fullRange = format == 12 || format == 13;
        if (!fullRange && format != 4 && format != 6) {
            return 0;
        }
        if (platform == PLATFORM_WINDOWS && encoding == 10) {
            return fullRange ? 6 : 0;
        }
        if (platform == PLATFORM_UNICODE) {
            return fullRange ? 5 : 3;
        }
        if (platform == PLATFORM_WINDOWS && encoding == 1) {
            return 4;
        }
        if (platform == PLATFORM_WINDOWS && encoding == 0) {
            return 1;
        }
        return 0;
    }

    private static void readSegmentMapping(ByteBuffer subtable, BitSet coverage) {
        int segCount = Short.toUnsignedInt(subtable.getShort(6)) / 2;
        int endCodes = 14;
        int startCodes = endCodes + segCount * 2 + 2;
        int idDeltas = startCodes + segCount * 2;
        int idRangeOffsets = idDeltas + segCount * 2;

        int next = 0;
        for (int segment = 0; segment < segCount; segment++) {
            int end = Short.toUnsignedInt(subtable.getShort(endCodes + segment * 2));
            int start = Short.toUnsignedInt(subtable.getShort(startCodes + segment * 2));
            int delta = subtable.getShort(idDeltas + segment * 2);
            int rangeOffsetPosition = idRangeOffsets + segment * 2;
            int rangeOffset = Short.toUnsignedInt(subtable.getShort(rangeOffsetPosition));

            for (int codePoint = Math.max(start, next); codePoint <= end && codePoint != 0xFFFF; codePoint++) {
                int glyph;
                if (rangeOffset == 0) {
                    glyph = (codePoint + delta) & 0xFFFF;
                }
                else {
                    int glyphPosition = rangeOffsetPosition + rangeOffset + (codePoint - start) * 2;
                    if (glyphPosition + 2 > subtable.limit()) {
                        break;
                    }
                    glyph = Short.toUnsignedInt(subtable.getShort(glyphPosition));
                    if (glyph != 0) {
                        glyph = (glyph + delta) & 0xFFFF;
                    }
                }

                if (glyph != 0) {
                    coverage.set(codePoint);
                }
            }
            next = Math.max(next, end + 1);
        }
    }

    private static void readTrimmedMapping(ByteBuffer subtable, BitSet coverage) {
        int firstCode = Short.toUnsignedInt(subtable.getShort(6));
        int entryCount = Short.toUnsignedInt(subtable.getShort(8));
        for (int i = 0; i < entryCount; i++) {
            if (subtable.getShort(10 + i * 2) != 0) {
                coverage.set(firstCode + i);
            }
        }
    }

    private static void readSegmentedCoverage(ByteBuffer subtable, BitSet coverage, boolean manyToOne) {
        long groups = Integer.toUnsignedLong(subtable.getInt(12));
        long next = 0;
        for (long group = 0; group < groups; group++) {
            int position = Math.toIntExact(16 + group * 12);
            long start = Integer.toUnsignedLong(subtable.getInt(position));
            long end = Math.min(Integer.toUnsignedLong(subtable.getInt(position + 4)), Character.MAX_CODE_POINT);
            long startGlyph = Integer.toUnsignedLong(subtable.getInt(position + 8));

            long first = Math.max(!manyToOne && startGlyph == 0 ? start + 1 : start, next);
            if (first <= end && (!manyToOne || startGlyph != 0)) {
                coverage.set((int) first, (int) end + 1);
            }
            next = Math.max(next, end + 1);
        }
    }

    private static int tag(String tag) {
        return tag.charAt(0) << 24 | tag.charAt(1) << 16 | tag.charAt(2) << 8 | tag.charAt(3);
    }
}
