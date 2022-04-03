/*
 * Copyright 2000-2009 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.openapi.vcs.changes.committed;

import com.intellij.codeInsight.hint.HintUtil;
import consulo.dataContext.DataManager;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.language.editor.CommonDataKeys;
import consulo.application.dumb.DumbAware;
import consulo.project.Project;
import consulo.ui.ex.popup.JBPopup;
import consulo.ui.ex.popup.JBPopupFactory;
import com.intellij.openapi.vcs.*;
import com.intellij.openapi.vcs.changes.ChangeList;
import com.intellij.openapi.vcs.changes.issueLinks.IssueLinkHtmlRenderer;
import com.intellij.openapi.vcs.versionBrowser.CommittedChangeList;
import consulo.ui.ex.awt.BrowserHyperlinkListener;
import consulo.ui.ex.awt.ScrollPaneFactory;
import com.intellij.util.text.DateFormatUtil;
import consulo.ui.ex.awt.UIUtil;
import com.intellij.xml.util.XmlStringUtil;
import org.jetbrains.annotations.NonNls;

import javax.swing.*;

/**
 * @author yole
 */
public class ChangeListDetailsAction extends AnAction implements DumbAware {
  public void actionPerformed(AnActionEvent e) {
    final Project project = e.getData(CommonDataKeys.PROJECT);
    final ChangeList[] changeLists = e.getData(VcsDataKeys.CHANGE_LISTS);
    if (changeLists != null && changeLists.length > 0 && changeLists [0] instanceof CommittedChangeList) {
      showDetailsPopup(project, (CommittedChangeList) changeLists [0]);
    }
  }

  public void update(final AnActionEvent e) {
    final Project project = e.getData(CommonDataKeys.PROJECT);
    final ChangeList[] changeLists = e.getData(VcsDataKeys.CHANGE_LISTS);
    e.getPresentation().setEnabled(project != null && changeLists != null && changeLists.length == 1 &&
      changeLists [0] instanceof CommittedChangeList);
  }

  public static void showDetailsPopup(final Project project, final CommittedChangeList changeList) {
    StringBuilder detailsBuilder = new StringBuilder("<html><head>");
    detailsBuilder.append(UIUtil.getCssFontDeclaration(UIUtil.getLabelFont())).append("</head><body>");
    final AbstractVcs vcs = changeList.getVcs();
    CachingCommittedChangesProvider provider = null;
    if (vcs != null) {
      provider = vcs.getCachingCommittedChangesProvider();
      if (provider != null && provider.getChangelistTitle() != null) {
        detailsBuilder.append(provider.getChangelistTitle()).append(" #").append(changeList.getNumber()).append("<br>");
      }
    }
    @NonNls String committer = "<b>" + changeList.getCommitterName() + "</b>";
    detailsBuilder.append(VcsBundle.message("changelist.details.committed.format", committer,
                                            DateFormatUtil.formatPrettyDateTime(changeList.getCommitDate())));
    detailsBuilder.append("<br>");

    if (provider != null) {
      final CommittedChangeList originalChangeList;
      if (changeList instanceof ReceivedChangeList) {
        originalChangeList = ((ReceivedChangeList) changeList).getBaseList();
      }
      else {
        originalChangeList = changeList;
      }
      for(ChangeListColumn column: provider.getColumns()) {
        if (ChangeListColumn.isCustom(column)) {
          String value = column.getValue(originalChangeList).toString();
          if (value.length() == 0) {
            value = "<none>";
          }
          detailsBuilder.append(column.getTitle()).append(": ").append(XmlStringUtil.escapeString(value)).append("<br>");
        }
      }
    }

    detailsBuilder.append(IssueLinkHtmlRenderer.formatTextWithLinks(project, changeList.getComment()));
    detailsBuilder.append("</body></html>");

    JEditorPane editorPane = new JEditorPane(UIUtil.HTML_MIME, detailsBuilder.toString());
    editorPane.setEditable(false);
    editorPane.setBackground(HintUtil.INFORMATION_COLOR);
    editorPane.select(0, 0);
    editorPane.addHyperlinkListener(BrowserHyperlinkListener.INSTANCE);
    JScrollPane scrollPane = ScrollPaneFactory.createScrollPane(editorPane);
    final JBPopup hint =
      JBPopupFactory.getInstance().createComponentPopupBuilder(scrollPane, editorPane)
        .setDimensionServiceKey(project, "changelist.details.popup", false)
        .setResizable(true)
        .setMovable(true)
        .setRequestFocus(true)
        .setTitle(VcsBundle.message("changelist.details.title"))
        .createPopup();
    hint.showInBestPositionFor(DataManager.getInstance().getDataContext());
  }

}
