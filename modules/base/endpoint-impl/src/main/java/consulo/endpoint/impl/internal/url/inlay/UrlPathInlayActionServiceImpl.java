// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.endpoint.impl.internal.url.inlay;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.endpoint.internal.UrlPathInlayActionService;
import consulo.endpoint.url.inlay.UrlPathInlayAction;
import consulo.endpoint.url.inlay.UrlPathInlayHint;
import consulo.language.editor.inlay.DeclarativePresentationTreeBuilder;
import consulo.language.psi.PsiFile;
import consulo.platform.base.icon.PlatformIconGroup;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.util.List;

@ServiceImpl
@Singleton
public final class UrlPathInlayActionServiceImpl implements UrlPathInlayActionService {
    private final Application myApplication;

    @Inject
    public UrlPathInlayActionServiceImpl(Application application) {
        myApplication = application;
    }

    @Override
    @RequiredReadAction
    public List<UrlPathInlayAction> getAvailableActions(PsiFile file, UrlPathInlayHint hint) {
        return myApplication.getExtensionPoint(UrlPathInlayAction.class).collectFiltered(it -> it.isAvailable(file, hint));
    }

    @Override
    public void buildPresentation(DeclarativePresentationTreeBuilder builder) {
        builder.icon(PlatformIconGroup.actionsInlayglobe());
        builder.icon(PlatformIconGroup.actionsInlaydroptriangle());
    }
}
