// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.ide.actions;

import consulo.annotation.component.ActionImpl;
import consulo.annotation.component.ActionParentRef;
import consulo.annotation.component.ActionRef;
import consulo.annotation.component.ActionRefAnchor;
import consulo.language.localize.LanguageLocalize;
import consulo.platform.base.localize.ActionLocalize;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.*;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.ui.ex.popup.ListPopup;

@ActionImpl(
    id = "CopyReferencePopupGroup",
    children = {
        @ActionRef(type = CopyFileReferenceGroup.class),
        @ActionRef(type = AnSeparator.class),
        @ActionRef(type = CopyExternalReferenceGroup.class)
    },
    parents = {
        @ActionParentRef(
            value = @ActionRef(type = CutCopyPasteGroup.class),
            anchor = ActionRefAnchor.AFTER,
            relatedToAction = @ActionRef(type = CopyPathsAction.class)
        ),
        @ActionParentRef(
            value = @ActionRef(type = EditorTabPopupMenuGroup.class),
            anchor = ActionRefAnchor.AFTER,
            relatedToAction = @ActionRef(type = CopyPathsAction.class)
        )
    }
)
public class CopyReferencePopup extends NonTrivialActionGroup implements AlwaysPerformingActionGroup {
    private static final int DEFAULT_WIDTH = 500;

    public CopyReferencePopup() {
        super(ActionLocalize.groupCopyreferencepopupgroupText(), true);

        getTemplatePresentation().setPerformGroup(true);
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(AnActionEvent e) {
        ListPopup popup = JBPopupFactory.getInstance().createActionGroupPopup(
            LanguageLocalize.popupTitleCopy().get(),
            this,
            e.getDataContext(),
            JBPopupFactory.ActionSelectionAid.MNEMONICS,
            false,
            null,
            -1,
            null,
            ActionPlaces.COPY_REFERENCE_POPUP,
            (o, aBoolean) -> true
        );

        popup.setMinimumWidth(DEFAULT_WIDTH);
        popup.setResizable(true);
        popup.showInBestPositionFor(e.getDataContext());
    }
}
