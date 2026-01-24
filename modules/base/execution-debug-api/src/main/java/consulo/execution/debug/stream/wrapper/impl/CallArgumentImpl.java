// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.execution.debug.stream.wrapper.impl;

import consulo.execution.debug.stream.wrapper.CallArgument;
import jakarta.annotation.Nonnull;

/**
 * @author Vitaliy.Bibaev
 */
public class CallArgumentImpl implements CallArgument {
  private final @Nonnull String myType;
  private final @Nonnull String myText;

  public CallArgumentImpl(@Nonnull String type, @Nonnull String text) {
    myType = type;
    myText = text;
  }

  @Override
  public @Nonnull String getType() {
    return myType;
  }

  @Override
  public @Nonnull String getText() {
    return myText;
  }
}
