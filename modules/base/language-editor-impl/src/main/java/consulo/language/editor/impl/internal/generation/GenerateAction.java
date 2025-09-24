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
package consulo.language.editor.impl.internal.generation;

import consulo.annotation.component.ActionImpl;
import consulo.codeEditor.Editor;
import consulo.dataContext.DataContext;
import consulo.language.editor.generation.GenerateActionPopupTemplateInjector;
import consulo.language.editor.localize.CodeInsightLocalize;
import consulo.platform.base.localize.ActionLocalize;
import consulo.project.DumbService;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.*;
import consulo.ui.ex.action.util.ActionGroupUtil;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.ui.ex.popup.ListPopup;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

@ActionImpl(id = "Generate")
public class GenerateAction extends DumbAwareAction {
    public GenerateAction() {
        super(ActionLocalize.actionGenerateText(), ActionLocalize.actionGenerateDescription());
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(@Nonnull AnActionEvent e) {
        DataContext dataContext = e.getDataContext();

        Project project = e.getRequiredData(Project.KEY);
        ListPopup popup = JBPopupFactory.getInstance().createActionGroupPopup(
            CodeInsightLocalize.generateListPopupTitle().get(),
            wrapGroup(getGroup(), dataContext, project),
            dataContext,
            JBPopupFactory.ActionSelectionAid.SPEEDSEARCH,
            false
        );
        popup.pack(true, true);

        popup.showInBestPositionFor(dataContext);
    }

    @Override
    public int getExecuteWeight() {
        return 1_000_000;
    }

    @Override
    public void update(@Nonnull AnActionEvent e) {
        if (ActionPlaces.isPopupPlace(e.getPlace())) {
            e.getPresentation().setEnabledAndVisible(isEnabled(e));
        }
        else {
            e.getPresentation().setEnabled(isEnabled(e));
        }
    }

    private static boolean isEnabled(@Nonnull AnActionEvent e) {
        return e.hasData(Project.KEY) && e.hasData(Editor.KEY) && !ActionGroupUtil.isGroupEmpty(getGroup(), e);
    }

    private static DefaultActionGroup getGroup() {
        return (DefaultActionGroup) ActionManager.getInstance().getAction(IdeActions.GROUP_GENERATE);
    }

    private static ActionGroup wrapGroup(ActionGroup actionGroup, DataContext dataContext, @Nonnull Project project) {
        boolean dumbMode = DumbService.isDumb(project);
        ActionGroup.Builder copy = ActionGroup.newImmutableBuilder();
        for (AnAction action : actionGroup.getChildren(null)) {
            if (dumbMode && !action.isDumbAware()) {
                continue;
            }

            if (action instanceof GenerateActionPopupTemplateInjector templateInjector) {
                AnAction editTemplateAction = templateInjector.createEditTemplateAction(dataContext);
                if (editTemplateAction != null) {
                    copy.add(new GenerateWrappingGroup(action, editTemplateAction));
                    continue;
                }
            }
            if (action instanceof ActionGroup g) {
                copy.add(wrapGroup(g, dataContext, project));
            }
            else {
                copy.add(action);
            }
        }
        return copy.build();
    }

    private static class GenerateWrappingGroup extends ActionGroup {
        private final AnAction myAction;
        private final AnAction myEditTemplateAction;

        public GenerateWrappingGroup(AnAction action, AnAction editTemplateAction) {
            myAction = action;
            myEditTemplateAction = editTemplateAction;
            copyFrom(action);
            setPopup(true);
        }

        @Override
        public boolean canBePerformed(@Nonnull DataContext context) {
            return true;
        }

        @Nonnull
        @Override
        public AnAction[] getChildren(@Nullable AnActionEvent e) {
            return new AnAction[]{myEditTemplateAction};
        }

        @Override
        @RequiredUIAccess
        public void actionPerformed(@Nonnull AnActionEvent e) {
            Project project = e.getRequiredData(Project.KEY);
            DumbService dumbService = DumbService.getInstance(project);
            try {
                dumbService.setAlternativeResolveEnabled(true);
                myAction.actionPerformed(e);
            }
            finally {
                dumbService.setAlternativeResolveEnabled(false);
            }
        }
    }
}