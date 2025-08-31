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
package consulo.ide.impl.idea.openapi.roots.libraries.ui.impl;

import consulo.application.AllIcons;
import consulo.application.Application;
import consulo.ide.impl.idea.openapi.ui.TitlePanel;
import consulo.ide.impl.idea.openapi.vfs.VfsUtilCore;
import consulo.ui.ex.awt.tree.table.TreeColumnInfo;
import consulo.ide.impl.idea.util.ui.ComboBoxCellEditor;
import consulo.project.Project;
import consulo.ui.ex.SimpleTextAttributes;
import consulo.ui.ex.awt.ColumnInfo;
import consulo.ui.ex.awt.DialogWrapper;
import consulo.ui.ex.awt.ScrollPaneFactory;
import consulo.ui.ex.awt.table.ComboBoxTableRenderer;
import consulo.ui.ex.awt.tree.CheckboxTree;
import consulo.ui.ex.awt.tree.CheckedTreeNode;
import consulo.ui.ex.awt.tree.ColoredTreeCellRenderer;
import consulo.ui.ex.awt.tree.TreeUtil;
import consulo.ui.image.Image;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nullable;
import org.jetbrains.annotations.NonNls;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This dialog allows selecting paths inside selected archives or directories.
 * The tree is three-level:
 * <ul>
 * <li>The root is a fake node that just holds child nodes.</li>
 * <li>The second level is archives or directories selected on the previous selection step.</li>
 * <li>The third level are detected roots inside previous selection.</li>
 * </ul>
 *
 * @author max
 * @author Constantine.Plotnikov
 */
public class DetectedRootsChooserDialog extends DialogWrapper {
  private static final ColumnInfo ROOT_COLUMN = new TreeColumnInfo("");
  private static final ColumnInfo<VirtualFileCheckedTreeNode, String> ROOT_TYPE_COLUMN = new ColumnInfo<>("") {
    @Override
    public String valueOf(VirtualFileCheckedTreeNode node) {
      SuggestedChildRootInfo rootInfo = node.getRootInfo();
      return rootInfo != null ? rootInfo.getRootTypeName(rootInfo.getSelectedRootType()) : "";
    }

    @Override
    public TableCellRenderer getRenderer(VirtualFileCheckedTreeNode node) {
      SuggestedChildRootInfo rootInfo = node.getRootInfo();
      if (rootInfo != null && isCellEditable(node)) {
        return new ComboBoxTableRenderer<>(rootInfo.getRootTypeNames());
      }
      return new DefaultTableCellRenderer();
    }

    @Override
    public TableCellEditor getEditor(VirtualFileCheckedTreeNode o) {
      final SuggestedChildRootInfo rootInfo = o.getRootInfo();
      if (rootInfo == null) return null;
      ComboBoxCellEditor editor = new ComboBoxCellEditor() {
        @Override
        protected List<String> getComboBoxItems() {
          return Arrays.asList(rootInfo.getRootTypeNames());
        }
      };
      editor.setClickCountToStart(1);
      return editor;
    }

    @Override
    public boolean isCellEditable(VirtualFileCheckedTreeNode node) {
      SuggestedChildRootInfo rootInfo = node.getRootInfo();
      return rootInfo != null && rootInfo.getDetectedRoot().getTypes().size() > 1;
    }

    @Override
    public void setValue(VirtualFileCheckedTreeNode node, String value) {
      SuggestedChildRootInfo rootInfo = node.getRootInfo();
      if (rootInfo != null) {
        rootInfo.setSelectedRootType(value);
      }
    }
  };

  private CheckboxTreeTable myTreeTable;
  private JScrollPane myPane;
  private String myDescription;

  public DetectedRootsChooserDialog(Component component, List<SuggestedChildRootInfo> suggestedRoots) {
    super(component, true);
    init(suggestedRoots);
  }

  public DetectedRootsChooserDialog(Project project, List<SuggestedChildRootInfo> suggestedRoots) {
    super(project, true);
    init(suggestedRoots);
  }

  @NonNls
  private void init(List<SuggestedChildRootInfo> suggestedRoots) {
    myDescription = "<html><body>" + Application.get().getName().get() +
      " just scanned files and detected the following " +
      StringUtil.pluralize("root", suggestedRoots.size()) + ".<br>" +
      "Select items in the tree below or press Cancel to cancel operation.</body></html>";
    myTreeTable = createTreeTable(suggestedRoots);
    myPane = ScrollPaneFactory.createScrollPane(myTreeTable);
    setTitle("Detected Roots");
    init();
  }

  private static CheckboxTreeTable createTreeTable(List<SuggestedChildRootInfo> suggestedRoots) {
    CheckedTreeNode root = createRoot(suggestedRoots);
    CheckboxTreeTable treeTable = new CheckboxTreeTable(root, new CheckboxTree.CheckboxTreeCellRenderer(true) {
      @Override
      public void customizeRenderer(
        JTree tree,
        Object value,
        boolean selected,
        boolean expanded,
        boolean leaf,
        int row,
        boolean hasFocus
      ) {
        if (!(value instanceof VirtualFileCheckedTreeNode)) return;
        VirtualFileCheckedTreeNode node = (VirtualFileCheckedTreeNode)value;
        VirtualFile file = node.getFile();
        String text;
        SimpleTextAttributes attributes;
        Image icon;
        boolean isValid = true;
        if (leaf) {
          VirtualFile ancestor = ((VirtualFileCheckedTreeNode)node.getParent()).getFile();
          if (ancestor != null) {
            text = VfsUtilCore.getRelativePath(file, ancestor, File.separatorChar);
            if (StringUtil.isEmpty(text)) {
              text = File.separator;
            }
          }
          else {
            text = file.getPresentableUrl();
          }
          if (text == null) {
            isValid = false;
            text = file.getPresentableUrl();
          }
          attributes = SimpleTextAttributes.REGULAR_ATTRIBUTES;
          icon = AllIcons.Nodes.TreeClosed;
        }
        else {
          text = file.getPresentableUrl();
          if (text == null) {
            isValid = false;
          }
          attributes = SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES;
          icon = AllIcons.Nodes.TreeClosed;
        }
        ColoredTreeCellRenderer textRenderer = getTextRenderer();
        textRenderer.setIcon(icon);
        if (!isValid) {
          textRenderer.append("[INVALID] ", SimpleTextAttributes.ERROR_ATTRIBUTES);
        }
        if (text != null) {
          textRenderer.append(text, attributes);
        }
      }
    }, new ColumnInfo[]{ROOT_COLUMN, ROOT_TYPE_COLUMN});

    int max = 0;
    for (SuggestedChildRootInfo info : suggestedRoots) {
      for (String s : info.getRootTypeNames()) {
        max = Math.max(max, treeTable.getFontMetrics(treeTable.getFont()).stringWidth(s));
      }
    }
    TableColumn column = treeTable.getColumnModel().getColumn(1);
    int width = max + 20;//add space for combobox button
    column.setPreferredWidth(width);
    column.setMaxWidth(width);
    treeTable.setRootVisible(false);
    TreeUtil.expandAll(treeTable.getTree());
    return treeTable;
  }

  private static CheckedTreeNode createRoot(List<SuggestedChildRootInfo> suggestedRoots) {
    CheckedTreeNode root = new CheckedTreeNode(null);
    Map<VirtualFile, CheckedTreeNode> rootCandidateNodes = new HashMap<>();
    for (SuggestedChildRootInfo rootInfo : suggestedRoots) {
      VirtualFile rootCandidate = rootInfo.getRootCandidate();
      CheckedTreeNode parent = rootCandidateNodes.get(rootCandidate);
      if (parent == null) {
        parent = new VirtualFileCheckedTreeNode(rootCandidate);
        rootCandidateNodes.put(rootCandidate, parent);
        root.add(parent);
      }
      parent.add(new VirtualFileCheckedTreeNode(rootInfo));
    }
    return root;
  }

  @Override
  protected JComponent createTitlePane() {
    return new TitlePanel("Choose Roots", myDescription);
  }

  @Nullable
  @Override
  protected JComponent createCenterPanel() {
    return myPane;
  }

  public SuggestedChildRootInfo[] getChosenRoots() {
    return myTreeTable.getCheckedNodes(SuggestedChildRootInfo.class);
  }

  @NonNls
  @Override
  protected String getDimensionServiceKey() {
    return "DetectedRootsChooserDialog";
  }

  private static class VirtualFileCheckedTreeNode extends CheckedTreeNode {
    private final VirtualFile myFile;

    private VirtualFileCheckedTreeNode(VirtualFile file) {
      super(file);
      myFile = file;
    }

    public VirtualFileCheckedTreeNode(SuggestedChildRootInfo rootInfo) {
      super(rootInfo);
      myFile = rootInfo.getDetectedRoot().getFile();
    }

    public VirtualFile getFile() {
      return myFile;
    }

    @Nullable
    private SuggestedChildRootInfo getRootInfo() {
      return userObject instanceof SuggestedChildRootInfo rootInfo ? rootInfo : null;
    }
  }
}
