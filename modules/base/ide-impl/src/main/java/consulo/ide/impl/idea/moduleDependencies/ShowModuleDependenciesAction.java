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
package consulo.ide.impl.idea.moduleDependencies;

import consulo.annotation.component.ActionImpl;
import consulo.language.editor.LangDataKeys;
import consulo.language.editor.scope.AnalysisScope;
import consulo.language.editor.scope.localize.AnalysisScopeLocalize;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.module.Module;
import consulo.module.ModuleManager;
import consulo.platform.base.localize.ActionLocalize;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.awt.DialogWrapper;
import consulo.ui.ex.content.Content;
import consulo.ui.ex.content.ContentFactory;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nonnull;

import javax.swing.*;
import java.awt.*;

/**
 * @author anna
 * @since 2005-02-09
 */
@ActionImpl(id = "ShowModulesDependencies")
public class ShowModuleDependenciesAction extends AnAction {
    public ShowModuleDependenciesAction() {
        super(ActionLocalize.actionShowmodulesdependenciesText(), ActionLocalize.actionShowmodulesdependenciesDescription());
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(@Nonnull AnActionEvent e) {
        Project project = e.getRequiredData(Project.KEY);
        ModulesDependenciesPanel panel;
        AnalysisScope scope = new AnalysisScope(project);
        Module[] modules = e.getData(LangDataKeys.MODULE_CONTEXT_ARRAY);
        if (modules != null) {
            panel = new ModulesDependenciesPanel(project, modules);
            scope = new AnalysisScope(modules);
        }
        else {
            PsiElement element = e.getData(PsiFile.KEY);
            Module module = element != null ? element.getModule() : null;
            if (module != null && ModuleManager.getInstance(project).getModules().length > 1) {
                MyModuleOrProjectScope dlg = new MyModuleOrProjectScope(module.getName());
                dlg.show();
                if (dlg.isOK()) {
                    if (!dlg.useProjectScope()) {
                        panel = new ModulesDependenciesPanel(project, new Module[]{module});
                        scope = new AnalysisScope(module);
                    }
                    else {
                        panel = new ModulesDependenciesPanel(project);
                    }
                }
                else {
                    return;
                }
            }
            else {
                panel = new ModulesDependenciesPanel(project);
            }
        }

        Content content = ContentFactory.getInstance().createContent(
            panel,
            AnalysisScopeLocalize.moduleDependenciesToolwindowTitle(StringUtil.capitalize(scope.getDisplayName())).get(),
            false
        );
        content.setDisposer(panel);
        panel.setContent(content);
        DependenciesAnalyzeManager.getInstance(project).addContent(content);
    }

    @Override
    public void update(@Nonnull AnActionEvent e) {
        e.getPresentation().setEnabled(e.hasData(Project.KEY));
    }

    private static class MyModuleOrProjectScope extends DialogWrapper {
        private final JRadioButton myProjectScope;
        private final JRadioButton myModuleScope;

        protected MyModuleOrProjectScope(String moduleName) {
            super(false);
            setTitle(AnalysisScopeLocalize.moduleDependenciesScopeDialogTitle());
            ButtonGroup group = new ButtonGroup();
            myProjectScope = new JRadioButton(AnalysisScopeLocalize.moduleDependenciesScopeDialogProjectButton().get());
            myModuleScope = new JRadioButton(AnalysisScopeLocalize.moduleDependenciesScopeDialogModuleButton(moduleName).get());
            group.add(myProjectScope);
            group.add(myModuleScope);
            myProjectScope.setSelected(true);
            init();
        }

        @Override
        protected JComponent createCenterPanel() {
            JPanel panel = new JPanel(new GridLayout(2, 1));
            panel.add(myProjectScope);
            panel.add(myModuleScope);
            return panel;
        }

        public boolean useProjectScope() {
            return myProjectScope.isSelected();
        }
    }
}
