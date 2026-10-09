package consulo.endpoint.impl.internal.view.documentation;

import consulo.language.psi.PsiElement;
import consulo.language.psi.SmartPsiElementPointer;
import org.jspecify.annotations.Nullable;

record EndpointElementDocumentation(
    SmartPsiElementPointer<PsiElement> pointer,
    @Nullable SmartPsiElementPointer<PsiElement> navigationPointer
) implements EndpointDocumentationContent {
}
