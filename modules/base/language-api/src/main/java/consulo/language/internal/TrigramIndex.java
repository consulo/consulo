/*
 * Copyright 2013-2025 consulo.io
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
package consulo.language.internal;

import consulo.index.io.ID;
import consulo.language.psi.stub.ScalarIndexExtension;
import consulo.virtualFileSystem.fileType.FileType;
import jakarta.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 2025-08-20
 */
public abstract class TrigramIndex extends ScalarIndexExtension<Integer> {
    public static final ID<Integer, Void> INDEX_ID = ID.create("Trigram.Index");

    public static boolean isIndexable(FileType fileType) {
        return !fileType.isBinary();
    }

    @Nonnull
    @Override
    public ID<Integer, Void> getName() {
        return INDEX_ID;
    }
}
