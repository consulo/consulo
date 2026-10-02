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
import consulo.endpoint.url.reference.UrlPathContextUtil;
import consulo.language.editor.hint.HintManager;
import consulo.language.editor.inlay.DeclarativeInlayHintsProviderFactory;
import consulo.language.editor.inlay.InlayActionHandler;
import consulo.language.editor.inlay.InlayActionPayload;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.SmartPsiElementPointer;
import consulo.language.sem.SemService;
import consulo.project.Project;
import consulo.ui.TextItemRender;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.ui.ex.popup.ListPopup;
import jakarta.inject.Inject;
import org.jspecify.annotations.Nullable;

import java.util.List;

@ExtensionImpl
public final class UrlPathInlayActionHandler implements InlayActionHandler {
    public static final String HANDLER_ID = "microservices.url.path.inlay";

    private final Application myApplication;

    @Inject
    public UrlPathInlayActionHandler(Application application) {
        myApplication = application;
    }

    @Override
    public String getHandlerId() {
        return HANDLER_ID;
    }

    @Override
    public boolean isPlainClickEnabled() {
        return true;
    }

    @Override
    @RequiredUIAccess
    public void handleClick(EditorMouseEvent e, InlayActionPayload payload) {
        if (!(payload instanceof InlayActionPayload.PsiPointerInlayActionPayload pointerPayload)) {
            return;
        }

        Editor editor = e.getEditor();
        Project project = editor.getProject();
        if (project == null) {
            return;
        }

        SmartPsiElementPointer<?> pointer = pointerPayload.getPointer();
        String tag = pointerPayload.getTag();
        String providerId = parseProviderId(tag);
        int hintIndex = parseHintIndex(tag);

        ReadAction.nonBlocking(() -> computeClickTarget(project, pointer, providerId, hintIndex))
            .inSmartMode(project)
            .expireWhen(editor::isDisposed)
            .finishOnUiThread(Application::getDefaultModalityState, target -> {
                if (target != null) {
                    performClick(editor, e, target);
                }
            })
            .submit(AppExecutorUtil.getAppExecutorService());
    }

    @RequiredReadAction
    private @Nullable ClickTarget computeClickTarget(
        Project project,
        SmartPsiElementPointer<?> pointer,
        @Nullable String providerId,
        int hintIndex
    ) {
        PsiElement element = pointer.getElement();
        if (element == null) {
            return null;
        }

        PsiFile file = element.getContainingFile();
        if (file == null) {
            return null;
        }

        UrlPathDeclarativeInlayHintsProviderFactory factory = myApplication.getExtensionPoint(DeclarativeInlayHintsProviderFactory.class)
            .findExtension(UrlPathDeclarativeInlayHintsProviderFactory.class);
        if (factory == null) {
            return null;
        }

        SemService semService = SemService.getSemService(project);
        return UrlPathContextUtil.forbidExpensiveUrlContext(() -> {
            List<UrlPathDeclarativeInlayHintsProvider> providers = factory.getUrlPathProvidersForLanguage(file.getLanguage());
            if (providerId != null) {
                for (UrlPathDeclarativeInlayHintsProvider provider : providers) {
                    if (provider.getId().equals(providerId)) {
                        ClickTarget target = toClickTarget(file, provider.collectHints(element, semService), hintIndex);
                        if (target != null) {
                            return target;
                        }
                    }
                }
            }

            for (UrlPathDeclarativeInlayHintsProvider provider : providers) {
                ClickTarget target = toClickTarget(file, provider.collectHints(element, semService), hintIndex);
                if (target != null) {
                    return target;
                }
            }
            return null;
        });
    }

    @RequiredReadAction
    private static @Nullable ClickTarget toClickTarget(PsiFile file, List<UrlPathInlayHint> hints, int hintIndex) {
        if (hints.isEmpty()) {
            return null;
        }

        UrlPathInlayHint hint = hintIndex >= 0 && hintIndex < hints.size() ? hints.get(hintIndex) : hints.get(0);
        return new ClickTarget(file, hint, hint.getAvailableActions(file));
    }

    @RequiredUIAccess
    private static void performClick(Editor editor, EditorMouseEvent e, ClickTarget target) {
        List<UrlPathInlayAction> actions = target.actions();
        if (actions.isEmpty()) {
            HintManager.getInstance().showInformationHint(editor, EndpointLocalize.microservicesInlayNoActionsMessage());
        }
        else if (actions.size() == 1) {
            actions.get(0).actionPerformed(target.file(), editor, target.hint(), e);
        }
        else {
            showPopupForActions(editor, target.file(), e, actions, target.hint());
        }
    }

    @RequiredUIAccess
    private static void showPopupForActions(
        Editor editor,
        PsiFile file,
        EditorMouseEvent event,
        List<UrlPathInlayAction> actions,
        UrlPathInlayHint hint
    ) {
        ListPopup popup = JBPopupFactory.getInstance().createListPopup(new UrlPathInlayActionsStep(file, editor, event, actions, hint));

        TextItemRender<UrlPathInlayAction> render = (presentation, item) -> {
            UrlPathInlayAction action = item.getValue();
            if (action != null) {
                presentation.withIcon(action.getIcon());
                presentation.append(action.getName());
            }
        };
        popup.setRender(render);

        popup.showBy(editor.getUIComponent(), event.getInputDetails());
    }

    private static @Nullable String parseProviderId(@Nullable String tag) {
        if (tag == null) {
            return null;
        }
        int separator = tag.lastIndexOf(UrlPathInlayHintsCollector.TAG_SEPARATOR);
        return separator < 0 ? null : tag.substring(0, separator);
    }

    private static int parseHintIndex(@Nullable String tag) {
        if (tag == null) {
            return -1;
        }
        int separator = tag.lastIndexOf(UrlPathInlayHintsCollector.TAG_SEPARATOR);
        try {
            return Integer.parseInt(separator < 0 ? tag : tag.substring(separator + 1));
        }
        catch (NumberFormatException e) {
            return -1;
        }
    }

    private record ClickTarget(PsiFile file, UrlPathInlayHint hint, List<UrlPathInlayAction> actions) {
    }
}
