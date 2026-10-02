/*
 * Copyright 2013-2026 consulo.io
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
package consulo.ide.impl.idea.find.impl;

import consulo.content.scope.ScopeDescriptor;
import consulo.content.scope.SearchScope;
import consulo.fileChooser.FileChooser;
import consulo.fileChooser.FileChooserDescriptor;
import consulo.fileChooser.FileChooserDescriptorFactory;
import consulo.find.FindInProjectSettings;
import consulo.find.FindModel;
import consulo.find.FindSettings;
import consulo.find.localize.FindLocalize;
import consulo.find.ui.ScopeChooserCombo;
import consulo.ide.impl.idea.openapi.actionSystem.impl.SimpleDataContext;
import consulo.language.psi.PsiBundle;
import consulo.language.util.ModuleUtilCore;
import consulo.localize.LocalizeValue;
import consulo.module.Module;
import consulo.module.ModuleManager;
import consulo.platform.Platform;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.ui.ComboBox;
import consulo.ui.ComboBoxStyle;
import consulo.ui.Component;
import consulo.ui.Label;
import consulo.ui.TextBoxWithHistory;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.*;
import consulo.util.lang.ObjectUtil;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;

import javax.swing.KeyStroke;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @author VISTALL
 * @since 2026-10-02
 */
class UnifiedFindPopupScopeUI {
    static final class ScopeType {
        final String name;
        final LocalizeValue text;

        ScopeType(String name, LocalizeValue text) {
            this.name = name;
            this.text = text;
        }
    }

    static final ScopeType PROJECT = new ScopeType("Project", FindLocalize.findPopupScopeProject());
    static final ScopeType MODULE = new ScopeType("Module", FindLocalize.findPopupScopeModule());
    static final ScopeType DIRECTORY = new ScopeType("Directory", FindLocalize.findPopupScopeDirectory());
    static final ScopeType SCOPE = new ScopeType("Scope", FindLocalize.findPopupScopeScope());

    private static final int DIRECTORY_COLUMNS = 40;

    private final FindUIHelper myHelper;
    private final Project myProject;
    private final UnifiedFindPopupPanel myPanel;

    private final Map<ScopeType, Component> myComponents = new LinkedHashMap<>();

    private final ComboBox<String> myModuleComboBox;
    private final TextBoxWithHistory myDirectoryField;
    private final ComboBox<ScopeDescriptor> myScopeComboBox;

    @RequiredUIAccess
    UnifiedFindPopupScopeUI(UnifiedFindPopupPanel panel) {
        myHelper = panel.getHelper();
        myProject = panel.getProject();
        myPanel = panel;

        Module[] modules = ModuleManager.getInstance(myProject).getModules();
        String[] names = new String[modules.length];
        for (int i = 0; i < modules.length; i++) {
            names[i] = modules[i].getName();
        }
        Arrays.sort(names, String.CASE_INSENSITIVE_ORDER);

        myModuleComboBox = ComboBox.create(names);
        myModuleComboBox.addStyle(ComboBoxStyle.INPLACE);
        myModuleComboBox.addValueListener(event -> myPanel.scheduleResultsUpdate());

        myDirectoryField = TextBoxWithHistory.create();
        myDirectoryField.setVisibleLength(DIRECTORY_COLUMNS);
        myDirectoryField.addValueListener(event -> myPanel.scheduleResultsUpdate());
        myDirectoryField.setSuffixComponent(createDirectoryToolbar().getUIComponent());

        myScopeComboBox = ComboBox.create(collectScopes());
        myScopeComboBox.addStyle(ComboBoxStyle.INPLACE);
        myScopeComboBox.setRender((presentation, item) -> {
            ScopeDescriptor descriptor = item.getValue();
            if (descriptor != null) {
                presentation.withIcon(descriptor.getIcon());
                presentation.append(StringUtil.notNullize(descriptor.getDisplayName()));
            }
        });
        selectScope(ObjectUtil.coalesce(
            myHelper.getModel().getCustomScope(),
            myHelper.getModel().getCustomScopeName(),
            FindSettings.getInstance().getDefaultScopeName()
        ));
        myScopeComboBox.addValueListener(event -> myPanel.scheduleResultsUpdate());

        myComponents.put(PROJECT, Label.create(LocalizeValue.empty()));
        myComponents.put(MODULE, myModuleComboBox);
        myComponents.put(DIRECTORY, myDirectoryField);
        myComponents.put(SCOPE, myScopeComboBox);
    }

    @RequiredUIAccess
    private ActionToolbar createDirectoryToolbar() {
        DumbAwareAction selectPathAction = DumbAwareAction.create("Select Path", PlatformIconGroup.nodesFolder(), e -> chooseDirectory());
        selectPathAction.registerCustomShortcutSet(
            new CustomShortcutSet(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.SHIFT_DOWN_MASK)),
            myDirectoryField
        );

        int mnemonicModifiers = Platform.current().os().isMac() ? InputEvent.ALT_DOWN_MASK | InputEvent.CTRL_DOWN_MASK : InputEvent.ALT_DOWN_MASK;
        MyRecursiveDirectoryAction recursiveDirectoryAction = new MyRecursiveDirectoryAction();
        recursiveDirectoryAction.registerCustomShortcutSet(
            new CustomShortcutSet(KeyStroke.getKeyStroke(KeyEvent.VK_Y, mnemonicModifiers)),
            myPanel.getUIComponent()
        );

        ActionToolbar toolbar = ActionToolbarFactory.getInstance().createActionToolbar(
            "FindPopupDirectoryBox",
            ActionGroup.newImmutableBuilder().add(selectPathAction).add(recursiveDirectoryAction).build(),
            ActionToolbar.Style.INPLACE
        );
        toolbar.setTargetUIComponent(myDirectoryField);
        toolbar.updateActionsAsync();
        return toolbar;
    }

    @RequiredUIAccess
    private void chooseDirectory() {
        FileChooserDescriptor descriptor = FileChooserDescriptorFactory.createSingleFolderDescriptor();

        String directory = getDirectory();
        VirtualFile toSelect = StringUtil.isEmptyOrSpaces(directory) ? null : LocalFileSystem.getInstance().findFileByPath(directory);

        myPanel.getCanClose().set(false);

        UIAccess uiAccess = UIAccess.current();
        FileChooser.chooseFile(descriptor, myProject, toSelect).whenCompleteAsync((file, error) -> {
            myPanel.getCanClose().set(true);

            if (file != null) {
                myHelper.getModel().setDirectoryName(file.getPresentableUrl());
                myDirectoryField.setValue(file.getPresentableUrl());
            }

            myDirectoryField.focus();
        }, uiAccess);
    }

    private List<ScopeDescriptor> collectScopes() {
        String moduleScopeName = PsiBundle.message("search.scope.module", "");
        int index = moduleScopeName.indexOf(' ');
        String moduleFilesScopeName = moduleScopeName.substring(0, index + 1);

        List<ScopeDescriptor> scopes = new ArrayList<>();
        ScopeChooserCombo.processScopes(
            myProject,
            SimpleDataContext.getProjectContext(myProject),
            ScopeChooserCombo.OPT_FROM_SELECTION | ScopeChooserCombo.OPT_USAGE_VIEW,
            descriptor -> {
                if (descriptor.getScope() == null) {
                    return true;
                }

                String displayName = descriptor.getDisplayName();
                if (displayName != null && displayName.startsWith(moduleFilesScopeName)) {
                    return true;
                }

                scopes.add(descriptor);
                return true;
            }
        );
        return scopes;
    }

    @RequiredUIAccess
    private void selectScope(@Nullable Object selection) {
        if (selection instanceof SearchScope scope) {
            myScopeComboBox.setValueByCondition(descriptor -> descriptor.scopeEquals(scope));
        }
        else if (selection instanceof String name) {
            myScopeComboBox.setValueByCondition(descriptor -> Objects.equals(descriptor.getDisplayName(), name));
        }

        if (myScopeComboBox.getValue() == null) {
            myScopeComboBox.selectFirst();
        }
    }

    Map<ScopeType, Component> getComponents() {
        return myComponents;
    }

    String getDirectory() {
        return StringUtil.notNullize(myDirectoryField.getValue());
    }

    void applyTo(FindSettings findSettings, ScopeType selectedScope) {
        ScopeDescriptor descriptor = myScopeComboBox.getValue();
        if (descriptor != null) {
            findSettings.setDefaultScopeName(descriptor.getDisplayName());
        }
    }

    void applyTo(FindModel findModel, ScopeType selectedScope) {
        if (selectedScope == PROJECT) {
            findModel.setProjectScope(true);
        }
        else if (selectedScope == DIRECTORY) {
            findModel.setDirectoryName(getDirectory());
        }
        else if (selectedScope == MODULE) {
            findModel.setModuleName(myModuleComboBox.getValue());
        }
        else if (selectedScope == SCOPE) {
            ScopeDescriptor descriptor = myScopeComboBox.getValue();
            SearchScope selectedCustomScope = descriptor == null ? null : descriptor.getScope();
            findModel.setCustomScopeName(selectedCustomScope == null ? null : selectedCustomScope.getDisplayName());
            findModel.setCustomScope(selectedCustomScope);
            findModel.setCustomScope(true);
        }
    }

    UnifiedFindPopupPanel.@Nullable Validation validate(FindModel model, @Nullable ScopeType selectedScope) {
        if (selectedScope == DIRECTORY && FindInProjectUtil.getDirectory(model) == null) {
            return new UnifiedFindPopupPanel.Validation(FindLocalize.findDirectoryNotFoundError(), myDirectoryField);
        }
        return null;
    }

    @RequiredUIAccess
    ScopeType initByModel(FindModel findModel) {
        initDirectories(findModel);

        String dirName = findModel.getDirectoryName();
        if (!StringUtil.isEmptyOrSpaces(dirName)) {
            VirtualFile dir = LocalFileSystem.getInstance().findFileByPath(dirName);
            if (dir != null) {
                Module module = ModuleUtilCore.findModuleForFile(dir, myProject);
                if (module != null) {
                    myModuleComboBox.setValue(module.getName());
                }
            }
        }

        ScopeType scope = getScope(findModel);
        if (scope == MODULE) {
            myModuleComboBox.setValue(findModel.getModuleName());
        }
        return scope;
    }

    @RequiredUIAccess
    private void initDirectories(FindModel findModel) {
        String directoryName = findModel.getDirectoryName();
        List<String> recent = new ArrayList<>(FindInProjectSettings.getInstance(myProject).getRecentDirectories());

        List<String> history = new ArrayList<>();
        if (!StringUtil.isEmpty(directoryName)) {
            recent.remove(directoryName);
            history.add(directoryName);
        }
        for (int i = recent.size() - 1; i >= 0; i--) {
            history.add(recent.get(i));
        }

        myDirectoryField.setHistory(history);
        myDirectoryField.setValue(history.isEmpty() ? "" : history.get(0));
    }

    private static ScopeType getScope(FindModel model) {
        if (model.isCustomScope()) {
            return SCOPE;
        }
        if (model.isProjectScope()) {
            return PROJECT;
        }
        if (model.getDirectoryName() != null) {
            return DIRECTORY;
        }
        if (model.getModuleName() != null) {
            return MODULE;
        }
        return PROJECT;
    }

    private class MyRecursiveDirectoryAction extends ToggleAction {
        MyRecursiveDirectoryAction() {
            super(FindLocalize.findScopeDirectoryRecursiveCheckbox(), LocalizeValue.localizeTODO("Recursively"), PlatformIconGroup.actionsShowastree());
        }

        @Override
        public boolean isSelected(AnActionEvent e) {
            return myHelper.getModel().isWithSubdirectories();
        }

        @Override
        @RequiredUIAccess
        public void setSelected(AnActionEvent e, boolean state) {
            myHelper.getModel().setWithSubdirectories(state);
            myPanel.scheduleResultsUpdate();
        }
    }
}
