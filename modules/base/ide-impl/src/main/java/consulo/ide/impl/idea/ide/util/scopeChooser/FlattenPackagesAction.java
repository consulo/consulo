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

/*
 * User: anna
 * Date: 16-Jan-2008
 */
package consulo.ide.impl.idea.ide.util.scopeChooser;

import consulo.application.AllIcons;
import consulo.ide.IdeBundle;
import consulo.ui.ex.action.AnActionEvent;
import consulo.language.editor.CommonDataKeys;
import consulo.ui.ex.action.ToggleAction;
import consulo.project.Project;
import consulo.ide.impl.idea.packageDependencies.DependencyUISettings;
import consulo.language.psi.PsiPackageSupportProviders;
import consulo.ui.annotation.RequiredUIAccess;

public final class FlattenPackagesAction extends ToggleAction {
  private final Runnable myUpdate;

  public FlattenPackagesAction(Runnable update) {
    super(IdeBundle.message("action.flatten.packages"),
          IdeBundle.message("action.flatten.packages"), AllIcons.ObjectBrowser.FlattenPackages);
    myUpdate = update;
  }

  @Override
  @RequiredUIAccess
  public void update(AnActionEvent e) {
    super.update(e);
    Project project = e.getData(CommonDataKeys.PROJECT);
    if(project == null) {
      return;
    }
    if(!PsiPackageSupportProviders.isPackageSupported(project)) {
      e.getPresentation().setVisible(false);
    }
  }

  @Override
  public boolean isSelected(AnActionEvent event) {
    return DependencyUISettings.getInstance().UI_FLATTEN_PACKAGES;
  }

  @Override
  public void setSelected(AnActionEvent event, boolean flag) {
    DependencyUISettings.getInstance().UI_FLATTEN_PACKAGES = flag;
    myUpdate.run();
  }
}
