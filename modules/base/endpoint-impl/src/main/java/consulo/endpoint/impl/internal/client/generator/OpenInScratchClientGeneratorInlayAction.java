// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.endpoint.impl.internal.client.generator;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.application.eap.EarlyAccessProgramManager;
import consulo.codeEditor.Editor;
import consulo.codeEditor.event.EditorMouseEvent;
import consulo.endpoint.client.generator.ClientGenerator;
import consulo.endpoint.client.generator.ClientGeneratorOpenInScratchService;
import consulo.endpoint.localize.EndpointLocalize;
import consulo.endpoint.url.UrlConstants;
import consulo.endpoint.url.inlay.UrlPathInlayAction;
import consulo.endpoint.url.inlay.UrlPathInlayHint;
import consulo.endpoint.url.reference.UrlPathContext;
import consulo.language.psi.PsiFile;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.ui.TextItemRender;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.popup.BaseListPopupStep;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.ui.ex.popup.ListPopup;
import consulo.ui.ex.popup.PopupStep;
import consulo.ui.image.Image;
import jakarta.inject.Inject;

import java.util.List;

@ExtensionImpl
public final class OpenInScratchClientGeneratorInlayAction implements UrlPathInlayAction {
    private final EarlyAccessProgramManager myEarlyAccessProgramManager;

    @Inject
    public OpenInScratchClientGeneratorInlayAction(EarlyAccessProgramManager earlyAccessProgramManager) {
        myEarlyAccessProgramManager = earlyAccessProgramManager;
    }

    @Override
    public Image getIcon() {
        return PlatformIconGroup.webreferencesOpenapi();
    }

    @Override
    public LocalizeValue getName() {
        return EndpointLocalize.clientGeneratorInlayActionName();
    }

    @Override
    @RequiredReadAction
    public boolean isAvailable(PsiFile file, UrlPathInlayHint urlPathInlayHint) {
        List<String> schemes = urlPathInlayHint.getContext().getSchemes();
        boolean available = !schemes.isEmpty() && UrlConstants.HTTP_SCHEMES.containsAll(schemes);
        return myEarlyAccessProgramManager.getState(ClientGeneratorInlayActionEapDescriptor.class) && available;
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(PsiFile file, Editor editor, UrlPathContext urlPathContext, EditorMouseEvent mouseEvent) {
        Project project = file.getProject();

        BaseListPopupStep<ClientGenerator> step = new BaseListPopupStep<>(
            EndpointLocalize.clientGeneratorInlayActionPopupTitle().get(),
            getClients(project)
        ) {
            @Override
            public String getTextFor(ClientGenerator value) {
                return value.getTitle().get();
            }

            @Override
            public PopupStep<?> onChosen(ClientGenerator selectedValue, boolean finalChoice) {
                return doFinalStep(
                    () -> ClientGeneratorOpenInScratchService.getInstance(project).createScratchFile(selectedValue, editor, urlPathContext)
                );
            }
        };

        ListPopup popup = JBPopupFactory.getInstance().createListPopup(step);

        TextItemRender<ClientGenerator> render = (presentation, item) -> {
            ClientGenerator clientGenerator = item.getValue();
            if (clientGenerator != null) {
                presentation.append(clientGenerator.getTitle());
            }
        };
        popup.setRender(render);

        popup.showBy(editor.getUIComponent(), mouseEvent.getInputDetails());
    }

    private static List<ClientGenerator> getClients(Project project) {
        return project.getExtensionPoint(ClientGenerator.class).getExtensionList();
    }
}
