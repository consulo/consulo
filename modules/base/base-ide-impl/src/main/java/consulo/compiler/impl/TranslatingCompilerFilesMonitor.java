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
package consulo.compiler.impl;

import com.intellij.compiler.impl.TranslatingCompilerFilesMonitorImpl;
import com.intellij.openapi.compiler.CompileContext;
import com.intellij.openapi.compiler.TranslatingCompiler;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Trinity;
import com.intellij.openapi.vfs.VirtualFile;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * @author VISTALL
 * @since 13:36/06.11.13
 */
public abstract class TranslatingCompilerFilesMonitor {
  @Nonnull
  public static TranslatingCompilerFilesMonitor getInstance() {
    return ServiceManager.getService(TranslatingCompilerFilesMonitor.class);
  }

  public abstract void suspendProject(Project project);

  public abstract void watchProject(Project project);

  public abstract boolean isSuspended(Project project);

  public abstract boolean isSuspended(int projectId);

  public abstract void collectFiles(CompileContext context,
                    TranslatingCompiler compiler,
                    Iterator<VirtualFile> scopeSrcIterator,
                    boolean forceCompile,
                    boolean isRebuild,
                    Collection<VirtualFile> toCompile,
                    Collection<Trinity<File, String, Boolean>> toDelete);

  public abstract void update(CompileContext context,
              @Nullable String outputRoot,
              Collection<TranslatingCompiler.OutputItem> successfullyCompiled,
              VirtualFile[] filesToRecompile) throws IOException;

  public abstract void updateOutputRootsLayout(Project project);

  public abstract List<String> getCompiledClassNames(VirtualFile srcFile, Project project);

  public abstract void scanSourceContent(TranslatingCompilerFilesMonitorImpl.ProjectRef projRef,
                         Collection<VirtualFile> roots,
                         int totalRootCount,
                         boolean isNewRoots);

  public abstract void ensureInitializationCompleted(Project project, ProgressIndicator indicator);

  public abstract void scanSourcesForCompilableFiles(Project project);

  public abstract boolean isMarkedForCompilation(Project project, VirtualFile file);
}
