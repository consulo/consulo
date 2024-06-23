// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.versionControlSystem.internal;

import consulo.disposer.Disposable;
import consulo.virtualFileSystem.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

public interface FilesProcessor extends Disposable {
  void processFiles(@NotNull Collection<VirtualFile> files);
}
