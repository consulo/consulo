package consulo.language.duplicateAnalysis.equivalence;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.component.extension.ExtensionPointName;
import consulo.language.psi.PsiElement;
import consulo.language.ast.TokenSet;
import org.jspecify.annotations.Nullable;

/**
 * @author Eugene.Kudelevsky
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public abstract class EquivalenceDescriptorProvider {
    public static final ExtensionPointName<EquivalenceDescriptorProvider> EP_NAME =
        ExtensionPointName.create(EquivalenceDescriptorProvider.class);

    // for using in tests only !!!
    public static boolean ourUseDefaultEquivalence = false;

    public abstract boolean isMyContext(PsiElement context);

    public abstract @Nullable EquivalenceDescriptor buildDescriptor(PsiElement element);

    // by default only PsiWhitespace ignored
    public TokenSet getIgnoredTokens() {
        return TokenSet.EMPTY;
    }

    public static @Nullable EquivalenceDescriptorProvider getInstance(PsiElement context) {
        if (ourUseDefaultEquivalence) {
            return null;
        }

        for (EquivalenceDescriptorProvider descriptorProvider : EP_NAME.getExtensions()) {
            if (descriptorProvider.isMyContext(context)) {
                return descriptorProvider;
            }
        }
        return null;
    }
}
