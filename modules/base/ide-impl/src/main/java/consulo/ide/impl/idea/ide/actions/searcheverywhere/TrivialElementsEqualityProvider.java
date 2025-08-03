// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.ide.actions.searcheverywhere;

import consulo.annotation.component.ExtensionImpl;

import consulo.searchEverywhere.SEResultsEqualityProvider;
import consulo.searchEverywhere.SearchEverywhereFoundElementInfo;
import jakarta.annotation.Nonnull;

import java.util.Objects;

@ExtensionImpl
public class TrivialElementsEqualityProvider implements SEResultsEqualityProvider {
    @Nonnull
    @Override
    public SEEqualElementsActionType compareItems(
        @Nonnull SearchEverywhereFoundElementInfo newItem,
        @Nonnull SearchEverywhereFoundElementInfo alreadyFoundItem
    ) {
        if (Objects.equals(newItem.getElement(), alreadyFoundItem.getElement())) {
            return newItem.getPriority() > alreadyFoundItem.getPriority()
                ? SEEqualElementsActionType.REPLACE
                : SEEqualElementsActionType.SKIP;
        }
        return SEEqualElementsActionType.DO_NOTHING;
    }
}
