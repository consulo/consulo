/*
 * Copyright 2013-2014 must-be.org
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
package consulo.compiler.server.compiler;

import consulo.compiler.impl.TranslatingCompilerFilesMonitor;
import com.intellij.openapi.compiler.CompileContext;
import com.intellij.openapi.compiler.CompilerManager;
import com.intellij.openapi.compiler.TranslatingCompiler;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Trinity;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class TranslatingCompilerFilesMonitorImpl extends TranslatingCompilerFilesMonitor {
  private static final Logger LOG = Logger.getInstance("#consulo.compiler.impl.TranslatingCompilerFilesMonitor");

  private final Object myDataLock = new Object();

  @Override
  public void suspendProject(Project project) {
  }

  @Override
  public void watchProject(Project project) {
  }

  @Override
  public boolean isSuspended(Project project) {
    return false;
  }

  @Override
  public boolean isSuspended(int projectId) {
    return false;
  }

  @Override
  public void collectFiles(CompileContext context,
                           TranslatingCompiler compiler,
                           Iterator<VirtualFile> scopeSrcIterator,
                           boolean forceCompile,
                           boolean isRebuild,
                           Collection<VirtualFile> toCompile,
                           Collection<Trinity<File, String, Boolean>> toDelete) {
    final Project project = context.getProject();

    final CompilerManager configuration = CompilerManager.getInstance(project);
    synchronized (myDataLock) {
      while (scopeSrcIterator.hasNext()) {
        final VirtualFile file = scopeSrcIterator.next();
        if (!file.isValid()) {
          if (LOG.isDebugEnabled()) {
            LOG.debug("Skipping invalid file " + file.getPresentableUrl());
          }
          continue;
        }

        if (compiler.isCompilableFile(file, context) && !configuration.isExcludedFromCompilation(file)) {
          toCompile.add(file);
        }
      }
    }
  }

  @Override
  public void update(CompileContext context,
                     @Nullable String outputRoot,
                     Collection<TranslatingCompiler.OutputItem> successfullyCompiled,
                     VirtualFile[] filesToRecompile) throws IOException {
  }

  @Override
  public void updateOutputRootsLayout(Project project) {
  }

  @Override
  public List<String> getCompiledClassNames(VirtualFile srcFile, Project project) {
    return Collections.emptyList();
  }

  @Override
  public void scanSourceContent(com.intellij.compiler.impl.TranslatingCompilerFilesMonitorImpl.ProjectRef projRef,
                                Collection<VirtualFile> roots,
                                int totalRootCount,
                                boolean isNewRoots) {
  }

  @Override
  public void ensureInitializationCompleted(Project project, ProgressIndicator indicator) {
  }

  @Override
  public void scanSourcesForCompilableFiles(Project project) {
  }

  @Override
  public boolean isMarkedForCompilation(Project project, VirtualFile file) {
    return false;
  }
}
