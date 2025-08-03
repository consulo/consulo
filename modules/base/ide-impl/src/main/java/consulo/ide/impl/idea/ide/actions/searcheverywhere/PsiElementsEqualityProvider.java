// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.ide.actions.searcheverywhere;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiElementNavigationItem;
import consulo.searchEverywhere.SEResultsEqualityProvider;
import consulo.searchEverywhere.SearchEverywhereFoundElementInfo;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Objects;

@ExtensionImpl(order = "first")
public class PsiElementsEqualityProvider implements SEResultsEqualityProvider {
    @Nonnull
    @Override
    public SEEqualElementsActionType compareItems(
        @Nonnull SearchEverywhereFoundElementInfo newItemInfo,
        @Nonnull SearchEverywhereFoundElementInfo alreadyFoundItemInfo
    ) {
        PsiElement newElementPsi = toPsi(newItemInfo.getElement());
        PsiElement alreadyFoundPsi = toPsi(alreadyFoundItemInfo.getElement());

        if (newElementPsi == null || alreadyFoundPsi == null) {
            return SEEqualElementsActionType.DO_NOTHING;
        }

        if (Objects.equals(newElementPsi, alreadyFoundPsi)) {
            return newItemInfo.priority > alreadyFoundItemInfo.priority
                ? SEEqualElementsActionType.REPLACE
                : SEEqualElementsActionType.SKIP;
        }

        return SEEqualElementsActionType.DO_NOTHING;
    }

    @Nullable
    public static PsiElement toPsi(Object o) {
        return o instanceof PsiElement psiElement
            ? psiElement
            : o instanceof PsiElementNavigationItem psiElementNavigationItem
            ? psiElementNavigationItem.getTargetElement()
            : null;
    }
}
