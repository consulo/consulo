// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.execution.debug.stream.ui;

import consulo.disposer.Disposable;
import consulo.execution.debug.stream.trace.TraceElement;

import java.util.List;

/**
 * @author Vitaliy.Bibaev
 */
public interface TraceContainer extends Disposable {
  void highlight(List<TraceElement> elements);

  void select(List<TraceElement> elements);

  void addSelectionListener(ValuesSelectionListener listener);

  boolean highlightedExists();
}
