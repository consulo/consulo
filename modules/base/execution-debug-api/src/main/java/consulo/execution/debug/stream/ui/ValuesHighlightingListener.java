// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.execution.debug.stream.ui;

import consulo.execution.debug.stream.trace.TraceElement;
import jakarta.annotation.Nonnull;

import java.util.EventListener;
import java.util.List;

/**
 * @author Vitaliy.Bibaev
 */
public interface ValuesHighlightingListener extends EventListener {
  void highlightingChanged(@Nonnull List<TraceElement> values, @Nonnull PropagationDirection direction);
}
