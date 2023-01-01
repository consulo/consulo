// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.module.content;

import consulo.index.io.data.DataInputOutputUtil;
import consulo.project.Project;
import consulo.virtualFileSystem.FileAttribute;
import consulo.virtualFileSystem.VirtualFile;

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
