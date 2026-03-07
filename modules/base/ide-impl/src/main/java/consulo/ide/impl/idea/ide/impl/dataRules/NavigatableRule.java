package consulo.ide.impl.idea.ide.impl.dataRules;

import consulo.dataContext.DataSnapshot;
import consulo.language.editor.PlatformDataKeys;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiNavigationSupport;
import consulo.navigation.Navigatable;
import consulo.navigation.OpenFileDescriptor;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public final class NavigatableRule {
    @Nullable
    static Navigatable getData(@Nonnull DataSnapshot dataProvider) {
        Navigatable navigatable = dataProvider.get(Navigatable.KEY);
        if (navigatable != null && navigatable instanceof OpenFileDescriptor openFileDescriptor) {
            if (openFileDescriptor.getFile().isValid()) {
                return openFileDescriptor;
            }
        }
        PsiElement element = dataProvider.get(PsiElement.KEY);
        if (element instanceof Navigatable navElem) {
            return navElem;
        }

        if (element != null) {
            return PsiNavigationSupport.getInstance().getDescriptor(element);
        }

        Object selection = dataProvider.get(PlatformDataKeys.SELECTED_ITEM);
        return selection instanceof Navigatable navSel ? navSel : null;
    }
}
