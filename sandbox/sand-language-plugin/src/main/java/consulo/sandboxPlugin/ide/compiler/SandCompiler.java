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
package consulo.sandboxPlugin.ide.compiler;

import consulo.annotation.component.ExtensionImpl;
import consulo.compiler.CompileContext;
import consulo.compiler.scope.CompileScope;
import consulo.compiler.CompilerMessageCategory;
import consulo.compiler.TranslatingCompiler;
import consulo.sandboxPlugin.ide.bundle.SandBundleType;
import consulo.virtualFileSystem.fileType.FileType;
import consulo.module.Module;
import consulo.virtualFileSystem.VirtualFile;
import consulo.util.collection.Chunk;
import consulo.sandboxPlugin.lang.SandFileType;

import jakarta.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 17.05.14
 */
@ExtensionImpl
public class SandCompiler implements TranslatingCompiler {
  private boolean myAddError = false;

  @Override
  public boolean isCompilableFile(VirtualFile file, CompileContext context) {
    return file.getFileType() == SandFileType.INSTANCE;
  }

  @Override
  public void compile(CompileContext context, Chunk<Module> moduleChunk, VirtualFile[] files, OutputSink sink) {
    SandBundleType sandBundleType = SandBundleType.INSTANCE.get();

    try {
      context.addMessage(CompilerMessageCategory.WARNING, "my warning", null, -1, -1);
      Thread.sleep(5000L);
    }
    catch (InterruptedException e) {
      e.printStackTrace();
    }

    if (myAddError) {
      context.addMessage(CompilerMessageCategory.ERROR, "my error", null, -1, -1);
    }
    myAddError = !myAddError;
  }

  @Nonnull
  @Override
  public FileType[] getInputFileTypes() {
    return new FileType[0];
  }

  @Nonnull
  @Override
  public FileType[] getOutputFileTypes() {
    return new FileType[0];
  }

  @Nonnull
  @Override
  public String getDescription() {
    return "test";
  }

  @Override
  public boolean validateConfiguration(CompileScope scope) {
    return true;
  }
}
