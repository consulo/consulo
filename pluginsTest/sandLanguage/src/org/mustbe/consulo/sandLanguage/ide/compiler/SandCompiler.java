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
package org.mustbe.consulo.sandLanguage.ide.compiler;

import com.intellij.openapi.compiler.CompileContext;
import com.intellij.openapi.compiler.CompileScope;
import com.intellij.openapi.compiler.CompilerManager;
import com.intellij.openapi.compiler.TranslatingCompiler;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.Chunk;
import org.jetbrains.annotations.NotNull;
import org.mustbe.consulo.sandLanguage.lang.SandFileType;

/**
 * @author VISTALL
 * @since 17.05.14
 */
public class SandCompiler implements TranslatingCompiler {
  @Override
  public boolean isCompilableFile(VirtualFile file, CompileContext context) {
    return file.getFileType() == SandFileType.INSTANCE;
  }

  @Override
  public void compile(CompileContext context, Chunk<Module> moduleChunk, VirtualFile[] files, OutputSink sink) {
    try {
      Thread.sleep(10000L);
    }
    catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  @NotNull
  @Override
  public FileType[] getInputFileTypes() {
    return new FileType[0];
  }

  @NotNull
  @Override
  public FileType[] getOutputFileTypes() {
    return new FileType[0];
  }

  @NotNull
  @Override
  public String getDescription() {
    return "test";
  }

  @Override
  public boolean validateConfiguration(CompileScope scope) {
    return true;
  }

  @Override
  public void init(@NotNull CompilerManager compilerManager) {

  }
}
