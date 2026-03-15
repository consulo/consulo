// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.execution.debug.stream.resolve;


import java.util.List;

/**
 * @author Vitaliy.Bibaev
 */
public interface ResolvedStreamChain {

  
  ResolvedStreamCall.Terminator getTerminator();

  
  List<ResolvedStreamCall.Intermediate> getIntermediateCalls();

  default int length() {
    return getIntermediateCalls().size() + 2;
  }
}
