// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.execution.debug.stream.resolve;

import consulo.execution.debug.stream.trace.TraceElement;
import consulo.execution.debug.stream.trace.TraceInfo;

import java.util.List;
import java.util.Map;

/**
 * @author Vitaliy.Bibaev
 */
public interface ValuesOrderResolver {
  
  Result resolve(TraceInfo info);

  interface Result {
    
    Map<TraceElement, List<TraceElement>> getDirectOrder();

    
    Map<TraceElement, List<TraceElement>> getReverseOrder();

    static Result of(Map<TraceElement, List<TraceElement>> direct, Map<TraceElement, List<TraceElement>> reverse) {
      return new Result() {
        @Override
        public Map<TraceElement, List<TraceElement>> getDirectOrder() {
          return direct;
        }

        @Override
        public Map<TraceElement, List<TraceElement>> getReverseOrder() {
          return reverse;
        }
      };
    }
  }
}
