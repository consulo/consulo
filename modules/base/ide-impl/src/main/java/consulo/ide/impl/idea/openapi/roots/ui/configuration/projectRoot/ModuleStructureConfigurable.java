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

package consulo.ide.impl.idea.openapi.roots.ui.configuration.projectRoot;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.AllIcons;
import consulo.application.ApplicationManager;
import consulo.application.dumb.DumbAware;
import consulo.configurable.*;
import consulo.configurable.internal.ConfigurableWeight;
import consulo.content.library.Library;
import consulo.dataContext.DataProvider;
import consulo.disposer.Disposable;
import consulo.ide.impl.idea.ide.projectView.impl.ModuleGroupUtil;
import consulo.ide.impl.idea.openapi.roots.ui.configuration.ModuleEditor;
import consulo.ide.impl.idea.openapi.roots.ui.configuration.ModulesConfiguratorImpl;
import consulo.ide.impl.idea.openapi.roots.ui.configuration.projectRoot.daemon.ModuleProjectStructureElement;
import consulo.ide.impl.idea.openapi.roots.ui.configuration.projectRoot.daemon.ProjectStructureElement;
import consulo.ide.impl.idea.openapi.ui.NamedConfigurable;
import consulo.ide.impl.roots.ui.configuration.ProjectConfigurableWeights;
import consulo.ide.moduleImport.ModuleImportProviders;
import consulo.ide.setting.ProjectStructureSettingsUtil;
import consulo.ide.setting.ShowSettingsUtil;
import consulo.language.editor.LangDataKeys;
import consulo.localize.LocalizeValue;
import consulo.module.ModifiableModuleModel;
import consulo.module.Module;
import consulo.module.ModuleManager;
import consulo.module.content.layer.ModifiableRootModel;
import consulo.module.content.layer.orderEntry.LibraryOrderEntry;
import consulo.module.content.layer.orderEntry.OrderEntry;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.project.localize.ProjectLocalize;
import consulo.project.ui.view.tree.ModuleGroup;
import consulo.project.ui.wm.WindowManager;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.*;
import consulo.ui.ex.awt.MasterDetailsStateService;
import consulo.ui.ex.awt.Messages;
import consulo.ui.ex.awt.tree.TreeUtil;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.util.dataholder.Key;
import consulo.util.lang.Comparing;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.util.List;
import java.util.*;

/**
 * User: anna
 * Date: 02-Jun-2006
 */
@ExtensionImpl
public class ModuleStructureConfigurable extends BaseStructureConfigurable implements ConfigurableWeight, ProjectConfigurable, NonDefaultProjectConfigurable {
  public static final String ID = "project.modules";

  private static final Comparator<MyNode> NODE_COMPARATOR = (o1, o2) -> {
    final MasterDetailsConfigurable configurable1 = o1.getConfigurable();
    final MasterDetailsConfigurable configurable2 = o2.getConfigurable();
    if (configurable1.getClass() == configurable2.getClass()) {
      return o1.getDisplayName().compareToIgnoreCase(o2.getDisplayName());
    }
    final Object editableObject1 = configurable1.getEditableObject();
    final Object editableObject2 = configurable2.getEditableObject();

    if (editableObject2 instanceof Module && editableObject1 instanceof ModuleGroup) return -1;
    if (editableObject1 instanceof Module && editableObject2 instanceof ModuleGroup) return 1;

    if (editableObject2 instanceof Module && editableObject1 instanceof String) return 1;
    if (editableObject1 instanceof Module && editableObject2 instanceof String) return -1;

    if (editableObject2 instanceof ModuleGroup && editableObject1 instanceof String) return 1;
    if (editableObject1 instanceof ModuleGroup && editableObject2 instanceof String) return -1;

    return 0;
  };

  private boolean myPlainMode;

  private final Project myProject;

  private final ModuleManager myModuleManager;

  @Inject
  public ModuleStructureConfigurable(Project project, ModuleManager manager) {
    super();
    myProject = project;
    myModuleManager = manager;
  }

  @Nullable
  @Override
  public String getParentId() {
    return StandardConfigurableIds.PROJECT_GROUP;
  }

  @Nullable
  @Override
  protected MasterDetailsStateService getStateService() {
    return MasterDetailsStateService.getInstance(myProject);
  }

  @Override
  protected String getComponentStateKey() {
    return "ModuleStructureConfigurable.UI";
  }

  @Override
  protected void initTree() {
    super.initTree();
    myTree.setRootVisible(false);
  }

  @Override
  protected ArrayList<AnAction> getAdditionalActions() {
    final ArrayList<AnAction> result = new ArrayList<>();
    result.add(ActionManager.getInstance().getAction(IdeActions.GROUP_MOVE_MODULE_TO_GROUP));
    return result;
  }

  @Override
  @Nonnull
  protected ArrayList<AnAction> createActions(final boolean fromPopup) {
    final ArrayList<AnAction> result = super.createActions(fromPopup);
    result.add(AnSeparator.getInstance());
    result.add(new MyGroupAction());
    return result;
  }

  @Override
  protected void loadTree() {
    createProjectNodes();

    ((DefaultTreeModel)myTree.getModel()).reload();

    myUiDisposed = false;
  }

  @Nonnull
  @Override
  protected Collection<? extends ProjectStructureElement> getProjectStructureElements() {
    final List<ProjectStructureElement> result = new ArrayList<>();
    for (Module module : myModuleManager.getModules()) {
      result.add(new ModuleProjectStructureElement(getModulesConfigurator(), module));
    }
    return result;
  }

  @Override
  protected void updateSelection(@Nullable final MasterDetailsConfigurable configurable) {
    ApplicationManager.getApplication().assertIsDispatchThread();
    super.updateSelection(configurable);
    if (configurable != null) {
      updateModuleEditorSelection(configurable);
    }
  }


  @Override
  protected boolean isAutoScrollEnabled() {
    return myAutoScrollEnabled;
  }

  private void updateModuleEditorSelection(final MasterDetailsConfigurable configurable) {
    if (configurable instanceof ModuleConfigurable) {
      final ModuleConfigurable moduleConfigurable = (ModuleConfigurable)configurable;
      final ModuleEditor editor = moduleConfigurable.getModuleEditor();
    }
  }

  private void createProjectNodes() {
    final Map<ModuleGroup, MyNode> moduleGroup2NodeMap = new HashMap<>();
    final Module[] modules = myModuleManager.getModules();
    for (final Module module : modules) {
      ModuleConfigurable configurable = new ModuleConfigurable(getModulesConfigurator(), module, TREE_UPDATER);
      final MyNode moduleNode = new MyNode(configurable);
      final String[] groupPath = myPlainMode ? null : getModulesConfigurator().getModuleModel().getModuleGroupPath(module);
      if (groupPath == null || groupPath.length == 0) {
        addNode(moduleNode, myRoot);
      }
      else {
        final MyNode moduleGroupNode = ModuleGroupUtil.buildModuleGroupPath(new ModuleGroup(groupPath), myRoot, moduleGroup2NodeMap, it -> addNode(it.getChild(), it.getParent()), moduleGroup -> {
          final NamedConfigurable moduleGroupConfigurable = createModuleGroupConfigurable(moduleGroup);
          return new MyNode(moduleGroupConfigurable, true);
        });
        addNode(moduleNode, moduleGroupNode);
      }
    }
    if (myProject.isDefault()) {  //do not add modules node in case of template project
      myRoot.removeAllChildren();
    }
  }


  public boolean updateProjectTree(final Module[] modules, final ModuleGroup group) {
    if (myRoot.getChildCount() == 0) return false; //isn't visible
    final MyNode[] nodes = new MyNode[modules.length];
    int i = 0;
    for (Module module : modules) {
      MyNode node = findModuleNode(module);
      LOG.assertTrue(node != null, "Module " + module.getName() + " is not in project.");
      node.removeFromParent();
      nodes[i++] = node;
    }
    for (final MyNode moduleNode : nodes) {
      final String[] groupPath = myPlainMode ? null : group != null ? group.getGroupPath() : null;
      if (groupPath == null || groupPath.length == 0) {
        addNode(moduleNode, myRoot);
      }
      else {
        final MyNode moduleGroupNode =
                ModuleGroupUtil.updateModuleGroupPath(new ModuleGroup(groupPath), myRoot, g -> findNodeByObject(myRoot, g), it -> addNode(it.getChild(), it.getParent()), moduleGroup -> {
                  final NamedConfigurable moduleGroupConfigurable = createModuleGroupConfigurable(moduleGroup);
                  return new MyNode(moduleGroupConfigurable, true);
                });
        addNode(moduleNode, moduleGroupNode);
      }
    }
    TreeUtil.sort(myRoot, getNodeComparator());
    ((DefaultTreeModel)myTree.getModel()).reload(myRoot);
    return true;
  }

  @Override
  protected Comparator<MyNode> getNodeComparator() {
    return NODE_COMPARATOR;
  }

  @RequiredUIAccess
  @Override
  public void initialize() {
    super.initialize();

    addItemsChangeListener(deletedItem -> {
      if (deletedItem instanceof Library) {
        final Library library = (Library)deletedItem;
        final MyNode node = findNodeByObject(myRoot, library);
        if (node != null) {
          final TreeNode parent = node.getParent();
          node.removeFromParent();
          ((DefaultTreeModel)myTree.getModel()).reload(parent);
        }
        // todo myContext.getDaemonAnalyzer().removeElement(new LibraryProjectStructureElement(library));
      }
    });
  }

  @Override
  @RequiredUIAccess
  public void apply() throws ConfigurationException {
    final Set<MyNode> roots = new HashSet<>();
    roots.add(myRoot);
    checkApply(
      roots,
      ProjectLocalize.renameMessagePrefixModule().get(),
      ProjectLocalize.renameModuleTitle().get()
    );

    if (getModulesConfigurator().isModified()) {
      getModulesConfigurator().apply();
    }
  }

  @RequiredUIAccess
  @Override
  public boolean isModified() {
    return getModulesConfigurator().isModified();
  }

  private ModulesConfiguratorImpl getModulesConfigurator() {
    ProjectStructureSettingsUtil util = (ProjectStructureSettingsUtil)ShowSettingsUtil.getInstance();
    return (ModulesConfiguratorImpl)util.getModulesModel(myProject);
  }

  @RequiredUIAccess
  @Override
  public JComponent createComponent(Disposable parentUIDisposable) {
    return new MyDataProviderWrapper(super.createComponent(parentUIDisposable));
  }

  @Override
  protected void processRemovedItems() {
    // do nothing
  }

  @Override
  protected boolean wasObjectStored(Object editableObject) {
    return false;
  }

  @Override
  public String getDisplayName() {
    return ProjectLocalize.projectRootsDisplayName().get();
  }

  public Project getProject() {
    return myProject;
  }

  public Module[] getModules() {
    if (getModulesConfigurator() != null) {
      final ModifiableModuleModel model = getModulesConfigurator().getModuleModel();
      return model.getModules();
    }
    else {
      return myModuleManager.getModules();
    }
  }

  public void removeLibraryOrderEntry(final Module module, final Library library) {
    final ModuleEditor moduleEditor = getModulesConfigurator().getModuleEditor(module);
    LOG.assertTrue(moduleEditor != null, "Current module editor was not initialized");
    final ModifiableRootModel modelProxy = moduleEditor.getModifiableRootModelProxy();
    final OrderEntry[] entries = modelProxy.getOrderEntries();
    for (OrderEntry entry : entries) {
      if (entry instanceof LibraryOrderEntry && Comparing.strEqual(entry.getPresentableName(), library.getName())) {
        modelProxy.removeOrderEntry(entry);
        break;
      }
    }

    //todo myContext.getDaemonAnalyzer().queueUpdate(new ModuleProjectStructureElement(myContext, module));
    myTree.repaint();
  }

  public static void addLibraryOrderEntry(final Module module, final Library library) {
    Component parent = TargetAWT.to(WindowManager.getInstance().suggestParentWindow(module.getProject()));

    ProjectStructureSettingsUtil util = (ProjectStructureSettingsUtil)ShowSettingsUtil.getInstance();
    ModulesConfiguratorImpl modulesModel = (ModulesConfiguratorImpl)util.getModulesModel(module.getProject());

    final ModuleEditor moduleEditor = modulesModel.getModuleEditor(module);
    LOG.assertTrue(moduleEditor != null, "Current module editor was not initialized");
    final ModifiableRootModel modelProxy = moduleEditor.getModifiableRootModelProxy();
    final OrderEntry[] entries = modelProxy.getOrderEntries();
    for (OrderEntry entry : entries) {
      if (entry instanceof LibraryOrderEntry && Comparing.strEqual(entry.getPresentableName(), library.getName())) {
        if (Messages.showYesNoDialog(
          parent,
          ProjectLocalize.projectRootsReplaceLibraryEntryMessage(entry.getPresentableName()).get(),
          ProjectLocalize.projectRootsReplaceLibraryEntryTitle().get(),
          Messages.getInformationIcon()
        ) == Messages.YES) {
          modelProxy.removeOrderEntry(entry);
          break;
        }
      }
    }
    modelProxy.addLibraryEntry(library);
    //todo myContext.getDaemonAnalyzer().queueUpdate(new ModuleProjectStructureElement(myContext, module));
    //myTree.repaint();
  }

  @Nullable
  public MyNode findModuleNode(final Module module) {
    return findNodeByObject(myRoot, module);
  }

  @RequiredUIAccess
  private void addModule(boolean anImport) {
    getModulesConfigurator().addModule(anImport).onSuccess(modules -> {
      for (Module module : modules) {
        addModuleNode(module);
      }
    });
  }

  private void addModuleNode(final Module module) {
    final MyNode node = new MyNode(new ModuleConfigurable(getModulesConfigurator(), module, TREE_UPDATER));
    final TreePath selectionPath = myTree.getSelectionPath();
    MyNode parent = null;
    if (selectionPath != null) {
      MyNode selected = (MyNode)selectionPath.getLastPathComponent();
      final Object o = selected.getConfigurable().getEditableObject();
      if (o instanceof ModuleGroup) {
        getModulesConfigurator().getModuleModel().setModuleGroupPath(module, ((ModuleGroup)o).getGroupPath());
        parent = selected;
      }
      else if (o instanceof Module) { //create near selected
        final ModifiableModuleModel modifiableModuleModel = getModulesConfigurator().getModuleModel();
        final String[] groupPath = modifiableModuleModel.getModuleGroupPath((Module)o);
        if (groupPath != null) {
          modifiableModuleModel.setModuleGroupPath(module, groupPath);
          parent = findNodeByObject(myRoot, new ModuleGroup(groupPath));
        }
      }
    }
    if (parent == null) parent = myRoot;
    addNode(node, parent);
    ((DefaultTreeModel)myTree.getModel()).reload(parent);
    selectNodeInTree(node);
    //todo final ProjectStructureDaemonAnalyzer daemonAnalyzer = myContext.getDaemonAnalyzer();
    //daemonAnalyzer.queueUpdate(new ModuleProjectStructureElement(myContext, module));
    //daemonAnalyzer.queueUpdateForAllElementsWithErrors(); //missing modules added
  }

  @Nullable
  public Module getSelectedModule() {
    final Object selectedObject = getSelectedObject();
    if (selectedObject instanceof Module) {
      return (Module)selectedObject;
    }
    if (selectedObject instanceof Library) {
      if (((Library)selectedObject).getTable() == null) {
        final MyNode node = (MyNode)myTree.getSelectionPath().getLastPathComponent();
        return (Module)((MyNode)node.getParent()).getConfigurable().getEditableObject();
      }
    }
    return null;
  }

  @Override
  @Nonnull
  public String getId() {
    return ID;
  }

  @Nullable
  public Module getModule(final String moduleName) {
    if (moduleName == null) return null;
    return (getModulesConfigurator() != null) ? getModulesConfigurator().getModule(moduleName) : myModuleManager.findModuleByName(moduleName);
  }

  private static TextConfigurable<ModuleGroup> createModuleGroupConfigurable(final ModuleGroup moduleGroup) {
    return new TextConfigurable<>(
      moduleGroup,
      moduleGroup.toString(),
      ProjectLocalize.moduleGroupBannerText(moduleGroup.toString()).get(),
      ProjectLocalize.projectRootsModuleGroupsText().get(),
      AllIcons.Nodes.ModuleGroup
    );
  }

  @Override
  public int getConfigurableWeight() {
    return ProjectConfigurableWeights.MODULES;
  }

  private class MyDataProviderWrapper extends JPanel implements DataProvider {
    public MyDataProviderWrapper(final JComponent component) {
      super(new BorderLayout());
      add(component, BorderLayout.CENTER);
    }

    @Override
    @Nullable
    public Object getData(@Nonnull Key<?> dataId) {
      if (LangDataKeys.MODULE_CONTEXT_ARRAY == dataId) {
        final TreePath[] paths = myTree.getSelectionPaths();
        if (paths != null) {
          ArrayList<Module> modules = new ArrayList<>();
          for (TreePath path : paths) {
            MyNode node = (MyNode)path.getLastPathComponent();
            final MasterDetailsConfigurable configurable = node.getConfigurable();
            LOG.assertTrue(configurable != null, "already disposed");
            final Object o = configurable.getEditableObject();
            if (o instanceof Module) {
              modules.add((Module)o);
            }
          }
          return !modules.isEmpty() ? modules.toArray(new Module[modules.size()]) : null;
        }
      }
      if (LangDataKeys.MODULE_CONTEXT == dataId) {
        return getSelectedModule();
      }
      if (LangDataKeys.MODIFIABLE_MODULE_MODEL == dataId) {
        return getModulesConfigurator().getModuleModel();
      }

      return null;
    }
  }

  private class MyGroupAction extends ToggleAction implements DumbAware {

    public MyGroupAction() {
      super("", "", AllIcons.ObjectBrowser.CompactEmptyPackages);
    }

    @RequiredUIAccess
    @Override
    public void update(final AnActionEvent e) {
      super.update(e);
      final Presentation presentation = e.getPresentation();
      LocalizeValue text = ProjectLocalize.projectRootsPlainModeActionTextDisabled();
      if (myPlainMode) {
        text = ProjectLocalize.projectRootsPlainModeActionTextEnabled();
      }
      presentation.setTextValue(text);
      presentation.setDescriptionValue(text);

      if (getModulesConfigurator() != null) {
        presentation.setVisible(getModulesConfigurator().getModuleModel().hasModuleGroups());
      }
    }

    @Override
    public boolean isSelected(AnActionEvent e) {
      return myPlainMode;
    }

    @Override
    public void setSelected(AnActionEvent e, boolean state) {
      myPlainMode = state;
      DefaultMutableTreeNode selection = null;
      final TreePath selectionPath = myTree.getSelectionPath();
      if (selectionPath != null) {
        selection = (DefaultMutableTreeNode)selectionPath.getLastPathComponent();
      }
      final ModifiableModuleModel model = getModulesConfigurator().getModuleModel();
      final Module[] modules = model.getModules();
      for (Module module : modules) {
        final String[] groupPath = model.getModuleGroupPath(module);
        updateProjectTree(new Module[]{module}, groupPath != null ? new ModuleGroup(groupPath) : null);
      }
      if (state) {
        removeModuleGroups();
      }
      if (selection != null) {
        TreeUtil.selectInTree(selection, true, myTree);
      }
    }

    private void removeModuleGroups() {
      for (int i = myRoot.getChildCount() - 1; i >= 0; i--) {
        final MyNode node = (MyNode)myRoot.getChildAt(i);
        if (node.getConfigurable().getEditableObject() instanceof ModuleGroup) {
          node.removeFromParent();
        }
      }
      ((DefaultTreeModel)myTree.getModel()).reload(myRoot);
    }
  }

  @Override
  protected AbstractAddGroup createAddAction() {
    return new AbstractAddGroup(ProjectLocalize.addNewHeaderText().get()) {
      @Override
      @Nonnull
      public AnAction[] getChildren(@Nullable final AnActionEvent e) {

        ArrayList<AnAction> result = new ArrayList<>();

        AnAction addModuleAction = new AddModuleAction(false);
        addModuleAction.getTemplatePresentation().setText("New Module");
        result.add(addModuleAction);

        AnAction importModuleAction = new AddModuleAction(true);
        importModuleAction.getTemplatePresentation().setText("Import Module");
        importModuleAction.getTemplatePresentation().setIcon(PlatformIconGroup.actionsImport());
        result.add(importModuleAction);

        return result.toArray(new AnAction[result.size()]);
      }
    };
  }

  @Override
  protected boolean removeModule(final Module module) {
    ModulesConfiguratorImpl modulesConfigurator = getModulesConfigurator();
    if (!modulesConfigurator.deleteModule(module)) {
      //wait for confirmation
      return false;
    }
    // todo myContext.getDaemonAnalyzer().removeElement(new ModuleProjectStructureElement(myContext, module));
    return true;
  }

  @Override
  @Nullable
  protected String getEmptySelectionString() {
    return ProjectLocalize.emptyModuleSelectionString().get();
  }

  public static boolean processModulesMoved(ModuleStructureConfigurable configurable, Module[] modules, @Nullable final ModuleGroup targetGroup) {
    if (configurable.updateProjectTree(modules, targetGroup)) { //inside project root editor
      if (targetGroup != null) {
        configurable.selectNodeInTree(targetGroup.toString());
      }
      else {
        configurable.selectNodeInTree(modules[0].getName());
      }
      return true;
    }
    return false;
  }

  private class AddModuleAction extends AnAction implements DumbAware {

    private final boolean myImport;

    public AddModuleAction(boolean anImport) {
      super(ProjectLocalize.addNewModuleTextFull(), LocalizeValue.empty(), AllIcons.Actions.Module);
      myImport = anImport;
    }

    @RequiredUIAccess
    @Override
    public void actionPerformed(final AnActionEvent e) {
      addModule(myImport);
    }

    @RequiredUIAccess
    @Override
    public void update(AnActionEvent e) {
      super.update(e);
      if (myImport) {
        Presentation presentation = e.getPresentation();
        presentation.setEnabledAndVisible(!ModuleImportProviders.getExtensions(true).isEmpty());
      }
    }
  }
}
