// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.endpoint.internal;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.application.Application;
import consulo.endpoint.url.inlay.UrlPathInlayAction;
import consulo.endpoint.url.inlay.UrlPathInlayHint;
import consulo.language.editor.inlay.DeclarativePresentationTreeBuilder;
import consulo.language.psi.PsiFile;

import java.util.List;

@ServiceAPI(ComponentScope.APPLICATION)
public interface UrlPathInlayActionService {
    static UrlPathInlayActionService getInstance() {
        return Application.get().getInstance(UrlPathInlayActionService.class);
    }

    @RequiredReadAction
    List<UrlPathInlayAction> getAvailableActions(PsiFile file, UrlPathInlayHint hint);

    void buildPresentation(DeclarativePresentationTreeBuilder builder);
}
