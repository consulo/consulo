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

package consulo.ide.impl.idea.ide.actions;

import consulo.application.dumb.DumbAware;
import consulo.dataContext.DataContext;
import consulo.ide.IdeView;
import consulo.ide.impl.idea.openapi.actionSystem.PopupAction;
import consulo.ide.localize.IdeLocalize;
import consulo.language.editor.LangDataKeys;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.*;
import consulo.ui.ex.action.util.ActionGroupUtil;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.ui.ex.popup.ListPopup;
import consulo.util.lang.function.Condition;
import jakarta.annotation.Nullable;

/**
 * @author Konstantin Bulenkov
 */
@SuppressWarnings({"MethodMayBeStatic"})
public class NewElementAction extends AnAction implements DumbAware, PopupAction {
    @Override
    @RequiredUIAccess
    public void actionPerformed(AnActionEvent event) {
        showPopup(event.getDataContext());
    }

    protected void showPopup(DataContext context) {
        createPopup(context).showInBestPositionFor(context);
    }

    protected ListPopup createPopup(DataContext dataContext) {
        return JBPopupFactory.getInstance().createActionGroupPopup(
            getPopupTitle(),
            getGroup(dataContext),
            dataContext,
            isShowNumbers(),
            isShowDisabledActions(),
            isHonorActionMnemonics(),
            getDisposeCallback(),
            getMaxRowCount(),
            getPreselectActionCondition(dataContext)
        );
    }

    protected int getMaxRowCount() {
        return -1;
    }

    @Nullable
    protected Condition<AnAction> getPreselectActionCondition(DataContext dataContext) {
        return dataContext.getData(LangDataKeys.PRESELECT_NEW_ACTION_CONDITION);
    }

    @Nullable
    protected Runnable getDisposeCallback() {
        return null;
    }

    protected boolean isHonorActionMnemonics() {
        return false;
    }

    protected boolean isShowDisabledActions() {
        return false;
    }

    protected boolean isShowNumbers() {
        return false;
    }

    protected String getPopupTitle() {
        return IdeLocalize.titlePopupNewElement().get();
    }

    @Override
    @RequiredUIAccess
    public void update(AnActionEvent e) {
        Presentation presentation = e.getPresentation();
        DataContext context = e.getDataContext();
        Project project = context.getData(Project.KEY);
        if (project == null) {
            presentation.setEnabled(false);
            return;
        }
        if (Boolean.TRUE.equals(context.getData(LangDataKeys.NO_NEW_ACTION))) {
            presentation.setEnabled(false);
            return;
        }
        IdeView ideView = context.getData(IdeView.KEY);
        if (ideView == null) {
            presentation.setEnabled(false);
            return;
        }

        presentation.setEnabled(!ActionGroupUtil.isGroupEmpty(getGroup(context), e));
    }

    protected ActionGroup getGroup(DataContext dataContext) {
        return (ActionGroup)ActionManager.getInstance().getAction(IdeActions.GROUP_WEIGHING_NEW);
    }
}
