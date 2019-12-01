package com.intellij.dupLocator.equivalence;

import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.TokenSet;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author Eugene.Kudelevsky
 */
public abstract class EquivalenceDescriptorProvider {
  public static final ExtensionPointName<EquivalenceDescriptorProvider> EP_NAME =
    ExtensionPointName.create("com.intellij.equivalenceDescriptorProvider");

  // for using in tests only !!!
  public static boolean ourUseDefaultEquivalence = false;

  public abstract boolean isMyContext(@Nonnull PsiElement context);

  @Nullable
  public abstract EquivalenceDescriptor buildDescriptor(@Nonnull PsiElement element);

  // by default only PsiWhitespace ignored
  public TokenSet getIgnoredTokens() {
    return TokenSet.EMPTY;
  }

  @Nullable
  public static EquivalenceDescriptorProvider getInstance(@Nonnull PsiElement context) {
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
