// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.execution.debug.stream.ui;

import jakarta.annotation.Nonnull;

import java.util.List;

/**
 * @author Vitaliy.Bibaev
 */
public interface ElementChooser<T extends ChooserOption> {

  void show(@Nonnull List<T> options, @Nonnull CallBack<T> callBack);

  interface CallBack<T> {
    void chosen(T element);
  }
}
