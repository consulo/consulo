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

package com.intellij.openapi.roots.ui.configuration.projectRoot;

import com.intellij.icons.AllIcons;
import com.intellij.ide.projectView.impl.ModuleGroup;
import com.intellij.ide.projectView.impl.ModuleGroupUtil;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.module.ModifiableModuleModel;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleConfigurationEditor;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectBundle;
import com.intellij.openapi.roots.LibraryOrderEntry;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.OrderEntry;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.roots.ui.configuration.ClasspathEditor;
import com.intellij.openapi.roots.ui.configuration.ModuleEditor;
import com.intellij.openapi.roots.ui.configuration.ModulesConfigurator;
import com.intellij.openapi.roots.ui.configuration.ProjectStructureConfigurable;
import com.intellij.openapi.roots.ui.configuration.projectRoot.daemon.LibraryProjectStructureElement;
import com.intellij.openapi.roots.ui.configuration.projectRoot.daemon.ModuleProjectStructureElement;
import com.intellij.openapi.roots.ui.configuration.projectRoot.daemon.ProjectStructureDaemonAnalyzer;
import com.intellij.openapi.roots.ui.configuration.projectRoot.daemon.ProjectStructureElement;
import com.intellij.openapi.ui.MasterDetailsComponent;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.NamedConfigurable;
import com.intellij.openapi.util.*;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.ui.navigation.Place;
import com.intellij.util.Consumer;
import com.intellij.util.Function;
import com.intellij.util.ui.tree.TreeUtil;
import consulo.awt.TargetAWT;
import consulo.moduleImport.ModuleImportProviders;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.dataholder.Key;
import org.jetbrains.annotations.NonNls;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;
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
@Singleton
public class ModuleStructureConfigurable extends BaseStructureConfigurable implements Place.Navigator {
  private static final Comparator<MyNode> NODE_COMPARATOR = new Comparator<MyNode>() {
    @Override
    public int compare(final MyNode o1, final MyNode o2) {
      final NamedConfigurable configurable1 = o1.getConfigurable();
      final NamedConfigurable configurable2 = o2.getConfigurable();
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
    }
  };

  private boolean myPlainMode;

  private final ModuleManager myModuleManager;

  @Inject
  public ModuleStructureConfigurable(Project project, ModuleManager manager) {
    super(project);
    myModuleManager = manager;
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
    final ArrayList<AnAction> result = new ArrayList<AnAction>();
    result.add(ActionManager.getInstance().getAction(IdeActions.GROUP_MOVE_MODULE_TO_GROUP));
    return result;
  }

  @Override
  public void addNode(MyNode nodeToAdd, MyNode parent) {
    super.addNode(nodeToAdd, parent);
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
    final List<ProjectStructureElement> result = new ArrayList<ProjectStructureElement>();
    for (Module module : myModuleManager.getModules()) {
      result.add(new ModuleProjectStructureElement(myContext, module));
    }
    return result;
  }

  @Override
  protected void updateSelection(@Nullable final NamedConfigurable configurable) {
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

  private void updateModuleEditorSelection(final NamedConfigurable configurable) {
    if (configurable instanceof ModuleConfigurable){
      final ModuleConfigurable moduleConfigurable = (ModuleConfigurable)configurable;
      final ModuleEditor editor = moduleConfigurable.getModuleEditor();
      if (editor != null) { //already deleted
        editor.init(myHistory);
      }
    }
  }



  private void createProjectNodes() {
    final Map<ModuleGroup, MyNode> moduleGroup2NodeMap = new HashMap<ModuleGroup, MyNode>();
    final Module[] modules = myModuleManager.getModules();
    for (final Module module : modules) {
      ModuleConfigurable configurable = new ModuleConfigurable(myContext.myModulesConfigurator, module, TREE_UPDATER);
      final MyNode moduleNode = new MyNode(configurable);
      final String[] groupPath = myPlainMode ? null : myContext.myModulesConfigurator.getModuleModel().getModuleGroupPath(module);
      if (groupPath == null || groupPath.length == 0){
        addNode(moduleNode, myRoot);
      } else {
        final MyNode moduleGroupNode = ModuleGroupUtil
          .buildModuleGroupPath(new ModuleGroup(groupPath), myRoot, moduleGroup2NodeMap,
                                new Consumer<ModuleGroupUtil.ParentChildRelation<MyNode>>() {
                                  @Override
                                  public void consume(final ModuleGroupUtil.ParentChildRelation<MyNode> parentChildRelation) {
                                    addNode(parentChildRelation.getChild(), parentChildRelation.getParent());
                                  }
                                },
                                new Function<ModuleGroup, MyNode>() {
                                  @Override
                                  public MyNode fun(final ModuleGroup moduleGroup) {
                                    final NamedConfigurable moduleGroupConfigurable =
                                      createModuleGroupConfigurable(moduleGroup);
                                    return new MyNode(moduleGroupConfigurable, true);
                                  }
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
    final MyNode [] nodes = new MyNode[modules.length];
    int i = 0;
    for (Module module : modules) {
      MyNode node = findModuleNode(module);
      LOG.assertTrue(node != null, "Module " + module.getName() + " is not in project.");
      node.removeFromParent();
      nodes[i ++] = node;
    }
    for (final MyNode moduleNode : nodes) {
      final String[] groupPath = myPlainMode
                                 ? null
                                 : group != null ? group.getGroupPath() : null;
      if (groupPath == null || groupPath.length == 0){
        addNode(moduleNode, myRoot);
      } else {
        final MyNode moduleGroupNode = ModuleGroupUtil
          .updateModuleGroupPath(new ModuleGroup(groupPath), myRoot, new Function<ModuleGroup, MyNode>() {
            @Override
            @Nullable
            public MyNode fun(final ModuleGroup group) {
              return findNodeByObject(myRoot, group);
            }
          }, new Consumer<ModuleGroupUtil.ParentChildRelation<MyNode>>() {
            @Override
            public void consume(final ModuleGroupUtil.ParentChildRelation<MyNode> parentChildRelation) {
              addNode(parentChildRelation.getChild(), parentChildRelation.getParent());
            }
          }, new Function<ModuleGroup, MyNode>() {
            @Override
            public MyNode fun(final ModuleGroup moduleGroup) {
              final NamedConfigurable moduleGroupConfigurable = createModuleGroupConfigurable(moduleGroup);
              return new MyNode(moduleGroupConfigurable, true);
            }
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

  @Override
  public void init(final StructureConfigurableContext context) {
    super.init(context);

    addItemsChangeListener(new ItemsChangeListener() {
      @Override
      public void itemChanged(@Nullable Object deletedItem) {
        if (deletedItem instanceof Library) {
          final Library library = (Library)deletedItem;
          final MyNode node = findNodeByObject(myRoot, library);
          if (node != null) {
            final TreeNode parent = node.getParent();
            node.removeFromParent();
            ((DefaultTreeModel)myTree.getModel()).reload(parent);
          }
          myContext.getDaemonAnalyzer().removeElement(new LibraryProjectStructureElement(myContext, library));
        }
      }

      @Override
      public void itemsExternallyChanged() {
        //do nothing
      }
    });
  }


  @Override
  @RequiredUIAccess
  public void apply() throws ConfigurationException {
    final Set<MyNode> roots = new HashSet<MyNode>();
    roots.add(myRoot);
    checkApply(roots, ProjectBundle.message("rename.message.prefix.module"), ProjectBundle.message("rename.module.title"));

    if (myContext.myModulesConfigurator.isModified()) {
      myContext.myModulesConfigurator.apply();
    }
  }

  @Override
  public boolean isModified() {
    return myContext.myModulesConfigurator.isModified();
  }

  @Override
  @RequiredUIAccess
  public void disposeUIResources() {
    super.disposeUIResources();
    myContext.myModulesConfigurator.disposeUIResources();
    ModuleStructureConfigurable.super.disposeUIResources();
  }

  @Override
  public void dispose() {}


  @Override
  public JComponent createComponent() {
    return new MyDataProviderWrapper(super.createComponent());
  }

  @Nonnull
  @Override
  public Couple<JComponent> createSplitterComponents() {
    Couple<JComponent> couple = super.createSplitterComponents();
    return Couple.of(new MyDataProviderWrapper(couple.getFirst()), couple.getSecond());
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
    return ProjectBundle.message("project.roots.display.name");
  }

  @Override
  @Nullable
  @NonNls
  public String getHelpTopic() {
    final String topic = super.getHelpTopic();
    if (topic != null) {
      return topic;
    }
    return "reference.settingsdialog.project.structure.module";
  }

  public AsyncResult<Void> selectOrderEntry(@Nonnull final Module module, @Nullable final OrderEntry orderEntry) {
    Place p = new Place();
    p.putPath(ProjectStructureConfigurable.CATEGORY, this);
    Runnable r = null;

    final MasterDetailsComponent.MyNode node = findModuleNode(module);
    if (node != null) {
      p.putPath(TREE_OBJECT, module);
      p.putPath(ModuleEditor.SELECTED_EDITOR_NAME, ClasspathEditor.NAME);
      r = new Runnable() {
        @Override
        public void run() {
          if (orderEntry != null) {
            ModuleEditor moduleEditor = ((ModuleConfigurable)node.getConfigurable()).getModuleEditor();
            ModuleConfigurationEditor editor = moduleEditor.getEditor(ClasspathEditor.NAME);
            if (editor instanceof ClasspathEditor) {
              ((ClasspathEditor)editor).selectOrderEntry(orderEntry);
            }
          }
        }
      };
    }
    final AsyncResult<Void> result = ProjectStructureConfigurable.getInstance(myProject).navigateTo(p, true);
    return r != null ? result.doWhenDone(r) : result;
  }


  public static ModuleStructureConfigurable getInstance(final Project project) {
    return ServiceManager.getService(project, ModuleStructureConfigurable.class);
  }

  public Project getProject() {
    return myProject;
  }

  public Module[] getModules() {
    if (myContext.myModulesConfigurator != null) {
      final ModifiableModuleModel model = myContext.myModulesConfigurator.getModuleModel();
      return model.getModules();
    } else {
      return myModuleManager.getModules();
    }
  }

  public void removeLibraryOrderEntry(final Module module, final Library library) {
    final ModuleEditor moduleEditor = myContext.myModulesConfigurator.getModuleEditor(module);
    LOG.assertTrue(moduleEditor != null, "Current module editor was not initialized");
    final ModifiableRootModel modelProxy = moduleEditor.getModifiableRootModelProxy();
    final OrderEntry[] entries = modelProxy.getOrderEntries();
    for (OrderEntry entry : entries) {
      if (entry instanceof LibraryOrderEntry && Comparing.strEqual(entry.getPresentableName(), library.getName())) {
        modelProxy.removeOrderEntry(entry);
        break;
      }
    }

    myContext.getDaemonAnalyzer().queueUpdate(new ModuleProjectStructureElement(myContext, module));
    myTree.repaint();
  }

  public void addLibraryOrderEntry(final Module module, final Library library) {
    Component parent = TargetAWT.to(WindowManager.getInstance().suggestParentWindow(module.getProject()));

    final ModuleEditor moduleEditor = myContext.myModulesConfigurator.getModuleEditor(module);
    LOG.assertTrue(moduleEditor != null, "Current module editor was not initialized");
    final ModifiableRootModel modelProxy = moduleEditor.getModifiableRootModelProxy();
    final OrderEntry[] entries = modelProxy.getOrderEntries();
    for (OrderEntry entry : entries) {
      if (entry instanceof LibraryOrderEntry && Comparing.strEqual(entry.getPresentableName(), library.getName())) {
        if (Messages.showYesNoDialog(parent,
                                     ProjectBundle.message("project.roots.replace.library.entry.message", entry.getPresentableName()),
                                     ProjectBundle.message("project.roots.replace.library.entry.title"),
                                     Messages.getInformationIcon()) == Messages.YES) {
          modelProxy.removeOrderEntry(entry);
          break;
        }
      }
    }
    modelProxy.addLibraryEntry(library);
    myContext.getDaemonAnalyzer().queueUpdate(new ModuleProjectStructureElement(myContext, module));
    myTree.repaint();
  }

  @Nullable
  public MyNode findModuleNode(final Module module) {
    return findNodeByObject(myRoot, module);
  }

  @RequiredUIAccess
  private void addModule(boolean anImport) {
    myContext.myModulesConfigurator.addModule(anImport).onSuccess(modules -> {
      for (Module module : modules) {
        addModuleNode(module);
      }
    });
  }

  private void addModuleNode(final Module module) {
    final MyNode node = new MyNode(new ModuleConfigurable(myContext.myModulesConfigurator, module, TREE_UPDATER));
    final TreePath selectionPath = myTree.getSelectionPath();
    MyNode parent = null;
    if (selectionPath != null) {
      MyNode selected = (MyNode)selectionPath.getLastPathComponent();
      final Object o = selected.getConfigurable().getEditableObject();
      if (o instanceof ModuleGroup) {
        myContext.myModulesConfigurator.getModuleModel().setModuleGroupPath(module, ((ModuleGroup)o).getGroupPath());
        parent = selected;
      } else if (o instanceof Module) { //create near selected
        final ModifiableModuleModel modifiableModuleModel = myContext.myModulesConfigurator.getModuleModel();
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
    final ProjectStructureDaemonAnalyzer daemonAnalyzer = myContext.getDaemonAnalyzer();
    daemonAnalyzer.queueUpdate(new ModuleProjectStructureElement(myContext, module));
    daemonAnalyzer.queueUpdateForAllElementsWithErrors(); //missing modules added
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
  @NonNls
  public String getId() {
    return "project.structure";
  }

  @Override
  @Nullable
  public Runnable enableSearch(String option) {
    return null;
  }


  @Nullable
  public Module getModule(final String moduleName) {
    if (moduleName == null) return null;
    return (myContext != null && myContext.myModulesConfigurator != null) ? myContext.myModulesConfigurator.getModule(moduleName) : myModuleManager.findModuleByName(moduleName);
  }

  public StructureConfigurableContext getContext() {
    return myContext;
  }

  private static TextConfigurable<ModuleGroup> createModuleGroupConfigurable(final ModuleGroup moduleGroup) {
    return new TextConfigurable<ModuleGroup>(moduleGroup, moduleGroup.toString(),
                                             ProjectBundle.message("module.group.banner.text", moduleGroup.toString()),
                                             ProjectBundle.message("project.roots.module.groups.text"),
                                             AllIcons.Nodes.ModuleGroup);
  }

  private class MyDataProviderWrapper extends JPanel implements DataProvider {
    public MyDataProviderWrapper(final JComponent component) {
      super(new BorderLayout());
      add(component, BorderLayout.CENTER);
    }

    @Override
    @Nullable
    public Object getData(@Nonnull @NonNls Key<?> dataId) {
      if (LangDataKeys.MODULE_CONTEXT_ARRAY == dataId){
        final TreePath[] paths = myTree.getSelectionPaths();
        if (paths != null) {
          ArrayList<Module> modules = new ArrayList<Module>();
          for (TreePath path : paths) {
            MyNode node = (MyNode)path.getLastPathComponent();
            final NamedConfigurable configurable = node.getConfigurable();
            LOG.assertTrue(configurable != null, "already disposed");
            final Object o = configurable.getEditableObject();
            if (o instanceof Module) {
              modules.add((Module)o);
            }
          }
          return !modules.isEmpty() ? modules.toArray(new Module[modules.size()]) : null;
        }
      }
      if (LangDataKeys.MODULE_CONTEXT == dataId){
        return getSelectedModule();
      }
      if (LangDataKeys.MODIFIABLE_MODULE_MODEL == dataId){
        return myContext.myModulesConfigurator.getModuleModel();
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
      String text = ProjectBundle.message("project.roots.plain.mode.action.text.disabled");
      if (myPlainMode){
        text = ProjectBundle.message("project.roots.plain.mode.action.text.enabled");
      }
      presentation.setText(text);
      presentation.setDescription(text);

      if (myContext.myModulesConfigurator != null) {
        presentation.setVisible(myContext.myModulesConfigurator.getModuleModel().hasModuleGroups());
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
      if (selectionPath != null){
        selection = (DefaultMutableTreeNode)selectionPath.getLastPathComponent();
      }
      final ModifiableModuleModel model = myContext.myModulesConfigurator.getModuleModel();
      final Module[] modules = model.getModules();
      for (Module module : modules) {
        final String[] groupPath = model.getModuleGroupPath(module);
        updateProjectTree(new Module[]{module}, groupPath != null ? new ModuleGroup(groupPath) : null);
      }
      if (state) {
        removeModuleGroups();
      }
      if (selection != null){
        TreeUtil.selectInTree(selection, true, myTree);
      }
    }

    private void removeModuleGroups() {
      for(int i = myRoot.getChildCount() - 1; i >=0; i--){
        final MyNode node = (MyNode)myRoot.getChildAt(i);
        if (node.getConfigurable().getEditableObject() instanceof ModuleGroup){
          node.removeFromParent();
        }
      }
      ((DefaultTreeModel)myTree.getModel()).reload(myRoot);
    }
  }

  @Override
  protected AbstractAddGroup createAddAction() {
    return new AbstractAddGroup(ProjectBundle.message("add.new.header.text")) {
      @Override
      @Nonnull
      public AnAction[] getChildren(@Nullable final AnActionEvent e) {

        ArrayList<AnAction> result = new ArrayList<AnAction>();

        AnAction addModuleAction = new AddModuleAction(false);
        addModuleAction.getTemplatePresentation().setText("New Module");
        result.add(addModuleAction);

        AnAction importModuleAction = new AddModuleAction(true);
        importModuleAction.getTemplatePresentation().setText("Import Module");
        importModuleAction.getTemplatePresentation().setIcon(AllIcons.ToolbarDecorator.Import);
        result.add(importModuleAction);

        return result.toArray(new AnAction[result.size()]);
      }
    };
  }

  @Override
  protected boolean removeModule(final Module module) {
    ModulesConfigurator modulesConfigurator = myContext.myModulesConfigurator;
    if (!modulesConfigurator.deleteModule(module)) {
      //wait for confirmation
      return false;
    }
    myContext.getDaemonAnalyzer().removeElement(new ModuleProjectStructureElement(myContext, module));
    return true;
  }

  @Override
  @Nullable
  protected String getEmptySelectionString() {
    return ProjectBundle.message("empty.module.selection.string");
  }

  private class AddModuleAction extends AnAction implements DumbAware {

    private final boolean myImport;

    public AddModuleAction(boolean anImport) {
      super(ProjectBundle.message("add.new.module.text.full"), null, AllIcons.Actions.Module);
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
      if(myImport) {
        Presentation presentation = e.getPresentation();
        presentation.setEnabledAndVisible(!ModuleImportProviders.getExtensions(true).isEmpty());
      }
    }
  }
}
