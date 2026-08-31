/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Set;

public interface CompileContextEx extends CompileContext {
    CompositeDependencyCache getDependencyCache();

    @Nullable Path getSourceFileByOutputFile(Path outputFile);

    void addMessage(CompilerMessage message);

    Set<Path> getTestOutputDirectories();

    /**
     * the same as FileIndex.isInTestSourceContent(), but takes into account generated output dirs
     */
    boolean isInTestSourceContent(Path fileOrDir);

    boolean isInSourceContent(Path fileOrDir);

    void addScope(CompileScope additionalScope);

    long getStartCompilationStamp();

    void recalculateOutputDirs();

    void markGenerated(Collection<Path> files);

    boolean isGenerated(Path file);

    void assignModule(Path root, Module module, boolean isTestSource, @Nullable Compiler compiler);
}
