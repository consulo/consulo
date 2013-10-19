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

/*
 * @author: Eugene Zhuravlev
 * Date: Jan 17, 2003
 * Time: 3:22:59 PM
 */
package com.intellij.compiler.impl.javaCompiler;

import com.intellij.CommonBundle;
import com.intellij.compiler.CompilerException;
import com.intellij.compiler.ModuleCompilerUtil;
import com.intellij.compiler.impl.CompileContextExProxy;
import com.intellij.compiler.impl.javaCompiler.javac.JavacCompiler;
import com.intellij.compiler.make.CacheCorruptedException;
import com.intellij.ide.highlighter.JavaClassFileType;
import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.compiler.*;
import com.intellij.openapi.compiler.ex.CompileContextEx;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.StdFileTypes;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ui.configuration.ProjectSettingsService;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.Chunk;
import com.intellij.util.ExceptionUtil;
import com.intellij.util.StringBuilderSpinAllocator;
import org.consulo.lombok.annotations.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Logger
public class AnnotationProcessingCompiler implements TranslatingCompiler {
  private final Project myProject;
  private final JavaCompilerConfiguration myCompilerConfiguration;

  public AnnotationProcessingCompiler(Project project) {
    myProject = project;
    myCompilerConfiguration = JavaCompilerConfiguration.getInstance(project);
  }

  @Override
  @NotNull
  public String getDescription() {
    return CompilerBundle.message("annotation.processing.compiler.description");
  }

  @Override
  public boolean isCompilableFile(VirtualFile file, CompileContext context) {
    if (!myCompilerConfiguration.isAnnotationProcessorsEnabled()) {
      return false;
    }
    return file.getFileType() == StdFileTypes.JAVA && !isExcludedFromAnnotationProcessing(file, context);
  }

  @Override
  public void compile(final CompileContext context, final Chunk<Module> moduleChunk, final VirtualFile[] files, OutputSink sink) {
    if (!myCompilerConfiguration.isAnnotationProcessorsEnabled()) {
      return;
    }
    final LocalFileSystem lfs = LocalFileSystem.getInstance();
    final CompileContextEx _context = new CompileContextExProxy((CompileContextEx)context) {
      @Override
      public VirtualFile getModuleOutputDirectory(Module module) {
        final String path = JavaAdditionalOutputDirectoriesProvider.getAnnotationProcessorsGenerationPath(module);
        return path != null ? lfs.findFileByPath(path) : null;
      }

      @Override
      public VirtualFile getModuleOutputDirectoryForTests(Module module) {
        return getModuleOutputDirectory(module);
      }
    };
    final JavacCompiler javacCompiler = getBackEndCompiler();
    final boolean processorMode = javacCompiler.setAnnotationProcessorMode(true);
    final BackendCompilerWrapper wrapper =
      new BackendCompilerWrapper(this, moduleChunk, myProject, Arrays.asList(files), _context, javacCompiler, sink);
    wrapper.setForceCompileTestsSeparately(true);
    try {
      wrapper.compile();
    }
    catch (CompilerException e) {
      _context.addMessage(CompilerMessageCategory.ERROR, ExceptionUtil.getThrowableText(e), null, -1, -1);
    }
    catch (CacheCorruptedException e) {
      LOGGER.info(e);
      _context.requestRebuildNextTime(e.getMessage());
    }
    finally {
      javacCompiler.setAnnotationProcessorMode(processorMode);
      final Set<VirtualFile> dirsToRefresh = new HashSet<VirtualFile>();
      ApplicationManager.getApplication().runReadAction(new Runnable() {
        @Override
        public void run() {
          for (Module module : moduleChunk.getNodes()) {
            final VirtualFile out = _context.getModuleOutputDirectory(module);
            if (out != null) {
              dirsToRefresh.add(out);
            }
          }
        }
      });
      for (VirtualFile root : dirsToRefresh) {
        root.refresh(false, true);
      }
    }
  }

  @NotNull
  @Override
  public FileType[] getInputFileTypes() {
    return new FileType[] {JavaFileType.INSTANCE};
  }

  @NotNull
  @Override
  public FileType[] getOutputFileTypes() {
    return new FileType[] {JavaFileType.INSTANCE, JavaClassFileType.INSTANCE};
  }

  private boolean isExcludedFromAnnotationProcessing(VirtualFile file, CompileContext context) {
    if (!myCompilerConfiguration.isAnnotationProcessorsEnabled()) {
      return true;
    }
    final Module module = context.getModuleByFile(file);
    if (module != null) {
      if (!myCompilerConfiguration.getAnnotationProcessingConfiguration(module).isEnabled()) {
        return true;
      }
      final String path = JavaAdditionalOutputDirectoriesProvider.getAnnotationProcessorsGenerationPath(module);
      final VirtualFile generationDir = path != null ? LocalFileSystem.getInstance().findFileByPath(path) : null;
      if (generationDir != null && VfsUtil.isAncestor(generationDir, file, false)) {
        return true;
      }
    }
    return CompilerManager.getInstance(myProject).isExcludedFromCompilation(file);
  }

  @Override
  public boolean validateConfiguration(CompileScope scope) {
    final List<Chunk<Module>> chunks = ModuleCompilerUtil.getSortedModuleChunks(myProject, Arrays.asList(scope.getAffectedModules()));
    for (final Chunk<Module> chunk : chunks) {
      final Set<Module> chunkModules = chunk.getNodes();
      if (chunkModules.size() <= 1) {
        continue; // no need to check one-module chunks
      }
      for (Module chunkModule : chunkModules) {
        if (myCompilerConfiguration.getAnnotationProcessingConfiguration(chunkModule).isEnabled()) {
          showCyclesNotSupportedForAnnotationProcessors(chunkModules.toArray(new Module[chunkModules.size()]));
          return false;
        }
      }
    }

    final JavacCompiler compiler = getBackEndCompiler();
    final boolean previousValue = compiler.setAnnotationProcessorMode(true);
    try {
      return compiler.checkCompiler(scope);
    }
    finally {
      compiler.setAnnotationProcessorMode(previousValue);
    }
  }

  @Override
  public void init(@NotNull CompilerManager compilerManager) {
  }

  private void showCyclesNotSupportedForAnnotationProcessors(Module[] modulesInChunk) {
    LOGGER.assertTrue(modulesInChunk.length > 0);
    String moduleNameToSelect = modulesInChunk[0].getName();
    final String moduleNames = getModulesString(modulesInChunk);
    Messages
      .showMessageDialog(myProject, CompilerBundle.message("error.annotation.processing.not.supported.for.module.cycles", moduleNames),
                         CommonBundle.getErrorTitle(), Messages.getErrorIcon());
    showConfigurationDialog(moduleNameToSelect, null);
  }

  private void showConfigurationDialog(String moduleNameToSelect, String tabNameToSelect) {
    ProjectSettingsService.getInstance(myProject).showModuleConfigurationDialog(moduleNameToSelect, tabNameToSelect);
  }

  private static String getModulesString(Module[] modulesInChunk) {
    final StringBuilder moduleNames = StringBuilderSpinAllocator.alloc();
    try {
      for (Module module : modulesInChunk) {
        if (moduleNames.length() > 0) {
          moduleNames.append("\n");
        }
        moduleNames.append("\"").append(module.getName()).append("\"");
      }
      return moduleNames.toString();
    }
    finally {
      StringBuilderSpinAllocator.dispose(moduleNames);
    }
  }

  private JavacCompiler getBackEndCompiler() {
    return (JavacCompiler)myCompilerConfiguration.findCompiler(JavaCompilerConfiguration.DEFAULT_COMPILER);
  }
}
