/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package consulo.versionControlSystem.impl.internal.ui.awt;

import consulo.application.CommonBundle;
import consulo.dataContext.DataProvider;
import consulo.project.Project;
import consulo.ui.ex.action.ActionManager;
import consulo.ui.ex.action.DefaultActionGroup;
import consulo.ui.ex.awt.*;
import consulo.util.dataholder.Key;
import consulo.util.lang.xml.XmlStringUtil;
import consulo.versionControlSystem.VcsBundle;
import consulo.versionControlSystem.VcsDataKeys;
import consulo.versionControlSystem.action.VcsActions;
import consulo.versionControlSystem.change.Change;
import consulo.versionControlSystem.change.commited.CommittedChangeListImpl;
import consulo.versionControlSystem.impl.internal.change.commited.InternalRepositoryChangesBrowser;
import consulo.versionControlSystem.internal.CommittedChangesBrowserUseCase;
import consulo.versionControlSystem.ui.awt.IssueLinkHtmlRenderer;
import consulo.versionControlSystem.ui.awt.LegacyDialog;
import consulo.versionControlSystem.versionBrowser.CommittedChangeList;
import consulo.versionControlSystem.versionBrowser.VcsRevisionNumberAware;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jetbrains.annotations.NonNls;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.function.Function;

/**
 * @author yole
 * @author max
 * @since 2006-07-20
 */
public class ChangeListViewerDialog extends DialogWrapper implements DataProvider, LegacyDialog {
  private Project myProject;
  private CommittedChangeList myChangeList;
  private InternalRepositoryChangesBrowser myChangesBrowser;
  private JEditorPane myCommitMessageArea;
  // do not related to local data/changes etc
  private final boolean myInAir;
  private Change[] myChanges;
  private Function<Change, Change> myConvertor;
  private JScrollPane commitMessageScroll;
  private VirtualFile myToSelect;

  public ChangeListViewerDialog(Project project, CommittedChangeList changeList) {
    super(project, true);
    myInAir = false;
    initCommitMessageArea(project, changeList);
    initDialog(project, changeList);
  }

  public ChangeListViewerDialog(Project project, CommittedChangeList changeList, VirtualFile toSelect) {
    super(project, true);
    myInAir = false;
    myToSelect = toSelect;
    initCommitMessageArea(project, changeList);
    initDialog(project, changeList);
  }

  public ChangeListViewerDialog(Component parent, Project project, Collection<Change> changes, boolean inAir) {
    super(parent, true);
    myInAir = inAir;
    initDialog(project, new CommittedChangeListImpl("", "", "", -1, new Date(0), changes));
  }

  public ChangeListViewerDialog(Project project, Collection<Change> changes, boolean inAir) {
    super(project, true);
    myInAir = inAir;
    initDialog(project, new CommittedChangeListImpl("", "", "", -1, new Date(0), changes));
  }

  private void initDialog(Project project, CommittedChangeList changeList) {
    myProject = project;
    myChangeList = changeList;
    Collection<Change> changes = myChangeList.getChanges();
    myChanges = changes.toArray(new Change[changes.size()]);

    setTitle(VcsBundle.message("dialog.title.changes.browser"));
    setCancelButtonText(CommonBundle.message("close.action.name"));
    setModal(false);

    init();
  }

  private void initCommitMessageArea(Project project, CommittedChangeList changeList) {
    myCommitMessageArea = new JEditorPane(UIUtil.HTML_MIME, "");
    myCommitMessageArea.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
    myCommitMessageArea.setEditable(false);
    @NonNls String text = IssueLinkHtmlRenderer.formatTextIntoHtml(project, changeList.getComment().trim());
    myCommitMessageArea.setBackground(UIUtil.getComboBoxDisabledBackground());
    myCommitMessageArea.addHyperlinkListener(BrowserHyperlinkListener.INSTANCE);
    commitMessageScroll = ScrollPaneFactory.createScrollPane(myCommitMessageArea);
    myCommitMessageArea.setText(text);
    myCommitMessageArea.setCaretPosition(0);
  }


  protected String getDimensionServiceKey() {
    return "VCS.ChangeListViewerDialog";
  }

  public Object getData(@Nonnull @NonNls Key<?> dataId) {
    if (VcsDataKeys.CHANGES == dataId) {
      return myChanges;
    }
    else if (VcsDataKeys.VCS_REVISION_NUMBER == dataId) {
      if (myChangeList instanceof VcsRevisionNumberAware) {
        return ((VcsRevisionNumberAware)myChangeList).getRevisionNumber();
      }
    }
    return null;
  }

  public void setConvertor(Function<Change, Change> convertor) {
    myConvertor = convertor;
  }

  public JComponent createCenterPanel() {
    JPanel mainPanel = new JPanel();
    mainPanel.setLayout(new BorderLayout());
    Splitter splitter = new Splitter(true, 0.8f);
    myChangesBrowser = new InternalRepositoryChangesBrowser(myProject, Collections.singletonList(myChangeList),
                                                    new ArrayList<Change>(myChangeList.getChanges()),
                                                    myChangeList, myToSelect) {

      @Override
      protected void buildToolBar(DefaultActionGroup toolBarGroup) {
        super.buildToolBar(toolBarGroup);
        toolBarGroup.add(ActionManager.getInstance().getAction(VcsActions.ACTION_COPY_REVISION_NUMBER));
      }

      @Override
      public Object getData(@Nonnull Key dataId) {
        Object data = super.getData(dataId);
        if (data != null) {
          return data;
        }
        return ChangeListViewerDialog.this.getData(dataId);
      }

      @Override
      protected void showDiffForChanges(Change[] changesArray, int indexInSelection) {
        if (myInAir && (myConvertor != null)) {
          Change[] convertedChanges = new Change[changesArray.length];
          for (int i = 0; i < changesArray.length; i++) {
            Change change = changesArray[i];
            convertedChanges[i] = myConvertor.apply(change);
          }
          super.showDiffForChanges(convertedChanges, indexInSelection);
        } else {
          super.showDiffForChanges(changesArray, indexInSelection);
        }
      }
    };
    myChangesBrowser.setUseCase(myInAir ? CommittedChangesBrowserUseCase.IN_AIR : null);
    splitter.setFirstComponent(myChangesBrowser);

    if (myCommitMessageArea != null) {
      JPanel commitPanel = new JPanel(new BorderLayout());
      JComponent separator = SeparatorFactory.createSeparator(VcsBundle.message("label.commit.comment"), myCommitMessageArea);
      commitPanel.add(separator, BorderLayout.NORTH);
      commitPanel.add(commitMessageScroll, BorderLayout.CENTER);

      splitter.setSecondComponent(commitPanel);
    }
    mainPanel.add(splitter, BorderLayout.CENTER);

    String description = getDescription();
    if (description != null) {
      JPanel descPanel = new JPanel();
      descPanel.add(new JLabel(XmlStringUtil.wrapInHtml(description)));
      descPanel.setBorder(BorderFactory.createEtchedBorder());
      mainPanel.add(descPanel, BorderLayout.NORTH);
    }
    return mainPanel;
  }

  @Override
  protected void dispose() {
    myChangesBrowser.dispose();
    super.dispose();
  }

  @Nonnull
  @Override
  protected Action[] createActions() {
    Action cancelAction = getCancelAction();
    cancelAction.putValue(DEFAULT_ACTION, Boolean.TRUE);
    return new Action[] {cancelAction};
  }

  @Override
  public JComponent getPreferredFocusedComponent() {
    return myChangesBrowser.getPreferredFocusedComponent();
  }

  /**
   * @return description that is added to the top of this dialog. May be null - then no description is shown.
   */
  protected @Nullable String getDescription() {
    return null;
  }
}
