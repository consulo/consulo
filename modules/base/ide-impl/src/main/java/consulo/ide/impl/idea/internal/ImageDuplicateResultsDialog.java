/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package consulo.ide.impl.idea.internal;

import consulo.application.AllIcons;
import consulo.component.PropertyName;
import consulo.dataContext.DataManager;
import consulo.ide.impl.idea.codeInsight.hint.ImplementationViewComponentImpl;
import consulo.ide.impl.idea.ide.util.PropertiesComponent;
import consulo.ide.impl.idea.openapi.module.ModuleUtil;
import consulo.ide.impl.idea.util.NotNullFunction;
import consulo.ide.impl.ui.impl.PopupChooserBuilder;
import consulo.language.editor.internal.DocumentationManagerHelper;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiManager;
import consulo.module.Module;
import consulo.module.ModuleManager;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.RelativePoint;
import consulo.ui.ex.SimpleTextAttributes;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.CustomShortcutSet;
import consulo.ui.ex.awt.*;
import consulo.ui.ex.awt.tree.ColoredTreeCellRenderer;
import consulo.ui.ex.awt.tree.Tree;
import consulo.ui.ex.awt.tree.TreeUtil;
import consulo.ui.ex.awt.util.ColorUtil;
import consulo.ui.ex.popup.JBPopup;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.util.io.FileUtil;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.List;
import java.util.*;

/**
 * @author Konstantin Bulenkov
 */
@SuppressWarnings("UseOfObsoleteCollectionType")
public class ImageDuplicateResultsDialog extends DialogWrapper {
  private final Project myProject;
  private final List<VirtualFile> myImages;
  private final Map<String, Set<VirtualFile>> myDuplicates;
  private Tree myTree;
  private ResourceModules myResourceModules = new ResourceModules();

  public ImageDuplicateResultsDialog(Project project, List<VirtualFile> images, Map<String, Set<VirtualFile>> duplicates) {
    super(project);
    myProject = project;
    myImages = images;
    PropertiesComponent.getInstance(myProject).loadFields(myResourceModules);
    myDuplicates = duplicates;
    setModal(false);
    myTree = new Tree(new MyRootNode());
    myTree.setRootVisible(true);
    myTree.setCellRenderer(new MyCellRenderer());
    init();
    TreeUtil.expandAll(myTree);
    setTitle("Image Duplicates");
    TreeUtil.selectFirstNode(myTree);
  }

  @Nonnull
  @Override
  protected Action[] createActions() {
    Action[] actions = new Action[4];
    actions[0] = new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
      }
    };
    actions[0].putValue(Action.NAME, "Fix all");
    actions[0].putValue(DEFAULT_ACTION, Boolean.TRUE);
    actions[0].putValue(FOCUSED_ACTION, Boolean.TRUE);
    actions[1] = new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
      }
    };
    actions[1].putValue(Action.NAME, "Fix selected");
    actions[2] = getCancelAction();
    actions[3] = getHelpAction();
    //return actions;
    return new Action[0];
  }

  @Override
  @RequiredUIAccess
  protected JComponent createCenterPanel() {
    JPanel panel = new JPanel(new BorderLayout());
    DataManager.registerDataProvider(panel, dataId -> {
      TreePath path = myTree.getSelectionPath();
      if (path != null) {
        Object component = path.getLastPathComponent();
        VirtualFile file = null;
        if (component instanceof MyFileNode fileNode) {
          component = fileNode.getParent();
        }
        if (component instanceof MyDuplicatesNode duplicatesNode) {
          file = duplicatesNode.getUserObject().iterator().next();
        }
        if (VirtualFile.KEY == dataId) {
          return file;
        }
        if (VirtualFile.KEY_OF_ARRAY == dataId && file != null) {
          return new VirtualFile[]{file};
        }
      }
      return null;
    });

    JBList list = new JBList(new ResourceModules().getModuleNames());
    NotNullFunction<Object, JComponent> modulesRenderer = new NotNullFunction<>() {
      @Nonnull
      @Override
      public JComponent apply(Object dom) {
        return new JBLabel(
          dom instanceof Module module ? module.getName() : dom.toString(),
          AllIcons.Nodes.Package,
          SwingConstants.LEFT
        );
      }
    };
    list.installCellRenderer(modulesRenderer);
    JPanel modulesPanel = ToolbarDecorator.createDecorator(list)
      .setAddAction(button -> {
        Module[] all = ModuleManager.getInstance(myProject).getModules();
        Arrays.sort(all, (o1, o2) -> o1.getName().compareTo(o2.getName()));
        JBList modules = new JBList(all);
        modules.installCellRenderer(modulesRenderer);
        new PopupChooserBuilder<>(modules)
          .setTitle("Add Resource Module")
          .setFilteringEnabled(o -> ((Module)o).getName())
          .setItemChoosenCallback(() -> {
            Object value = modules.getSelectedValue();
            if (value instanceof Module module && !myResourceModules.contains(module)) {
              myResourceModules.add(module);
              ((DefaultListModel)list.getModel()).addElement(module.getName());
            }
            ((DefaultTreeModel)myTree.getModel()).reload();
            TreeUtil.expandAll(myTree);
          }).createPopup().show(button.getPreferredPopupPoint());
      })
      .setRemoveAction(button -> {
        Object[] values = list.getSelectedValues();
        for (Object value : values) {
          myResourceModules.remove((String)value);
          ((DefaultListModel)list.getModel()).removeElement(value);
        }
        ((DefaultTreeModel)myTree.getModel()).reload();
        TreeUtil.expandAll(myTree);
      })
      .disableDownAction()
      .disableUpAction()
      .createPanel();
    modulesPanel.setPreferredSize(new Dimension(-1, 60));
    JPanel top = new JPanel(new BorderLayout());
    top.add(new JLabel("Image modules:"), BorderLayout.NORTH);
    top.add(modulesPanel, BorderLayout.CENTER);

    panel.add(top, BorderLayout.NORTH);
    panel.add(new JBScrollPane(myTree), BorderLayout.CENTER);
    new AnAction() {
      @Override
      @RequiredUIAccess
      public void actionPerformed(@Nonnull AnActionEvent e) {
        VirtualFile file = getFileFromSelection();
        if (file != null) {
          PsiFile psiFile = PsiManager.getInstance(myProject).findFile(file);
          if (psiFile != null) {
            ImplementationViewComponentImpl viewComponent = new ImplementationViewComponentImpl(new PsiElement[]{psiFile}, 0);
            TreeSelectionListener listener = e1 -> {
              VirtualFile selection = getFileFromSelection();
              if (selection != null) {
                PsiFile newElement = PsiManager.getInstance(myProject).findFile(selection);
                if (newElement != null) {
                  viewComponent.update(new PsiElement[]{newElement}, 0);
                }
              }
            };
            myTree.addTreeSelectionListener(listener);

            JBPopup popup =
              JBPopupFactory.getInstance()
                .createComponentPopupBuilder(viewComponent, viewComponent.getPreferredFocusableComponent())
                .setProject(myProject)
                .setDimensionServiceKey(myProject, DocumentationManagerHelper.JAVADOC_LOCATION_AND_SIZE, false)
                .setResizable(true)
                .setMovable(true)
                .setRequestFocus(false)
                .setCancelCallback(() -> {
                  myTree.removeTreeSelectionListener(listener);
                  return true;
                })
                .setTitle("Image Preview")
                .createPopup();

            Window window = ImageDuplicateResultsDialog.this.getWindow();
            popup.show(new RelativePoint(window, new Point(window.getWidth(), 0)));
            viewComponent.setHint(popup, "Image Preview");
          }
        }
      }
    }.registerCustomShortcutSet(CustomShortcutSet.fromString("ENTER"), panel);

    int total = 0;
    for (Set set : myDuplicates.values()) total+=set.size();
    total-=myDuplicates.size();
    JLabel label = new JLabel(
      "<html>Press <b>Enter</b> to preview image<br>" +
        "Total images found: " + myImages.size() + ". Total duplicates found: " + total+"</html>"
    );
    panel.add(label, BorderLayout.SOUTH);
    return panel;
  }

  @Override
  protected String getDimensionServiceKey() {
    return "image.duplicates.dialog";
  }

  @Override
  @RequiredUIAccess
  public JComponent getPreferredFocusedComponent() {
    return myTree;
  }

  @Nullable
  private VirtualFile getFileFromSelection() {
    TreePath path = myTree.getSelectionPath();
    if (path != null) {
      Object component = path.getLastPathComponent();
      VirtualFile file = null;
      if (component instanceof MyFileNode fileNode) {
        component = fileNode.getParent();
      }
      if (component instanceof MyDuplicatesNode duplicatesNode) {
        file = duplicatesNode.getUserObject().iterator().next();
      }
      return file;
    }
    return null;
  }

  private class MyRootNode extends DefaultMutableTreeNode {
    private MyRootNode() {
      Vector vector = new Vector();
      for (Set<VirtualFile> files : myDuplicates.values()) {
        vector.add(new MyDuplicatesNode(this, files));
      }
      children = vector;
    }
  }

  private class MyDuplicatesNode extends DefaultMutableTreeNode {
    private final Set<VirtualFile> myFiles;

    public MyDuplicatesNode(DefaultMutableTreeNode node, Set<VirtualFile> files) {
      super(files);
      myFiles = files;
      setParent(node);
      Vector vector = new Vector();
      for (VirtualFile file : files) {
        vector.add(new MyFileNode(this, file));
      }
      children = vector;
    }

    @Override
    public Set<VirtualFile> getUserObject() {
      return (Set<VirtualFile>)super.getUserObject();
    }
  }

  private static class MyFileNode extends DefaultMutableTreeNode {
    public MyFileNode(DefaultMutableTreeNode node, VirtualFile file) {
      super(file);
      setParent(node);
    }

    @Override
    public VirtualFile getUserObject() {
      return (VirtualFile)super.getUserObject();
    }
  }

  private class MyCellRenderer extends ColoredTreeCellRenderer {
    @Override
    public void customizeCellRenderer(
      JTree tree,
      Object value,
      boolean selected,
      boolean expanded,
      boolean leaf,
      int row,
      boolean hasFocus
    ) {
      if (value instanceof MyFileNode fileNode) {
        VirtualFile file = fileNode.getUserObject();
        Module module = ModuleUtil.findModuleForFile(file, myProject);
        if (module != null) {
          setIcon(AllIcons.Nodes.Module);
          append(
            "[" + module.getName() + "] ",
            new SimpleTextAttributes(SimpleTextAttributes.STYLE_BOLD, UIUtil.getTreeForeground())
          );
          append(getRelativePathToProject(myProject, file));
        }
        else {
          append(getRelativePathToProject(myProject, file));
        }
      }
      else if (value instanceof MyDuplicatesNode duplicatesNode) {
        Set<VirtualFile> files = duplicatesNode.getUserObject();
        for (VirtualFile file : files) {
          Module module = ModuleUtil.findModuleForFile(file, myProject);

          if (module != null && myResourceModules.contains(module)) {
            append("Icons can be replaced to ");
            append(
              getRelativePathToProject(myProject, file),
              new SimpleTextAttributes(SimpleTextAttributes.STYLE_BOLD, ColorUtil.fromHex("008000"))
            );
            return;
          }
        }
        append("Icon conflict");
      } else if (value instanceof MyRootNode) {
        append("All conflicts");
      }
    }
  }

  private static String getRelativePathToProject(Project project, VirtualFile file) {
    String path = project.getBasePath();
    assert path != null;
    String result = FileUtil.getRelativePath(
      path,
      file.getPath().replace('/', File.separatorChar),
      File.separatorChar
    );
    assert result != null;
    return result;
  }

  static class ResourceModules {
    @PropertyName(value = "resource.modules", defaultValue = "icons")
    public String modules;

    public List<String> getModuleNames() {
      return Arrays.asList(StringUtil.splitByLines(modules == null ? "icons" : modules));
    }

    public boolean contains(Module module) {
      return getModuleNames().contains(module.getName());
    }

    public void add(Module module) {
      if (StringUtil.isEmpty(modules)) {
        modules = module.getName();
      } else {
        modules += "\n" + module.getName();
      }
    }

    public void remove(String value) {
      List<String> names = new ArrayList<>(getModuleNames());
      names.remove(value);
      modules = StringUtil.join(names, "\n");
    }
  }
}
