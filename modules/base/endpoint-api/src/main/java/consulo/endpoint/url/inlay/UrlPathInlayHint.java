// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.endpoint.url.inlay;

import consulo.annotation.access.RequiredReadAction;
import consulo.endpoint.internal.UrlPathInlayActionService;
import consulo.endpoint.url.reference.UrlPathContext;
import consulo.language.editor.inlay.DeclarativePresentationTreeBuilder;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.SmartPsiElementPointer;
import org.jspecify.annotations.Nullable;

import java.util.List;

public interface UrlPathInlayHint {
    int getOffset();

    int getPriority();

    Style getStyle();

    @RequiredReadAction
    default List<UrlPathInlayAction> getAvailableActions(PsiFile file) {
        return UrlPathInlayActionService.getInstance().getAvailableActions(file, this);
    }

    default void buildPresentation(DeclarativePresentationTreeBuilder builder) {
        UrlPathInlayActionService.getInstance().buildPresentation(builder);
    }

    UrlPathContext getContext();

    default @Nullable SmartPsiElementPointer<PsiElement> getAttachedTo() {
        return null;
    }

    enum Style {
        BLOCK,
        INLINE
    }
}
