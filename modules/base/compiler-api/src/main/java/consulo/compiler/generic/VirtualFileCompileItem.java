/*
 * Copyright 2000-2010 JetBrains s.r.o.
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
package consulo.compiler.generic;

import consulo.compiler.generic.CompileItem;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;

/**
 * @author nik
 */
public abstract class VirtualFileCompileItem<OutputState> extends CompileItem<String, VirtualFilePersistentState, OutputState> {
    protected final VirtualFile myFile;

    public VirtualFileCompileItem(@Nonnull VirtualFile file) {
        myFile = file;
    }

    @Nonnull
    public VirtualFile getFile() {
        return myFile;
    }

    @Nonnull
    @Override
    public VirtualFilePersistentState computeSourceState() {
        return new VirtualFilePersistentState(myFile.getTimeStamp());
    }

    @Override
    public boolean isSourceUpToDate(@Nonnull VirtualFilePersistentState state) {
        return myFile.getTimeStamp() == state.getSourceTimestamp();
    }

    @Nonnull
    @Override
    public String getKey() {
        return myFile.getUrl();
    }
}
