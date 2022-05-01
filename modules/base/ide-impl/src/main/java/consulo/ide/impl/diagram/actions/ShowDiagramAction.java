/*
 * Copyright 2013-2016 consulo.io
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
package consulo.ide.impl.diagram.actions;

import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.language.editor.CommonDataKeys;
import consulo.language.psi.PsiElement;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ide.impl.diagram.provider.GraphProvider;
import consulo.application.eap.EarlyAccessProgramDescriptor;
import consulo.application.eap.EarlyAccessProgramManager;
import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 22:29/15.10.13
 */
public class ShowDiagramAction extends AnAction {
  public static class DiagramSupport extends EarlyAccessProgramDescriptor {
    @Nonnull
    @Override
    public String getName() {
      return "Diagram Support";
    }

    @Override
    public boolean isAvailable() {
      return false;
    }

    @Nonnull
    @Override
    public String getDescription() {
      return "";
    }
  }

  @RequiredUIAccess
  @Override
  public void actionPerformed(@Nonnull AnActionEvent e) {
  }

  @RequiredUIAccess
  @Override
  public void update(@Nonnull AnActionEvent e) {
    boolean state = EarlyAccessProgramManager.is(DiagramSupport.class);
    if (state) {
      PsiElement psiElement = e.getData(CommonDataKeys.PSI_ELEMENT);
      state = false;
      if (psiElement != null) {
        state = false;
        for (GraphProvider graphProvider : GraphProvider.EP_NAME.getExtensionList()) {
          if (graphProvider.isSupported(psiElement)) {
            state = true;
            break;
          }
        }
      }
    }
    e.getPresentation().setEnabledAndVisible(state);
  }
}
