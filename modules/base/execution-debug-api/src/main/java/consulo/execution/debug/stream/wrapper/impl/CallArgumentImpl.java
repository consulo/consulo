// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.execution.debug.stream.wrapper.impl;

import consulo.execution.debug.stream.wrapper.CallArgument;

/**
 * @author Vitaliy.Bibaev
 */
public class CallArgumentImpl implements CallArgument {
  private final String myType;
  private final String myText;

  public CallArgumentImpl(String type, String text) {
    myType = type;
    myText = text;
  }

  @Override
  public String getType() {
    return myType;
  }

  @Override
  public String getText() {
    return myText;
  }
}
