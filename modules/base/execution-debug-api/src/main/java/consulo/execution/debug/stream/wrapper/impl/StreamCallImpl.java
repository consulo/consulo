// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.execution.debug.stream.wrapper.impl;

import consulo.execution.debug.stream.wrapper.CallArgument;
import consulo.execution.debug.stream.wrapper.StreamCall;
import consulo.execution.debug.stream.wrapper.StreamCallType;

import consulo.document.util.TextRange;

import java.util.List;

/**
 * @author Vitaliy.Bibaev
 */
public abstract class StreamCallImpl implements StreamCall {
  private final String myName;
  private final String myGenericArgs;
  private final List<CallArgument> myArgs;
  private final StreamCallType myType;
  private final TextRange myTextRange;

  StreamCallImpl(String name,
                 String genericArgs,
                 List<CallArgument> args,
                 StreamCallType type,
                 TextRange range) {
    myName = name;
    myGenericArgs = genericArgs;
    myArgs = args;
    myType = type;
    myTextRange = range;
  }

  @Override
  public TextRange getTextRange() {
    return myTextRange;
  }

  @Override
  public String getName() {
    return myName;
  }

  @Override
  public String getGenericArguments() {
    return myGenericArgs;
  }

  @Override
  public List<CallArgument> getArguments() {
    return myArgs;
  }

  @Override
  public StreamCallType getType() {
    return myType;
  }
}
