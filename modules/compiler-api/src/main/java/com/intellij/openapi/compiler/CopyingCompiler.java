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
package com.intellij.openapi.compiler;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.io.IOUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Compiler which copies the compiled files to a different directory.
 */
public abstract class CopyingCompiler implements PackagingCompiler {
  public abstract VirtualFile[] getFilesToCopy(CompileContext context);

  public abstract String getDestinationPath(CompileContext context, VirtualFile sourceFile);

  @Override
  public final void processOutdatedItem(CompileContext context, File file, @Nullable ValidityState state) {
    if (state != null) {
      final String destinationPath = ((DestinationFileInfo)state).getDestinationPath();
      new File(destinationPath).delete();
    }
  }

  @Override
  @NotNull
  public final ProcessingItem[] getProcessingItems(final CompileContext context) {
    return ApplicationManager.getApplication().runReadAction((Computable<ProcessingItem[]>)() -> {
      final VirtualFile[] filesToCopy = getFilesToCopy(context);
      final ProcessingItem[] items = new ProcessingItem[filesToCopy.length];
      for (int idx = 0; idx < filesToCopy.length; idx++) {
        final VirtualFile file = filesToCopy[idx];
        items[idx] = new CopyItem(file, getDestinationPath(context, file));
      }
      return items;
    });
  }

  @Override
  public ProcessingItem[] process(CompileContext context, ProcessingItem[] items) {
    final List<ProcessingItem> successfullyProcessed = new ArrayList<ProcessingItem>(items.length);
    for (ProcessingItem item : items) {
      final CopyItem copyItem = (CopyItem)item;
      final String toPath = copyItem.getDestinationPath();
      try {
        if (isDirectoryCopying()) {
          FileUtil.copyDir(copyItem.getFile(), new File(toPath));
        }
        else {
          FileUtil.copy(copyItem.getFile(), new File(toPath));
        }

        successfullyProcessed.add(copyItem);
      }
      catch (IOException e) {
        context.addMessage(CompilerMessageCategory.ERROR, CompilerBundle.message("error.copying", item.getFile().getPath(), toPath, e.getMessage()), null, -1,
                           -1);
      }
    }
    return successfullyProcessed.toArray(new ProcessingItem[successfullyProcessed.size()]);
  }

  protected boolean isDirectoryCopying() {
    return false;
  }

  @Override
  @NotNull
  public String getDescription() {
    return CompilerBundle.message("file.copying.compiler.description");
  }

  @Override
  public boolean validateConfiguration(CompileScope scope) {
    return true;
  }

  @Override
  public ValidityState createValidityState(DataInput in) throws IOException {
    return new DestinationFileInfo(IOUtil.readString(in), true);
  }

  private static class CopyItem implements FileProcessingCompiler.ProcessingItem {
    private final File myFile;
    private final DestinationFileInfo myInfo;

    public CopyItem(@NotNull VirtualFile file, @NotNull String destinationPath) {
      myFile = VfsUtilCore.virtualToIoFile(file);
      myInfo = new DestinationFileInfo(destinationPath, new File(destinationPath).exists());
    }

    @Override
    @NotNull
    public File getFile() {
      return myFile;
    }

    @Override
    public ValidityState getValidityState() {
      return myInfo;
    }

    public String getDestinationPath() {
      return myInfo.getDestinationPath();
    }
  }

  private static class DestinationFileInfo implements ValidityState {
    private final String destinationPath;
    private final boolean myFileExists;

    public DestinationFileInfo(String destinationPath, boolean fileExists) {
      this.destinationPath = destinationPath;
      myFileExists = fileExists;
    }

    @Override
    public boolean equalsTo(ValidityState otherState) {
      if (!(otherState instanceof DestinationFileInfo)) {
        return false;
      }
      DestinationFileInfo destinationFileInfo = (DestinationFileInfo)otherState;
      return (myFileExists == destinationFileInfo.myFileExists) && (destinationPath.equals(destinationFileInfo.destinationPath));
    }

    @Override
    public void save(DataOutput out) throws IOException {
      IOUtil.writeString(destinationPath, out);
    }

    public String getDestinationPath() {
      return destinationPath;
    }
  }

}
