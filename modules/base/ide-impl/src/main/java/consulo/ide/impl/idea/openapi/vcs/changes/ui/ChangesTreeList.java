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
package consulo.ide.impl.idea.openapi.vcs.changes.ui;

import consulo.application.AllIcons;
import consulo.application.ApplicationManager;
import consulo.application.dumb.DumbAware;
import consulo.dataContext.DataSink;
import consulo.dataContext.TypeSafeDataProvider;
import consulo.ide.impl.idea.ide.projectView.impl.ProjectViewTree;
import consulo.ide.impl.idea.ide.util.PropertiesComponent;
import consulo.ide.impl.idea.openapi.diff.DiffBundle;
import consulo.ide.impl.idea.openapi.util.io.FileUtil;
import consulo.ide.impl.idea.openapi.vcs.changes.issueLinks.TreeLinkMouseListener;
import consulo.ide.impl.idea.ui.SmartExpander;
import consulo.ide.impl.idea.util.containers.ContainerUtil;
import consulo.language.editor.FileColorManager;
import consulo.language.editor.PlatformDataKeys;
import consulo.platform.Platform;
import consulo.project.Project;
import consulo.ui.ex.CopyProvider;
import consulo.ui.ex.action.*;
import consulo.ui.ex.awt.NonOpaquePanel;
import consulo.ui.ex.awt.PopupHandler;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awt.event.DoubleClickListener;
import consulo.ui.ex.awt.internal.laf.WideSelectionTreeUI;
import consulo.ui.ex.awt.speedSearch.TreeSpeedSearch;
import consulo.ui.ex.awt.tree.CheckboxTree;
import consulo.ui.ex.awt.tree.Tree;
import consulo.ui.ex.awt.tree.TreeState;
import consulo.ui.ex.awt.tree.TreeUtil;
import consulo.ui.ex.awt.tree.action.CollapseAllAction;
import consulo.ui.ex.awt.tree.action.ExpandAllAction;
import consulo.ui.ex.keymap.KeymapManager;
import consulo.util.dataholder.Key;
import consulo.util.lang.EmptyRunnable;
import consulo.util.lang.ObjectUtil;
import consulo.util.lang.StringUtil;
import consulo.util.lang.ref.Ref;
import consulo.versionControlSystem.FilePath;
import consulo.versionControlSystem.VcsBundle;
import consulo.versionControlSystem.change.Change;
import consulo.versionControlSystem.change.ContentRevision;
import consulo.versionControlSystem.localize.VcsLocalize;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.intellij.lang.annotations.JdkConstants;
import org.jetbrains.annotations.NonNls;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.plaf.basic.BasicTreeUI;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.event.*;
import java.util.List;
import java.util.*;

/**
 * @author max
 */
public abstract class ChangesTreeList<T> extends Tree implements TypeSafeDataProvider {

  @Nonnull
  protected final Project myProject;
  private final boolean myShowCheckboxes;
  private final int myCheckboxWidth;
  private final boolean myHighlightProblems;
  private boolean myShowFlatten;
  private boolean myIsModelFlat;

  @Nonnull
  private final Set<T> myIncludedChanges;
  @Nonnull
  private Runnable myDoubleClickHandler = EmptyRunnable.getInstance();
  private boolean myAlwaysExpandList;

  @Nonnull
  private final MyTreeCellRenderer myNodeRenderer;

  @NonNls private static final String ROOT = "root";

  @NonNls private final static String FLATTEN_OPTION_KEY = "ChangesBrowser.SHOW_FLATTEN";

  @Nullable private final Runnable myInclusionListener;
  @jakarta.annotation.Nullable
  private ChangeNodeDecorator myChangeDecorator;
  private Runnable myGenericSelectionListener;
  @Nonnull
  private final CopyProvider myTreeCopyProvider;
  private TreeState myNonFlatTreeState;

  public ChangesTreeList(@Nonnull Project project,
                         @Nonnull Collection<T> initiallyIncluded,
                         final boolean showCheckboxes,
                         final boolean highlightProblems,
                         @Nullable final Runnable inclusionListener,
                         @jakarta.annotation.Nullable final ChangeNodeDecorator decorator) {
    super(ChangesBrowserNode.create(project, ROOT));
    myProject = project;
    myShowCheckboxes = showCheckboxes;
    myHighlightProblems = highlightProblems;
    myInclusionListener = inclusionListener;
    myChangeDecorator = decorator;
    myIncludedChanges = new HashSet<>(initiallyIncluded);
    myAlwaysExpandList = true;
    final ChangesBrowserNodeRenderer nodeRenderer = new ChangesBrowserNodeRenderer(myProject, () -> myShowFlatten, myHighlightProblems);
    myNodeRenderer = new MyTreeCellRenderer(nodeRenderer);
    myCheckboxWidth = new JCheckBox().getPreferredSize().width;

    setHorizontalAutoScrollingEnabled(false);
    setRootVisible(false);
    setShowsRootHandles(true);
    setOpaque(false);
    new TreeSpeedSearch(this, ChangesBrowserNode.TO_TEXT_CONVERTER);
    setCellRenderer(myNodeRenderer);

    new MyToggleSelectionAction().registerCustomShortcutSet(new CustomShortcutSet(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0)), this);
    registerKeyboardAction(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        myDoubleClickHandler.run();
      }
    }, KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

    addKeyListener(new KeyAdapter() {
      @Override
      public void keyPressed(KeyEvent e) {
        if (KeyEvent.VK_ENTER == e.getKeyCode() && e.getModifiers() == 0) {
          if (getSelectionCount() <= 1) {
            Object lastPathComponent = getLastSelectedPathComponent();
            if (!(lastPathComponent instanceof DefaultMutableTreeNode)) {
              return;
            }
            DefaultMutableTreeNode node = (DefaultMutableTreeNode)lastPathComponent;
            if (!node.isLeaf()) {
              return;
            }
          }
          myDoubleClickHandler.run();
          e.consume();
        }
      }
    });

    new DoubleClickListener() {
      @Override
      protected boolean onDoubleClick(MouseEvent e) {
        TreePath clickPath =
                getUI() instanceof WideSelectionTreeUI ? getClosestPathForLocation(e.getX(), e.getY()) : getPathForLocation(e.getX(), e.getY());
        if (clickPath == null) return false;

        final int row = getRowForLocation(e.getPoint().x, e.getPoint().y);
        if (row >= 0) {
          if (myShowCheckboxes) {
            final Rectangle baseRect = getRowBounds(row);
            baseRect.setSize(myCheckboxWidth, baseRect.height);
            if (baseRect.contains(e.getPoint())) return false;
          }
        }

        myDoubleClickHandler.run();
        return true;
      }
    }.installOn(this);

    new TreeLinkMouseListener(myNodeRenderer.myTextRenderer) {
      @Override
      protected int getRendererRelativeX(@Nonnull MouseEvent e, @Nonnull JTree tree, @Nonnull TreePath path) {
        int x = super.getRendererRelativeX(e, tree, path);

        return !myShowCheckboxes ? x : x - myCheckboxWidth;
      }
    }.installOn(this);
    SmartExpander.installOn(this);

    setShowFlatten(PropertiesComponent.getInstance(myProject).isTrueValue(FLATTEN_OPTION_KEY));

    String emptyText = StringUtil.capitalize(DiffBundle.message("diff.count.differences.status.text", 0));
    setEmptyText(emptyText);

    myTreeCopyProvider = new ChangesBrowserNodeCopyProvider(this);
  }

  public void setEmptyText(@Nonnull String emptyText) {
    getEmptyText().setText(emptyText);
  }

  public void addSelectionListener(final Runnable runnable) {
    myGenericSelectionListener = runnable;
    addTreeSelectionListener(new TreeSelectionListener() {
      @Override
      public void valueChanged(TreeSelectionEvent e) {
        myGenericSelectionListener.run();
      }
    });
  }

  public void setChangeDecorator(@Nullable ChangeNodeDecorator changeDecorator) {
    myChangeDecorator = changeDecorator;
  }

  public void setDoubleClickHandler(@Nonnull final Runnable doubleClickHandler) {
    myDoubleClickHandler = doubleClickHandler;
  }

  public void installPopupHandler(ActionGroup group) {
    PopupHandler.installUnknownPopupHandler(this, group, ActionManager.getInstance());
  }

  public JComponent getPreferredFocusedComponent() {
    return this;
  }

  public boolean isShowFlatten() {
    return myShowFlatten;
  }

  /**
   * Does nothing as ChangesTreeList is currently not wrapped in JScrollPane by default.
   * Left not to break API (used in several plugins).
   *
   * @deprecated to remove in 2017.
   */
  @SuppressWarnings("unused")
  @Deprecated
  public void setScrollPaneBorder(Border border) {
  }

  public void setShowFlatten(final boolean showFlatten) {
    final List<T> wasSelected = getSelectedChanges();
    if (!myAlwaysExpandList && !myShowFlatten) {
      myNonFlatTreeState = TreeState.createOn(this, getRoot());
    }
    myShowFlatten = showFlatten;
    setChangesToDisplay(getChanges());
    if (!myAlwaysExpandList && !myShowFlatten && myNonFlatTreeState != null) {
      myNonFlatTreeState.applyTo(this, getRoot());
    }
    select(wasSelected);
  }

  private void setChildIndent(boolean isFlat) {
    BasicTreeUI treeUI = (BasicTreeUI)getUI();

    treeUI.setLeftChildIndent(!isFlat ? UIUtil.getTreeLeftChildIndent() : 0);
    treeUI.setRightChildIndent(!isFlat ? UIUtil.getTreeRightChildIndent() : 0);
  }

  protected boolean isCurrentModelFlat() {
    boolean isFlat = true;
    Enumeration enumeration = getRoot().depthFirstEnumeration();

    while (isFlat && enumeration.hasMoreElements()) {
      isFlat = ((ChangesBrowserNode)enumeration.nextElement()).getLevel() <= 1;
    }

    return isFlat;
  }

  public void setChangesToDisplay(final List<T> changes) {
    setChangesToDisplay(changes, null);
  }

  public void setChangesToDisplay(final List<T> changes, @Nullable final VirtualFile toSelect) {
    final DefaultTreeModel model = buildTreeModel(changes, myChangeDecorator);
    TreeState state = null;
    if (!myAlwaysExpandList) {
      state = TreeState.createOn(this, getRoot());
    }
    setModel(model);
    myIsModelFlat = isCurrentModelFlat();
    setChildIndent(myShowFlatten && myIsModelFlat);
    if (!myAlwaysExpandList) {
      //noinspection ConstantConditions
      state.applyTo(this, getRoot());
      return;
    }

    final Runnable runnable = new Runnable() {
      @Override
      public void run() {
        if (myProject.isDisposed()) return;
        TreeUtil.expandAll(ChangesTreeList.this);

        int selectedTreeRow = -1;

        if (myShowCheckboxes) {
          if (myIncludedChanges.size() > 0) {
            ChangesBrowserNode root = (ChangesBrowserNode)model.getRoot();
            Enumeration enumeration = root.depthFirstEnumeration();

            while (enumeration.hasMoreElements()) {
              ChangesBrowserNode node = (ChangesBrowserNode)enumeration.nextElement();
              @SuppressWarnings("unchecked")
              final CheckboxTree.NodeState state = getNodeStatus(node);
              if (node != root && state == CheckboxTree.NodeState.CLEAR) {
                collapsePath(new TreePath(node.getPath()));
              }
            }

            enumeration = root.depthFirstEnumeration();
            while (enumeration.hasMoreElements()) {
              ChangesBrowserNode node = (ChangesBrowserNode)enumeration.nextElement();
              @SuppressWarnings("unchecked")
              final CheckboxTree.NodeState state = getNodeStatus(node);
              if (state == CheckboxTree.NodeState.FULL && node.isLeaf()) {
                selectedTreeRow = getRowForPath(new TreePath(node.getPath()));
                break;
              }
            }
          }
        }
        if (toSelect != null) {
          int rowInTree = findRowContainingFile((TreeNode)model.getRoot(), toSelect);
          if (rowInTree > -1) {
            selectedTreeRow = rowInTree;
          }
        }

        if (selectedTreeRow >= 0) {
          setSelectionRow(selectedTreeRow);
        }
        TreeUtil.showRowCentered(ChangesTreeList.this, selectedTreeRow, false);
      }
    };
    if (ApplicationManager.getApplication().isDispatchThread()) {
      runnable.run();
    } else {
      SwingUtilities.invokeLater(runnable);
    }
  }

  private int findRowContainingFile(@Nonnull TreeNode root, @Nonnull final VirtualFile toSelect) {
    final Ref<Integer> row = Ref.create(-1);
    TreeUtil.traverse(root, node -> {
      if (node instanceof DefaultMutableTreeNode) {
        Object userObject = ((DefaultMutableTreeNode)node).getUserObject();
        if (userObject instanceof Change) {
          if (matches((Change)userObject, toSelect)) {
            TreeNode[] path = ((DefaultMutableTreeNode)node).getPath();
            row.set(getRowForPath(new TreePath(path)));
          }
        }
      }

      return row.get() == -1;
    });
    return row.get();
  }

  private static boolean matches(@Nonnull Change change, @Nonnull VirtualFile file) {
    VirtualFile virtualFile = change.getVirtualFile();
    return virtualFile != null && virtualFile.equals(file) || seemsToBeMoved(change, file);
  }

  private static boolean seemsToBeMoved(Change change, VirtualFile toSelect) {
    ContentRevision afterRevision = change.getAfterRevision();
    if (afterRevision == null) return false;
    FilePath file = afterRevision.getFile();
    return FileUtil.pathsEqual(file.getPath(), toSelect.getPath());
  }

  protected abstract DefaultTreeModel buildTreeModel(final List<T> changes, final ChangeNodeDecorator changeNodeDecorator);

  private void toggleSelection() {
    toggleChanges(getSelectedChanges());
  }

  /**
   * TODO: This method does not respect T type parameter while filling the result - just "Change" class is used
   * TODO: ("ChangesBrowserNode.getAllChangesUnder()").
   */
  @Nonnull
  public List<T> getChanges() {
    //noinspection unchecked
    return ((ChangesBrowserNode)getRoot()).getAllChangesUnder();
  }

  @Nonnull
  public List<T> getSelectedChanges() {
    final TreePath[] paths = getSelectionPaths();
    if (paths == null) {
      return Collections.emptyList();
    }
    else {
      LinkedHashSet<T> changes = ContainerUtil.newLinkedHashSet();
      for (TreePath path : paths) {
        //noinspection unchecked
        changes.addAll(getSelectedObjects((ChangesBrowserNode)path.getLastPathComponent()));
      }
      return ContainerUtil.newArrayList(changes);
    }
  }

  @Nonnull
  private List<T> getSelectedChangesOrAllIfNone() {
    List<T> changes = getSelectedChanges();
    if (!changes.isEmpty()) return changes;
    return getChanges();
  }

  protected abstract List<T> getSelectedObjects(final ChangesBrowserNode<T> node);

  @Nullable
  protected abstract T getLeadSelectedObject(final ChangesBrowserNode node);

  @Nullable
  public T getHighestLeadSelection() {
    final TreePath path = getSelectionPath();
    if (path == null) {
      return null;
    }
    //noinspection unchecked
    return getLeadSelectedObject((ChangesBrowserNode<T>)path.getLastPathComponent());
  }

  @Nullable
  public T getLeadSelection() {
    final TreePath path = getSelectionPath();
    //noinspection unchecked
    return path == null ? null : ContainerUtil.getFirstItem(getSelectedObjects(((ChangesBrowserNode<T>)path.getLastPathComponent())));
  }

  @Nonnull
  ChangesBrowserNode<?> getRoot() {
    return (ChangesBrowserNode<?>)getModel().getRoot();
  }

  private void notifyInclusionListener() {
    if (myInclusionListener != null) {
      myInclusionListener.run();
    }
  }

  // no listener supposed to be called
  public void setIncludedChanges(final Collection<T> changes) {
    myIncludedChanges.clear();
    myIncludedChanges.addAll(changes);
    repaint();
  }

  public void includeChange(final T change) {
    includeChanges(Collections.singleton(change));
  }

  public void includeChanges(final Collection<T> changes) {
    myIncludedChanges.addAll(changes);
    notifyInclusionListener();
    repaint();
  }

  public void excludeChange(final T change) {
    excludeChanges(Collections.singleton(change));
  }

  public void excludeChanges(final Collection<T> changes) {
    myIncludedChanges.removeAll(changes);
    notifyInclusionListener();
    repaint();
  }

  protected void toggleChanges(final Collection<T> changes) {
    boolean hasExcluded = false;
    for (T value : changes) {
      if (!myIncludedChanges.contains(value)) {
        hasExcluded = true;
        break;
      }
    }

    if (hasExcluded) {
      includeChanges(changes);
    }
    else {
      excludeChanges(changes);
    }
  }

  public boolean isIncluded(final T change) {
    return myIncludedChanges.contains(change);
  }

  @Nonnull
  public Collection<T> getIncludedChanges() {
    return myIncludedChanges;
  }

  public void expandAll() {
    TreeUtil.expandAll(this);
  }

  public AnAction[] getTreeActions() {
    final ToggleShowDirectoriesAction directoriesAction = new ToggleShowDirectoriesAction();
    final ExpandAllAction expandAllAction = new ExpandAllAction(this) {
      @Override
      public void update(AnActionEvent e) {
        e.getPresentation().setEnabledAndVisible(!myShowFlatten || !myIsModelFlat);
      }
    };
    final CollapseAllAction collapseAllAction = new CollapseAllAction(this) {
      @Override
      public void update(AnActionEvent e) {
        e.getPresentation().setEnabledAndVisible(!myShowFlatten || !myIsModelFlat);
      }
    };
    final AnAction[] actions = new AnAction[]{directoriesAction, expandAllAction, collapseAllAction};
    directoriesAction.registerCustomShortcutSet(
            new CustomShortcutSet(KeyStroke.getKeyStroke(KeyEvent.VK_P, Platform.current().os().isMac() ? InputEvent.META_DOWN_MASK : InputEvent.CTRL_DOWN_MASK)),
            this);
    expandAllAction.registerCustomShortcutSet(
            new CustomShortcutSet(KeymapManager.getInstance().getActiveKeymap().getShortcuts(IdeActions.ACTION_EXPAND_ALL)),
            this);
    collapseAllAction.registerCustomShortcutSet(
            new CustomShortcutSet(KeymapManager.getInstance().getActiveKeymap().getShortcuts(IdeActions.ACTION_COLLAPSE_ALL)),
            this);
    return actions;
  }

  public void setSelectionMode(@JdkConstants.TreeSelectionMode int mode) {
    getSelectionModel().setSelectionMode(mode);
  }

  private class MyTreeCellRenderer extends JPanel implements TreeCellRenderer {
    private final ChangesBrowserNodeRenderer myTextRenderer;
    private final JCheckBox myCheckBox;


    public MyTreeCellRenderer(@Nonnull ChangesBrowserNodeRenderer textRenderer) {
      super(new BorderLayout());
      myCheckBox = new JCheckBox();
      myTextRenderer = textRenderer;

      if (myShowCheckboxes) {
        add(myCheckBox, BorderLayout.WEST);
      }

      add(myTextRenderer, BorderLayout.CENTER);
      setOpaque(false);
    }

    @Override
    public Component getTreeCellRendererComponent(JTree tree,
                                                  Object value,
                                                  boolean selected,
                                                  boolean expanded,
                                                  boolean leaf,
                                                  int row,
                                                  boolean hasFocus) {

      if (UIUtil.isUnderGTKLookAndFeel() || UIUtil.isUnderNimbusLookAndFeel()) {
        NonOpaquePanel.setTransparent(this);
        NonOpaquePanel.setTransparent(myCheckBox);
      } else {
        setBackground(null);
        myCheckBox.setBackground(null);
        myCheckBox.setOpaque(false);
      }

      myTextRenderer.setOpaque(false);
      myTextRenderer.setTransparentIconBackground(true);
      myTextRenderer.setToolTipText(null);
      myTextRenderer.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);
      if (myShowCheckboxes) {
        @SuppressWarnings("unchecked")
        CheckboxTree.NodeState state = getNodeStatus((ChangesBrowserNode)value);
        myCheckBox.setSelected(state != CheckboxTree.NodeState.CLEAR);
        //noinspection unchecked
        myCheckBox.setEnabled(tree.isEnabled() && isNodeEnabled((ChangesBrowserNode)value));
        revalidate();

        return this;
      }
      else {
        return myTextRenderer;
      }
    }

    @Override
    public String getToolTipText() {
      return myTextRenderer.getToolTipText();
    }
  }


  private CheckboxTree.NodeState getNodeStatus(ChangesBrowserNode<T> node) {
    boolean hasIncluded = false;
    boolean hasExcluded = false;

    for (T change : getSelectedObjects(node)) {
      if (myIncludedChanges.contains(change)) {
        hasIncluded = true;
      }
      else {
        hasExcluded = true;
      }
    }

    if (hasIncluded && hasExcluded) return CheckboxTree.NodeState.PARTIAL;
    if (hasIncluded) return CheckboxTree.NodeState.FULL;
    return CheckboxTree.NodeState.CLEAR;
  }

  protected boolean isNodeEnabled(ChangesBrowserNode<T> node) {
    return getNodeStatus(node) != CheckboxTree.NodeState.PARTIAL;
  }

  private class MyToggleSelectionAction extends AnAction implements DumbAware {
    @Override
    public void actionPerformed(AnActionEvent e) {
      toggleChanges(getSelectedChangesOrAllIfNone());
    }
  }

  public class ToggleShowDirectoriesAction extends ToggleAction implements DumbAware {
    public ToggleShowDirectoriesAction() {
      super(VcsLocalize.changesActionShowDirectoriesText().get(),
            VcsLocalize.changesActionShowDirectoriesDescription().get(),
            AllIcons.Actions.GroupByPackage);
    }

    @Override
    public boolean isSelected(AnActionEvent e) {
      return (! myProject.isDisposed()) && !PropertiesComponent.getInstance(myProject).isTrueValue(FLATTEN_OPTION_KEY);
    }

    @Override
    public void setSelected(AnActionEvent e, boolean state) {
      PropertiesComponent.getInstance(myProject).setValue(FLATTEN_OPTION_KEY, String.valueOf(!state));
      setShowFlatten(!state);
    }
  }

  public void select(final List<T> changes) {
    final List<TreePath> treeSelection = new ArrayList<>(changes.size());
    TreeUtil.traverse(getRoot(), node -> {
      @SuppressWarnings("unchecked")
      final T change = (T) ((DefaultMutableTreeNode) node).getUserObject();
      if (changes.contains(change)) {
        treeSelection.add(new TreePath(((DefaultMutableTreeNode) node).getPath()));
      }
      return true;
    });
    setSelectionPaths(treeSelection.toArray(new TreePath[treeSelection.size()]));
    if (treeSelection.size() == 1) scrollPathToVisible(treeSelection.get(0));
  }

  public void setAlwaysExpandList(boolean alwaysExpandList) {
    myAlwaysExpandList = alwaysExpandList;
  }

  @Override
  public void calcData(Key<?> key, DataSink sink) {
    if (PlatformDataKeys.COPY_PROVIDER == key) {
      sink.put(PlatformDataKeys.COPY_PROVIDER, myTreeCopyProvider);
    }
  }

  @Override
  public boolean isFileColorsEnabled() {
    return ProjectViewTree.isFileColorsEnabledFor(this);
  }

  @Override
  public Color getFileColorFor(Object object) {
    VirtualFile file;
    if (object instanceof FilePath) {
      file = ((FilePath)object).getVirtualFile();
    }
    else if (object instanceof Change) {
      file = ((Change)object).getVirtualFile();
    }
    else {
      file = ObjectUtil.tryCast(object, VirtualFile.class);
    }

    if (file != null) {
      return FileColorManager.getInstance(myProject).getFileColor(file);
    }
    return super.getFileColorFor(object);
  }

  @Override
  public Dimension getPreferredScrollableViewportSize() {
    Dimension size = super.getPreferredScrollableViewportSize();
    size = new Dimension(size.width + 10, size.height);
    return size;
  }

  @Override
  protected void processMouseEvent(MouseEvent e) {
    if (e.getID() == MouseEvent.MOUSE_PRESSED) {
      if (!isEnabled()) return;
      int row = getRowForLocation(e.getX(), e.getY());
      if (row >= 0) {
        final Rectangle baseRect = getRowBounds(row);
        baseRect.setSize(myCheckboxWidth, baseRect.height);
        if (baseRect.contains(e.getPoint())) {
          setSelectionRow(row);
          toggleSelection();
        }
      }
    }
    super.processMouseEvent(e);
  }

  @Override
  public int getToggleClickCount() {
    return -1;
  }
}
