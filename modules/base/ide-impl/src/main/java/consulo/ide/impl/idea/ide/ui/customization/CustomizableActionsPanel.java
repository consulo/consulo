/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package consulo.ide.impl.idea.ide.ui.customization;

import consulo.application.AllIcons;
import consulo.ide.IdeBundle;
import consulo.application.Application;
import consulo.ide.impl.idea.openapi.keymap.impl.ui.KeymapGroupImpl;
import consulo.ui.ex.action.ActionManager;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnSeparator;
import consulo.ide.impl.idea.openapi.actionSystem.ex.QuickList;
import consulo.ide.impl.idea.openapi.actionSystem.ex.QuickListsManager;
import consulo.fileChooser.FileChooserDescriptor;
import consulo.ide.impl.idea.openapi.keymap.impl.ui.ActionsTree;
import consulo.ide.impl.idea.openapi.keymap.impl.ui.ActionsTreeUtil;
import consulo.configurable.ConfigurationException;
import consulo.project.Project;
import consulo.project.ProjectManager;
import consulo.ui.ex.awt.DialogWrapper;
import consulo.ui.ex.awt.Messages;
import consulo.ui.ex.awt.TextFieldWithBrowseButton;
import consulo.ui.ex.awt.VerticalFlowLayout;
import consulo.ide.impl.idea.openapi.util.NotWorkingIconLoader;
import consulo.util.lang.ObjectUtil;
import consulo.util.lang.Pair;
import consulo.ide.impl.idea.openapi.util.io.FileUtil;
import consulo.util.lang.StringUtil;
import consulo.ide.impl.idea.openapi.vfs.VfsUtil;
import consulo.virtualFileSystem.VirtualFile;
import consulo.project.ui.internal.IdeFrameEx;
import consulo.project.ui.internal.WindowManagerEx;
import consulo.ide.impl.idea.packageDependencies.ui.TreeExpansionMonitor;
import consulo.ui.ex.awt.event.DocumentAdapter;
import consulo.ui.ex.awt.InsertPathAction;
import consulo.ui.ex.awt.ScrollPaneFactory;
import consulo.ui.ex.awt.tree.Tree;
import consulo.ui.ex.awt.internal.ImageLoader;
import consulo.ui.ex.awt.EmptyIcon;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awt.BorderLayoutPanel;
import consulo.ui.ex.awt.tree.TreeUtil;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.disposer.Disposable;
import consulo.logging.Logger;

import jakarta.annotation.Nullable;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.tree.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.*;

/**
 * User: anna
 * Date: Mar 17, 2005
 */
public class CustomizableActionsPanel implements Disposable {
  private static final Logger LOG = Logger.getInstance(CustomizableActionsPanel.class);

  private JButton myEditIconButton;
  private JButton myRemoveActionButton;
  private JButton myAddActionButton;
  private JButton myMoveActionDownButton;
  private JButton myMoveActionUpButton;
  private final JPanel myPanel;
  private final JTree myActionsTree;
  private JButton myAddSeparatorButton;

  private CustomActionsSchemaImpl mySelectedSchema;

  private JButton myRestoreAllDefaultButton;
  private JButton myRestoreDefaultButton;
  private final DefaultTreeModel myModel;

  public CustomizableActionsPanel() {
    myPanel = new JPanel(new BorderLayout());

    KeymapGroupImpl rootGroup = new KeymapGroupImpl("root", null, null);
    final DefaultMutableTreeNode root = new DefaultMutableTreeNode(rootGroup);
    myModel = new DefaultTreeModel(root);
    myActionsTree = new Tree(myModel);

    myActionsTree.setRootVisible(false);
    myActionsTree.setShowsRootHandles(true);
    UIUtil.setLineStyleAngled(myActionsTree);
    myActionsTree.setCellRenderer(new MyTreeCellRenderer());

    final ActionManager actionManager = ActionManager.getInstance();
    myActionsTree.getSelectionModel().addTreeSelectionListener(e -> {
      final TreePath[] selectionPaths = myActionsTree.getSelectionPaths();
      final boolean isSingleSelection = selectionPaths != null && selectionPaths.length == 1;
      myAddActionButton.setEnabled(isSingleSelection);
      if (isSingleSelection) {
        final DefaultMutableTreeNode node = (DefaultMutableTreeNode)selectionPaths[0].getLastPathComponent();
        String actionId = getActionId(node);
        if (actionId != null) {
          final AnAction action = actionManager.getAction(actionId);
          myEditIconButton.setEnabled(action != null && action.getTemplatePresentation() != null);
        }
        else {
          myEditIconButton.setEnabled(false);
        }
      }
      else {
        myEditIconButton.setEnabled(false);
      }
      myAddSeparatorButton.setEnabled(isSingleSelection);
      myRemoveActionButton.setEnabled(selectionPaths != null);
      if (selectionPaths != null) {
        for (TreePath selectionPath : selectionPaths) {
          if (selectionPath.getPath() != null && selectionPath.getPath().length <= 2) {
            setButtonsDisabled();
            return;
          }
        }
      }
      myMoveActionUpButton.setEnabled(isMoveSupported(myActionsTree, -1));
      myMoveActionDownButton.setEnabled(isMoveSupported(myActionsTree, 1));
      myRestoreDefaultButton.setEnabled(!findActionsUnderSelection().isEmpty());
    });

    myAddActionButton = new JButton(IdeBundle.message("button.add.action.after"));
    myAddActionButton.addActionListener(e -> {
      final List<TreePath> expandedPaths = TreeUtil.collectExpandedPaths(myActionsTree);
      final TreePath selectionPath = myActionsTree.getLeadSelectionPath();
      if (selectionPath != null) {
        DefaultMutableTreeNode node = (DefaultMutableTreeNode)selectionPath.getLastPathComponent();
        final FindAvailableActionsDialog dlg = new FindAvailableActionsDialog();
        dlg.show();
        if (dlg.isOK()) {
          final Set<Object> toAdd = dlg.getTreeSelectedActionIds();
          if (toAdd == null) return;
          for (final Object o : toAdd) {
            final ActionUrl url = new ActionUrl(ActionUrl.getGroupPath(new TreePath(node.getPath())), o, ActionUrl.ADDED, node.getParent().getIndex(node) + 1);
            addCustomizedAction(url);
            ActionUrl.changePathInActionsTree(myActionsTree, url);
            if (o instanceof String) {
              DefaultMutableTreeNode current = new DefaultMutableTreeNode(url.getComponent());
              current.setParent((DefaultMutableTreeNode)node.getParent());
              editToolbarIcon((String)o, current);
            }
          }
          myModel.reload();
        }
      }
      TreeUtil.restoreExpandedPaths(myActionsTree, expandedPaths);
    });

    myEditIconButton = new JButton(IdeBundle.message("button.edit.action.icon"));
    myEditIconButton.addActionListener(e -> {
      myRestoreAllDefaultButton.setEnabled(true);
      final List<TreePath> expandedPaths = TreeUtil.collectExpandedPaths(myActionsTree);
      final TreePath selectionPath = myActionsTree.getLeadSelectionPath();
      if (selectionPath != null) {
        EditIconDialog dlg = new EditIconDialog((DefaultMutableTreeNode)selectionPath.getLastPathComponent());
        dlg.show();
        if (dlg.isOK()) {
          myActionsTree.repaint();
        }
      }
      TreeUtil.restoreExpandedPaths(myActionsTree, expandedPaths);
    });

    myAddSeparatorButton = new JButton(IdeBundle.message("button.add.separator"));
    myAddSeparatorButton.addActionListener(e -> {
      final List<TreePath> expandedPaths = TreeUtil.collectExpandedPaths(myActionsTree);
      final TreePath selectionPath = myActionsTree.getLeadSelectionPath();
      if (selectionPath != null) {
        DefaultMutableTreeNode node = (DefaultMutableTreeNode)selectionPath.getLastPathComponent();
        final ActionUrl url = new ActionUrl(ActionUrl.getGroupPath(selectionPath), AnSeparator.getInstance(), ActionUrl.ADDED, node.getParent().getIndex(node) + 1);
        ActionUrl.changePathInActionsTree(myActionsTree, url);
        addCustomizedAction(url);
        myModel.reload();
      }
      TreeUtil.restoreExpandedPaths(myActionsTree, expandedPaths);
    });

    myRemoveActionButton = new JButton(IdeBundle.message("button.remove"));
    myRemoveActionButton.addActionListener(e -> {
      final List<TreePath> expandedPaths = TreeUtil.collectExpandedPaths(myActionsTree);
      final TreePath[] selectionPath = myActionsTree.getSelectionPaths();
      if (selectionPath != null) {
        for (TreePath treePath : selectionPath) {
          final ActionUrl url = CustomizationUtil.getActionUrl(treePath, ActionUrl.DELETED);
          ActionUrl.changePathInActionsTree(myActionsTree, url);
          addCustomizedAction(url);
        }
        myModel.reload();
      }
      TreeUtil.restoreExpandedPaths(myActionsTree, expandedPaths);
    });

    myMoveActionUpButton = new JButton(IdeBundle.message("button.move.up.u"));
    myMoveActionUpButton.addActionListener(e -> {
      final List<TreePath> expandedPaths = TreeUtil.collectExpandedPaths(myActionsTree);
      final TreePath[] selectionPath = myActionsTree.getSelectionPaths();
      if (selectionPath != null) {
        for (TreePath treePath : selectionPath) {
          final ActionUrl url = CustomizationUtil.getActionUrl(treePath, ActionUrl.MOVE);
          final int absolutePosition = url.getAbsolutePosition();
          url.setInitialPosition(absolutePosition);
          url.setAbsolutePosition(absolutePosition - 1);
          ActionUrl.changePathInActionsTree(myActionsTree, url);
          addCustomizedAction(url);
        }
        myModel.reload();
        TreeUtil.restoreExpandedPaths(myActionsTree, expandedPaths);
        for (TreePath path : selectionPath) {
          myActionsTree.addSelectionPath(path);
        }
      }
    });

    myMoveActionDownButton = new JButton(IdeBundle.message("button.move.down.d"));
    myMoveActionDownButton.addActionListener(e -> {
      final List<TreePath> expandedPaths = TreeUtil.collectExpandedPaths(myActionsTree);
      final TreePath[] selectionPath = myActionsTree.getSelectionPaths();
      if (selectionPath != null) {
        for (int i = selectionPath.length - 1; i >= 0; i--) {
          TreePath treePath = selectionPath[i];
          final ActionUrl url = CustomizationUtil.getActionUrl(treePath, ActionUrl.MOVE);
          final int absolutePosition = url.getAbsolutePosition();
          url.setInitialPosition(absolutePosition);
          url.setAbsolutePosition(absolutePosition + 1);
          ActionUrl.changePathInActionsTree(myActionsTree, url);
          addCustomizedAction(url);
        }
        myModel.reload();
        TreeUtil.restoreExpandedPaths(myActionsTree, expandedPaths);
        for (TreePath path : selectionPath) {
          myActionsTree.addSelectionPath(path);
        }
      }
    });

    myRestoreAllDefaultButton = new JButton("Restore All Defaults");
    myRestoreAllDefaultButton.addActionListener(e -> {
      mySelectedSchema.copyFrom(new CustomActionsSchemaImpl(Application.get()));
      patchActionsTreeCorrespondingToSchema(root);
      myRestoreAllDefaultButton.setEnabled(false);
    });

    myRestoreDefaultButton = new JButton("Restore Default");
    myRestoreDefaultButton.addActionListener(e -> {
      final List<ActionUrl> otherActions = new ArrayList<>(mySelectedSchema.getActions());
      otherActions.removeAll(findActionsUnderSelection());
      mySelectedSchema.copyFrom(new CustomActionsSchemaImpl(Application.get()));
      for (ActionUrl otherAction : otherActions) {
        mySelectedSchema.addAction(otherAction);
      }
      final List<TreePath> treePaths = TreeUtil.collectExpandedPaths(myActionsTree);
      patchActionsTreeCorrespondingToSchema(root);
      restorePathsAfterTreeOptimization(treePaths);
      myRestoreDefaultButton.setEnabled(false);
    });

    patchActionsTreeCorrespondingToSchema(root);

    TreeExpansionMonitor.install(myActionsTree);

    BorderLayoutPanel rightPanel = new BorderLayoutPanel();

    myPanel.add(rightPanel, BorderLayout.EAST);

    JPanel topPanel = new JPanel(new VerticalFlowLayout());
    topPanel.add(myAddActionButton);
    topPanel.add(myAddSeparatorButton);
    topPanel.add(myEditIconButton);
    topPanel.add(myRemoveActionButton);
    topPanel.add(myMoveActionUpButton);
    topPanel.add(myMoveActionDownButton);

    rightPanel.addToTop(topPanel);

    JPanel bottomPanel = new JPanel(new VerticalFlowLayout());
    bottomPanel.add(myRestoreAllDefaultButton);
    bottomPanel.add(myRestoreDefaultButton);

    rightPanel.addToBottom(bottomPanel);

    myPanel.add(ScrollPaneFactory.createScrollPane(myActionsTree), BorderLayout.CENTER);

    setButtonsDisabled();
  }

  private List<ActionUrl> findActionsUnderSelection() {
    final ArrayList<ActionUrl> actions = new ArrayList<>();
    final TreePath[] selectionPaths = myActionsTree.getSelectionPaths();
    if (selectionPaths != null) {
      for (TreePath path : selectionPaths) {
        final ActionUrl selectedUrl = CustomizationUtil.getActionUrl(path, ActionUrl.MOVE);
        final ArrayList<String> selectedGroupPath = new ArrayList<>(selectedUrl.getGroupPath());
        final Object component = selectedUrl.getComponent();
        if (component instanceof KeymapGroupImpl) {
          selectedGroupPath.add(((KeymapGroupImpl)component).getName());
          for (ActionUrl action : mySelectedSchema.getActions()) {
            final ArrayList<String> groupPath = action.getGroupPath();
            final int idx = Collections.indexOfSubList(groupPath, selectedGroupPath);
            if (idx > -1) {
              actions.add(action);
            }
          }
        }
      }
    }
    return actions;
  }

  private void addCustomizedAction(ActionUrl url) {
    mySelectedSchema.addAction(url);
    myRestoreAllDefaultButton.setEnabled(true);
  }

  private void editToolbarIcon(String actionId, DefaultMutableTreeNode node) {
    final AnAction anAction = ActionManager.getInstance().getAction(actionId);
    if (isToolbarAction(node) && anAction.getTemplatePresentation() != null && anAction.getTemplatePresentation().getIcon() == null) {
      final int exitCode = Messages.showOkCancelDialog(IdeBundle.message("error.adding.action.without.icon.to.toolbar"), IdeBundle.message("title.unable.to.add.action.without.icon.to.toolbar"),
                                                       Messages.getInformationIcon());
      if (exitCode == Messages.OK) {
        mySelectedSchema.addIconCustomization(actionId, null);
        anAction.getTemplatePresentation().setIcon(AllIcons.Toolbar.Unknown);
        anAction.setDefaultIcon(false);
        node.setUserObject(Pair.create(actionId, AllIcons.Toolbar.Unknown));
        myActionsTree.repaint();
        setCustomizationSchemaForCurrentProjects();
      }
    }
  }

  private void setButtonsDisabled() {
    myRemoveActionButton.setEnabled(false);
    myAddActionButton.setEnabled(false);
    myEditIconButton.setEnabled(false);
    myAddSeparatorButton.setEnabled(false);
    myMoveActionDownButton.setEnabled(false);
    myMoveActionUpButton.setEnabled(false);
  }

  private static boolean isMoveSupported(JTree tree, int dir) {
    final TreePath[] selectionPaths = tree.getSelectionPaths();
    if (selectionPaths != null) {
      DefaultMutableTreeNode parent = null;
      for (TreePath treePath : selectionPaths) {
        if (treePath.getLastPathComponent() != null) {
          final DefaultMutableTreeNode node = (DefaultMutableTreeNode)treePath.getLastPathComponent();
          if (parent == null) {
            parent = (DefaultMutableTreeNode)node.getParent();
          }
          if (parent != node.getParent()) {
            return false;
          }
          if (dir > 0) {
            if (parent.getIndex(node) == parent.getChildCount() - 1) {
              return false;
            }
          }
          else {
            if (parent.getIndex(node) == 0) {
              return false;
            }
          }
        }
      }
      return true;
    }
    return false;
  }


  public JPanel getPanel() {
    return myPanel;
  }

  private static void setCustomizationSchemaForCurrentProjects() {
    final Project[] openProjects = ProjectManager.getInstance().getOpenProjects();
    for (Project project : openProjects) {
      final IdeFrameEx frame = WindowManagerEx.getInstanceEx().getIdeFrame(project);
      if (frame != null) {
        frame.updateView();
      }

      //final FavoritesManager favoritesView = FavoritesManager.getInstance(project);
      //final String[] availableFavoritesLists = favoritesView.getAvailableFavoritesLists();
      //for (String favoritesList : availableFavoritesLists) {
      //  favoritesView.getFavoritesTreeViewPanel(favoritesList).updateTreePopupHandler();
      //}
    }
    final IdeFrameEx frame = WindowManagerEx.getInstanceEx().getIdeFrame(null);
    if (frame != null) {
      frame.updateView();
    }
  }

  public void apply() throws ConfigurationException {
    final List<TreePath> treePaths = TreeUtil.collectExpandedPaths(myActionsTree);
    if (mySelectedSchema != null) {
      CustomizationUtil.optimizeSchema(myActionsTree, mySelectedSchema);
    }
    restorePathsAfterTreeOptimization(treePaths);
    CustomActionsSchemaImpl.getInstance().copyFrom(mySelectedSchema);
    setCustomizationSchemaForCurrentProjects();
  }

  private void restorePathsAfterTreeOptimization(final List<TreePath> treePaths) {
    for (final TreePath treePath : treePaths) {
      myActionsTree.expandPath(CustomizationUtil.getPathByUserObjects(myActionsTree, treePath));
    }
  }

  public void reset() {
    Application application = Application.get();

    mySelectedSchema = new CustomActionsSchemaImpl(application);
    mySelectedSchema.copyFrom(CustomActionsSchemaImpl.getInstance());
    patchActionsTreeCorrespondingToSchema((DefaultMutableTreeNode)myModel.getRoot());
    myRestoreAllDefaultButton.setEnabled(mySelectedSchema.isModified(new CustomActionsSchemaImpl(application)));
  }

  public boolean isModified() {
    CustomizationUtil.optimizeSchema(myActionsTree, mySelectedSchema);
    return CustomActionsSchemaImpl.getInstance().isModified(mySelectedSchema);
  }

  private void patchActionsTreeCorrespondingToSchema(DefaultMutableTreeNode root) {
    root.removeAllChildren();
    if (mySelectedSchema != null) {
      mySelectedSchema.fillActionGroups(root);
      for (final ActionUrl actionUrl : mySelectedSchema.getActions()) {
        ActionUrl.changePathInActionsTree(myActionsTree, actionUrl);
      }
    }
    myModel.reload();
  }

  @Override
  public void dispose() {

  }

  private static class MyTreeCellRenderer extends DefaultTreeCellRenderer {
    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
      super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
      if (value instanceof DefaultMutableTreeNode) {
        Object userObject = ((DefaultMutableTreeNode)value).getUserObject();
        consulo.ui.image.Image icon = null;
        if (userObject instanceof KeymapGroupImpl) {
          KeymapGroupImpl group = (KeymapGroupImpl)userObject;
          String name = group.getName();
          setText(name != null ? name : group.getId());
          icon = ObjectUtil.notNull(group.getIcon(), AllIcons.Nodes.Folder);
        }
        else if (userObject instanceof String) {
          String actionId = (String)userObject;
          AnAction action = ActionManager.getInstance().getAction(actionId);
          String name = action != null ? action.getTemplatePresentation().getText() : null;
          setText(!StringUtil.isEmptyOrSpaces(name) ? name : actionId);
          if (action != null) {
            consulo.ui.image.Image actionIcon = action.getTemplatePresentation().getIcon();
            if (actionIcon != null) {
              icon = actionIcon;
            }
          }
        }
        else if (userObject instanceof Pair) {
          String actionId = (String)((Pair)userObject).first;
          AnAction action = ActionManager.getInstance().getAction(actionId);
          setText(action != null ? action.getTemplatePresentation().getText() : actionId);
          icon = (consulo.ui.image.Image)((Pair)userObject).second;
        }
        else if (userObject instanceof AnSeparator) {
          setText("-------------");
        }
        else if (userObject instanceof QuickList) {
          setText(((QuickList)userObject).getDisplayName());
          icon = AllIcons.Actions.QuickList;
        }
        else {
          throw new IllegalArgumentException("unknown userObject: " + userObject);
        }

        setIcon(TargetAWT.to(ActionsTree.getEvenIcon(icon)));

        if (sel) {
          setForeground(UIUtil.getTreeSelectionForeground());
        }
        else {
          setForeground(UIUtil.getTreeForeground());
        }
      }
      return this;
    }
  }

  private static boolean isToolbarAction(DefaultMutableTreeNode node) {
    return node.getParent() != null &&
           ((DefaultMutableTreeNode)node.getParent()).getUserObject() instanceof KeymapGroupImpl &&
           ((KeymapGroupImpl)((DefaultMutableTreeNode)node.getParent()).getUserObject()).getName().equals(ActionsTreeUtil.MAIN_TOOLBAR);
  }

  @Nullable
  private static String getActionId(DefaultMutableTreeNode node) {
    return (String)(node.getUserObject() instanceof String ? node.getUserObject() : node.getUserObject() instanceof Pair ? ((Pair)node.getUserObject()).first : null);
  }

  protected boolean doSetIcon(DefaultMutableTreeNode node, @Nullable String path, Component component) {
    if (StringUtil.isNotEmpty(path) && !new File(path).isFile()) {
      Messages.showErrorDialog(component, IdeBundle.message("error.file.not.found.message", path), IdeBundle.message("title.choose.action.icon"));
      return false;
    }

    String actionId = getActionId(node);
    if (actionId == null) return false;

    final AnAction action = ActionManager.getInstance().getAction(actionId);
    if (action != null && action.getTemplatePresentation() != null) {
      if (StringUtil.isNotEmpty(path)) {
        Image image = null;
        try {
          image = ImageLoader.loadFromStream(VfsUtil.convertToURL(VfsUtil.pathToUrl(path.replace(File.separatorChar, '/'))).openStream());
        }
        catch (IOException e) {
          LOG.debug(e);
        }
        Icon icon = new File(path).exists() ? NotWorkingIconLoader.getIcon(image) : null;
        if (icon != null) {
          if (icon.getIconWidth() > EmptyIcon.ICON_18.getIconWidth() || icon.getIconHeight() > EmptyIcon.ICON_18.getIconHeight()) {
            Messages.showErrorDialog(component, IdeBundle.message("custom.icon.validation.message"), IdeBundle.message("title.choose.action.icon"));
            return false;
          }
          node.setUserObject(Pair.create(actionId, icon));
          mySelectedSchema.addIconCustomization(actionId, path);
        }
      }
      else {
        node.setUserObject(Pair.create(actionId, null));
        mySelectedSchema.removeIconCustomization(actionId);
        final DefaultMutableTreeNode nodeOnToolbar = findNodeOnToolbar(actionId);
        if (nodeOnToolbar != null) {
          editToolbarIcon(actionId, nodeOnToolbar);
          node.setUserObject(nodeOnToolbar.getUserObject());
        }
      }
      return true;
    }
    return false;
  }

  private static TextFieldWithBrowseButton createBrowseField() {
    TextFieldWithBrowseButton textField = new TextFieldWithBrowseButton();
    textField.setPreferredSize(new Dimension(200, textField.getPreferredSize().height));
    textField.setMinimumSize(new Dimension(200, textField.getPreferredSize().height));
    final FileChooserDescriptor fileChooserDescriptor = new FileChooserDescriptor(true, false, false, false, false, false) {
      @Override
      public boolean isFileSelectable(VirtualFile file) {
        //noinspection HardCodedStringLiteral
        return file.getName().endsWith(".png");
      }
    };
    textField.addBrowseFolderListener(IdeBundle.message("title.browse.icon"), IdeBundle.message("prompt.browse.icon.for.selected.action"), null, fileChooserDescriptor);
    InsertPathAction.addTo(textField.getTextField(), fileChooserDescriptor);
    return textField;
  }

  private class EditIconDialog extends DialogWrapper {
    private final DefaultMutableTreeNode myNode;
    protected TextFieldWithBrowseButton myTextField;

    protected EditIconDialog(DefaultMutableTreeNode node) {
      super(false);
      setTitle(IdeBundle.message("title.choose.action.icon"));
      init();
      myNode = node;
      final String actionId = getActionId(node);
      if (actionId != null) {
        final String iconPath = mySelectedSchema.getIconPath(actionId);
        myTextField.setText(FileUtil.toSystemDependentName(iconPath));
      }
    }

    @Override
    public JComponent getPreferredFocusedComponent() {
      return myTextField.getChildComponent();
    }

    @Override
    protected String getDimensionServiceKey() {
      return getClass().getName();
    }

    @Override
    protected JComponent createCenterPanel() {
      myTextField = createBrowseField();
      JPanel northPanel = new JPanel(new BorderLayout());
      northPanel.add(myTextField, BorderLayout.NORTH);
      return northPanel;
    }

    @Override
    protected void doOKAction() {
      if (myNode != null) {
        if (!doSetIcon(myNode, myTextField.getText(), getContentPane())) {
          return;
        }
        final Object userObject = myNode.getUserObject();
        if (userObject instanceof Pair) {
          String actionId = (String)((Pair)userObject).first;
          final AnAction action = ActionManager.getInstance().getAction(actionId);
          final consulo.ui.image.Image icon = (consulo.ui.image.Image)((Pair)userObject).second;
          action.getTemplatePresentation().setIcon(icon);
          action.setDefaultIcon(icon == null);
          editToolbarIcon(actionId, myNode);
        }
        myActionsTree.repaint();
      }
      setCustomizationSchemaForCurrentProjects();
      super.doOKAction();
    }
  }

  @Nullable
  private DefaultMutableTreeNode findNodeOnToolbar(String actionId) {
    final TreeNode toolbar = ((DefaultMutableTreeNode)myModel.getRoot()).getChildAt(1);
    for (int i = 0; i < toolbar.getChildCount(); i++) {
      final DefaultMutableTreeNode child = (DefaultMutableTreeNode)toolbar.getChildAt(i);
      final String childId = getActionId(child);
      if (childId != null && childId.equals(actionId)) {
        return child;
      }
    }
    return null;
  }

  private class FindAvailableActionsDialog extends DialogWrapper {
    private JTree myTree;
    private JButton mySetIconButton;
    private TextFieldWithBrowseButton myTextField;

    FindAvailableActionsDialog() {
      super(false);
      setTitle(IdeBundle.message("action.choose.actions.to.add"));
      init();
    }

    @Override
    protected JComponent createCenterPanel() {
      KeymapGroupImpl rootGroup = ActionsTreeUtil.createMainGroup(null, null, QuickListsManager.getInstance().getAllQuickLists());
      DefaultMutableTreeNode root = ActionsTreeUtil.createNode(rootGroup);
      DefaultTreeModel model = new DefaultTreeModel(root);
      myTree = new Tree();
      myTree.setModel(model);
      myTree.setCellRenderer(new MyTreeCellRenderer());
      final ActionManager actionManager = ActionManager.getInstance();

      mySetIconButton = new JButton(IdeBundle.message("button.set.icon"));
      mySetIconButton.setEnabled(false);
      mySetIconButton.addActionListener(e -> {
        final TreePath selectionPath = myTree.getSelectionPath();
        if (selectionPath != null) {
          doSetIcon((DefaultMutableTreeNode)selectionPath.getLastPathComponent(), myTextField.getText(), getContentPane());
          myTree.repaint();
        }
      });
      myTextField = createBrowseField();
      myTextField.getTextField().getDocument().addDocumentListener(new DocumentAdapter() {
        @Override
        protected void textChanged(DocumentEvent e) {
          enableSetIconButton(actionManager);
        }
      });
      JPanel northPanel = new JPanel(new BorderLayout());
      northPanel.add(myTextField, BorderLayout.CENTER);
      final JLabel label = new JLabel(IdeBundle.message("label.icon.path"));
      label.setLabelFor(myTextField.getChildComponent());
      northPanel.add(label, BorderLayout.WEST);
      northPanel.add(mySetIconButton, BorderLayout.EAST);
      northPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 5, 0));
      JPanel panel = new JPanel(new BorderLayout());
      panel.add(northPanel, BorderLayout.NORTH);

      panel.add(ScrollPaneFactory.createScrollPane(myTree), BorderLayout.CENTER);
      myTree.getSelectionModel().addTreeSelectionListener(e -> {
        enableSetIconButton(actionManager);
        final TreePath selectionPath = myTree.getSelectionPath();
        if (selectionPath != null) {
          final DefaultMutableTreeNode node = (DefaultMutableTreeNode)selectionPath.getLastPathComponent();
          final String actionId = getActionId(node);
          if (actionId != null) {
            final String iconPath = mySelectedSchema.getIconPath(actionId);
            myTextField.setText(FileUtil.toSystemDependentName(iconPath));
          }
        }
      });
      return panel;
    }

    @Override
    protected void doOKAction() {
      final ActionManager actionManager = ActionManager.getInstance();
      TreeUtil.traverseDepth((TreeNode)myModel.getRoot(), node -> {
        if (node instanceof DefaultMutableTreeNode) {
          final DefaultMutableTreeNode mutableNode = (DefaultMutableTreeNode)node;
          final Object userObject = mutableNode.getUserObject();
          if (userObject instanceof Pair) {
            String actionId = (String)((Pair)userObject).first;
            final AnAction action = actionManager.getAction(actionId);
            consulo.ui.image.Image icon = (consulo.ui.image.Image)((Pair)userObject).second;
            action.getTemplatePresentation().setIcon(icon);
            action.setDefaultIcon(icon == null);
            editToolbarIcon(actionId, mutableNode);
          }
        }
        return true;
      });
      super.doOKAction();
      setCustomizationSchemaForCurrentProjects();
    }

    protected void enableSetIconButton(ActionManager actionManager) {
      final TreePath selectionPath = myTree.getSelectionPath();
      Object userObject = null;
      if (selectionPath != null) {
        userObject = ((DefaultMutableTreeNode)selectionPath.getLastPathComponent()).getUserObject();
        if (userObject instanceof String) {
          final AnAction action = actionManager.getAction((String)userObject);
          if (action != null && action.getTemplatePresentation() != null && action.getTemplatePresentation().getIcon() != null) {
            mySetIconButton.setEnabled(true);
            return;
          }
        }
      }
      mySetIconButton.setEnabled(myTextField.getText().length() != 0 && selectionPath != null && new DefaultMutableTreeNode(selectionPath).isLeaf() && !(userObject instanceof AnSeparator));
    }

    @Nullable
    public Set<Object> getTreeSelectedActionIds() {
      TreePath[] paths = myTree.getSelectionPaths();
      if (paths == null) return null;

      Set<Object> actions = new HashSet<>();
      for (TreePath path : paths) {
        Object node = path.getLastPathComponent();
        if (node instanceof DefaultMutableTreeNode) {
          DefaultMutableTreeNode defNode = (DefaultMutableTreeNode)node;
          Object userObject = defNode.getUserObject();
          actions.add(userObject);
        }
      }
      return actions;
    }

    @Override
    protected String getDimensionServiceKey() {
      return "#consulo.ide.impl.idea.ide.ui.customization.CustomizableActionsPanel.FindAvailableActionsDialog";
    }
  }
}
