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
import consulo.application.Application;
import consulo.configurable.ConfigurationException;
import consulo.disposer.Disposable;
import consulo.fileChooser.FileChooserDescriptor;
import consulo.ide.impl.idea.openapi.actionSystem.ex.QuickList;
import consulo.ide.impl.idea.openapi.actionSystem.ex.QuickListsManager;
import consulo.ide.impl.idea.openapi.keymap.impl.ui.ActionsTree;
import consulo.ide.impl.idea.openapi.keymap.impl.ui.ActionsTreeUtil;
import consulo.ide.impl.idea.openapi.keymap.impl.ui.KeymapGroupImpl;
import consulo.ide.impl.idea.openapi.util.io.FileUtil;
import consulo.ide.impl.idea.openapi.vfs.VfsUtil;
import consulo.ide.impl.idea.packageDependencies.ui.TreeExpansionMonitor;
import consulo.ide.localize.IdeLocalize;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.project.ProjectManager;
import consulo.project.ui.internal.IdeFrameEx;
import consulo.project.ui.internal.WindowManagerEx;
import consulo.ui.Button;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionManager;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnSeparator;
import consulo.ui.ex.awt.*;
import consulo.ui.ex.awt.event.DocumentAdapter;
import consulo.ui.ex.awt.tree.Tree;
import consulo.ui.ex.awt.tree.TreeUtil;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.ex.keymap.localize.KeyMapLocalize;
import consulo.ui.image.Image;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.VerticalLayout;
import consulo.util.lang.ObjectUtil;
import consulo.util.lang.Pair;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.VirtualFile;
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

  private Button myEditIconButton;
  private Button myRemoveActionButton;
  private Button myAddActionButton;
  private Button myMoveActionDownButton;
  private Button myMoveActionUpButton;
  private final JPanel myPanel;
  private final JTree myActionsTree;
  private Button myAddSeparatorButton;

  private CustomActionsSchemaImpl mySelectedSchema;

  private Button myRestoreAllDefaultButton;
  private Button myRestoreDefaultButton;
  private final DefaultTreeModel myModel;

  @RequiredUIAccess
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

    myAddActionButton = Button.create(IdeLocalize.buttonAddActionAfter());
    myAddActionButton.addClickListener(e -> {
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
            final ActionUrl url =
              new ActionUrl(ActionUrl.getGroupPath(new TreePath(node.getPath())), o, ActionUrl.ADDED, node.getParent().getIndex(node) + 1);
            addCustomizedAction(url);
            ActionUrl.changePathInActionsTree(myActionsTree, url);
            if (o instanceof String s) {
              DefaultMutableTreeNode current = new DefaultMutableTreeNode(url.getComponent());
              current.setParent((DefaultMutableTreeNode)node.getParent());
              editToolbarIcon(s, current);
            }
          }
          myModel.reload();
        }
      }
      TreeUtil.restoreExpandedPaths(myActionsTree, expandedPaths);
    });

    myEditIconButton = Button.create(IdeLocalize.buttonEditActionIcon());
    myEditIconButton.addClickListener(e -> {
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

    myAddSeparatorButton = Button.create(IdeLocalize.buttonAddSeparator());
    myAddSeparatorButton.addClickListener(e -> {
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

    myRemoveActionButton = Button.create(IdeLocalize.buttonRemove());
    myRemoveActionButton.addClickListener(e -> {
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

    myMoveActionUpButton = Button.create(IdeLocalize.buttonMoveUpU());
    myMoveActionUpButton.addClickListener(e -> {
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

    myMoveActionDownButton = Button.create(IdeLocalize.buttonMoveDownD());
    myMoveActionDownButton.addClickListener(e -> {
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

    myRestoreAllDefaultButton = Button.create(LocalizeValue.localizeTODO("Restore All Defaults"));
    myRestoreAllDefaultButton.addClickListener(e -> {
      mySelectedSchema.copyFrom(new CustomActionsSchemaImpl(Application.get()));
      patchActionsTreeCorrespondingToSchema(root);
      myRestoreAllDefaultButton.setEnabled(false);
    });

    myRestoreDefaultButton = Button.create(LocalizeValue.localizeTODO("Restore Default"));
    myRestoreDefaultButton.addClickListener(e -> {
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

    DockLayout rightPanel = DockLayout.create();

    myPanel.add(TargetAWT.to(rightPanel), BorderLayout.EAST);

    VerticalLayout topPanel = VerticalLayout.create();
    topPanel.add(myAddActionButton);
    topPanel.add(myAddSeparatorButton);
    topPanel.add(myEditIconButton);
    topPanel.add(myRemoveActionButton);
    topPanel.add(myMoveActionUpButton);
    topPanel.add(myMoveActionDownButton);

    rightPanel.top(topPanel);

    VerticalLayout bottomPanel = VerticalLayout.create();
    bottomPanel.add(myRestoreAllDefaultButton);
    bottomPanel.add(myRestoreDefaultButton);

    rightPanel.bottom(bottomPanel);

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
        if (component instanceof KeymapGroupImpl keymapGroup) {
          selectedGroupPath.add(keymapGroup.getName());
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

  @RequiredUIAccess
  private void editToolbarIcon(String actionId, DefaultMutableTreeNode node) {
    final AnAction anAction = ActionManager.getInstance().getAction(actionId);
    if (isToolbarAction(node) && anAction.getTemplatePresentation() != null && anAction.getTemplatePresentation().getIcon() == null) {
      final int exitCode = Messages.showOkCancelDialog(
        IdeLocalize.errorAddingActionWithoutIconToToolbar().get(),
        IdeLocalize.titleUnableToAddActionWithoutIconToToolbar().get(),
        UIUtil.getInformationIcon()
      );
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
      if (value instanceof DefaultMutableTreeNode mutableTreeNode) {
        Object userObject = mutableTreeNode.getUserObject();
        Image icon = null;
        if (userObject instanceof KeymapGroupImpl group) {
          String name = group.getName();
          setText(name != null ? name : group.getId());
          icon = ObjectUtil.notNull(group.getIcon(), AllIcons.Nodes.Folder);
        }
        else if (userObject instanceof String actionId) {
          AnAction action = ActionManager.getInstance().getAction(actionId);
          String name = action != null ? action.getTemplatePresentation().getText() : null;
          setText(!StringUtil.isEmptyOrSpaces(name) ? name : actionId);
          if (action != null) {
            Image actionIcon = action.getTemplatePresentation().getIcon();
            if (actionIcon != null) {
              icon = actionIcon;
            }
          }
        }
        else if (userObject instanceof Pair pair) {
          String actionId = (String)pair.first;
          AnAction action = ActionManager.getInstance().getAction(actionId);
          setText(action != null ? action.getTemplatePresentation().getText() : actionId);
          icon = (Image)pair.second;
        }
        else if (userObject instanceof AnSeparator) {
          setText("-------------");
        }
        else if (userObject instanceof QuickList quickList) {
          setText(quickList.getDisplayName());
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
    return node.getParent() instanceof DefaultMutableTreeNode parentNode
      && parentNode.getUserObject() instanceof KeymapGroupImpl keymapGroup
      && keymapGroup.getName().equals(KeyMapLocalize.mainToolbarTitle().get());
  }

  @Nullable
  private static String getActionId(DefaultMutableTreeNode node) {
    Object userObject = node.getUserObject();
    return userObject instanceof String actionId ? actionId : userObject instanceof Pair pair ? (String)pair.first : null;
  }

  @RequiredUIAccess
  protected boolean doSetIcon(DefaultMutableTreeNode node, @Nullable String path, Component component) {
    if (StringUtil.isNotEmpty(path) && !new File(path).isFile()) {
      Messages.showErrorDialog(
        component,
        IdeLocalize.errorFileNotFoundMessage(path).get(),
        IdeLocalize.titleChooseActionIcon().get()
      );
      return false;
    }

    String actionId = getActionId(node);
    if (actionId == null) return false;

    final AnAction action = ActionManager.getInstance().getAction(actionId);
    if (action != null && action.getTemplatePresentation() != null) {
      if (StringUtil.isNotEmpty(path)) {
        Image image = null;
        try {
          image = Image.fromUrl(VfsUtil.convertToURL(VfsUtil.pathToUrl(path.replace(File.separatorChar, '/'))));
        }
        catch (IOException e) {
          LOG.debug(e);
        }
        if (image != null) {
          if (image.getWidth() > Image.DEFAULT_ICON_SIZE || image.getHeight() > Image.DEFAULT_ICON_SIZE) {
            Messages.showErrorDialog(
              component,
              IdeLocalize.customIconValidationMessage().get(),
              IdeLocalize.titleChooseActionIcon().get()
            );
            return false;
          }
          node.setUserObject(Pair.create(actionId, image));
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
    textField.addBrowseFolderListener(
      IdeLocalize.titleBrowseIcon().get(),
      IdeLocalize.promptBrowseIconForSelectedAction().get(),
      null,
      fileChooserDescriptor
    );
    InsertPathAction.addTo(textField.getTextField(), fileChooserDescriptor);
    return textField;
  }

  private class EditIconDialog extends DialogWrapper {
    private final DefaultMutableTreeNode myNode;
    protected TextFieldWithBrowseButton myTextField;

    protected EditIconDialog(DefaultMutableTreeNode node) {
      super(false);
      setTitle(IdeLocalize.titleChooseActionIcon());
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
    @RequiredUIAccess
    protected void doOKAction() {
      if (myNode != null) {
        if (!doSetIcon(myNode, myTextField.getText(), getContentPane())) {
          return;
        }
        final Object userObject = myNode.getUserObject();
        if (userObject instanceof Pair pair) {
          String actionId = (String)pair.first;
          final AnAction action = ActionManager.getInstance().getAction(actionId);
          final Image icon = (Image)pair.second;
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
      setTitle(IdeLocalize.actionChooseActionsToAdd());
      init();
    }

    @Override
    @RequiredUIAccess
    protected JComponent createCenterPanel() {
      KeymapGroupImpl rootGroup =
        ActionsTreeUtil.createMainGroup(null, null, QuickListsManager.getInstance().getAllQuickLists());
      DefaultMutableTreeNode root = ActionsTreeUtil.createNode(rootGroup);
      DefaultTreeModel model = new DefaultTreeModel(root);
      myTree = new Tree();
      myTree.setModel(model);
      myTree.setCellRenderer(new MyTreeCellRenderer());
      final ActionManager actionManager = ActionManager.getInstance();

      mySetIconButton = new JButton(IdeLocalize.buttonSetIcon().get());
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
      final JLabel label = new JLabel(IdeLocalize.labelIconPath().get());
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
    @RequiredUIAccess
    protected void doOKAction() {
      final ActionManager actionManager = ActionManager.getInstance();
      TreeUtil.traverseDepth((TreeNode)myModel.getRoot(), node -> {
        if (node instanceof DefaultMutableTreeNode mutableNode) {
          final Object userObject = mutableNode.getUserObject();
          if (userObject instanceof Pair pair) {
            String actionId = (String)pair.first;
            final AnAction action = actionManager.getAction(actionId);
            Image icon = (Image)pair.second;
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
        if (userObject instanceof String actionId) {
          final AnAction action = actionManager.getAction(actionId);
          if (action != null && action.getTemplatePresentation() != null && action.getTemplatePresentation().getIcon() != null) {
            mySetIconButton.setEnabled(true);
            return;
          }
        }
      }
      mySetIconButton.setEnabled(
        myTextField.getText().length() != 0 && selectionPath != null && new DefaultMutableTreeNode(selectionPath).isLeaf()
          && !(userObject instanceof AnSeparator)
      );
    }

    @Nullable
    public Set<Object> getTreeSelectedActionIds() {
      TreePath[] paths = myTree.getSelectionPaths();
      if (paths == null) return null;

      Set<Object> actions = new HashSet<>();
      for (TreePath path : paths) {
        Object node = path.getLastPathComponent();
        if (node instanceof DefaultMutableTreeNode defNode) {
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
