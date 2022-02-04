// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.actions.searcheverywhere;

import javax.annotation.Nonnull;

import java.util.Objects;

public class TrivialElementsEqualityProvider implements SEResultsEqualityProvider {

  @Nonnull
  @Override
  public SEEqualElementsActionType compareItems(@Nonnull SearchEverywhereFoundElementInfo newItem, @Nonnull SearchEverywhereFoundElementInfo alreadyFoundItem) {
    if (Objects.equals(newItem.getElement(), alreadyFoundItem.getElement())) {
      return newItem.getPriority() > alreadyFoundItem.getPriority() ? SEEqualElementsActionType.REPLACE : SEEqualElementsActionType.SKIP;
    }
    return SEEqualElementsActionType.DO_NOTHING;
  }
}
