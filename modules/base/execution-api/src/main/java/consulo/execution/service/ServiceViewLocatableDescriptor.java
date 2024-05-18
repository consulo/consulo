// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.execution.service;

import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nullable;

public interface ServiceViewLocatableDescriptor {
  @Nullable
  default VirtualFile getVirtualFile() {
    return null;
  }
}
