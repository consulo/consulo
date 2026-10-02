// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.endpoint.impl.internal.url.inlay;

import consulo.annotation.access.RequiredReadAction;
import consulo.endpoint.localize.EndpointLocalize;
import consulo.endpoint.url.inlay.UrlPathInlayAction;
import consulo.endpoint.url.inlay.UrlPathInlayHint;
import consulo.endpoint.url.reference.UrlPathContextUtil;
import consulo.language.editor.inlay.DeclarativeInlayHintsCollector;
import consulo.language.editor.inlay.DeclarativeInlayPosition;
import consulo.language.editor.inlay.DeclarativeInlayTreeSink;
import consulo.language.editor.inlay.HintFormat;
import consulo.language.editor.inlay.InlayActionData;
import consulo.language.editor.inlay.InlayActionPayload;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.SmartPointerManager;
import consulo.language.psi.SmartPsiElementPointer;
import consulo.language.sem.SemService;
import consulo.localize.LocalizeValue;

import java.util.List;

public final class UrlPathInlayHintsCollector implements DeclarativeInlayHintsCollector.SharedBypassCollector {
    static final char TAG_SEPARATOR = '#';

    private final UrlPathDeclarativeInlayHintsProvider myProvider;
    private final PsiFile myFile;
    private final SemService mySemService;

    public UrlPathInlayHintsCollector(UrlPathDeclarativeInlayHintsProvider provider, PsiFile file, SemService semService) {
        myProvider = provider;
        myFile = file;
        mySemService = semService;
    }

    @Override
    @RequiredReadAction
    public void collectFromElement(PsiElement element, DeclarativeInlayTreeSink sink) {
        UrlPathContextUtil.forbidExpensiveUrlContext(() -> collect(element, sink));
    }

    @RequiredReadAction
    private boolean collect(PsiElement element, DeclarativeInlayTreeSink sink) {
        List<UrlPathInlayHint> hints = myProvider.collectHints(element, mySemService);
        if (hints.isEmpty()) {
            return true;
        }

        SmartPsiElementPointer<PsiElement> pointer = SmartPointerManager.createPointer(element);
        for (int i = 0; i < hints.size(); i++) {
            UrlPathInlayHint hint = hints.get(i);
            List<UrlPathInlayAction> actions = hint.getAvailableActions(myFile);

            InlayActionData actionData = new InlayActionData(
                new InlayActionPayload.PsiPointerInlayActionPayload(pointer, myProvider.getId() + TAG_SEPARATOR + i),
                UrlPathInlayActionHandler.HANDLER_ID
            );

            DeclarativeInlayPosition position = switch (hint.getStyle()) {
                case BLOCK -> new DeclarativeInlayPosition.AboveLineIndentedPosition(hint.getOffset(), hint.getPriority(), 0);
                case INLINE -> new DeclarativeInlayPosition.InlineInlayPosition(hint.getOffset(), false);
            };

            sink.addPresentation(
                position,
                null,
                getTooltip(actions).get(),
                HintFormat.DEFAULT,
                builder -> builder.clickHandlerScope(actionData, hint::buildPresentation)
            );
        }
        return true;
    }

    private static LocalizeValue getTooltip(List<UrlPathInlayAction> actions) {
        if (actions.size() == 1) {
            return EndpointLocalize.microservicesInlayOneActionTooltip(actions.get(0).getName());
        }
        else {
            return EndpointLocalize.microservicesInlayTooltip();
        }
    }
}
