// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.execution.debug.stream.wrapper.impl;

import consulo.execution.debug.stream.trace.impl.handler.type.GenericType;
import consulo.execution.debug.stream.wrapper.CallArgument;
import consulo.execution.debug.stream.wrapper.IntermediateStreamCall;
import consulo.execution.debug.stream.wrapper.StreamCallType;
import consulo.document.util.TextRange;

import java.util.List;

/**
 * @author Vitaliy.Bibaev
 */
public class IntermediateStreamCallImpl extends StreamCallImpl implements IntermediateStreamCall {

  private final GenericType myTypeBefore;
  private final GenericType myTypeAfter;

  public IntermediateStreamCallImpl(String name,
                                    String genericArgs,
                                    List<CallArgument> args,
                                    GenericType typeBefore,
                                    GenericType typeAfter,
                                    TextRange range) {
    super(name, genericArgs, args, StreamCallType.INTERMEDIATE, range);
    myTypeBefore = typeBefore;
    myTypeAfter = typeAfter;
  }

  @Override
  public GenericType getTypeBefore() {
    return myTypeBefore;
  }

  @Override
  public GenericType getTypeAfter() {
    return myTypeAfter;
  }
}
