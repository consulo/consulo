// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.execution.debug.stream.resolve.impl;

import consulo.execution.debug.stream.resolve.ResolvedStreamCall;
import consulo.execution.debug.stream.resolve.ResolvedStreamChain;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Vitaliy.Bibaev
 */
public class ResolvedStreamChainImpl implements ResolvedStreamChain {

  private final ResolvedStreamCall.Terminator myTerminator;
  private final List<ResolvedStreamCall.Intermediate> myIntermediateCalls;

  ResolvedStreamChainImpl(ResolvedStreamCall.Terminator terminator,
                          List<ResolvedStreamCall.Intermediate> intermediates) {
    myTerminator = terminator;
    myIntermediateCalls = List.copyOf(intermediates);
  }

  @Override
  public ResolvedStreamCall.Terminator getTerminator() {
    return myTerminator;
  }

  @Override
  public List<ResolvedStreamCall.Intermediate> getIntermediateCalls() {
    return myIntermediateCalls;
  }

  public static class Builder {
    private final List<ResolvedStreamCall.Intermediate> myIntermediates = new ArrayList<>();
    private ResolvedStreamCall.Terminator myTerminator;

    public void addIntermediate(ResolvedStreamCall.Intermediate intermediate) {
      myIntermediates.add(intermediate);
    }

    public void setTerminator(ResolvedStreamCall.Terminator terminator) {
      myTerminator = terminator;
    }

    public ResolvedStreamChain build() {
      if (myTerminator == null) {
        throw new IllegalStateException("terminator not specified");
      }

      return new ResolvedStreamChainImpl(myTerminator, myIntermediates);
    }
  }
}
