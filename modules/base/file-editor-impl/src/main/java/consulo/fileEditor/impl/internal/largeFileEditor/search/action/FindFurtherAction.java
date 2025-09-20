// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.fileEditor.impl.internal.largeFileEditor.search.action;

import consulo.application.dumb.DumbAware;
import consulo.fileEditor.impl.internal.largeFileEditor.search.RangeSearch;
import consulo.fileEditor.localize.FileEditorLocalize;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.ex.action.ActionUpdateThread;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.image.Image;
import jakarta.annotation.Nonnull;

public final class FindFurtherAction extends AnAction implements DumbAware {
    private final boolean directionForward;
    private final RangeSearch myRangeSearch;

    public FindFurtherAction(boolean directionForward, RangeSearch rangeSearch) {
        this.directionForward = directionForward;
        this.myRangeSearch = rangeSearch;

        LocalizeValue text;
        LocalizeValue description;
        Image icon;

        if (directionForward) {
            text = FileEditorLocalize.largeFileEditorFindFurtherForwardActionText();
            description = FileEditorLocalize.largeFileEditorFindFurtherForwardActionDescription();
            icon = PlatformIconGroup.actionsFindandshownextmatches();
        }
        else {
            text = FileEditorLocalize.largeFileEditorFindFurtherBackwardActionText();
            description = FileEditorLocalize.largeFileEditorFindFurtherBackwardActionDescription();
            icon = PlatformIconGroup.actionsFindandshowprevmatches();
        }

        getTemplatePresentation().setTextValue(text);
        getTemplatePresentation().setDescriptionValue(description);
        getTemplatePresentation().setIcon(icon);
    }

    @Override
    public void update(@Nonnull AnActionEvent e) {
        boolean enabled = myRangeSearch.isButtonFindFurtherEnabled(directionForward);
        e.getPresentation().setEnabled(enabled);
    }

    @Override
    public @Nonnull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    @Override
    public void actionPerformed(@Nonnull AnActionEvent e) {
        myRangeSearch.onClickSearchFurther(directionForward);
    }
}
