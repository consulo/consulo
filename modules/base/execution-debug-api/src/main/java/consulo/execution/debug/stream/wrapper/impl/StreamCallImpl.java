// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.execution.debug.stream.wrapper.impl;

import consulo.execution.debug.stream.wrapper.CallArgument;
import consulo.execution.debug.stream.wrapper.StreamCall;
import consulo.execution.debug.stream.wrapper.StreamCallType;

import consulo.document.util.TextRange;
import jakarta.annotation.Nonnull;

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

  StreamCallImpl(@Nonnull String name,
                 @Nonnull String genericArgs,
                 @Nonnull List<CallArgument> args,
                 @Nonnull StreamCallType type,
                 @Nonnull TextRange range) {
    myName = name;
    myGenericArgs = genericArgs;
    myArgs = args;
    myType = type;
    myTextRange = range;
  }

  @Override
  public @Nonnull TextRange getTextRange() {
    return myTextRange;
  }

  @Override
  public @Nonnull String getName() {
    return myName;
  }

  @Override
  public @Nonnull String getGenericArguments() {
    return myGenericArgs;
  }

  @Override
  public @Nonnull List<CallArgument> getArguments() {
    return myArgs;
  }

  @Override
  public @Nonnull StreamCallType getType() {
    return myType;
  }
}
