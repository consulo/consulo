// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.editor.action;

import consulo.annotation.UsedInPlugin;

@UsedInPlugin
public abstract class EnterBetweenBracesAndBracketsDelegate extends EnterBetweenBracesDelegate {
  @Override
  public boolean isBracePair(char lBrace, char rBrace) {
    return super.isBracePair(lBrace, rBrace) || (lBrace == '[' && rBrace == ']');
  }
}
