
package com.intellij.openapi.paths;

import com.intellij.openapi.extensions.Extensions;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.ElementManipulators;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import consulo.logging.Logger;

import javax.annotation.Nonnull;

import java.util.List;

/**
 * @author Dmitry Avdeev
 */
public abstract class PathReferenceProviderBase implements PathReferenceProvider {

  private static final Logger LOG = Logger.getInstance(PathReferenceProviderBase.class);

  @Override
  public boolean createReferences(@Nonnull final PsiElement psiElement, final @Nonnull List<PsiReference> references, final boolean soft) {

    final TextRange range = ElementManipulators.getValueTextRange(psiElement);
    int offset = range.getStartOffset();
    int endOffset = range.getEndOffset();
    final String elementText = psiElement.getText();
    for (DynamicContextProvider provider: Extensions.getExtensions(DynamicContextProvider.EP_NAME)) {
      final int dynamicOffset = provider.getOffset(psiElement, offset, elementText);
      if (dynamicOffset == -1) {
        return false;
      } else if (dynamicOffset != offset) {
        offset = dynamicOffset;
      }
    }

    final int pos = getLastPosOfURL(offset, elementText);
    if (pos != -1 && pos < endOffset) {
      endOffset = pos;
    }
    try {
      final String text = elementText.substring(offset, endOffset);
      return createReferences(psiElement, offset, text, references, soft);
    } catch (StringIndexOutOfBoundsException e) {
      LOG.error("Cannot process string: '" + psiElement.getParent().getParent().getText() + "'", e);
      return false;
    }
  }

  public abstract boolean createReferences(@Nonnull final PsiElement psiElement,
                                  final int offset,
                                  String text,
                                  final @Nonnull List<PsiReference> references,
                                  final boolean soft);

  public static int getLastPosOfURL(final int offset, @Nonnull String url) {
    for (int i = offset; i < url.length(); i++) {
      switch (url.charAt(i)) {
        case '?':
        case '#':
          return i;
      }
    }
    return -1;
  }

}
