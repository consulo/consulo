// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.ide.actions;

import consulo.component.util.localize.BundleBase;
import consulo.ide.impl.idea.ide.BrowserUtil;
import consulo.language.editor.PlatformDataKeys;
import consulo.ui.ex.CopyProvider;
import consulo.ui.ex.action.ActionsBundle;
import consulo.ui.ex.awt.CopyPasteManager;
import consulo.dataContext.DataContext;
import consulo.application.dumb.DumbAware;
import consulo.ide.impl.idea.openapi.util.text.StringUtil;
import consulo.ide.impl.idea.util.io.URLUtil;
import consulo.webBrowser.WebSearchEngine;
import consulo.webBrowser.WebSearchOptions;
import consulo.ui.annotation.RequiredUIAccess;

import jakarta.annotation.Nonnull;

import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.Presentation;
import jakarta.inject.Inject;
import java.awt.datatransfer.DataFlavor;

public class SearchWebAction extends AnAction implements DumbAware {
  private final WebSearchOptions myWebSearchOptions;

  @Inject
  public SearchWebAction(WebSearchOptions webSearchOptions) {
    myWebSearchOptions = webSearchOptions;
  }

  @RequiredUIAccess
  @Override
  public void actionPerformed(@Nonnull AnActionEvent e) {
    DataContext dataContext = e.getDataContext();
    CopyProvider provider = dataContext.getData(PlatformDataKeys.COPY_PROVIDER);
    if (provider == null) {
      return;
    }
    provider.performCopy(dataContext);
    String content = CopyPasteManager.getInstance().getContents(DataFlavor.stringFlavor);
    if (StringUtil.isNotEmpty(content)) {
      WebSearchEngine engine = myWebSearchOptions.getEngine();
      BrowserUtil.browse(BundleBase.format(engine.getUrlTemplate(), URLUtil.encodeURIComponent(content)));
    }
  }

  @RequiredUIAccess
  @Override
  public void update(@Nonnull AnActionEvent e) {
    Presentation presentation = e.getPresentation();
    DataContext dataContext = e.getDataContext();
    CopyProvider provider = e.getData(PlatformDataKeys.COPY_PROVIDER);
    boolean available = provider != null && provider.isCopyEnabled(dataContext) && provider.isCopyVisible(dataContext);
    presentation.setEnabled(available);
    presentation.setVisible(available);

    WebSearchEngine engine = myWebSearchOptions.getEngine();

    presentation.setText(BundleBase.format(ActionsBundle.message("action.$SearchWeb.0.text", engine.getPresentableName())));
    presentation.setDescription(BundleBase.format(ActionsBundle.message("action.$SearchWeb.0.description", engine.getPresentableName())));
  }
}
