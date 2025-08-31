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
package consulo.versionControlSystem.impl.internal.change.commited;

import consulo.annotation.component.ActionImpl;
import consulo.application.dumb.DumbAware;
import consulo.application.util.DateFormatUtil;
import consulo.dataContext.DataManager;
import consulo.language.editor.ui.awt.HintUtil;
import consulo.platform.base.localize.ActionLocalize;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.awt.BrowserHyperlinkListener;
import consulo.ui.ex.awt.ScrollPaneFactory;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.popup.JBPopup;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.util.lang.xml.XmlStringUtil;
import consulo.versionControlSystem.AbstractVcs;
import consulo.versionControlSystem.CachingCommittedChangesProvider;
import consulo.versionControlSystem.ChangeListColumn;
import consulo.versionControlSystem.VcsDataKeys;
import consulo.versionControlSystem.change.ChangeList;
import consulo.versionControlSystem.change.commited.ReceivedChangeList;
import consulo.versionControlSystem.localize.VcsLocalize;
import consulo.versionControlSystem.ui.awt.IssueLinkHtmlRenderer;
import consulo.versionControlSystem.versionBrowser.CommittedChangeList;
import jakarta.annotation.Nonnull;

import javax.swing.*;

/**
 * @author yole
 */
@ActionImpl(id = "CommittedChanges.Details")
public class ChangeListDetailsAction extends AnAction implements DumbAware {
    public ChangeListDetailsAction() {
        super(
            ActionLocalize.actionCommittedchangesDetailsText(),
            ActionLocalize.actionCommittedchangesDetailsDescription()
        );
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(@Nonnull AnActionEvent e) {
        Project project = e.getRequiredData(Project.KEY);
        ChangeList[] changeLists = e.getRequiredData(VcsDataKeys.CHANGE_LISTS);
        if (changeLists.length > 0 && changeLists[0] instanceof CommittedChangeList committedChangeList) {
            showDetailsPopup(project, committedChangeList);
        }
    }

    @Override
    public void update(@Nonnull AnActionEvent e) {
        Project project = e.getData(Project.KEY);
        ChangeList[] changeLists = e.getData(VcsDataKeys.CHANGE_LISTS);
        e.getPresentation().setEnabled(
            project != null && changeLists != null && changeLists.length == 1
                && changeLists[0] instanceof CommittedChangeList
        );
    }

    public static void showDetailsPopup(Project project, CommittedChangeList changeList) {
        StringBuilder detailsBuilder = new StringBuilder("<html><head>");
        detailsBuilder.append(UIUtil.getCssFontDeclaration(UIUtil.getLabelFont())).append("</head><body>");
        AbstractVcs vcs = changeList.getVcs();
        CachingCommittedChangesProvider provider = null;
        if (vcs != null) {
            provider = vcs.getCachingCommittedChangesProvider();
            if (provider != null && provider.getChangelistTitle() != null) {
                detailsBuilder.append(provider.getChangelistTitle()).append(" #").append(changeList.getNumber()).append("<br>");
            }
        }
        String committer = "<b>" + changeList.getCommitterName() + "</b>";
        detailsBuilder.append(VcsLocalize.changelistDetailsCommittedFormat(
            committer,
            DateFormatUtil.formatPrettyDateTime(changeList.getCommitDate())
        ).get());
        detailsBuilder.append("<br>");

        if (provider != null) {
            CommittedChangeList originalChangeList;
            if (changeList instanceof ReceivedChangeList receivedChangeList) {
                originalChangeList = receivedChangeList.getBaseList();
            }
            else {
                originalChangeList = changeList;
            }
            for (ChangeListColumn column : provider.getColumns()) {
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
        JBPopup hint = JBPopupFactory.getInstance().createComponentPopupBuilder(scrollPane, editorPane)
            .setDimensionServiceKey(project, "changelist.details.popup", false)
            .setResizable(true)
            .setMovable(true)
            .setRequestFocus(true)
            .setTitle(VcsLocalize.changelistDetailsTitle())
            .createPopup();
        hint.showInBestPositionFor(DataManager.getInstance().getDataContext());
    }
}
