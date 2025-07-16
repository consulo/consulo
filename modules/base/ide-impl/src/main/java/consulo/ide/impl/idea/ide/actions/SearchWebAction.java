// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.ide.actions;

import consulo.application.dumb.DumbAware;
import consulo.component.util.localize.BundleBase;
import consulo.dataContext.DataContext;
import consulo.webBrowser.BrowserUtil;
import consulo.ide.impl.idea.util.io.URLUtil;
import consulo.platform.base.localize.ActionLocalize;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.CopyProvider;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.Presentation;
import consulo.ui.ex.awt.CopyPasteManager;
import consulo.util.lang.StringUtil;
import consulo.webBrowser.WebSearchEngine;
import consulo.webBrowser.WebSearchOptions;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;

import java.awt.datatransfer.DataFlavor;

public class SearchWebAction extends AnAction implements DumbAware {
    private final WebSearchOptions myWebSearchOptions;

    @Inject
    public SearchWebAction(WebSearchOptions webSearchOptions) {
        myWebSearchOptions = webSearchOptions;
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(@Nonnull AnActionEvent e) {
        CopyProvider provider = e.getRequiredData(CopyProvider.KEY);
        provider.performCopy(e.getDataContext());
        String content = CopyPasteManager.getInstance().getContents(DataFlavor.stringFlavor);
        if (StringUtil.isNotEmpty(content)) {
            WebSearchEngine engine = myWebSearchOptions.getEngine();
            BrowserUtil.browse(BundleBase.format(engine.getUrlTemplate(), URLUtil.encodeURIComponent(content)));
        }
    }

    @Override
    public void update(@Nonnull AnActionEvent e) {
        Presentation presentation = e.getPresentation();
        DataContext dataContext = e.getDataContext();
        CopyProvider provider = e.getData(CopyProvider.KEY);
        boolean available = provider != null && provider.isCopyEnabled(dataContext) && provider.isCopyVisible(dataContext);
        presentation.setEnabledAndVisible(available);

        String engineName = myWebSearchOptions.getEngine().getPresentableName();
        presentation.setTextValue(ActionLocalize.action$searchweb0Text(engineName));
        presentation.setDescriptionValue(ActionLocalize.action$searchweb0Description(engineName));
    }
}
