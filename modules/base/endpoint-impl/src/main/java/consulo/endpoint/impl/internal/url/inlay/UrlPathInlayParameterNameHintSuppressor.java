// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.endpoint.impl.internal.url.inlay;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.language.editor.inlay.InlayInfo;
import consulo.language.editor.inlay.ParameterNameHintsSuppressor;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;

@ExtensionImpl
public final class UrlPathInlayParameterNameHintSuppressor implements ParameterNameHintsSuppressor {
    private static final int URL_PATH_INLAY_SEARCH_LIMIT = 3;

    @Override
    @RequiredReadAction
    public boolean isSuppressedFor(PsiFile file, InlayInfo inlayInfo) {
        if (!UrlPathDeclarativeInlayHintsProvider.isUrlPathInlaysEnabledForLanguage(file.getLanguage())) {
            return false;
        }
        PsiElement element = file.findElementAt(inlayInfo.getOffset());
        if (element == null) {
            return false;
        }
        return UrlPathDeclarativeInlayHintsProvider.shouldHaveUrlPathInlayAroundOffset(
            element,
            inlayInfo.getOffset(),
            URL_PATH_INLAY_SEARCH_LIMIT
        );
    }
}
