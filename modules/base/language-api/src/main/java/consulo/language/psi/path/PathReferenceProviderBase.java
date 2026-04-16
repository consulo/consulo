package consulo.language.psi.path;

import consulo.annotation.access.RequiredReadAction;
import consulo.document.util.TextRange;
import consulo.language.psi.ElementManipulators;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiReference;
import consulo.logging.Logger;

import java.util.List;

/**
 * @author Dmitry Avdeev
 */
public abstract class PathReferenceProviderBase implements PathReferenceProvider {
    private static final Logger LOG = Logger.getInstance(PathReferenceProviderBase.class);

    @Override
    @RequiredReadAction
    public boolean createReferences(PsiElement psiElement, List<PsiReference> references, boolean soft) {
        TextRange range = ElementManipulators.getValueTextRange(psiElement);
        int offset = range.getStartOffset();
        int endOffset = range.getEndOffset();
        String elementText = psiElement.getText();
        for (DynamicContextProvider provider : DynamicContextProvider.EP_NAME.getExtensionList()) {
            int dynamicOffset = provider.getOffset(psiElement, offset, elementText);
            if (dynamicOffset == -1) {
                return false;
            }
            else if (dynamicOffset != offset) {
                offset = dynamicOffset;
            }
        }

        int pos = getLastPosOfURL(offset, elementText);
        if (pos != -1 && pos < endOffset) {
            endOffset = pos;
        }
        try {
            String text = elementText.substring(offset, endOffset);
            return createReferences(psiElement, offset, text, references, soft);
        }
        catch (StringIndexOutOfBoundsException e) {
            LOG.error("Cannot process string: '" + psiElement.getRequiredParent().getRequiredParent().getText() + "'", e);
            return false;
        }
    }

    public abstract boolean createReferences(
        PsiElement psiElement,
        int offset,
        String text,
        List<PsiReference> references,
        boolean soft
    );

    public static int getLastPosOfURL(int offset, String url) {
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
