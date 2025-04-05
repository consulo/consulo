// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.ide.actions.searcheverywhere;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.component.extension.ExtensionPointName;
import jakarta.annotation.Nonnull;

import java.util.Collection;
import java.util.List;

@ExtensionAPI(ComponentScope.APPLICATION)
public interface SEResultsEqualityProvider {
    ExtensionPointName<SEResultsEqualityProvider> EP_NAME = ExtensionPointName.create(SEResultsEqualityProvider.class);

    enum SEEqualElementsActionType {
        DO_NOTHING,
        SKIP,
        REPLACE
    }

    @Nonnull
    SEEqualElementsActionType compareItems(
        @Nonnull SearchEverywhereFoundElementInfo newItem,
        @Nonnull SearchEverywhereFoundElementInfo alreadyFoundItem
    );

    @Nonnull
    static List<SEResultsEqualityProvider> getProviders() {
        return EP_NAME.getExtensionList();
    }

    @Nonnull
    static SEResultsEqualityProvider composite(@Nonnull Collection<? extends SEResultsEqualityProvider> providers) {
        return new SEResultsEqualityProvider() {
            @Nonnull
            @Override
            public SEEqualElementsActionType compareItems(
                @Nonnull SearchEverywhereFoundElementInfo newItem,
                @Nonnull SearchEverywhereFoundElementInfo alreadyFoundItem
            ) {
                return providers.stream()
                    .map(provider -> provider.compareItems(newItem, alreadyFoundItem))
                    .filter(action -> action != SEEqualElementsActionType.DO_NOTHING)
                    .findFirst()
                    .orElse(SEEqualElementsActionType.DO_NOTHING);
            }
        };
    }
}
