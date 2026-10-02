package consulo.endpoint.impl.internal.view.documentation;

import consulo.language.psi.PsiElement;
import consulo.language.psi.SmartPsiElementPointer;
import org.jspecify.annotations.Nullable;

record EndpointElementDocumentation(
    SmartPsiElementPointer<PsiElement> pointer,
    @Nullable SmartPsiElementPointer<PsiElement> originalPointer,
    @Nullable SmartPsiElementPointer<PsiElement> navigationPointer,
    @Nullable String html
) implements EndpointDocumentationContent {

    EndpointElementDocumentation withHtml(@Nullable String newHtml) {
        return new EndpointElementDocumentation(pointer, originalPointer, navigationPointer, newHtml);
    }
}
