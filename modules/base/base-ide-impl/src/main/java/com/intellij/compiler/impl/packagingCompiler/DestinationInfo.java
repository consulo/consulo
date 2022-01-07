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
package com.intellij.compiler.impl.packagingCompiler;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.LocalFileSystem;
import javax.annotation.Nullable;
import javax.annotation.Nonnull;

/**
 * @author nik
 */
public abstract class DestinationInfo {
  private VirtualFile myOutputFile;
  private final String myOutputPath;
  private final String myOutputFilePath;

  protected DestinationInfo(@Nonnull final String outputPath, @Nullable final VirtualFile outputFile, @Nonnull String outputFilePath) {
    myOutputFilePath = outputFilePath;
    myOutputFile = outputFile;
    myOutputPath = outputPath;
  }

  @Nonnull
  public String getOutputPath() {
    return myOutputPath;
  }

  @Nullable
  public VirtualFile getOutputFile() {
    return myOutputFile;
  }

  @Nonnull
  public String getOutputFilePath() {
    return myOutputFilePath;
  }

  public void update() {
    if (myOutputFile != null && !myOutputFile.isValid()) {
      myOutputFile = null;
    }
    if (myOutputFile == null) {
      myOutputFile = LocalFileSystem.getInstance().findFileByPath(myOutputFilePath);
    }
  }
}
