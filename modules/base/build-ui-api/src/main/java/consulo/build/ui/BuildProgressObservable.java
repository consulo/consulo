// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.build.ui;

import consulo.build.ui.progress.BuildProgressListener;
import consulo.disposer.Disposable;

import jakarta.annotation.Nonnull;

//@ApiStatus.Experimental
public interface BuildProgressObservable {
  void addListener(@Nonnull BuildProgressListener listener, @Nonnull Disposable disposable);
}
