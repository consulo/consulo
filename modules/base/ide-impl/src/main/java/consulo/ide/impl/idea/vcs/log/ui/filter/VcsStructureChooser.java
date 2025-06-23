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
package consulo.ide.impl.idea.vcs.log.ui.filter;

import consulo.application.AllIcons;
import consulo.application.util.function.Computable;
import consulo.fileChooser.FileChooserDescriptor;
import consulo.ide.impl.idea.openapi.fileChooser.ex.FileNodeDescriptor;
import consulo.ide.impl.idea.openapi.fileChooser.ex.FileSystemTreeImpl;
import consulo.ide.impl.idea.openapi.vcs.changes.ui.VirtualFileListCellRenderer;
import consulo.ide.impl.idea.util.containers.Convertor;
import consulo.ide.impl.idea.util.treeWithCheckedNodes.SelectionManager;
import consulo.ide.impl.idea.util.treeWithCheckedNodes.TreeNodeState;
import consulo.module.Module;
import consulo.module.ModuleManager;
import consulo.module.content.ModuleRootManager;
import consulo.project.Project;
import consulo.ui.ex.SimpleTextAttributes;
import consulo.ui.ex.awt.*;
import consulo.ui.ex.awt.tree.ColoredTreeCellRenderer;
import consulo.ui.ex.awt.tree.Tree;
import consulo.versionControlSystem.FilePath;
import consulo.versionControlSystem.FilePathComparator;
import consulo.versionControlSystem.change.ChangeListManager;
import consulo.versionControlSystem.impl.internal.change.PlusMinus;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.*;

/**
 * @author irengrig
 * @since 2011-02-03
 */
public class VcsStructureChooser extends DialogWrapper {
  private final static int MAX_FOLDERS = 100;
  public static final Border BORDER = IdeBorderFactory.createBorder(SideBorder.TOP | SideBorder.LEFT);
  public static final String CAN_NOT_ADD_TEXT =
    "<html>Selected: <font color=red>(You have added " + MAX_FOLDERS + " elements. No more is allowed.)</font></html>";
  private static final String VCS_STRUCTURE_CHOOSER_KEY = "git4idea.history.wholeTree.VcsStructureChooser";

  @Nonnull
  private final Project myProject;
  @Nonnull
  private final List<VirtualFile> myRoots;
  @Nonnull
  private final Map<VirtualFile, String> myModulesSet;
  @Nonnull
  private final Set<VirtualFile> mySelectedFiles = new HashSet<>();

  @Nonnull
  private final SelectionManager mySelectionManager;

  private Tree myTree;

  public VcsStructureChooser(
    @Nonnull Project project,
    @Nonnull String title,
    @Nonnull Collection<VirtualFile> initialSelection,
    @Nonnull List<VirtualFile> roots
  ) {
    super(project, true);
    setTitle(title);
    myProject = project;
    myRoots = roots;
    mySelectionManager = new SelectionManager(MAX_FOLDERS, 500, MyNodeConverter.getInstance());
    myModulesSet = calculateModules(roots);

    init();

    mySelectionManager.setSelection(initialSelection);

    checkEmpty();
  }

  @Nonnull
  private Map<VirtualFile, String> calculateModules(@Nonnull List<VirtualFile> roots) {
    Map<VirtualFile, String> result = new HashMap<>();

    final ModuleManager moduleManager = ModuleManager.getInstance(myProject);
    // assertion for read access inside
    Module[] modules = myProject.getApplication().runReadAction((Computable<Module[]>)() -> moduleManager.getModules());

    TreeSet<VirtualFile> checkSet = new TreeSet<>(FilePathComparator.getInstance());
    checkSet.addAll(roots);
    for (Module module : modules) {
      VirtualFile[] files = ModuleRootManager.getInstance(module).getContentRoots();
      for (VirtualFile file : files) {
        VirtualFile floor = checkSet.floor(file);
        if (floor != null) {
          result.put(file, module.getName());
        }
      }
    }
    return result;
  }

  @Nonnull
  public Collection<VirtualFile> getSelectedFiles() {
    return mySelectedFiles;
  }

  private void checkEmpty() {
    setOKActionEnabled(!mySelectedFiles.isEmpty());
  }

  @Override
  @Nonnull
  protected String getDimensionServiceKey() {
    return VCS_STRUCTURE_CHOOSER_KEY;
  }

  @Override
  @Nonnull
  public JComponent getPreferredFocusedComponent() {
    return myTree;
  }

  @Override
  protected JComponent createCenterPanel() {
    myTree = new Tree();
    myTree.setBorder(BORDER);
    myTree.setShowsRootHandles(true);
    myTree.setRootVisible(false);
    myTree.setExpandableItemsEnabled(false);

    FileChooserDescriptor descriptor = new FileChooserDescriptor(true, true, true, true, false, true) {
      @Override
      public boolean isFileVisible(VirtualFile file, boolean showHiddenFiles) {
        if (!super.isFileVisible(file, showHiddenFiles)) return false;
        if (myRoots.contains(file)) return false;
        ChangeListManager changeListManager = ChangeListManager.getInstance(myProject);
        return !changeListManager.isIgnoredFile(file) && !changeListManager.isUnversioned(file);
      }
    };
    descriptor.withRoots(new ArrayList<>(myRoots)).withShowHiddenFiles(true).withHideIgnored(true);
    final MyCheckboxTreeCellRenderer cellRenderer =
      new MyCheckboxTreeCellRenderer(mySelectionManager, myModulesSet, myProject, myTree, myRoots);
    FileSystemTreeImpl fileSystemTree =
      new FileSystemTreeImpl(myProject, descriptor, myTree, cellRenderer, null, o -> {
        DefaultMutableTreeNode lastPathComponent = ((DefaultMutableTreeNode)o.getLastPathComponent());
        Object uo = lastPathComponent.getUserObject();
        if (uo instanceof FileNodeDescriptor) {
          VirtualFile file = ((FileNodeDescriptor)uo).getElement().getFile();
          String module = myModulesSet.get(file);
          if (module != null) return module;
          return file == null ? "" : file.getName();
        }
        return o.toString();
      });

    fileSystemTree.getTreeBuilder().getUi().setNodeDescriptorComparator((o1, o2) -> {
      if (o1 instanceof FileNodeDescriptor && o2 instanceof FileNodeDescriptor) {
        VirtualFile f1 = ((FileNodeDescriptor)o1).getElement().getFile();
        VirtualFile f2 = ((FileNodeDescriptor)o2).getElement().getFile();

        boolean isDir1 = f1.isDirectory();
        boolean isDir2 = f2.isDirectory();
        if (isDir1 != isDir2) return isDir1 ? -1 : 1;

        return f1.getPath().compareToIgnoreCase(f2.getPath());
      }
      return o1.getIndex() - o2.getIndex();
    });

    new ClickListener() {
      @Override
      public boolean onClick(@Nonnull MouseEvent e, int clickCount) {
        int row = myTree.getRowForLocation(e.getX(), e.getY());
        if (row < 0) return false;
        Object o = myTree.getPathForRow(row).getLastPathComponent();
        if (getTreeRoot() == o || getFile(o) == null) return false;

        Rectangle rowBounds = myTree.getRowBounds(row);
        cellRenderer.setBounds(rowBounds);
        Rectangle checkBounds = cellRenderer.myCheckbox.getBounds();
        checkBounds.setLocation(rowBounds.getLocation());

        if (checkBounds.height == 0) checkBounds.height = rowBounds.height;

        if (checkBounds.contains(e.getPoint())) {
          mySelectionManager.toggleSelection((DefaultMutableTreeNode)o);
          myTree.revalidate();
          myTree.repaint();
        }
        return true;
      }
    }.installOn(myTree);

    myTree.addKeyListener(new KeyAdapter() {
      public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_SPACE) {
          TreePath[] paths = myTree.getSelectionPaths();
          if (paths == null) return;
          for (TreePath path : paths) {
            if (path == null) continue;
            Object o = path.getLastPathComponent();
            if (getTreeRoot() == o || getFile(o) == null) return;
            mySelectionManager.toggleSelection((DefaultMutableTreeNode)o);
          }

          myTree.revalidate();
          myTree.repaint();
          e.consume();
        }
      }
    });

    JBPanel panel = new JBPanel(new BorderLayout());
    panel.add(new JBScrollPane(fileSystemTree.getTree()), BorderLayout.CENTER);
    final JLabel selectedLabel = new JLabel("");
    selectedLabel.setBorder(JBUI.Borders.empty(2, 0));
    panel.add(selectedLabel, BorderLayout.SOUTH);

    mySelectionManager.setSelectionChangeListener(new PlusMinus<>() {
      @Override
      public void plus(VirtualFile virtualFile) {
        mySelectedFiles.add(virtualFile);
        recalculateErrorText();
      }

      private void recalculateErrorText() {
        checkEmpty();
        if (mySelectionManager.canAddSelection()) {
          selectedLabel.setText("");
        }
        else {
          selectedLabel.setText(CAN_NOT_ADD_TEXT);
        }
        selectedLabel.revalidate();
      }

      @Override
      public void minus(VirtualFile virtualFile) {
        mySelectedFiles.remove(virtualFile);
        recalculateErrorText();
      }
    });
    panel.setPreferredSize(JBUI.size(400, 300));
    return panel;
  }

  @Nonnull
  private DefaultMutableTreeNode getTreeRoot() {
    return (DefaultMutableTreeNode)myTree.getModel().getRoot();
  }

  @Nullable
  private static VirtualFile getFile(@Nonnull Object node) {
    if (!(((DefaultMutableTreeNode)node).getUserObject() instanceof FileNodeDescriptor)) return null;
    FileNodeDescriptor descriptor = (FileNodeDescriptor)((DefaultMutableTreeNode)node).getUserObject();
    if (descriptor.getElement().getFile() == null) return null;
    return descriptor.getElement().getFile();
  }

  private static class MyCheckboxTreeCellRenderer extends JPanel implements TreeCellRenderer {
    @Nonnull
    private final WithModulesListCellRenderer myTextRenderer;
    @Nonnull
    public final JCheckBox myCheckbox;
    @Nonnull
    private final SelectionManager mySelectionManager;
    @Nonnull
    private final Map<VirtualFile, String> myModulesSet;
    @Nonnull
    private final Collection<VirtualFile> myRoots;
    @Nonnull
    private final ColoredTreeCellRenderer myColoredRenderer;
    @Nonnull
    private final JLabel myEmpty;
    @Nonnull
    private final JList myFictive;

    private MyCheckboxTreeCellRenderer(@Nonnull SelectionManager selectionManager,
                                       @Nonnull Map<VirtualFile, String> modulesSet,
                                       @Nonnull Project project,
                                       @Nonnull JTree tree,
                                       @Nonnull Collection<VirtualFile> roots) {
      super(new BorderLayout());
      mySelectionManager = selectionManager;
      myModulesSet = modulesSet;
      myRoots = roots;
      setBackground(tree.getBackground());
      myColoredRenderer = new ColoredTreeCellRenderer() {
        @Override
        public void customizeCellRenderer(@Nonnull JTree tree,
                                          Object value,
                                          boolean selected,
                                          boolean expanded,
                                          boolean leaf,
                                          int row,
                                          boolean hasFocus) {
          append(value.toString());
        }
      };
      myFictive = new JBList();
      myFictive.setBackground(tree.getBackground());
      myFictive.setSelectionBackground(UIUtil.getListSelectionBackground());
      myFictive.setSelectionForeground(UIUtil.getListSelectionForeground());

      myTextRenderer = new WithModulesListCellRenderer(project, myModulesSet) {
        @Override
        protected void putParentPath(Object value, FilePath path, FilePath self) {
          if (myRoots.contains(self.getVirtualFile())) {
            super.putParentPath(value, path, self);
          }
        }
      };
      myTextRenderer.setBackground(tree.getBackground());

      myCheckbox = new JCheckBox();
      myCheckbox.setBackground(tree.getBackground());
      myEmpty = new JLabel("");

      add(myCheckbox, BorderLayout.WEST);
      add(myTextRenderer, BorderLayout.CENTER);
      myCheckbox.setVisible(true);
    }

    @Override
    public Component getTreeCellRendererComponent(JTree tree,
                                                  Object value,
                                                  boolean selected,
                                                  boolean expanded,
                                                  boolean leaf,
                                                  int row,
                                                  boolean hasFocus) {
      invalidate();
      if (value == null) return myEmpty;
      VirtualFile file = getFile(value);
      DefaultMutableTreeNode node = (DefaultMutableTreeNode)value;
      if (file == null) {
        Object uo = node.getUserObject();
        if (uo instanceof String) {
          myColoredRenderer.getTreeCellRendererComponent(tree, node, selected, expanded, leaf, row, hasFocus);
          return myColoredRenderer;
        }
        return myEmpty;
      }
      myCheckbox.setVisible(true);
      TreeNodeState state = mySelectionManager.getState(node);
      myCheckbox.setEnabled(TreeNodeState.CLEAR.equals(state) || TreeNodeState.SELECTED.equals(state));
      myCheckbox.setSelected(!TreeNodeState.CLEAR.equals(state));
      myCheckbox.setOpaque(false);
      myCheckbox.setBackground(null);
      setBackground(null);
      myTextRenderer.getListCellRendererComponent(myFictive, file, 0, selected, hasFocus);
      revalidate();
      return this;
    }
  }

  private static class MyNodeConverter implements Convertor<DefaultMutableTreeNode, VirtualFile> {
    @Nonnull
    private final static MyNodeConverter ourInstance = new MyNodeConverter();

    @Nonnull
    public static MyNodeConverter getInstance() {
      return ourInstance;
    }

    @Override
    public VirtualFile convert(DefaultMutableTreeNode o) {
      return ((FileNodeDescriptor)o.getUserObject()).getElement().getFile();
    }
  }

  private static class WithModulesListCellRenderer extends VirtualFileListCellRenderer {
    @Nonnull
    private final Map<VirtualFile, String> myModules;

    private WithModulesListCellRenderer(@Nonnull Project project, @Nonnull Map<VirtualFile, String> modules) {
      super(project, true);
      myModules = modules;
    }

    @Override
    protected String getName(@Nonnull FilePath path) {
      String module = myModules.get(path.getVirtualFile());
      if (module != null) {
        return module;
      }
      return super.getName(path);
    }

    @Override
    protected void renderIcon(@Nonnull FilePath path) {
      String module = myModules.get(path.getVirtualFile());
      if (module != null) {
        setIcon(AllIcons.Nodes.Module);
      }
      else {
        if (path.isDirectory()) {
          setIcon(AllIcons.Nodes.TreeClosed);
        }
        else {
          setIcon(path.getFileType().getIcon());
        }
      }
    }

    @Override
    protected void putParentPathImpl(@Nonnull Object value, @Nonnull String parentPath, @Nonnull FilePath self) {
      append(self.getPath(), SimpleTextAttributes.GRAYED_ATTRIBUTES);
    }
  }
}
