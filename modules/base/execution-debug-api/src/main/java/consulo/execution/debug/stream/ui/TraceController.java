// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.execution.debug.stream.ui;

import consulo.execution.debug.stream.trace.TraceElement;
import consulo.execution.debug.stream.trace.Value;
import consulo.execution.debug.stream.wrapper.StreamCall;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * @author Vitaliy.Bibaev
 */
public interface TraceController extends ValuesHighlightingListener {
  @Nullable Value getStreamResult();

  
  List<TraceElement> getTrace();

  @Nullable StreamCall getNextCall();

  @Nullable StreamCall getPrevCall();

  
  List<TraceElement> getNextValues(TraceElement element);

  
  List<TraceElement> getPrevValues(TraceElement element);

  default boolean isSelectionExists() {
    return isSelectionExists(PropagationDirection.BACKWARD) || isSelectionExists(PropagationDirection.FORWARD);
  }

  boolean isSelectionExists(PropagationDirection direction);

  void register(TraceContainer listener);
}
