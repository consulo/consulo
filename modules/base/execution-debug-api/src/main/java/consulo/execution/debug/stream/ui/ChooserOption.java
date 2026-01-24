// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.execution.debug.stream.ui;


import consulo.document.util.TextRange;
import jakarta.annotation.Nonnull;

import java.util.stream.Stream;

/**
 * @author Vitaliy.Bibaev
 */
public interface ChooserOption {
  @Nonnull
  Stream<TextRange> rangeStream();

  @Nonnull
 
  String getText();
}
