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
import consulo.language.psi.PsiElement;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ide.impl.diagram.provider.GraphProvider;
import consulo.application.eap.EarlyAccessProgramManager;
import jakarta.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 2013-10-15
 */
public class ShowDiagramAction extends AnAction {
    @RequiredUIAccess
    @Override
    public void actionPerformed(@Nonnull AnActionEvent e) {
    }

    @RequiredUIAccess
    @Override
    public void update(@Nonnull AnActionEvent e) {
        boolean state = EarlyAccessProgramManager.is(DiagramSupportEapDescriptor.class);
        if (state) {
            PsiElement psiElement = e.getData(PsiElement.KEY);
            state = false;
            if (psiElement != null) {
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
