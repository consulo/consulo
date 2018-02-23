/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package com.intellij.openapi.vcs.changes.shelf;

import com.intellij.openapi.vcs.changes.CommitContext;
import com.intellij.openapi.vfs.CharsetToolkit;
import javax.annotation.Nonnull;

import java.io.*;

public class CompoundShelfFileProcessor {
  private final File shelfDir;

  public CompoundShelfFileProcessor(@Nonnull File shelfDir) {
    this.shelfDir = shelfDir;
  }

  public interface ContentProvider {
    void writeContentTo(@Nonnull Writer writer, @Nonnull CommitContext commitContext) throws IOException;
  }

  public void savePathFile(@Nonnull ContentProvider contentProvider,
                           @Nonnull final File patchPath,
                           @Nonnull CommitContext commitContext)
          throws IOException {
    OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(patchPath), CharsetToolkit.UTF8_CHARSET);
    try {
      contentProvider.writeContentTo(writer, commitContext);
    }
    finally {
      writer.close();
    }
  }

  @Nonnull
  public File getBaseDir() {
    return shelfDir;
  }
}