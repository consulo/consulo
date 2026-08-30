/*
 * Copyright 2013-2016 consulo.io
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

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.application.Application;
import consulo.project.Project;
import consulo.util.lang.Trinity;
import consulo.util.lang.ref.SimpleReference;
import org.jspecify.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * @author VISTALL
 * @since 2013-11-06
 */
@ServiceAPI(ComponentScope.APPLICATION)
public abstract class TranslatingCompilerFilesMonitor {
    public static TranslatingCompilerFilesMonitor getInstance() {
        return Application.get().getInstance(TranslatingCompilerFilesMonitor.class);
    }

    public static final class ProjectRef extends SimpleReference<Project> {
        public static class ProjectClosedException extends RuntimeException {
        }

        public ProjectRef(Project project) {
            super(project);
        }

        @Override
        public Project get() {
            Project project = super.get();
            if (project != null && project.isDisposed()) {
                throw new ProjectClosedException();
            }
            return project;
        }
    }

    public abstract void suspendProject(Project project);

    public abstract void watchProject(Project project);

    public abstract boolean isSuspended(Project project);

    public abstract void collectFiles(
        CompileContext context,
        TranslatingCompiler compiler,
        Iterator<Path> scopeSrcIterator,
        boolean forceCompile,
        boolean isRebuild,
        Collection<Path> toCompile,
        Collection<Trinity<File, String, Boolean>> toDelete
    );

    public abstract void update(
        CompileContext context,
        @Nullable String outputRoot,
        Collection<TranslatingCompiler.OutputItem> successfullyCompiled,
        Collection<Path> filesToRecompile
    ) throws IOException;

    public abstract List<String> getCompiledClassNames(Path srcFile, Project project);

    public abstract boolean isMarkedForCompilation(Project project, Path file);
}
