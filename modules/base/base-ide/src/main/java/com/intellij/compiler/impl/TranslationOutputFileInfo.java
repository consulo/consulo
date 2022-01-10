/*
 * Copyright 2013-2019 consulo.io
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
package com.intellij.compiler.impl;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.vfs.newvfs.FileAttribute;
import com.intellij.util.indexing.FileBasedIndex;
import consulo.logging.Logger;
import consulo.util.lang.StringUtil;

import javax.annotation.Nullable;
import java.io.*;

public class TranslationOutputFileInfo {
  private static final Logger LOG = Logger.getInstance(TranslationOutputFileInfo.class);
  private static final FileAttribute ourOutputFileAttribute = new FileAttribute("_make_output_file_info_", 4, true);

  @Nullable
  public static TranslationOutputFileInfo loadOutputInfo(final VirtualFile file) {
    try {
      final DataInputStream is = ourOutputFileAttribute.readAttribute(file);
      if (is != null) {
        try {
          return new TranslationOutputFileInfo(is);
        }
        finally {
          is.close();
        }
      }
    }
    catch (RuntimeException e) {
      final Throwable cause = e.getCause();
      if (cause instanceof IOException) {
        LOG.info(e); // ignore IO exceptions
      }
      else {
        throw e;
      }
    }
    catch (IOException ignored) {
      LOG.info(ignored);
    }
    return null;
  }

  public static void saveOutputInfo(VirtualFile file, TranslationOutputFileInfo descriptor) {
    try {
      try (DataOutputStream out = ourOutputFileAttribute.writeAttribute(file)) {
        descriptor.save(out);
      }
    }
    catch (IOException ignored) {
      LOG.info(ignored);
    }
  }

  private final int mySourcePath;

  private final String myClassName;

  TranslationOutputFileInfo(final VirtualFile sourcePath, @Nullable String className) throws IOException {
    mySourcePath = FileBasedIndex.getFileId(sourcePath);
    myClassName = className;
  }

  TranslationOutputFileInfo(final DataInput in) throws IOException {
    mySourcePath = in.readInt();
    myClassName = StringUtil.nullize(in.readUTF());
  }

  @Nullable
  public VirtualFile getSourceFile() {
    return VirtualFileManager.getInstance().findFileById(mySourcePath);
  }

  @Nullable
  public String getClassName() {
    return myClassName;
  }

  public void save(final DataOutput out) throws IOException {
    out.writeInt(mySourcePath);
    out.writeUTF(myClassName == null ? "" : myClassName);
  }
}
