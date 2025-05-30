// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.ide.impl.idea.codeInsight.hints;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.codeEditor.Editor;
import consulo.codeEditor.event.EditorMouseEvent;
import consulo.dataContext.DataContext;
import consulo.document.Document;
import consulo.ide.impl.idea.codeInsight.hints.action.InlayMenuGroup;
import consulo.language.editor.inlay.DeclarativeInlayHintsProvider;
import consulo.language.editor.inlay.DeclarativeInlayPayload;
import consulo.language.editor.inlay.InlayActionData;
import consulo.language.editor.inlay.InlayActionHandler;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiFile;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.ex.RelativePoint;
import consulo.ui.ex.action.ActionManager;
import consulo.ui.ex.awt.UIExAWTDataKey;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.ui.ex.popup.ListPopup;
import jakarta.inject.Singleton;

import java.util.stream.Collectors;

@ServiceAPI(ComponentScope.APPLICATION)
@ServiceImpl
@Singleton
public class DeclarativeInlayActionService {

    public void invokeInlayMenu(InlayData hintData, EditorMouseEvent e, RelativePoint relativePoint) {
        Project project = e.getEditor().getProject();
        if (project == null) {
            return;
        }
        Document document = e.getEditor().getDocument();
        PsiFile psiFile = PsiDocumentManager.getInstance(project).getPsiFile(document);
        if (psiFile == null) {
            return;
        }

        String providerId = hintData.getProviderId();
        var providerInfo = InlayHintsProviderFactory.findProviderInfo(psiFile.getLanguage(), providerId);
        if (providerInfo == null) {
            return;
        }
        LocalizeValue providerName = providerInfo.getProviderName();

        InlayMenuGroup inlayMenuActionGroup = ActionManager.getInstance().getAction(InlayMenuGroup.class);

        DataContext dataContext = DataContext.builder()
            .add(Project.KEY, project)
            .add(PsiFile.KEY, psiFile)
            .add(Editor.KEY, e.getEditor())
            .add(UIExAWTDataKey.CONTEXT_COMPONENT, e.getEditor().getComponent())
            .add(DeclarativeInlayHintsProvider.PROVIDER_ID, providerId)
            .add(DeclarativeInlayHintsProvider.PROVIDER_NAME, providerName)
            .add(DeclarativeInlayHintsProvider.INLAY_PAYLOADS,
                hintData.getPayloads() != null
                    ? hintData.getPayloads().stream()
                    .collect(Collectors.toMap(DeclarativeInlayPayload::getPayloadName, DeclarativeInlayPayload::getPayload))
                    : null
            )
            .build();

        ListPopup popupMenu = JBPopupFactory.getInstance()
            .createActionGroupPopup(
                null,
                inlayMenuActionGroup,
                dataContext,
                JBPopupFactory.ActionSelectionAid.SPEEDSEARCH,
                false
            );
        popupMenu.show(relativePoint);
    }

    public void invokeActionHandler(InlayActionData actionData, EditorMouseEvent e) {
        String handlerId = actionData.getHandlerId();
        InlayActionHandler handler = InlayActionHandler.getActionHandler(handlerId);
        if (handler != null) {
            logActionHandlerInvoked(handlerId, handler.getClass());
            handler.handleClick(e, actionData.getPayload());
        }
    }

    public void logActionHandlerInvoked(String handlerId, Class<? extends InlayActionHandler> handlerClass) {
        //   InlayActionHandlerUsagesCollector.clickHandled(handlerId, handlerClass);
    }
}
