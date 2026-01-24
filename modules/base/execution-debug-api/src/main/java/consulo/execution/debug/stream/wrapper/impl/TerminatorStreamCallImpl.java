// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.execution.debug.stream.wrapper.impl;

import consulo.execution.debug.stream.trace.impl.handler.type.GenericType;
import consulo.execution.debug.stream.wrapper.CallArgument;
import consulo.execution.debug.stream.wrapper.StreamCallType;
import consulo.execution.debug.stream.wrapper.TerminatorStreamCall;
import consulo.document.util.TextRange;
import jakarta.annotation.Nonnull;

import java.util.List;

/**
 * @author Vitaliy.Bibaev
 */
public class TerminatorStreamCallImpl extends StreamCallImpl implements TerminatorStreamCall {
  private final GenericType myTypeBefore;
  private final GenericType myReturnType;
  private final Boolean myReturnsVoid;

  public TerminatorStreamCallImpl(@Nonnull String name,
                                  @Nonnull String genericArgs,
                                  @Nonnull List<CallArgument> args,
                                  @Nonnull GenericType typeBefore,
                                  @Nonnull GenericType resultType,
                                  @Nonnull TextRange range,
                                  @Nonnull Boolean returnsVoid
                                  ) {
    super(name, genericArgs, args, StreamCallType.TERMINATOR, range);
    myTypeBefore = typeBefore;
    myReturnType = resultType;
    myReturnsVoid = returnsVoid;
  }

  @Override
  public @Nonnull GenericType getTypeBefore() {
    return myTypeBefore;
  }

  @Override
  public @Nonnull GenericType getResultType() {
    return myReturnType;
  }

  @Override
  public Boolean returnsVoid() {
    return myReturnsVoid;
  }
}
