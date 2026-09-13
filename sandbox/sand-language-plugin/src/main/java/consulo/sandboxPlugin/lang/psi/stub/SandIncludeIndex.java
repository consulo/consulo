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
package consulo.sandboxPlugin.lang.psi.stub;

import consulo.annotation.component.ExtensionImpl;
import consulo.index.io.DataIndexer;
import consulo.index.io.EnumeratorStringDescriptor;
import consulo.index.io.ID;
import consulo.index.io.KeyDescriptor;
import consulo.index.io.VoidDataExternalizer;
import consulo.index.io.data.DataExternalizer;
import consulo.language.psi.stub.DefaultFileTypeSpecificInputFilter;
import consulo.language.psi.stub.FileBasedIndex;
import consulo.language.psi.stub.FileBasedIndexExtension;
import consulo.language.psi.stub.FileContent;
import consulo.sandboxPlugin.lang.SandFileType;

import java.util.HashMap;
import java.util.Map;

/**
 * Inverted index of {@code #include "name"} specs: key = included file name, value = none.
 * The reverse lookup ("which files include X") makes per-file context computation an index
 * query instead of a whole-project simulation — the include graph is derived data, never
 * held in memory.
 */
@ExtensionImpl
public class SandIncludeIndex extends FileBasedIndexExtension<String, Void> {
    public static final ID<String, Void> INDEX_ID = ID.create("sand.includes");

    @Override
    public ID<String, Void> getName() {
        return INDEX_ID;
    }

    @Override
    public DataIndexer<String, Void, FileContent> getIndexer() {
        return inputData -> {
            Map<String, Void> result = new HashMap<>();
            for (String rawLine : inputData.getContentAsText().toString().split("\n", -1)) {
                String line = rawLine.strip();
                if (!line.startsWith("#include")) {
                    continue;
                }
                String[] parts = line.split("\\s+");
                if (parts.length > 1) {
                    result.put(parts[1].replace("\"", ""), null);
                }
            }
            return result;
        };
    }

    @Override
    public KeyDescriptor<String> getKeyDescriptor() {
        return EnumeratorStringDescriptor.INSTANCE;
    }

    @Override
    public DataExternalizer<Void> getValueExternalizer() {
        return VoidDataExternalizer.INSTANCE;
    }

    @Override
    public FileBasedIndex.InputFilter getInputFilter() {
        return new DefaultFileTypeSpecificInputFilter(SandFileType.INSTANCE);
    }

    @Override
    public boolean dependsOnFileContent() {
        return true;
    }

    @Override
    public int getVersion() {
        return 1;
    }
}
