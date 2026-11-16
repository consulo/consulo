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
package consulo.ide.impl.idea.find.actions;

import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorPopupHelper;
import consulo.language.psi.PsiElement;
import consulo.localize.LocalizeValue;
import consulo.project.DumbService;
import consulo.project.Project;
import consulo.ui.RelativePoint2D;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionManager;
import consulo.usage.PsiElementUsageTarget;
import consulo.usage.ShowUsagesManager;
import consulo.usage.UsageTarget;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

/**
 * Opens the popup of {@link ShowUsagesAction}, or of {@link UnifiedShowUsagesPopup} outside of the desktop frontend.
 *
 * @author VISTALL
 * @since 2026-11-16
 */
@Singleton
@ServiceImpl
public class ShowUsagesManagerImpl implements ShowUsagesManager {
    private final Project myProject;
    private final Application myApplication;

    @Inject
    public ShowUsagesManagerImpl(Project project, Application application) {
        myProject = project;
        myApplication = application;
    }

    @Override
    @RequiredUIAccess
    public void startFindUsages(Editor editor, PsiElement element, @Nullable RelativePoint2D point) {
        DumbService dumbService = DumbService.getInstance(myProject);
        if (dumbService.isDumb()) {
            dumbService.showDumbModeNotification(LocalizeValue.localizeTODO("Usage search is not available until indices are ready"));
        }
        else {
            RelativePoint2D popupPosition = point != null ? point : EditorPopupHelper.getInstance().guessBestPopupLocation(editor);
            startFindUsages(element, popupPosition, editor);
        }
    }

    @Override
    @RequiredUIAccess
    public void startFindUsages(PsiElement element, RelativePoint2D popupPosition, @Nullable Editor editor) {
        if (!myApplication.isSwingApplication()) {
            if (editor != null) {
                UnifiedShowUsagesPopup.show(editor, myProject, element, popupPosition);
            }
            return;
        }

        ShowUsagesAction action = (ShowUsagesAction) ActionManager.getInstance().getAction(ShowUsagesAction.ID);
        action.startFindUsages(element, popupPosition, editor, ShowUsagesAction.getUsagesPageSize());
    }

    @Override
    @RequiredUIAccess
    public boolean showUsages(UsageTarget target, RelativePoint2D popupPosition, @Nullable Editor editor) {
        if (!(target instanceof PsiElementUsageTarget elementUsageTarget)) {
            return false;
        }
        PsiElement element = elementUsageTarget.getElement();
        if (element == null) {
            return false;
        }
        startFindUsages(element, popupPosition, editor);
        return true;
    }
}
