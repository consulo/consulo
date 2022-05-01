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

import consulo.language.editor.CommonDataKeys;
import consulo.language.editor.LangDataKeys;
import consulo.language.editor.scope.AnalysisScope;
import consulo.language.editor.scope.AnalysisScopeBundle;
import consulo.dataContext.DataContext;
import consulo.module.Module;
import consulo.module.ModuleManager;
import consulo.ide.impl.idea.openapi.module.ModuleUtil;
import consulo.project.Project;
import consulo.ui.ex.awt.DialogWrapper;
import consulo.ide.impl.idea.openapi.util.text.StringUtil;
import consulo.language.psi.PsiElement;
import consulo.ui.ex.content.Content;
import consulo.ui.ex.content.ContentFactory;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;

import javax.swing.*;
import java.awt.*;

/**
 * User: anna
 * Date: Feb 9, 2005
 */
public class ShowModuleDependenciesAction extends AnAction {
  @Override
  public void actionPerformed(AnActionEvent e) {
    final DataContext dataContext = e.getDataContext();
    final Project project = dataContext.getData(CommonDataKeys.PROJECT);
    if (project == null){
      return;
    }
    ModulesDependenciesPanel panel;
    AnalysisScope scope = new AnalysisScope(project);
    final Module[] modules = dataContext.getData(LangDataKeys.MODULE_CONTEXT_ARRAY);
    if (modules != null){
      panel = new ModulesDependenciesPanel(project, modules);
      scope = new AnalysisScope(modules);
    } else {
      final PsiElement element = dataContext.getData(LangDataKeys.PSI_FILE);
      final Module module = element != null ? ModuleUtil.findModuleForPsiElement(element) : null;
      if (module != null && ModuleManager.getInstance(project).getModules().length > 1){
        MyModuleOrProjectScope dlg = new MyModuleOrProjectScope(module.getName());
        dlg.show();
        if (dlg.isOK()){
          if (!dlg.useProjectScope()){
            panel = new ModulesDependenciesPanel(project, new Module[]{module});
            scope = new AnalysisScope(module);
          } else {
            panel = new ModulesDependenciesPanel(project);
          }
        } else {
          return;
        }
      } else {
        panel = new ModulesDependenciesPanel(project);
      }
    }

    Content content = ContentFactory.getInstance().createContent(panel,
                                                                 AnalysisScopeBundle.message(
                                                                                    "module.dependencies.toolwindow.title",
                                                                                    StringUtil.capitalize(scope.getDisplayName())),
                                                                 false);
    content.setDisposer(panel);
    panel.setContent(content);
    DependenciesAnalyzeManager.getInstance(project).addContent(content);
  }

  @Override
  public void update(AnActionEvent e) {
    final DataContext dataContext = e.getDataContext();
    final Project project = dataContext.getData(CommonDataKeys.PROJECT);
    e.getPresentation().setEnabled(project != null);
  }

  private static class MyModuleOrProjectScope extends DialogWrapper{
    private final JRadioButton myProjectScope;
    private final JRadioButton myModuleScope;
    protected MyModuleOrProjectScope(String moduleName) {
      super(false);
      setTitle(AnalysisScopeBundle.message("module.dependencies.scope.dialog.title"));
      ButtonGroup group = new ButtonGroup();
      myProjectScope = new JRadioButton(AnalysisScopeBundle.message("module.dependencies.scope.dialog.project.button"));
      myModuleScope = new JRadioButton(AnalysisScopeBundle.message("module.dependencies.scope.dialog.module.button", moduleName));
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

    public boolean useProjectScope(){
      return myProjectScope.isSelected();
    }
  }
}
