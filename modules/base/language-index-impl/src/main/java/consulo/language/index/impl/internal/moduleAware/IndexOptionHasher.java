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
package consulo.language.index.impl.internal.moduleAware;

import consulo.index.io.data.DataExternalizer;
import consulo.language.internal.psi.stub.IndexOptionImpl;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Arrays;

/**
 * Stable content hash for {@link IndexOptionImpl.SharablePerOption} payloads. Hash is the
 * 32-bit {@link Arrays#hashCode(byte[])} of the serialized form produced by the payload's
 * own {@link DataExternalizer}. Stable across JVMs and processes as long as the plugin
 * satisfies the determinism contract (see {@code MODULE_AWARE_INDEX.md}).
 *
 * <p>32-bit today — a collision (1-in-4B) produces at most one false cache hit, no
 * correctness issue since the stored blob is still validated by revalidation. Upgrade to
 * 64-bit xxhash if real-world collisions appear.</p>
 */
public final class IndexOptionHasher {
    private IndexOptionHasher() {
    }

    public static int hash(IndexOptionImpl.SharablePerOption<?> option) {
        return Arrays.hashCode(serialize(option));
    }

    public static byte[] serialize(IndexOptionImpl.SharablePerOption<?> option) {
        try (ByteArrayOutputStream bytes = new ByteArrayOutputStream();
             DataOutputStream out = new DataOutputStream(bytes)) {
            writeValue(out, option);
            out.flush();
            return bytes.toByteArray();
        }
        catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T extends Record> void writeValue(DataOutputStream out,
                                                      IndexOptionImpl.SharablePerOption<?> option) throws IOException {
        IndexOptionImpl.SharablePerOption<T> typed = (IndexOptionImpl.SharablePerOption<T>) option;
        typed.externalizer().save(out, typed.value());
    }
}
