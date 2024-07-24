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
package consulo.ide.impl.idea.openapi.vcs.changes.ui;

import consulo.application.impl.internal.IdeaModalityState;
import consulo.dataContext.DataSink;
import consulo.dataContext.TypeSafeDataProvider;
import consulo.diff.DiffDialogHints;
import consulo.diff.internal.DiffUserDataKeysEx;
import consulo.disposer.Disposable;
import consulo.ide.impl.idea.openapi.actionSystem.ex.ActionUtil;
import consulo.project.ui.impl.internal.VirtualFileDeleteProvider;
import consulo.versionControlSystem.impl.internal.change.RemoteRevisionsCache;
import consulo.ide.impl.idea.openapi.vcs.changes.actions.diff.ShowDiffAction;
import consulo.ide.impl.idea.openapi.vcs.changes.actions.diff.ShowDiffContext;
import consulo.ide.impl.idea.util.containers.ContainerUtil;
import consulo.logging.Logger;
import consulo.navigation.Navigatable;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.DeleteProvider;
import consulo.ui.ex.action.*;
import consulo.ui.ex.awt.ScrollPaneFactory;
import consulo.ui.ex.awt.action.CheckboxAction;
import consulo.util.dataholder.Key;
import consulo.util.lang.ObjectUtil;
import consulo.versionControlSystem.AbstractVcs;
import consulo.versionControlSystem.FilePath;
import consulo.versionControlSystem.VcsDataKeys;
import consulo.versionControlSystem.change.*;
import consulo.versionControlSystem.impl.internal.change.ui.awt.ChangeNodeDecorator;
import consulo.versionControlSystem.impl.internal.change.ui.awt.ChangesBrowserNode;
import consulo.versionControlSystem.impl.internal.change.ui.awt.ChangesTreeList;
import consulo.versionControlSystem.internal.ChangesBrowserApi;
import consulo.versionControlSystem.localize.VcsLocalize;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.intellij.lang.annotations.JdkConstants;
import org.jetbrains.annotations.Contract;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.io.File;
import java.util.List;
import java.util.*;
import java.util.stream.Stream;

import static consulo.versionControlSystem.change.ChangesUtil.getAfterRevisionsFiles;
import static consulo.versionControlSystem.change.ChangesUtil.getNavigatableArray;
import static consulo.versionControlSystem.impl.internal.change.ui.awt.ChangesBrowserNode.UNVERSIONED_FILES_TAG;
import static consulo.versionControlSystem.impl.internal.change.ui.awt.ChangesListView.*;

public abstract class ChangesBrowserBase<T> extends JPanel implements ChangesBrowserApi, TypeSafeDataProvider, Disposable {
  private static final Logger LOG = Logger.getInstance(ChangesBrowserBase.class);

  // for backgroundable rollback to mark
  private boolean myDataIsDirty;
  protected final Class<T> myClass;
  protected final ChangesTreeList<T> myViewer;
  protected final JScrollPane myViewerScrollPane;
  protected ChangeList mySelectedChangeList;
  protected List<T> myChangesToDisplay;
  protected final Project myProject;
  private final boolean myCapableOfExcludingChanges;
  protected final JPanel myHeaderPanel;
  private JComponent myBottomPanel;
  private DefaultActionGroup myToolBarGroup;
  private String myToggleActionTitle = VcsLocalize.commitDialogIncludeActionName().get();

  private JComponent myDiffBottomComponent;

  public static Key<ChangesBrowserApi> DATA_KEY = ChangesBrowserApi.DATA_KEY;
  private AnAction myDiffAction;
  private final VirtualFile myToSelect;
  @Nonnull
  private final DeleteProvider myDeleteProvider = new VirtualFileDeleteProvider();

  public void setChangesToDisplay(final List<T> changes) {
    myChangesToDisplay = changes;
    myViewer.setChangesToDisplay(changes);
  }

  public void setDecorator(final ChangeNodeDecorator decorator) {
    myViewer.setChangeDecorator(decorator);
  }

  protected ChangesBrowserBase(final Project project,
                               @Nonnull List<T> changes,
                               final boolean capableOfExcludingChanges,
                               final boolean highlightProblems,
                               @Nullable final Runnable inclusionListener,
                               ChangesBrowser.MyUseCase useCase,
                               @Nullable VirtualFile toSelect,
                               Class<T> clazz) {
    super(new BorderLayout());
    setFocusable(false);

    myClass = clazz;
    myDataIsDirty = false;
    myProject = project;
    myCapableOfExcludingChanges = capableOfExcludingChanges;
    myToSelect = toSelect;

    ChangeNodeDecorator decorator =
      ChangesBrowser.MyUseCase.LOCAL_CHANGES.equals(useCase) ? RemoteRevisionsCache.getInstance(myProject).getChangesNodeDecorator() : null;

    myViewer = new ChangesTreeList<>(myProject, changes, capableOfExcludingChanges, highlightProblems, inclusionListener, decorator) {
      @Override
      protected DefaultTreeModel buildTreeModel(final List<T> changes, ChangeNodeDecorator changeNodeDecorator) {
        return ChangesBrowserBase.this.buildTreeModel(changes, changeNodeDecorator, isShowFlatten());
      }

      @Override
      protected List<T> getSelectedObjects(final ChangesBrowserNode<T> node) {
        return ChangesBrowserBase.this.getSelectedObjects(node);
      }

      @Override
      @Nullable
      protected T getLeadSelectedObject(final ChangesBrowserNode node) {
        return ChangesBrowserBase.this.getLeadSelectedObject(node);
      }

      @Override
      public void setScrollPaneBorder(Border border) {
        myViewerScrollPane.setBorder(border);
      }
    };
    myViewerScrollPane = ScrollPaneFactory.createScrollPane(myViewer);
    myHeaderPanel = new JPanel(new BorderLayout());
  }

  protected void init() {
    add(myViewerScrollPane, BorderLayout.CENTER);

    myHeaderPanel.add(createToolbar(), BorderLayout.CENTER);
    add(myHeaderPanel, BorderLayout.NORTH);

    myBottomPanel = new JPanel(new BorderLayout());
    add(myBottomPanel, BorderLayout.SOUTH);

    myViewer.installPopupHandler(myToolBarGroup);
    myViewer.setDoubleClickHandler(getDoubleClickHandler());
  }

  @Nonnull
  protected abstract DefaultTreeModel buildTreeModel(final List<T> changes, ChangeNodeDecorator changeNodeDecorator, boolean showFlatten);

  @Nonnull
  protected abstract List<T> getSelectedObjects(@Nonnull ChangesBrowserNode<T> node);

  @Nullable
  protected abstract T getLeadSelectedObject(@Nonnull ChangesBrowserNode node);

  @Nonnull
  protected Runnable getDoubleClickHandler() {
    return this::showDiff;
  }

  protected void setInitialSelection(
    final List<? extends ChangeList> changeLists,
    @Nonnull List<T> changes,
    final ChangeList initialListSelection
  ) {
    mySelectedChangeList = initialListSelection;
  }

  @Override
  public void dispose() {
  }

  public void addToolbarAction(AnAction action) {
    myToolBarGroup.add(action);
  }

  public void setDiffBottomComponent(JComponent diffBottomComponent) {
    myDiffBottomComponent = diffBottomComponent;
  }

  public void setToggleActionTitle(final String toggleActionTitle) {
    myToggleActionTitle = toggleActionTitle;
  }

  public JPanel getHeaderPanel() {
    return myHeaderPanel;
  }

  public ChangesTreeList<T> getViewer() {
    return myViewer;
  }

  @Nonnull
  public JScrollPane getViewerScrollPane() {
    return myViewerScrollPane;
  }

  public void calcData(Key<?> key, DataSink sink) {
    if (key == VcsDataKeys.CHANGES) {
      List<Change> list = getSelectedChanges();
      if (list.isEmpty()) list = getAllChanges();
      sink.put(VcsDataKeys.CHANGES, list.toArray(new Change[list.size()]));
    }
    else if (key == VcsDataKeys.CHANGES_SELECTION) {
      sink.put(VcsDataKeys.CHANGES_SELECTION, getChangesSelection());
    }
    else if (key == VcsDataKeys.CHANGE_LISTS) {
      sink.put(VcsDataKeys.CHANGE_LISTS, getSelectedChangeLists());
    }
    else if (key == VcsDataKeys.CHANGE_LEAD_SELECTION) {
      final Change highestSelection = ObjectUtil.tryCast(myViewer.getHighestLeadSelection(), Change.class);
      sink.put(VcsDataKeys.CHANGE_LEAD_SELECTION, (highestSelection == null) ? new Change[]{} : new Change[]{highestSelection});
    }
    else if (key == VirtualFile.KEY_OF_ARRAY) {
      sink.put(VirtualFile.KEY_OF_ARRAY, getSelectedFiles().toArray(VirtualFile[]::new));
    }
    else if (key == Navigatable.KEY_OF_ARRAY) {
      sink.put(Navigatable.KEY_OF_ARRAY, getNavigatableArray(myProject, getSelectedFiles()));
    }
    else if (VcsDataKeys.IO_FILE_ARRAY.equals(key)) {
      sink.put(VcsDataKeys.IO_FILE_ARRAY, getSelectedIoFiles());
    }
    else if (key == DATA_KEY) {
      sink.put(DATA_KEY, this);
    }
    else if (VcsDataKeys.SELECTED_CHANGES_IN_DETAILS.equals(key)) {
      final List<Change> selectedChanges = getSelectedChanges();
      sink.put(VcsDataKeys.SELECTED_CHANGES_IN_DETAILS, selectedChanges.toArray(new Change[selectedChanges.size()]));
    }
    else if (UNVERSIONED_FILES_DATA_KEY.equals(key)) {
      sink.put(UNVERSIONED_FILES_DATA_KEY, getVirtualFiles(myViewer.getSelectionPaths(), UNVERSIONED_FILES_TAG));
    }
    else if (DeleteProvider.KEY.equals(key)) {
      sink.put(DeleteProvider.KEY, myDeleteProvider);
    }
  }

  public void select(List<T> changes) {
    myViewer.select(changes);
  }

  public JComponent getBottomPanel() {
    return myBottomPanel;
  }

  private class ToggleChangeAction extends CheckboxAction {
    public ToggleChangeAction() {
      super(myToggleActionTitle);
    }

    @Override
    public boolean isSelected(AnActionEvent e) {
      T change = ObjectUtil.tryCast(e.getData(VcsDataKeys.CURRENT_CHANGE), myClass);
      return change != null && myViewer.isIncluded(change);
    }

    @Override
    public void setSelected(AnActionEvent e, boolean state) {
      T change = ObjectUtil.tryCast(e.getData(VcsDataKeys.CURRENT_CHANGE), myClass);
      if (change == null) return;

      if (state) {
        myViewer.includeChange(change);
      }
      else {
        myViewer.excludeChange(change);
      }
    }
  }

  protected void showDiffForChanges(Change[] changesArray, final int indexInSelection) {
    final ShowDiffContext context = new ShowDiffContext(isInFrame() ? DiffDialogHints.FRAME : DiffDialogHints.MODAL);

    context.addActions(createDiffActions());
    if (myDiffBottomComponent != null) {
      context.putChainContext(DiffUserDataKeysEx.BOTTOM_PANEL, myDiffBottomComponent);
    }

    updateDiffContext(context);

    ShowDiffAction.showDiffForChange(myProject, Arrays.asList(changesArray), indexInSelection, context);
  }

  protected void updateDiffContext(@Nonnull ShowDiffContext context) {
  }

  private boolean canShowDiff() {
    return ShowDiffAction.canShowDiff(myProject, getChangesSelection().getChanges());
  }

  private void showDiff() {
    ChangesSelection selection = getChangesSelection();
    List<Change> changes = selection.getChanges();

    Change[] changesArray = changes.toArray(new Change[changes.size()]);
    showDiffForChanges(changesArray, selection.getIndex());

    afterDiffRefresh();
  }

  @Nonnull
  protected ChangesSelection getChangesSelection() {
    final Change leadSelection = ObjectUtil.tryCast(myViewer.getLeadSelection(), Change.class);
    List<Change> changes = getSelectedChanges();

    if (changes.size() < 2) {
      List<Change> allChanges = getAllChanges();
      if (allChanges.size() > 1 || changes.isEmpty()) {
        changes = allChanges;
      }
    }

    if (leadSelection != null) {
      int indexInSelection = changes.indexOf(leadSelection);
      if (indexInSelection == -1) {
        return new ChangesSelection(Collections.singletonList(leadSelection), 0);
      }
      else {
        return new ChangesSelection(changes, indexInSelection);
      }
    }
    else {
      return new ChangesSelection(changes, 0);
    }
  }

  protected void afterDiffRefresh() {
  }

  private static boolean isInFrame() {
    return IdeaModalityState.current().equals(IdeaModalityState.nonModal());
  }

  protected List<AnAction> createDiffActions() {
    List<AnAction> actions = new ArrayList<>();
    if (myCapableOfExcludingChanges) {
      actions.add(new ToggleChangeAction());
    }
    return actions;
  }

  public void rebuildList() {
    myViewer.setChangesToDisplay(getCurrentDisplayedObjects(), myToSelect);
  }

  public void setAlwayExpandList(final boolean value) {
    myViewer.setAlwaysExpandList(value);
  }

  @Nonnull
  protected JComponent createToolbar() {
    DefaultActionGroup toolbarGroups = new DefaultActionGroup();
    myToolBarGroup = new DefaultActionGroup();
    toolbarGroups.add(myToolBarGroup);
    buildToolBar(myToolBarGroup);

    toolbarGroups.addSeparator();
    DefaultActionGroup treeActionsGroup = new DefaultActionGroup();
    toolbarGroups.add(treeActionsGroup);
    for (AnAction action : myViewer.getTreeActions()) {
      treeActionsGroup.add(action);
    }

    ActionToolbar toolbar = ActionManager.getInstance().createActionToolbar(ActionPlaces.TOOLBAR, toolbarGroups, true);
    toolbar.setTargetComponent(this);
    return toolbar.getComponent();
  }

  protected void buildToolBar(final DefaultActionGroup toolBarGroup) {
    myDiffAction = new DumbAwareAction() {
      @Override
      @RequiredUIAccess
      public void update(@Nonnull AnActionEvent e) {
        e.getPresentation().setEnabled(canShowDiff());
      }

      @Override
      @RequiredUIAccess
      public void actionPerformed(@Nonnull AnActionEvent e) {
        showDiff();
      }
    };
    ActionUtil.copyFrom(myDiffAction, "ChangesView.Diff");
    myDiffAction.registerCustomShortcutSet(myViewer, null);
    toolBarGroup.add(myDiffAction);
  }

  @Nonnull
  public Set<AbstractVcs> getAffectedVcses() {
    return ChangesUtil.getAffectedVcses(getCurrentDisplayedChanges(), myProject);
  }

  @Nonnull
  public abstract List<Change> getCurrentIncludedChanges();

  @Nonnull
  public List<Change> getCurrentDisplayedChanges() {
    return mySelectedChangeList != null ? ContainerUtil.newArrayList(mySelectedChangeList.getChanges()) : Collections.emptyList();
  }

  @Nonnull
  public abstract List<T> getCurrentDisplayedObjects();

  @Nonnull
  public List<VirtualFile> getIncludedUnversionedFiles() {
    return Collections.emptyList();
  }

  public int getUnversionedFilesCount() {
    return 0;
  }

  public ChangeList getSelectedChangeList() {
    return mySelectedChangeList;
  }

  public JComponent getPreferredFocusedComponent() {
    return myViewer.getPreferredFocusedComponent();
  }

  private ChangeList[] getSelectedChangeLists() {
    if (mySelectedChangeList != null) {
      return new ChangeList[]{mySelectedChangeList};
    }
    return null;
  }

  private File[] getSelectedIoFiles() {
    final List<Change> changes = getSelectedChanges();
    final List<File> files = new ArrayList<>();
    for (Change change : changes) {
      final ContentRevision afterRevision = change.getAfterRevision();
      if (afterRevision != null) {
        final FilePath file = afterRevision.getFile();
        final File ioFile = file.getIOFile();
        files.add(ioFile);
      }
    }
    return files.toArray(new File[files.size()]);
  }

  @Nonnull
  public abstract List<Change> getSelectedChanges();

  @Nonnull
  public abstract List<Change> getAllChanges();

  @Nonnull
  protected Stream<VirtualFile> getSelectedFiles() {
    return Stream.concat(
      getAfterRevisionsFiles(getSelectedChanges().stream()),
      getVirtualFiles(myViewer.getSelectionPaths(), null)
    ).distinct();
  }

  public AnAction getDiffAction() {
    return myDiffAction;
  }

  public boolean isDataIsDirty() {
    return myDataIsDirty;
  }

  public void setDataIsDirty(boolean dataIsDirty) {
    myDataIsDirty = dataIsDirty;
  }

  public void setSelectionMode(@JdkConstants.TreeSelectionMode int mode) {
    myViewer.setSelectionMode(mode);
  }

  @Contract(pure = true)
  @Nonnull
  protected static <T> List<Change> findChanges(@Nonnull Collection<T> items) {
    return ContainerUtil.findAll(items, Change.class);
  }

  static boolean isUnderUnversioned(@Nonnull ChangesBrowserNode node) {
    return isUnderTag(new TreePath(node.getPath()), UNVERSIONED_FILES_TAG);
  }
}
