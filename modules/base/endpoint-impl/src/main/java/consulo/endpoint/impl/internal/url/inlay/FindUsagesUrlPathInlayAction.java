// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.endpoint.impl.internal.url.inlay;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.application.Application;
import consulo.application.ReadAction;
import consulo.application.util.concurrent.AppExecutorUtil;
import consulo.codeEditor.Editor;
import consulo.codeEditor.event.EditorMouseEvent;
import consulo.endpoint.localize.EndpointLocalize;
import consulo.endpoint.url.inlay.UrlPathInlayAction;
import consulo.endpoint.url.inlay.UrlPathInlayHint;
import consulo.endpoint.url.reference.UrlPathContext;
import consulo.endpoint.url.reference.UrlPathReference;
import consulo.language.psi.NavigatablePsiElement;
import consulo.language.psi.PsiFile;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.image.Image;
import org.jspecify.annotations.Nullable;

@ExtensionImpl(order = "first")
public final class FindUsagesUrlPathInlayAction implements UrlPathInlayAction {
    @Override
    public Image getIcon() {
        return PlatformIconGroup.javaeeWebservice();
    }

    @Override
    public LocalizeValue getName() {
        return EndpointLocalize.microservicesInlayFindUsagesUrlPath();
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(PsiFile file, Editor editor, UrlPathContext urlPathContext, EditorMouseEvent mouseEvent) {
        Project project = editor.getProject();
        if (project == null) {
            return;
        }

        ReadAction.nonBlocking(() -> createSearchableElement(project, urlPathContext))
            .inSmartMode(file.getProject())
            .coalesceBy(file, FindUsagesUrlPathInlayAction.class)
            .finishOnUiThread(Application::getDefaultModalityState, element -> {
                if (element != null) {
                    element.navigate(true);
                }
            })
            .submit(AppExecutorUtil.getAppExecutorService());
    }

    @RequiredReadAction
    private static @Nullable NavigatablePsiElement createSearchableElement(Project project, UrlPathContext urlPathContext) {
        if (!urlPathContext.getResolveRequests().iterator().hasNext()) {
            return null;
        }
        return UrlPathReference.createSearchableElement(project, urlPathContext);
    }

    @Override
    @RequiredReadAction
    public boolean isAvailable(PsiFile file, UrlPathInlayHint urlPathInlayHint) {
        return true;
    }
}
