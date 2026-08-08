// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.ide.actions;

import consulo.annotation.component.ActionImpl;
import consulo.application.dumb.DumbAware;
import consulo.component.util.localize.BundleBase;
import consulo.dataContext.DataContext;
import consulo.ui.ex.action.coroutine.ActionSafeReadLock;
import consulo.util.concurrent.coroutine.Coroutine;
import consulo.util.io.URLUtil;
import consulo.webBrowser.BrowserUtil;
import consulo.platform.base.localize.ActionLocalize;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.UIAccess;
import consulo.ui.ex.CopyProvider;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.AnActionWithAsyncUpdate;
import consulo.ui.clipboard.DataTransferType;
import consulo.ui.ex.CopyPasteManager;
import consulo.util.lang.StringUtil;
import consulo.webBrowser.WebSearchEngine;
import consulo.webBrowser.WebSearchOptions;
import jakarta.inject.Inject;


@ActionImpl(id = "$SearchWeb")
public class SearchWebAction extends AnAction implements DumbAware, AnActionWithAsyncUpdate {
    private final WebSearchOptions myWebSearchOptions;

    @Inject
    public SearchWebAction(WebSearchOptions webSearchOptions) {
        super(
            ActionLocalize.action$searchweb0Text(webSearchOptions.getEngine().getPresentableName()),
            ActionLocalize.action$searchweb0Description(webSearchOptions.getEngine().getPresentableName())
        );
        myWebSearchOptions = webSearchOptions;
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(AnActionEvent e) {
        CopyProvider provider = e.getRequiredData(CopyProvider.KEY);
        provider.performCopy(e.getDataContext());

        UIAccess uiAccess = UIAccess.current();
        CopyPasteManager.getInstance().getContentsAsync(DataTransferType.TEXT).whenCompleteAsync((content, throwable) -> {
            if (throwable == null && StringUtil.isNotEmpty(content)) {
                WebSearchEngine engine = myWebSearchOptions.getEngine();
                BrowserUtil.browse(BundleBase.format(engine.getUrlTemplate(), URLUtil.encodeURIComponent(content)));
            }
        }, uiAccess);
    }

    @Override
    public Coroutine<?, ?> updateAsync(AnActionEvent e) {
        return ActionSafeReadLock.run(e, presentation -> {
            DataContext dataContext = e.getDataContext();
            CopyProvider provider = e.getData(CopyProvider.KEY);
            boolean available = provider != null && provider.isCopyEnabled(dataContext) && provider.isCopyVisible(dataContext);
            presentation.setEnabledAndVisible(available);
        }).toCoroutine();
    }
}
