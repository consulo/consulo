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

import consulo.annotation.component.ActionImpl;
import consulo.application.dumb.DumbAware;
import consulo.dataContext.DataContext;
import consulo.language.editor.util.IdeView;
import consulo.ide.impl.idea.openapi.actionSystem.PopupAction;
import consulo.ide.localize.IdeLocalize;
import consulo.language.editor.LangDataKeys;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.*;
import consulo.ui.ex.action.util.ActionGroupUtil;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.ui.ex.popup.ListPopup;
import consulo.ui.image.Image;
import org.jspecify.annotations.Nullable;
import jakarta.inject.Inject;

import java.util.function.Predicate;

/**
 * @author Konstantin Bulenkov
 */
@ActionImpl(id = "NewElement")
public class NewElementAction extends AnAction implements DumbAware, PopupAction {
    private final ActionManager myActionManager;

    @Inject
    public NewElementAction(ActionManager actionManager) {
        myActionManager = actionManager;
    }

    public NewElementAction(
        ActionManager actionManager,
        LocalizeValue text,
        LocalizeValue description,
        @Nullable Image icon
    ) {
        super(text, description, icon);
        myActionManager = actionManager;
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(AnActionEvent event) {
        showPopup(event);
    }

    protected void showPopup(AnActionEvent event) {
        DataContext context = event.getDataContext();
        createPopup(context).showInBestPositionFor(context);
    }

    
    protected ListPopup createPopup(DataContext dataContext) {
        return JBPopupFactory.getInstance().createActionGroupPopup(
            getPopupTitle().get(),
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

    protected @Nullable Predicate<AnAction> getPreselectActionCondition(DataContext dataContext) {
        return dataContext.getData(LangDataKeys.PRESELECT_NEW_ACTION_CONDITION);
    }

    protected @Nullable Runnable getDisposeCallback() {
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

    
    protected LocalizeValue getPopupTitle() {
        return IdeLocalize.titlePopupNewElement();
    }

    @Override
    @RequiredUIAccess
    public void update(AnActionEvent e) {
        Presentation presentation = e.getPresentation();
        Project project = e.getData(Project.KEY);
        IdeView ideView = e.getData(IdeView.KEY);

        if (project == null || ideView == null || !isEnabled(e, ideView) || Boolean.TRUE.equals(e.getData(LangDataKeys.NO_NEW_ACTION))) {
            presentation.setEnabled(false);
            return;
        }

        presentation.setEnabled(!ActionGroupUtil.isGroupEmpty(getGroup(e.getDataContext()), e));
    }

    protected boolean isEnabled(AnActionEvent e, IdeView ideView) {
        return true;
    }

    protected ActionGroup getGroup(DataContext dataContext) {
        return (ActionGroup) myActionManager.getAction(IdeActions.GROUP_WEIGHING_NEW);
    }
}
