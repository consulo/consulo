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
package consulo.ide.impl.idea.ide.navigationToolbar;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.dumb.DumbAware;
import consulo.application.ui.UISettings;
import consulo.dataContext.DataManager;
import consulo.ide.impl.idea.ide.StandardTargetWeights;
import consulo.ide.impl.idea.ide.impl.SelectInTargetPsiWrapper;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFileSystemItem;
import consulo.project.Project;
import consulo.project.ui.view.SelectInManager;
import consulo.project.ui.wm.IdeFrame;
import consulo.project.ui.wm.IdeRootPaneNorthExtension;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.inject.Inject;

import javax.swing.*;

/**
 * @author Anna Kozlova
 * @author Konstantin Bulenkov
 */
@ExtensionImpl
public class SelectInNavBarTarget extends SelectInTargetPsiWrapper implements DumbAware {
  public static final String NAV_BAR_ID = "NavBar";

  @Inject
  public SelectInNavBarTarget(final Project project) {
    super(project);
  }

  @Override
  public String getToolWindowId() {
    return NAV_BAR_ID;
  }

  @Override
  protected boolean canSelect(final PsiFileSystemItem file) {
    return UISettings.getInstance().SHOW_NAVIGATION_BAR;
  }

  @Override
  protected void select(final Object selector, final VirtualFile virtualFile, final boolean requestFocus) {
    selectInNavBar();
  }

  @Override
  protected void select(final PsiElement element, boolean requestFocus) {
    selectInNavBar();
  }

  private static void selectInNavBar() {
    DataManager.getInstance().getDataContextFromFocus().thenAccept(context -> {
        final IdeFrame frame = context.getData(IdeFrame.KEY);
        if (frame != null) {
          final IdeRootPaneNorthExtension navBarExt = frame.getNorthExtension(NavBarRootPaneExtension.NAV_BAR);
          if (navBarExt != null) {
            final JComponent c = navBarExt.getComponent();
            final NavBarPanel panel = (NavBarPanel)c.getClientProperty("NavBarPanel");
            panel.rebuildAndSelectTail(true);
          }
        }
      });
  }

  @Override
  public float getWeight() {
    return StandardTargetWeights.NAV_BAR_WEIGHT;
  }

  @Override
  public String getMinorViewId() {
    return null;
  }

  @Override
  protected boolean canWorkWithCustomObjects() {
    return false;
  }

  public String toString() {
    return SelectInManager.NAV_BAR;
  }
}
