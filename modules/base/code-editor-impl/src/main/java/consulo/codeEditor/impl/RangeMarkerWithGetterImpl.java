// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.codeEditor.impl;

import consulo.document.internal.DocumentEx;
import consulo.document.impl.RangeMarkerImpl;

import javax.annotation.Nonnull;
import java.util.function.Supplier;

/**
 * @see HardReferencingRangeMarkerTree
 */
class RangeMarkerWithGetterImpl extends RangeMarkerImpl implements Supplier<RangeMarkerWithGetterImpl> {
  RangeMarkerWithGetterImpl(@Nonnull DocumentEx document, int start, int end, boolean register) {
    super(document, start, end, register, true);
  }

  @Override
  public final RangeMarkerWithGetterImpl get() {
    return this;
  }
}
