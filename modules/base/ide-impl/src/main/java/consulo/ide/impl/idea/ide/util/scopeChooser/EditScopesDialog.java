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

package consulo.ide.impl.idea.ide.util.scopeChooser;

import consulo.ide.IdeBundle;
import consulo.ide.impl.idea.openapi.options.ex.SingleConfigurableEditor;
import consulo.project.Project;
import consulo.ui.ex.awt.DialogWrapper;
import consulo.ui.ex.InputValidator;
import consulo.ui.ex.awt.MasterDetailsStateService;
import consulo.ui.ex.awt.Messages;
import consulo.language.editor.packageDependency.DependencyValidationManager;
import consulo.content.scope.NamedScope;
import consulo.content.scope.PackageSet;
import consulo.annotation.DeprecationInfo;
import consulo.ui.annotation.RequiredUIAccess;

import jakarta.annotation.Nullable;

/**
 * @author anna
 * @since 2006-07-03
 */
public class EditScopesDialog extends SingleConfigurableEditor {
  private NamedScope mySelectedScope;
  private final boolean myCheckShared;

  public EditScopesDialog(Project project, ScopeChooserConfigurable configurable, boolean checkShared) {
    super(project, configurable, "scopes");
    myCheckShared = checkShared;
  }

  @RequiredUIAccess
  @Override
  protected void doOKAction() {
    Object selectedObject = ((ScopeChooserConfigurable)getConfigurable()).getSelectedObject();
    if (selectedObject instanceof NamedScope) {
      mySelectedScope = (NamedScope)selectedObject;
    }
    super.doOKAction();
    if (myCheckShared && mySelectedScope != null) {
      Project project = getProject();
      final DependencyValidationManager manager = DependencyValidationManager.getInstance(project);
      NamedScope scope = manager.getScope(mySelectedScope.getName());
      if (scope == null) {
        if (Messages.showYesNoDialog(IdeBundle.message("scope.unable.to.save.scope.message"), IdeBundle.message("scope.unable.to.save.scope.title"), Messages.getErrorIcon()) ==
            DialogWrapper.OK_EXIT_CODE) {
          String newName = Messages.showInputDialog(project, IdeBundle.message("add.scope.name.label"), IdeBundle.message("scopes.save.dialog.title.shared"), Messages.getQuestionIcon(),
                                                          mySelectedScope.getName(), new InputValidator() {
                    @Override
                    public boolean checkInput(String inputString) {
                      return inputString != null && inputString.length() > 0 && manager.getScope(inputString) == null;
                    }

                    @Override
                    public boolean canClose(String inputString) {
                      return checkInput(inputString);
                    }
                  });
          if (newName != null) {
            PackageSet packageSet = mySelectedScope.getValue();
            scope = new NamedScope(newName, packageSet != null ? packageSet.createCopy() : null);
            mySelectedScope = scope;
            manager.addScope(mySelectedScope);
          }
        }
      }
    }
  }

  @Deprecated
  @DeprecationInfo("Use ShowSettingsUtil")
  public static EditScopesDialog showDialog(Project project, @Nullable String scopeToSelect) {
    return showDialog(project, scopeToSelect, false);
  }

  @Deprecated
  @DeprecationInfo("Use ShowSettingsUtil")

  public static EditScopesDialog showDialog(Project project, @Nullable String scopeToSelect, boolean checkShared) {
    ScopeChooserConfigurable configurable = new ScopeChooserConfigurable(project, () -> MasterDetailsStateService.getInstance(project));
    EditScopesDialog dialog = new EditScopesDialog(project, configurable, checkShared);
    if (scopeToSelect != null) {
      configurable.selectNodeInTree(scopeToSelect);
    }
    dialog.show();
    return dialog;
  }

  public NamedScope getSelectedScope() {
    return mySelectedScope;
  }
}
