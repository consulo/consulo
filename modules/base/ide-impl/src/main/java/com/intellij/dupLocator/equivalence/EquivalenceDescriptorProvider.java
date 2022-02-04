package com.intellij.dupLocator.equivalence;

import consulo.component.extension.ExtensionPointName;
import consulo.language.psi.PsiElement;
import consulo.language.ast.TokenSet;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author Eugene.Kudelevsky
 */
public abstract class EquivalenceDescriptorProvider {
  public static final ExtensionPointName<EquivalenceDescriptorProvider> EP_NAME =
    ExtensionPointName.create("consulo.equivalenceDescriptorProvider");

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
