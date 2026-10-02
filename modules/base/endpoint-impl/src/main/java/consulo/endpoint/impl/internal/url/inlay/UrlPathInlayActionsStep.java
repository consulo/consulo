// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.endpoint.impl.internal.url.inlay;

import consulo.codeEditor.Editor;
import consulo.codeEditor.event.EditorMouseEvent;
import consulo.endpoint.localize.EndpointLocalize;
import consulo.endpoint.url.inlay.UrlPathInlayAction;
import consulo.endpoint.url.inlay.UrlPathInlayHint;
import consulo.language.psi.PsiFile;
import consulo.ui.ex.popup.BaseListPopupStep;
import consulo.ui.ex.popup.PopupStep;
import consulo.ui.image.Image;

import java.util.List;

public final class UrlPathInlayActionsStep extends BaseListPopupStep<UrlPathInlayAction> {
    private final PsiFile myFile;
    private final Editor myEditor;
    private final EditorMouseEvent myEvent;
    private final UrlPathInlayHint myHint;

    public UrlPathInlayActionsStep(
        PsiFile file,
        Editor editor,
        EditorMouseEvent event,
        List<UrlPathInlayAction> actions,
        UrlPathInlayHint hint
    ) {
        super(EndpointLocalize.microservicesInlayActionsTitle().get(), actions);
        myFile = file;
        myEditor = editor;
        myEvent = event;
        myHint = hint;
    }

    @Override
    public Image getIconFor(UrlPathInlayAction value) {
        return value.getIcon();
    }

    @Override
    public String getTextFor(UrlPathInlayAction value) {
        return value.getName().get();
    }

    @Override
    public PopupStep<?> onChosen(UrlPathInlayAction selectedValue, boolean finalChoice) {
        return doFinalStep(() -> selectedValue.actionPerformed(myFile, myEditor, myHint, myEvent));
    }
}
