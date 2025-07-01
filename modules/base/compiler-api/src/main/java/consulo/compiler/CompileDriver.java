/*
 * Copyright 2013-2023 consulo.io
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
package consulo.compiler;

import consulo.compiler.scope.CompileScope;
import consulo.module.Module;
import consulo.virtualFileSystem.VirtualFile;

import jakarta.annotation.Nullable;

import java.io.File;
import java.util.function.Predicate;

/**
 * @author VISTALL
 * @since 2023-04-17
 */
public interface CompileDriver {
    /**
     * @param file a file to delete
     * @return true if and only if the file existed and was successfully deleted
     * Note: the behaviour is different from FileUtil.delete() which returns true if the file absent on the disk
     */
    boolean deleteFile(File file);

    void dropDependencyCache(CompileContextEx context);

    CompileScope attachIntermediateOutputDirectories(CompileScope originalScope, Predicate<Compiler> filter);

    @Nullable
    VirtualFile getGenerationOutputDir(IntermediateOutputCompiler compiler, Module module, boolean forTestSources);

    Predicate<Compiler> getCompilerFilter();
}
