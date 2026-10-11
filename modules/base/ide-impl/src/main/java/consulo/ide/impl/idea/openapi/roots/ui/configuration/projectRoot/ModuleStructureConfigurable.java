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
import consulo.configurable.Configurable;
import consulo.configurable.ConfigurationException;
import consulo.configurable.MasterDetailsConfigurable;
import consulo.configurable.NonDefaultProjectConfigurable;
import consulo.configurable.ProjectConfigurable;
import consulo.configurable.StandardConfigurableIds;
import consulo.configurable.UnifiedMasterDetailsComponent;
import consulo.configurable.internal.ConfigurableWeight;
import consulo.dataContext.DataSink;
import consulo.ide.impl.idea.openapi.roots.ui.configuration.ModuleEditor;
import consulo.ide.impl.idea.openapi.roots.ui.configuration.ModulesConfiguratorImpl;
import consulo.ide.impl.module.importing.ModuleImportProviders;
import consulo.ide.impl.roots.ui.configuration.ProjectConfigurableWeights;
import consulo.ide.setting.ProjectStructureSettingsUtil;
import consulo.ide.setting.ShowSettingsUtil;
import consulo.language.editor.LangDataKeys;
import consulo.localize.LocalizeValue;
import consulo.module.ModifiableModuleModel;
import consulo.module.Module;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.platform.base.localize.CommonLocalize;
import consulo.project.Project;
import consulo.project.localize.ProjectLocalize;
import consulo.project.ui.view.tree.ModuleGroup;
import consulo.ui.TextItemPresentation;
import consulo.ui.Tree;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.ActionManager;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.AnActionWithSyncUpdate;
import consulo.ui.ex.action.AnSeparator;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.ui.ex.action.IdeActions;
import consulo.ui.ex.action.Presentation;
import consulo.ui.ex.action.ToggleAction;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.ui.ex.popup.ListPopup;
import jakarta.inject.Inject;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * @author anna
 * @since 2006-06-02
 */
@ExtensionImpl
public class ModuleStructureConfigurable extends UnifiedMasterDetailsComponent
    implements ConfigurableWeight, ProjectConfigurable, NonDefaultProjectConfigurable, Configurable.NoMargin, Configurable.NoScroll {
    public static final String ID = "project.modules";

    private final Project myProject;
    private final Map<List<String>, ModuleGroup> myGroups = new HashMap<>();
    private final Map<Module, String> myModuleNames = new HashMap<>();

    private boolean myPlainMode;

    @Inject
    public ModuleStructureConfigurable(Project project) {
        myProject = project;
    }

    @Override
    public LocalizeValue getDisplayName() {
        return ProjectLocalize.projectRootsDisplayName();
    }

    @Override
    public @Nullable String getParentId() {
        return StandardConfigurableIds.PROJECT_GROUP;
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public int getConfigurableWeight() {
        return ProjectConfigurableWeights.MODULES;
    }

    @Override
    protected LocalizeValue getEmptySelectionText() {
        return ProjectLocalize.emptyModuleSelectionString();
    }

    @Override
    protected boolean isMultipleSelectAllowed() {
        return true;
    }

    @Override
    protected boolean isExpandAllByDefault() {
        return true;
    }

    @Override
    protected boolean isRemovedWhenEmpty(Object value) {
        return value instanceof ModuleGroup;
    }

    @Override
    protected @Nullable Comparator<Object> getValueComparator() {
        return Comparator.<Object>comparingInt(value -> value instanceof ModuleGroup ? 0 : 1).thenComparing(this::getValueText);
    }

    @Override
    protected String getValueText(Object value) {
        if (value instanceof Module module) {
            return getModuleName(module);
        }
        return String.valueOf(value);
    }

    @Override
    protected void renderValue(Object value, TextItemPresentation presentation) {
        if (value instanceof Module module) {
            presentation.withIcon(PlatformIconGroup.nodesModule());
            presentation.append(getModuleName(module));
        }
        else if (value instanceof ModuleGroup moduleGroup) {
            presentation.withIcon(PlatformIconGroup.nodesModulegroup());
            presentation.append(moduleGroup.toString());
        }
    }

    @RequiredUIAccess
    @Override
    protected void loadTree() {
        if (myProject.isDefault()) {
            return;
        }

        ModifiableModuleModel moduleModel = getModulesConfigurator().getModuleModel();
        for (Module module : moduleModel.getModules()) {
            String[] groupPath = myPlainMode ? null : moduleModel.getModuleGroupPath(module);
            addValue(groupPath == null || groupPath.length == 0 ? null : ensureGroup(groupPath), module);
        }
    }

    private ModuleGroup ensureGroup(String[] groupPath) {
        ModuleGroup parent = null;
        for (int i = 1; i <= groupPath.length; i++) {
            List<String> key = List.of(Arrays.copyOf(groupPath, i));
            ModuleGroup group = myGroups.computeIfAbsent(key, it -> new ModuleGroup(it.toArray(String[]::new)));
            if (!isAttached(group)) {
                addValue(parent, group);
            }
            parent = group;
        }
        return Objects.requireNonNull(parent);
    }

    @RequiredUIAccess
    @Override
    protected @Nullable MasterDetailsConfigurable<?> createConfigurable(Object value) {
        if (value instanceof Module module) {
            return new ModuleConfigurable(getModulesConfigurator(), module, this::onModuleRenamed);
        }
        if (value instanceof ModuleGroup moduleGroup) {
            return new ModuleGroupConfigurable(moduleGroup);
        }
        return null;
    }

    @Override
    protected List<AnAction> createActions() {
        return List.of(new AddModuleGroupAction(), new RemoveModuleAction(), AnSeparator.getInstance(), new PlainModeAction());
    }

    @Override
    protected List<AnAction> createPopupActions() {
        List<AnAction> actions = new ArrayList<>();
        actions.add(new AddModuleGroupAction());
        actions.add(new RemoveModuleAction());
        AnAction moveToGroup = ActionManager.getInstance().getAction(IdeActions.GROUP_MOVE_MODULE_TO_GROUP);
        if (moveToGroup != null) {
            actions.add(AnSeparator.getInstance());
            actions.add(moveToGroup);
        }
        return actions;
    }

    @Override
    protected void uiDataSnapshot(DataSink sink) {
        List<Module> modules = getSelectedModules();
        sink.set(Project.KEY, myProject);
        sink.set(LangDataKeys.MODULE_CONTEXT_ARRAY, modules.isEmpty() ? null : modules.toArray(Module.EMPTY_ARRAY));
        sink.set(LangDataKeys.MODULE_CONTEXT, getSelectedValue() instanceof Module module ? module : null);
        sink.set(LangDataKeys.MODIFIABLE_MODULE_MODEL, getModulesConfigurator().getModuleModel());
    }

    @RequiredUIAccess
    @Override
    public boolean isModified() {
        return getModulesConfigurator().isModified();
    }

    @RequiredUIAccess
    @Override
    public void apply() throws ConfigurationException {
        checkModuleNames();

        super.apply();

        ModulesConfiguratorImpl modulesConfigurator = getModulesConfigurator();
        if (modulesConfigurator.isModified()) {
            modulesConfigurator.apply();
        }
        myModuleNames.clear();
    }

    @RequiredUIAccess
    @Override
    public void reset() {
        myModuleNames.clear();

        super.reset();
    }

    @RequiredUIAccess
    @Override
    public void disposeUIResources() {
        super.disposeUIResources();

        myGroups.clear();
    }

    public Project getProject() {
        return myProject;
    }

    @RequiredUIAccess
    public CompletableFuture<@Nullable ModuleEditor> selectModule(String moduleName) {
        Object value = findValue(candidate -> candidate instanceof Module module && moduleName.equals(getModuleName(module)));
        if (value == null) {
            value = findValue(candidate -> candidate instanceof Module module && moduleName.equals(module.getName()));
        }
        return selectModuleValue(value instanceof Module module ? module : null);
    }

    @RequiredUIAccess
    public CompletableFuture<@Nullable ModuleEditor> selectModule(Module module) {
        return selectModuleValue(isAttached(module) ? module : null);
    }

    @RequiredUIAccess
    private CompletableFuture<@Nullable ModuleEditor> selectModuleValue(@Nullable Module module) {
        if (module == null) {
            return CompletableFuture.completedFuture(null);
        }

        UIAccess uiAccess = UIAccess.current();
        return select(module).handleAsync((value, error) -> getModulesConfigurator().getModuleEditor(module), uiAccess);
    }

    @RequiredUIAccess
    public static boolean processModulesMoved(ModuleStructureConfigurable configurable, Module[] modules, @Nullable ModuleGroup targetGroup) {
        if (configurable.getTree() == null) {
            return false;
        }

        UIAccess uiAccess = UIAccess.current();
        configurable.reloadTree(null).whenCompleteAsync((value, error) -> {
            Object toSelect = targetGroup == null || configurable.myPlainMode
                ? modules[0]
                : configurable.myGroups.get(List.of(targetGroup.getGroupPath()));
            configurable.select(toSelect);
        }, uiAccess);
        return true;
    }

    private String getModuleName(Module module) {
        String name = myModuleNames.get(module);
        return name != null ? name : getModulesConfigurator().getRealName(module);
    }

    @RequiredUIAccess
    private void onModuleRenamed(Module module, String name) {
        myModuleNames.put(module, name);
        if (isAttached(module)) {
            refreshValue(module);
        }
    }

    private List<Module> getSelectedModules() {
        List<Module> modules = new ArrayList<>();
        for (Object value : getSelectedValues()) {
            if (value instanceof Module module) {
                modules.add(module);
            }
        }
        return modules;
    }

    @RequiredUIAccess
    private void checkModuleNames() throws ConfigurationException {
        Set<String> names = new HashSet<>();
        for (Module module : getModulesConfigurator().getModuleModel().getModules()) {
            String name = getModuleName(module);
            if (name.isBlank()) {
                select(module);
                throw new ConfigurationException(ProjectLocalize.moduleNameEmptyError(), ProjectLocalize.renameModuleTitle());
            }
            if (!names.add(name)) {
                select(module);
                throw new ConfigurationException(ProjectLocalize.moduleNameAlreadyExistsError(name), ProjectLocalize.renameModuleTitle());
            }
        }
    }

    private ModulesConfiguratorImpl getModulesConfigurator() {
        ProjectStructureSettingsUtil util = ShowSettingsUtil.getInstance();
        return (ModulesConfiguratorImpl) util.getModulesModel(myProject);
    }

    @RequiredUIAccess
    private void addModule(boolean anImport) {
        UIAccess uiAccess = UIAccess.current();
        Object selected = getSelectedValue();
        getModulesConfigurator().addModule(anImport).onSuccess(modules -> uiAccess.execute(() -> {
            if (modules.isEmpty()) {
                return;
            }

            ModifiableModuleModel moduleModel = getModulesConfigurator().getModuleModel();
            String[] groupPath = null;
            if (selected instanceof ModuleGroup moduleGroup) {
                groupPath = moduleGroup.getGroupPath();
            }
            else if (selected instanceof Module selectedModule) {
                groupPath = moduleModel.getModuleGroupPath(selectedModule);
            }

            if (groupPath != null) {
                for (Module module : modules) {
                    moduleModel.setModuleGroupPath(module, groupPath);
                }
            }

            reloadTree(modules.get(0));
        }));
    }

    @RequiredUIAccess
    private CompletableFuture<?> removeModules(List<Module> modules, int index) {
        if (index >= modules.size()) {
            return CompletableFuture.completedFuture(null);
        }

        Tree<Object> tree = getTree();
        UIAccess uiAccess = UIAccess.current();
        return getModulesConfigurator().deleteModule(modules.get(index), tree)
            .thenComposeAsync(removed -> removeModules(modules, index + 1), uiAccess);
    }

    private class AddModuleGroupAction extends DumbAwareAction {
        private AddModuleGroupAction() {
            super(ProjectLocalize.addNewHeaderText(), LocalizeValue.empty(), PlatformIconGroup.generalAdd());
        }

        @RequiredUIAccess
        @Override
        public void actionPerformed(AnActionEvent e) {
            ActionGroup.Builder group = ActionGroup.newImmutableBuilder();
            group.add(new AddModuleAction(false));
            if (!ModuleImportProviders.getExtensions(true).isEmpty()) {
                group.add(new AddModuleAction(true));
            }

            ListPopup popup = JBPopupFactory.getInstance().createActionGroupPopup(
                ProjectLocalize.addNewHeaderText().get(),
                group.build(),
                e.getDataContext(),
                JBPopupFactory.ActionSelectionAid.SPEEDSEARCH,
                false
            );
            popup.showUnderneathOf(e);
        }
    }

    private class AddModuleAction extends DumbAwareAction {
        private final boolean myImport;

        private AddModuleAction(boolean anImport) {
            super(
                anImport ? ProjectLocalize.moduleImportAction() : ProjectLocalize.moduleNewAction(),
                LocalizeValue.empty(),
                anImport ? PlatformIconGroup.actionsImport() : PlatformIconGroup.nodesModule()
            );
            myImport = anImport;
        }

        @RequiredUIAccess
        @Override
        public void actionPerformed(AnActionEvent e) {
            addModule(myImport);
        }
    }

    private class RemoveModuleAction extends DumbAwareAction implements AnActionWithSyncUpdate {
        private RemoveModuleAction() {
            super(CommonLocalize.buttonDelete(), CommonLocalize.buttonDelete(), PlatformIconGroup.generalRemove());
        }

        @RequiredUIAccess
        @Override
        public void actionPerformed(AnActionEvent e) {
            List<Module> modules = getSelectedModules();
            if (modules.isEmpty()) {
                return;
            }

            UIAccess uiAccess = UIAccess.current();
            removeModules(modules, 0).whenCompleteAsync((value, error) -> reloadTree(null), uiAccess);
        }

        @Override
        public void update(AnActionEvent e) {
            e.getPresentation().setEnabled(!getSelectedModules().isEmpty());
        }
    }

    private class PlainModeAction extends ToggleAction implements AnActionWithSyncUpdate {
        private PlainModeAction() {
            super(LocalizeValue.empty(), LocalizeValue.empty(), PlatformIconGroup.objectbrowserCompactemptypackages());
        }

        @Override
        public void update(AnActionEvent e) {
            super.update(e);

            Presentation presentation = e.getPresentation();
            LocalizeValue text = myPlainMode
                ? ProjectLocalize.projectRootsPlainModeActionTextEnabled()
                : ProjectLocalize.projectRootsPlainModeActionTextDisabled();
            presentation.setText(text);
            presentation.setDescription(text);
            presentation.setVisible(getModulesConfigurator().getModuleModel().hasModuleGroups());
        }

        @Override
        public boolean isSelected(AnActionEvent e) {
            return myPlainMode;
        }

        @RequiredUIAccess
        @Override
        public void setSelected(AnActionEvent e, boolean state) {
            myPlainMode = state;
            reloadTree(null);
        }
    }
}
