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
package consulo.ide.impl.idea.openapi.editor.richcopy;

import consulo.language.editor.impl.internal.richcopy.model.SyntaxInfoCompressor;
import net.jpountz.lz4.LZ4CompressorWithLength;
import net.jpountz.lz4.LZ4DecompressorWithLength;
import net.jpountz.lz4.LZ4Factory;

/**
 * Packs the whole markup at once by the pure Java LZ4: the native one is loaded from a library unpacked into a temporary directory,
 * which is not worth it for a copied text
 */
public final class Lz4SyntaxInfoCompressor implements SyntaxInfoCompressor {
    public static final Lz4SyntaxInfoCompressor INSTANCE = new Lz4SyntaxInfoCompressor();

    private final LZ4CompressorWithLength myCompressor;
    private final LZ4DecompressorWithLength myDecompressor;

    private Lz4SyntaxInfoCompressor() {
        LZ4Factory factory = LZ4Factory.fastestJavaInstance();
        myCompressor = new LZ4CompressorWithLength(factory.fastCompressor());
        myDecompressor = new LZ4DecompressorWithLength(factory.fastDecompressor());
    }

    @Override
    public byte[] compress(byte[] data, int offset, int length) {
        return myCompressor.compress(data, offset, length);
    }

    @Override
    public byte[] decompress(byte[] data) {
        return myDecompressor.decompress(data);
    }
}
