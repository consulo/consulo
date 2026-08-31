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

import consulo.compiler.util.CompilerUtil;
import consulo.util.io.FileUtil;

import java.nio.file.Path;

/**
 * @author nik
 */
public abstract class PathCompileItem<OutputState> extends CompileItem<String, VirtualFilePersistentState, OutputState> {
    protected final Path myFile;

    public PathCompileItem(Path file) {
        myFile = file;
    }

    public Path getFile() {
        return myFile;
    }

    @Override
    public VirtualFilePersistentState computeSourceState() {
        return new VirtualFilePersistentState(CompilerUtil.lastModified(myFile));
    }

    @Override
    public boolean isSourceUpToDate(VirtualFilePersistentState state) {
        return CompilerUtil.lastModified(myFile) == state.getSourceTimestamp();
    }

    @Override
    public String getKey() {
        return FileUtil.toSystemIndependentName(myFile.toString());
    }
}
