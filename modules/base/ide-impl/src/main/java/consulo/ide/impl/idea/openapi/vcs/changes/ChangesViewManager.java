/*
 * Copyright 2000-2015 JetBrains s.r.o.
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

package consulo.ide.impl.idea.openapi.vcs.changes;

import consulo.annotation.component.ComponentProfiles;
import consulo.annotation.component.ServiceImpl;
import consulo.application.AllIcons;
import consulo.application.ApplicationManager;
import consulo.application.dumb.DumbAware;
import consulo.application.impl.internal.IdeaModalityState;
import consulo.application.util.NotNullLazyValue;
import consulo.component.persist.PersistentStateComponent;
import consulo.component.persist.State;
import consulo.component.persist.Storage;
import consulo.component.persist.StoragePathMacros;
import consulo.diff.DiffPlaces;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.document.FileDocumentManager;
import consulo.ide.impl.idea.diff.util.DiffUtil;
import consulo.ide.impl.idea.openapi.vcs.changes.actions.IgnoredSettingsAction;
import consulo.ide.impl.idea.openapi.vcs.changes.shelf.ShelveChangesManager;
import consulo.ide.impl.idea.openapi.vcs.changes.ui.*;
import consulo.ide.impl.idea.util.FunctionUtil;
import consulo.language.impl.DebugUtil;
import consulo.logging.Logger;
import consulo.platform.Platform;
import consulo.project.Project;
import consulo.ui.ex.JBColor;
import consulo.ui.ex.TreeExpander;
import consulo.ui.ex.action.*;
import consulo.ui.ex.awt.*;
import consulo.ui.ex.awt.dnd.DnDEvent;
import consulo.ui.ex.awt.tree.TreeUtil;
import consulo.ui.ex.awt.util.Alarm;
import consulo.ui.ex.content.Content;
import consulo.util.lang.StringUtil;
import consulo.util.xml.serializer.annotation.Attribute;
import consulo.versionControlSystem.ProjectLevelVcsManager;
import consulo.versionControlSystem.VcsBundle;
import consulo.versionControlSystem.VcsConfiguration;
import consulo.versionControlSystem.VcsException;
import consulo.versionControlSystem.change.*;
import consulo.versionControlSystem.localize.VcsLocalize;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.intellij.lang.annotations.JdkConstants;

import javax.swing.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

import static java.util.stream.Collectors.toList;

@State(name = "ChangesViewManager", storages = @Storage(file = StoragePathMacros.WORKSPACE_FILE))
@Singleton
@ServiceImpl(profiles = ComponentProfiles.AWT)
public class ChangesViewManager implements ChangesViewI, Disposable, PersistentStateComponent<ChangesViewManager.State> {

  private static final Logger LOG = Logger.getInstance(ChangesViewManager.class);

  @Nonnull
  private final ChangesListView myView;
  private JPanel myProgressLabel;

  private final Alarm myRepaintAlarm;

  private boolean myDisposed = false;

  @Nonnull
  private final ChangeListListener myListener = new MyChangeListListener();
  @Nonnull
  private final Project myProject;
  @Nonnull
  private final ChangesViewContentI myContentManager;

  @Nonnull
  private ChangesViewManager.State myState = new ChangesViewManager.State();

  private JBSplitter mySplitter;

  private boolean myDetailsOn;
  @Nonnull
  private final NotNullLazyValue<MyChangeProcessor> myDiffDetails = new NotNullLazyValue<MyChangeProcessor>() {
    @Nonnull
    @Override
    protected MyChangeProcessor compute() {
      return new MyChangeProcessor(myProject);
    }
  };

  @Nonnull
  private final TreeSelectionListener myTsl;
  private Content myContent;

  @Nonnull
  public static ChangesViewI getInstance(@Nonnull Project project) {
    return project.getComponent(ChangesViewI.class);
  }

  @Inject
  public ChangesViewManager(@Nonnull Project project, @Nonnull ChangesViewContentI contentManager) {
    myProject = project;
    myContentManager = contentManager;
    myView = new ChangesListView(project);
    myRepaintAlarm = new Alarm(Alarm.ThreadToUse.SWING_THREAD, project);
    myTsl = new TreeSelectionListener() {
      @Override
      public void valueChanged(TreeSelectionEvent e) {
        if (LOG.isDebugEnabled()) {
          TreePath[] paths = myView.getSelectionPaths();
          String joinedPaths = paths != null ? StringUtil.join(paths, FunctionUtil.string(), ", ") : null;
          String message = "selection changed. selected:  " + joinedPaths;

          if (LOG.isTraceEnabled()) {
            LOG.trace(message + " from: " + DebugUtil.currentStackTrace());
          }
          else {
            LOG.debug(message);
          }
        }
        SwingUtilities.invokeLater(new Runnable() {
          @Override
          public void run() {
            changeDetails();
          }
        });
      }
    };
  }

  public void projectOpened() {
    final ChangeListManager changeListManager = ChangeListManager.getInstance(myProject);
    changeListManager.addChangeListListener(myListener);
    Disposer.register(myProject, () -> changeListManager.removeChangeListListener(myListener));

    myContent = new MyChangeViewContent(createChangeViewComponent(), ChangesViewContentManager.LOCAL_CHANGES, false);
    myContent.setCloseable(false);
    myProject.getUIAccess().give(() -> myContentManager.addContent(myContent));

    scheduleRefresh();
    myProject.getMessageBus().connect().subscribe(RemoteRevisionChangeListener.class, () -> ApplicationManager.getApplication().invokeLater(() -> refreshView(), IdeaModalityState.NON_MODAL, myProject.getDisposed()));

    myDetailsOn = VcsConfiguration.getInstance(myProject).LOCAL_CHANGES_DETAILS_PREVIEW_SHOWN;
    changeDetails();
  }

  @Override
  public void dispose() {
    myView.removeTreeSelectionListener(myTsl);
    myDisposed = true;
    myRepaintAlarm.cancelAllRequests();
  }

  private JComponent createChangeViewComponent() {
    SimpleToolWindowPanel panel = new SimpleToolWindowPanel(false, true);

    EmptyAction.registerWithShortcutSet("ChangesView.Refresh", CommonShortcuts.getRerun(), panel);
    EmptyAction.registerWithShortcutSet("ChangesView.NewChangeList", CommonShortcuts.getNew(), panel);
    EmptyAction.registerWithShortcutSet("ChangesView.RemoveChangeList", CommonShortcuts.getDelete(), panel);
    EmptyAction.registerWithShortcutSet(IdeActions.MOVE_TO_ANOTHER_CHANGE_LIST, CommonShortcuts.getMove(), panel);
    EmptyAction.registerWithShortcutSet("ChangesView.Rename", CommonShortcuts.getRename(), panel);
    EmptyAction.registerWithShortcutSet("ChangesView.SetDefault", new CustomShortcutSet(KeyStroke.getKeyStroke(KeyEvent.VK_U, InputEvent.ALT_DOWN_MASK | ctrlMask())), panel);
    EmptyAction.registerWithShortcutSet("ChangesView.Diff", CommonShortcuts.getDiff(), panel);

    DefaultActionGroup group = (DefaultActionGroup)ActionManager.getInstance().getAction("ChangesViewToolbar");
    ActionToolbar toolbar = ActionManager.getInstance().createActionToolbar(ActionPlaces.CHANGES_VIEW_TOOLBAR, group, false);
    toolbar.setTargetComponent(myView);
    JComponent toolbarComponent = toolbar.getComponent();
    JPanel toolbarPanel = new JPanel(new BorderLayout());
    toolbarPanel.add(toolbarComponent, BorderLayout.WEST);

    DefaultActionGroup visualActionsGroup = new DefaultActionGroup();
    final Expander expander = new Expander();
    visualActionsGroup.add(CommonActionsManager.getInstance().createExpandAllAction(expander, panel));
    visualActionsGroup.add(CommonActionsManager.getInstance().createCollapseAllAction(expander, panel));

    ToggleShowFlattenAction showFlattenAction = new ToggleShowFlattenAction();
    showFlattenAction.registerCustomShortcutSet(new CustomShortcutSet(KeyStroke.getKeyStroke(KeyEvent.VK_P, ctrlMask())), panel);
    visualActionsGroup.add(showFlattenAction);
    visualActionsGroup.add(ActionManager.getInstance().getAction(IdeActions.ACTION_COPY));
    visualActionsGroup.add(new ToggleShowIgnoredAction());
    visualActionsGroup.add(new IgnoredSettingsAction());
    visualActionsGroup.add(new ToggleDetailsAction());
    visualActionsGroup.add(new ContextHelpAction(ChangesListView.HELP_ID));
    ActionToolbar actionToolbar = ActionManager.getInstance().createActionToolbar(ActionPlaces.CHANGES_VIEW_TOOLBAR, visualActionsGroup, false);
    actionToolbar.setTargetComponent(panel);
    toolbarPanel.add(actionToolbar.getComponent(), BorderLayout.CENTER);


    myView.setMenuActions((DefaultActionGroup)ActionManager.getInstance().getAction("ChangesViewPopupMenu"));

    myView.setShowFlatten(myState.myShowFlatten);

    myProgressLabel = new JPanel(new BorderLayout());

    panel.setToolbar(toolbarPanel);

    final JPanel content = new JPanel(new BorderLayout());
    mySplitter = new JBSplitter(false, "ChangesViewManager.DETAILS_SPLITTER_PROPORTION", 0.5f);
    mySplitter.setHonorComponentsMinimumSize(false);
    final JScrollPane scrollPane = ScrollPaneFactory.createScrollPane(myView);
    final JPanel wrapper = new JPanel(new BorderLayout());
    wrapper.add(scrollPane, BorderLayout.CENTER);
    mySplitter.setFirstComponent(wrapper);
    content.add(mySplitter, BorderLayout.CENTER);
    content.add(myProgressLabel, BorderLayout.SOUTH);
    panel.setContent(content);

    ChangesDnDSupport.install(myProject, myView);
    myView.addTreeSelectionListener(myTsl);
    return panel;
  }

  private void changeDetails() {
    if (!myDetailsOn) {
      if (myDiffDetails.isComputed()) {
        myDiffDetails.getValue().clear();

        if (mySplitter.getSecondComponent() != null) {
          setChangeDetailsPanel(null);
        }
      }
    }
    else {
      myDiffDetails.getValue().refresh();

      if (mySplitter.getSecondComponent() == null) {
        setChangeDetailsPanel(myDiffDetails.getValue().getComponent());
      }
    }
  }

  private void setChangeDetailsPanel(@Nullable JComponent component) {
    mySplitter.setSecondComponent(component);
    mySplitter.getFirstComponent().setBorder(component == null ? null : IdeBorderFactory.createBorder(SideBorder.RIGHT));
    mySplitter.revalidate();
    mySplitter.repaint();
  }

  @JdkConstants.InputEventMask
  private static int ctrlMask() {
    return Platform.current().os().isMac() ? InputEvent.META_DOWN_MASK : InputEvent.CTRL_DOWN_MASK;
  }

  private void updateProgressComponent(@Nonnull final Supplier<JComponent> progress) {
    //noinspection SSBasedInspection
    SwingUtilities.invokeLater(() -> {
      if (myProgressLabel != null) {
        myProgressLabel.removeAll();
        myProgressLabel.add(progress.get());
        myProgressLabel.setMinimumSize(JBUI.emptySize());
      }
    });
  }

  public void updateProgressText(String text, boolean isError) {
    updateProgressComponent(createTextStatusFactory(text, isError));
  }

  @Override
  public void setBusy(final boolean b) {
    UIUtil.invokeLaterIfNeeded(new Runnable() {
      @Override
      public void run() {
        myView.setPaintBusy(b);
      }
    });
  }

  @Nonnull
  public static Supplier<JComponent> createTextStatusFactory(final String text, final boolean isError) {
    return new Supplier<JComponent>() {
      @Override
      public JComponent get() {
        JLabel label = new JLabel(text);
        label.setForeground(isError ? JBColor.RED : UIUtil.getLabelForeground());
        return label;
      }
    };
  }

  @Override
  public void scheduleRefresh() {
    if (ApplicationManager.getApplication().isHeadlessEnvironment()) return;
    if (myProject.isDisposed()) return;
    int was = myRepaintAlarm.cancelAllRequests();
    if (LOG.isDebugEnabled()) {
      LOG.debug("schedule refresh, was " + was);
    }
    if (!myRepaintAlarm.isDisposed()) {
      myRepaintAlarm.addRequest(new Runnable() {
        public void run() {
          refreshView();
        }
      }, 100, IdeaModalityState.NON_MODAL);
    }
  }

  private void refreshView() {
    if (myDisposed || !myProject.isInitialized() || ApplicationManager.getApplication().isUnitTestMode()) return;
    if (!ProjectLevelVcsManager.getInstance(myProject).hasActiveVcss()) return;

    ChangeListManagerImpl changeListManager = ChangeListManagerImpl.getInstanceImpl(myProject);

    TreeModelBuilder treeModelBuilder =
            new TreeModelBuilder(myProject, myView.isShowFlatten()).setChangeLists(changeListManager.getChangeListsCopy()).setLocallyDeletedPaths(changeListManager.getDeletedFiles())
                    .setModifiedWithoutEditing(changeListManager.getModifiedWithoutEditing()).setSwitchedFiles(changeListManager.getSwitchedFilesMap())
                    .setSwitchedRoots(changeListManager.getSwitchedRoots()).setLockedFolders(changeListManager.getLockedFolders())
                    .setLogicallyLockedFiles(changeListManager.getLogicallyLockedFolders()).setUnversioned(changeListManager.getUnversionedFiles());
    if (myState.myShowIgnored) {
      treeModelBuilder.setIgnored(changeListManager.getIgnoredFiles(), changeListManager.isIgnoredInUpdateMode());
    }
    myView.updateModel(treeModelBuilder.build());

    changeDetails();
  }

  @Nonnull
  @Override
  public ChangesViewManager.State getState() {
    return myState;
  }

  @Override
  public void loadState(@Nonnull ChangesViewManager.State state) {
    myState = state;
  }

  @Override
  public void setShowFlattenMode(boolean state) {
    myState.myShowFlatten = state;
    myView.setShowFlatten(state);
    refreshView();
  }

  @Override
  public void selectFile(@Nullable VirtualFile vFile) {
    if (vFile == null) return;
    Change change = ChangeListManager.getInstance(myProject).getChange(vFile);
    Object objectToFind = change != null ? change : vFile;

    DefaultMutableTreeNode root = (DefaultMutableTreeNode)myView.getModel().getRoot();
    DefaultMutableTreeNode node = TreeUtil.findNodeWithObject(root, objectToFind);
    if (node != null) {
      TreeUtil.selectNode(myView, node);
    }
  }

  @Override
  public void refreshChangesViewNodeAsync(@Nonnull final VirtualFile file) {
    ApplicationManager.getApplication().invokeLater(new Runnable() {
      public void run() {
        refreshChangesViewNode(file);
      }
    }, myProject.getDisposed());
  }

  private void refreshChangesViewNode(@Nonnull VirtualFile file) {
    ChangeListManager changeListManager = ChangeListManager.getInstance(myProject);
    Object userObject = changeListManager.isUnversioned(file) ? file : changeListManager.getChange(file);

    if (userObject != null) {
      DefaultMutableTreeNode root = (DefaultMutableTreeNode)myView.getModel().getRoot();
      DefaultMutableTreeNode node = TreeUtil.findNodeWithObject(root, userObject);

      if (node != null) {
        myView.getModel().nodeChanged(node);
      }
    }
  }

  public static class State {

    @Attribute("flattened_view")
    public boolean myShowFlatten = true;

    @Attribute("show_ignored")
    public boolean myShowIgnored;
  }

  private class MyChangeListListener extends ChangeListAdapter {

    public void changeListAdded(ChangeList list) {
      scheduleRefresh();
    }

    public void changeListRemoved(ChangeList list) {
      scheduleRefresh();
    }

    public void changeListRenamed(ChangeList list, String oldName) {
      scheduleRefresh();
    }

    public void changesMoved(Collection<Change> changes, ChangeList fromList, ChangeList toList) {
      scheduleRefresh();
    }

    public void defaultListChanged(final ChangeList oldDefaultList, ChangeList newDefaultList) {
      scheduleRefresh();
    }

    public void changeListUpdateDone() {
      scheduleRefresh();
      ChangeListManagerImpl changeListManager = ChangeListManagerImpl.getInstanceImpl(myProject);
      VcsException updateException = changeListManager.getUpdateException();
      setBusy(false);
      if (updateException == null) {
        Supplier<JComponent> additionalUpdateInfo = changeListManager.getAdditionalUpdateInfo();

        if (additionalUpdateInfo != null) {
          updateProgressComponent(additionalUpdateInfo);
        }
        else {
          updateProgressText("", false);
        }
      }
      else {
        updateProgressText(VcsLocalize.errorUpdatingChanges(updateException.getMessage()).get(), true);
      }
    }
  }

  private class Expander implements TreeExpander {
    public void expandAll() {
      TreeUtil.expandAll(myView);
    }

    public boolean canExpand() {
      return true;
    }

    public void collapseAll() {
      TreeUtil.collapseAll(myView, 2);
      TreeUtil.expand(myView, 1);
    }

    public boolean canCollapse() {
      return true;
    }
  }

  private class ToggleShowFlattenAction extends ToggleAction implements DumbAware {
    public ToggleShowFlattenAction() {
      super(
        VcsLocalize.changesActionShowDirectoriesText(),
        VcsLocalize.changesActionShowDirectoriesDescription(),
        AllIcons.Actions.GroupByPackage
      );
    }

    public boolean isSelected(AnActionEvent e) {
      return !myState.myShowFlatten;
    }

    public void setSelected(AnActionEvent e, boolean state) {
      setShowFlattenMode(!state);
    }
  }

  private class ToggleShowIgnoredAction extends ToggleAction implements DumbAware {
    public ToggleShowIgnoredAction() {
      super(
        VcsLocalize.changesActionShowIgnoredText(),
        VcsLocalize.changesActionShowIgnoredDescription(),
        AllIcons.Actions.ShowHiddens
      );
    }

    public boolean isSelected(AnActionEvent e) {
      return myState.myShowIgnored;
    }

    public void setSelected(AnActionEvent e, boolean state) {
      myState.myShowIgnored = state;
      refreshView();
    }
  }

  private class ToggleDetailsAction extends ToggleAction implements DumbAware {
    private ToggleDetailsAction() {
      super("Preview Diff", null, AllIcons.Actions.PreviewDetails);
    }

    @Override
    public boolean isSelected(AnActionEvent e) {
      return myDetailsOn;
    }

    @Override
    public void setSelected(AnActionEvent e, boolean state) {
      myDetailsOn = state;
      VcsConfiguration.getInstance(myProject).LOCAL_CHANGES_DETAILS_PREVIEW_SHOWN = myDetailsOn;
      changeDetails();
    }
  }

  private class MyChangeProcessor extends CacheChangeProcessor {
    public MyChangeProcessor(@Nonnull Project project) {
      super(project, DiffPlaces.CHANGES_VIEW);
      Disposer.register(project, this);
    }

    @Override
    public boolean isWindowFocused() {
      return DiffUtil.isFocusedComponent(myProject, myContent.getComponent());
    }

    @Nonnull
    @Override
    protected List<Change> getSelectedChanges() {
      List<Change> result = myView.getSelectedChanges().collect(toList());
      if (result.isEmpty()) result = myView.getChanges().collect(toList());
      return result;
    }

    @Nonnull
    @Override
    protected List<Change> getAllChanges() {
      return myView.getChanges().collect(toList());
    }

    @Override
    protected void selectChange(@Nonnull Change change) {
      DefaultMutableTreeNode root = (DefaultMutableTreeNode)myView.getModel().getRoot();
      DefaultMutableTreeNode node = TreeUtil.findNodeWithObject(root, change);
      if (node != null) {
        TreePath path = TreeUtil.getPathFromRoot(node);
        TreeUtil.selectPath(myView, path, false);
      }
    }
  }

  private class MyChangeViewContent extends DnDTargetContentAdapter {
    private MyChangeViewContent(JComponent component, String displayName, boolean isLockable) {
      super(component, displayName, isLockable);
    }

    @Override
    public void drop(DnDEvent event) {
      Object attachedObject = event.getAttachedObject();
      if (attachedObject instanceof ShelvedChangeListDragBean) {
        FileDocumentManager.getInstance().saveAllDocuments();
        ShelvedChangeListDragBean shelvedBean = (ShelvedChangeListDragBean)attachedObject;
        ShelveChangesManager.getInstance(myProject).unshelveSilentlyAsynchronously(myProject, shelvedBean.getShelvedChangelists(), shelvedBean.getChanges(), shelvedBean.getBinaryFiles(), null);
      }
    }

    @Override
    public boolean update(DnDEvent event) {
      Object attachedObject = event.getAttachedObject();
      if (attachedObject instanceof ShelvedChangeListDragBean) {
        ShelvedChangeListDragBean shelveBean = (ShelvedChangeListDragBean)attachedObject;
        event.setDropPossible(!shelveBean.getShelvedChangelists().isEmpty());
        return false;
      }
      return true;
    }
  }
}
