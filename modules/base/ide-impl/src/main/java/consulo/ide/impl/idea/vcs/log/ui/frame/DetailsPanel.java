/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package consulo.ide.impl.idea.vcs.log.ui.frame;

import com.google.common.primitives.Ints;
import consulo.disposer.Disposable;
import consulo.application.ApplicationManager;
import consulo.colorScheme.event.EditorColorsListener;
import consulo.colorScheme.EditorColorsScheme;
import consulo.ide.impl.idea.openapi.progress.util.ProgressWindow;
import consulo.ide.impl.idea.openapi.roots.ui.componentsList.components.ScrollablePanel;
import consulo.ui.ex.awt.OnePixelDivider;
import consulo.ui.ex.awt.VerticalFlowLayout;
import consulo.util.lang.StringUtil;
import consulo.ide.impl.idea.openapi.vcs.history.VcsHistoryUtil;
import consulo.ui.ex.awt.IdeBorderFactory;
import consulo.ide.impl.idea.ui.SeparatorComponent;
import consulo.ui.ex.awt.JBLabel;
import consulo.ide.impl.idea.ui.components.JBLoadingPanel;
import consulo.ui.ex.awt.JBScrollPane;
import consulo.ide.impl.idea.util.containers.ContainerUtil;
import consulo.ui.ex.awt.StatusText;
import consulo.ui.ex.awt.UIUtil;
import consulo.versionControlSystem.log.VcsFullCommitDetails;
import consulo.versionControlSystem.log.VcsRef;
import consulo.ide.impl.idea.vcs.log.data.VcsLogDataImpl;
import consulo.ide.impl.idea.vcs.log.ui.VcsLogColorManager;
import jakarta.annotation.Nonnull;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * @author Kirill Likhodedov
 */
class DetailsPanel extends JPanel implements EditorColorsListener {
  private static final int MAX_ROWS = 50;

  @Nonnull
  private final VcsLogDataImpl myLogData;

  @Nonnull
  private final JScrollPane myScrollPane;
  @Nonnull
  private final JPanel myMainContentPanel;
  @Nonnull
  private final StatusText myEmptyText;

  @Nonnull
  private final JBLoadingPanel myLoadingPanel;
  @Nonnull
  private final VcsLogColorManager myColorManager;

  @Nonnull
  private List<Integer> mySelection = ContainerUtil.emptyList();
  @Nonnull
  private Set<VcsFullCommitDetails> myCommitDetails = Collections.emptySet();

  DetailsPanel(@Nonnull VcsLogDataImpl logData,
               @Nonnull VcsLogColorManager colorManager,
               @Nonnull Disposable parent) {
    myLogData = logData;
    myColorManager = colorManager;

    myScrollPane = new JBScrollPane(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    myMainContentPanel = new ScrollablePanel() {
      @Override
      public boolean getScrollableTracksViewportWidth() {
        boolean expanded = false;
        for (Component c : getComponents()) {
          if (c instanceof CommitPanel && ((CommitPanel)c).isExpanded()) {
            expanded = true;
            break;
          }
        }
        return !expanded;
      }

      @Override
      public Dimension getPreferredSize() {
        Dimension preferredSize = super.getPreferredSize();
        int height = Math.max(preferredSize.height, myScrollPane.getViewport().getHeight());
        JBScrollPane scrollPane = UIUtil.getParentOfType(JBScrollPane.class, this);
        if (scrollPane == null || getScrollableTracksViewportWidth()) {
          return new Dimension(preferredSize.width, height);
        }
        else {
          return new Dimension(Math.max(preferredSize.width, scrollPane.getViewport().getWidth()), height);
        }
      }

      @Override
      public Color getBackground() {
        return CommitPanel.getCommitDetailsBackground();
      }

      @Override
      protected void paintChildren(Graphics g) {
        if (StringUtil.isNotEmpty(myEmptyText.getText())) {
          myEmptyText.paint(this, g);
        }
        else {
          super.paintChildren(g);
        }
      }
    };
    myEmptyText = new StatusText(myMainContentPanel) {
      @Override
      protected boolean isStatusVisible() {
        return StringUtil.isNotEmpty(getText());
      }
    };
    myMainContentPanel.setLayout(new VerticalFlowLayout(VerticalFlowLayout.TOP, 0, 0, true, false));

    myMainContentPanel.setOpaque(false);
    myScrollPane.setViewportView(myMainContentPanel);
    myScrollPane.setBorder(IdeBorderFactory.createEmptyBorder());
    myScrollPane.setViewportBorder(IdeBorderFactory.createEmptyBorder());

    myLoadingPanel = new JBLoadingPanel(new BorderLayout(), parent, ProgressWindow.DEFAULT_PROGRESS_DIALOG_POSTPONE_TIME_MILLIS) {
      @Override
      public Color getBackground() {
        return CommitPanel.getCommitDetailsBackground();
      }
    };
    myLoadingPanel.add(myScrollPane);

    setLayout(new BorderLayout());
    add(myLoadingPanel, BorderLayout.CENTER);

    myEmptyText.setText("Commit details");
  }

  @Override
  public void globalSchemeChange(EditorColorsScheme scheme) {
    for (int i = 0; i < mySelection.size(); i++) {
      CommitPanel commitPanel = getCommitPanel(i);
      commitPanel.update();
    }
  }

  @Override
  public Color getBackground() {
    return CommitPanel.getCommitDetailsBackground();
  }

  public void installCommitSelectionListener(@Nonnull VcsLogGraphTable graphTable) {
    graphTable.getSelectionModel().addListSelectionListener(new CommitSelectionListenerForDetails(graphTable));
  }

  public void branchesChanged() {
    for (int i = 0; i < mySelection.size(); i++) {
      CommitPanel commitPanel = getCommitPanel(i);
      commitPanel.updateBranches();
    }
  }

  private void rebuildCommitPanels(int[] selection) {
    myEmptyText.setText("");

    int selectionLength = selection.length;

    // for each commit besides the first there are two components: Separator and CommitPanel
    int existingCount = (myMainContentPanel.getComponentCount() + 1) / 2;
    int requiredCount = Math.min(selectionLength, MAX_ROWS);
    for (int i = existingCount; i < requiredCount; i++) {
      if (i > 0) {
        myMainContentPanel.add(new SeparatorComponent(0, OnePixelDivider.BACKGROUND, null));
      }
      myMainContentPanel.add(new CommitPanel(myLogData, myColorManager));
    }

    // clear superfluous items
    while (myMainContentPanel.getComponentCount() > 2 * requiredCount - 1) {
      myMainContentPanel.remove(myMainContentPanel.getComponentCount() - 1);
    }

    if (selectionLength > MAX_ROWS) {
      myMainContentPanel.add(new SeparatorComponent(0, OnePixelDivider.BACKGROUND, null));
      JBLabel label = new JBLabel("(showing " + MAX_ROWS + " of " + selectionLength + " selected commits)");
      label.setFont(VcsHistoryUtil.getCommitDetailsFont());
      label.setBorder(CommitPanel.getDetailsBorder());
      myMainContentPanel.add(label);
    }

    mySelection = Ints.asList(Arrays.copyOf(selection, requiredCount));

    repaint();
  }

  @Nonnull
  private CommitPanel getCommitPanel(int index) {
    return (CommitPanel)myMainContentPanel.getComponent(2 * index);
  }

  private class CommitSelectionListenerForDetails extends CommitSelectionListener {
    public CommitSelectionListenerForDetails(VcsLogGraphTable graphTable) {
      super(DetailsPanel.this.myLogData, graphTable, DetailsPanel.this.myLoadingPanel);
    }

    @Override
    protected void onDetailsLoaded(@Nonnull List<VcsFullCommitDetails> detailsList) {
      Set<VcsFullCommitDetails> newCommitDetails = ContainerUtil.newHashSet(detailsList);
      for (int i = 0; i < mySelection.size(); i++) {
        CommitPanel commitPanel = getCommitPanel(i);
        commitPanel.setCommit(detailsList.get(i));
      }

      if (!ContainerUtil.intersects(myCommitDetails, newCommitDetails)) {
        myScrollPane.getVerticalScrollBar().setValue(0);
      }
      myCommitDetails = newCommitDetails;
    }

    @Override
    protected void onSelection(@Nonnull int[] selection) {
      rebuildCommitPanels(selection);
      final List<Integer> currentSelection = mySelection;
      ApplicationManager.getApplication().executeOnPooledThread(() -> {
        List<Collection<VcsRef>> result = ContainerUtil.newArrayList();
        for (Integer row : currentSelection) {
          result.add(myGraphTable.getModel().getRefsAtRow(row));
        }
        ApplicationManager.getApplication().invokeLater(() -> {
          if (currentSelection == mySelection) {
            for (int i = 0; i < currentSelection.size(); i++) {
              CommitPanel commitPanel = getCommitPanel(i);
              commitPanel.setRefs(result.get(i));
            }
          }
        });
      });
    }

    @Override
    protected void onEmptySelection() {
      myEmptyText.setText("No commits selected");
      myMainContentPanel.removeAll();
      mySelection = ContainerUtil.emptyList();
      myCommitDetails = Collections.emptySet();
    }

    @Nonnull
    @Override
    protected List<Integer> getSelectionToLoad() {
      return mySelection;
    }
  }
}
