// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.fileEditor.impl.internal.largeFileEditor.search.action;

import consulo.application.dumb.DumbAware;
import consulo.fileEditor.impl.internal.largeFileEditor.search.SearchTaskOptions;
import consulo.fileEditor.internal.largeFileEditor.LfeSearchManager;
import consulo.fileEditor.localize.FileEditorLocalize;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nonnull;

public final class LargeFileFindAllAction extends AnAction implements DumbAware {
    //private static final Logger logger = Logger.getInstance(FindAllAction.class);
    private final LfeSearchManager searchManager;

    public LargeFileFindAllAction(LfeSearchManager searchManager) {
        this.searchManager = searchManager;

        getTemplatePresentation().setDescriptionValue(FileEditorLocalize.largeFileEditorActionDescriptionSearchEntireFileAndShowToolwindow());
        getTemplatePresentation().setTextValue(FileEditorLocalize.largeFileActionPresentationFindallactionText());
        getTemplatePresentation().setIcon(PlatformIconGroup.actionsFindentirefile());
    }

    @Override
    public void actionPerformed(@Nonnull AnActionEvent e) {
        if (StringUtil.isEmpty(searchManager.getSearchReplaceComponent().getSearchTextComponent().getText())) {
            return;
        }

        searchManager.launchNewRangeSearch(SearchTaskOptions.NO_LIMIT, SearchTaskOptions.NO_LIMIT, true);
    }
}
