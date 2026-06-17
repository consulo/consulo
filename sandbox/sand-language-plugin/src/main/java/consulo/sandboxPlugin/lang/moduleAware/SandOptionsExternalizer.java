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
package consulo.sandboxPlugin.lang.moduleAware;

import consulo.index.io.data.DataExternalizer;
import consulo.index.io.data.DataInputOutputUtil;
import consulo.index.io.data.IOUtil;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Deterministic externaliser for {@link SandOptions} — symbols written in sorted order so
 * different {@code Set} iteration orders don't produce different bytes.
 */
public final class SandOptionsExternalizer implements DataExternalizer<SandOptions> {
    public static final SandOptionsExternalizer INSTANCE = new SandOptionsExternalizer();

    private SandOptionsExternalizer() {
    }

    @Override
    public void save(DataOutput out, SandOptions value) throws IOException {
        IOUtil.writeUTF(out, value.target());

        List<String> sorted = new ArrayList<>(value.symbols());
        sorted.sort(null);
        DataInputOutputUtil.writeINT(out, sorted.size());
        for (String symbol : sorted) {
            IOUtil.writeUTF(out, symbol);
        }
    }

    @Override
    public SandOptions read(DataInput in) throws IOException {
        String target = IOUtil.readUTF(in);
        int count = DataInputOutputUtil.readINT(in);
        Set<String> symbols = new HashSet<>(count);
        for (int i = 0; i < count; i++) {
            symbols.add(IOUtil.readUTF(in));
        }
        return new SandOptions(Set.copyOf(symbols), target);
    }
}
