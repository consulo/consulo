// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.endpoint.url.inlay;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.codeEditor.Editor;
import consulo.codeEditor.event.EditorMouseEvent;
import consulo.endpoint.url.reference.UrlPathContext;
import consulo.language.psi.PsiFile;
import consulo.localize.LocalizeValue;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.image.Image;

@ExtensionAPI(ComponentScope.APPLICATION)
public interface UrlPathInlayAction {
    Image getIcon();

    LocalizeValue getName();

    @RequiredUIAccess
    default void actionPerformed(PsiFile file, Editor editor, UrlPathContext urlPathContext, EditorMouseEvent mouseEvent) {
    }

    @RequiredUIAccess
    default void actionPerformed(PsiFile file, Editor editor, UrlPathInlayHint urlPathInlayHint, EditorMouseEvent mouseEvent) {
        actionPerformed(file, editor, urlPathInlayHint.getContext(), mouseEvent);
    }

    @RequiredReadAction
    boolean isAvailable(PsiFile file, UrlPathInlayHint urlPathInlayHint);
}
