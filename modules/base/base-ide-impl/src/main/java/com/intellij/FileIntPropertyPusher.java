// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.impl.FilePropertyPusher;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.newvfs.FileAttribute;
import com.intellij.util.io.DataInputOutputUtil;
import javax.annotation.Nonnull;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public interface FileIntPropertyPusher<T> extends FilePropertyPusher<T> {
  @Nonnull
  FileAttribute getAttribute();

  int toInt(@Nonnull T property) throws IOException;

  @Nonnull
  T fromInt(int val) throws IOException;

  @Override
  default void persistAttribute(@Nonnull Project project, @Nonnull VirtualFile fileOrDir, @Nonnull T actualValue) throws IOException {
    try (DataInputStream stream = getAttribute().readAttribute(fileOrDir)) {
      if (stream != null) {
        int storedIntValue = DataInputOutputUtil.readINT(stream);
        if (storedIntValue == toInt(actualValue)) return;
      }
      else if (actualValue == getDefaultValue()) {
        return;
      }
    }

    try (DataOutputStream stream = getAttribute().writeAttribute(fileOrDir)) {
      DataInputOutputUtil.writeINT(stream, toInt(actualValue));
    }

    propertyChanged(project, fileOrDir, actualValue);
  }

  void propertyChanged(@Nonnull Project project, @Nonnull VirtualFile fileOrDir, @Nonnull T actualProperty);
}
