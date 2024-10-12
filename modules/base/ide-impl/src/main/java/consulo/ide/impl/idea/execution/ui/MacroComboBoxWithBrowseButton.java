/*
 * Copyright 2000-2017 JetBrains s.r.o.
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
package consulo.ide.impl.idea.execution.ui;

import consulo.fileChooser.FileChooserDescriptor;
import consulo.fileChooser.FileChooserFactory;
import consulo.language.editor.LangDataKeys;
import consulo.localize.LocalizeValue;
import consulo.module.Module;
import consulo.module.macro.ModulePathMacroManager;
import consulo.project.Project;
import consulo.project.macro.ProjectPathMacroManager;
import consulo.ui.ex.awt.ComboBox;
import consulo.ui.ex.awt.ComponentWithBrowseButton;
import consulo.ui.ex.awt.TextAccessor;
import consulo.ui.ex.awt.TextComponentAccessor;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;

public class MacroComboBoxWithBrowseButton extends ComponentWithBrowseButton<ComboBox<String>> implements TextAccessor {
    private Module module;
    private boolean always;

    public MacroComboBoxWithBrowseButton(FileChooserDescriptor descriptor, Project project) {
        super(new ComboBox<>(new MacroComboBoxModel()), null);

        ComboBox<String> combobox = getChildComponent();
        combobox.setEditable(true);
        descriptor.withShowHiddenFiles(true);
        addActionListener(new BrowseFolderActionListener<ComboBox<String>>(
            LocalizeValue.empty(),
            LocalizeValue.empty(),
            this,
            project,
            descriptor,
            accessor
        ) {
            private Module getModule() {
                Module module = MacroComboBoxWithBrowseButton.this.module;
                if (module == null) {
                    module = myFileChooserDescriptor.getUserData(LangDataKeys.MODULE_CONTEXT);
                }
                if (module == null) {
                    module = myFileChooserDescriptor.getUserData(Module.KEY);
                }
                return module;
            }

            @Nullable
            @Override
            protected Project getProject() {
                Project project = (Project)super.getProject();
                if (project != null) {
                    return project;
                }
                Module module = getModule();
                return module == null ? null : module.getProject();
            }

            @Nonnull
            @Override
            protected String expandPath(@Nonnull String path) {
                Project project = getProject();
                if (project != null) {
                    path = ProjectPathMacroManager.getInstance(project).expandPath(path);
                }

                Module module = getModule();
                if (module != null) {
                    path = ModulePathMacroManager.getInstance(module).expandPath(path);
                }

                return super.expandPath(path);
            }
        });
        ComboBoxEditor editor = combobox.getEditor();
        if (editor != null && editor.getEditorComponent() instanceof JTextField textField) {
            FileChooserFactory.getInstance().installFileCompletion(textField, descriptor, true, null);
        }
    }

    private MacroComboBoxModel getModel() {
        return getChildComponent().getModel() instanceof MacroComboBoxModel macroComboBoxModel ? macroComboBoxModel : null;
    }

    @Override
    public String getText() {
        return accessor.getText(getChildComponent());
    }

    @Override
    public void setText(String text) {
        accessor.setText(getChildComponent(), text != null ? text : "");
    }

    public void setModule(Module module) {
        this.module = module;
        configure();
    }

    public void showModuleMacroAlways() {
        always = true;
        configure();
    }

    private void configure() {
        MacroComboBoxModel model = getModel();
        if (model != null) {
            model.useModuleDir(always || module != null);
        }
    }

    private final TextComponentAccessor<ComboBox<String>> accessor = new TextComponentAccessor<>() {
        @Override
        public String getText(ComboBox<String> component) {
            Object item = component == null ? null : component.getSelectedItem();
            return item == null ? "" : item.toString();
        }

        @Override
        public void setText(ComboBox<String> component, @Nonnull String text) {
            if (component != null) {
                component.setSelectedItem(text);
            }
        }
    };
}
