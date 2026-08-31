// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.language.editor.documentation;

import consulo.annotation.access.RequiredReadAction;
import consulo.document.util.TextRange;
import consulo.language.editor.internal.DocumentationManagerHelper;
import consulo.language.psi.PsiDocCommentBase;
import consulo.language.psi.PsiElement;
import org.jspecify.annotations.Nullable;

public final class PsiCommentInlineDocumentation implements InlineDocumentation {

    private final PsiDocCommentBase myComment; // might be fake

    public PsiCommentInlineDocumentation(PsiDocCommentBase comment) {
        myComment = comment;
    }

    public PsiDocCommentBase getComment() {
        return myComment;
    }

    public PsiElement getContext() {
        PsiElement owner = myComment.getOwner();
        return owner == null ? myComment : owner;
    }

    @Override
    public TextRange getDocumentationRange() {
        return myComment.getTextRange(); // fake comments are still expected to return text range
    }

    @Override
    public @Nullable TextRange getDocumentationOwnerRange() {
        PsiElement owner = myComment.getOwner();
        return owner == null ? null : owner.getTextRange();
    }

    @RequiredReadAction
    @Override
    public @Nullable String renderText() {
        return DocumentationManagerHelper.getProviderFromElement(myComment).generateRenderedDoc(myComment);
    }
}
