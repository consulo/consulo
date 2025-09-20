// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.fileEditor.impl.internal.largeFileEditor.search.action;

import consulo.application.dumb.DumbAware;
import consulo.fileEditor.impl.internal.largeFileEditor.search.SearchTaskOptions;
import consulo.fileEditor.internal.largeFileEditor.LfeSearchManager;
import consulo.fileEditor.localize.FileEditorLocalize;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nonnull;

public final class FindForwardBackwardAction extends AnAction implements DumbAware {

    private final LfeSearchManager searchManager;
    private final boolean directionForward;

    public FindForwardBackwardAction(boolean directionForward, LfeSearchManager searchManager) {
        this.directionForward = directionForward;
        this.searchManager = searchManager;

        getTemplatePresentation().setDescriptionValue(directionForward ?
            FileEditorLocalize.largeFileEditorFindForwardActionDescription() :
            FileEditorLocalize.largeFileEditorFindBackwardActionDescription());
        getTemplatePresentation().setTextValue(directionForward ?
            FileEditorLocalize.largeFileEditorFindForwardActionText() :
            FileEditorLocalize.largeFileEditorFindBackwardActionText());
        getTemplatePresentation().setIcon(directionForward ? PlatformIconGroup.actionsFindforward() : PlatformIconGroup.actionsFindbackward());
    }

    @RequiredUIAccess
    @Override
    public void actionPerformed(@Nonnull AnActionEvent e) {
        if (StringUtil.isEmpty(searchManager.getSearchReplaceComponent().getSearchTextComponent().getText())) {
            return;
        }
        searchManager.launchNewRangeSearch(
            directionForward ? searchManager.getLargeFileEditor().getCaretPageNumber() : SearchTaskOptions.NO_LIMIT,
            directionForward ? SearchTaskOptions.NO_LIMIT : searchManager.getLargeFileEditor().getCaretPageNumber(),
            directionForward);
    }
}
