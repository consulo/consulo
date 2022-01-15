// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.actions.searcheverywhere;

import com.intellij.openapi.extensions.ExtensionPointName;
import javax.annotation.Nonnull;

import java.util.Collection;
import java.util.List;

public interface SEResultsEqualityProvider {

  ExtensionPointName<SEResultsEqualityProvider> EP_NAME = ExtensionPointName.create("com.intellij.searchEverywhereResultsEqualityProvider");

  enum SEEqualElementsActionType {
    DO_NOTHING,
    SKIP,
    REPLACE
  }

  @Nonnull
  SEEqualElementsActionType compareItems(@Nonnull SearchEverywhereFoundElementInfo newItem, @Nonnull SearchEverywhereFoundElementInfo alreadyFoundItem);

  @Nonnull
  static List<SEResultsEqualityProvider> getProviders() {
    return EP_NAME.getExtensionList();
  }

  @Nonnull
  static SEResultsEqualityProvider composite(@Nonnull Collection<? extends SEResultsEqualityProvider> providers) {
    return new SEResultsEqualityProvider() {
      @Nonnull
      @Override
      public SEEqualElementsActionType compareItems(@Nonnull SearchEverywhereFoundElementInfo newItem, @Nonnull SearchEverywhereFoundElementInfo alreadyFoundItem) {
        return providers.stream().map(provider -> provider.compareItems(newItem, alreadyFoundItem)).filter(action -> action != SEEqualElementsActionType.DO_NOTHING).findFirst()
                .orElse(SEEqualElementsActionType.DO_NOTHING);
      }
    };
  }
}
