// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.actions;

import com.intellij.BundleBase;
import com.intellij.ide.BrowserUtil;
import com.intellij.ide.CopyProvider;
import com.intellij.idea.ActionsBundle;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.io.URLUtil;
import consulo.ide.actions.webSearch.WebSearchEngine;
import consulo.ide.actions.webSearch.WebSearchOptions;
import consulo.ui.annotation.RequiredUIAccess;

import javax.annotation.Nonnull;
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
