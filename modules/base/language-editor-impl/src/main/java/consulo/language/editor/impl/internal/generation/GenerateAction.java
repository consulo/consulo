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
import consulo.util.concurrent.coroutine.Coroutine;
import consulo.util.concurrent.coroutine.step.CodeExecution;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

@ActionImpl(id = "Generate")
public class GenerateAction extends DumbAwareAction implements AnActionWithAsyncUpdate {
    public GenerateAction() {
        super(ActionLocalize.actionGenerateText(), ActionLocalize.actionGenerateDescription());
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(AnActionEvent e) {
        DataContext dataContext = e.getDataContext();

        Project project = e.getRequiredData(Project.KEY);
        ListPopup popup = JBPopupFactory.getInstance().createActionGroupPopup(
            CodeInsightLocalize.generateListPopupTitle().get(),
            new GenerateWrapperGroup(getGroup()),
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
    public Coroutine<?, ?> updateAsync(AnActionEvent e) {
        if (!e.hasData(Project.KEY) || !e.hasData(Editor.KEY)) {
            return Coroutine.first(CodeExecution.run(() -> setEnabled(e, false)));
        }
        return ActionGroupUtil.isGroupEmptyAsync(getGroup(), e)
            .then(CodeExecution.consume(empty -> setEnabled(e, !empty)));
    }

    private static void setEnabled(AnActionEvent e, boolean enabled) {
        if (ActionPlaces.isPopupPlace(e.getPlace())) {
            e.getPresentation().setEnabledAndVisible(enabled);
        }
        else {
            e.getPresentation().setEnabled(enabled);
        }
    }

    private static DefaultActionGroup getGroup() {
        return (DefaultActionGroup) ActionManager.getInstance().getAction(IdeActions.GROUP_GENERATE);
    }

    private static class GenerateWrapperGroup extends ActionGroup {
        private final ActionGroup myOriginal;

        GenerateWrapperGroup(ActionGroup original) {
            myOriginal = original;
        }

        @Override
        public AnAction[] getChildren(@Nullable AnActionEvent e) {
            return myOriginal.getChildren(e);
        }

        @Override
        public Coroutine<?, List<AnAction>> getChildrenAsync(@Nullable AnActionEvent e) {
            Project project = e == null ? null : e.getData(Project.KEY);
            DataContext dataContext = e == null ? null : e.getDataContext();
            boolean dumbMode = project != null && DumbService.isDumb(project);
            return myOriginal.getChildrenAsync(e)
                .then(CodeExecution.<List<AnAction>, List<AnAction>>apply(children -> wrapChildren(children, dumbMode, dataContext)));
        }

        private static List<AnAction> wrapChildren(List<AnAction> children, boolean dumbMode, @Nullable DataContext dataContext) {
            List<AnAction> result = new ArrayList<>(children.size());
            for (AnAction action : children) {
                if (dumbMode && !action.isDumbAware()) {
                    continue;
                }
                if (action instanceof GenerateActionPopupTemplateInjector templateInjector) {
                    AnAction editTemplateAction =
                        dataContext == null ? null : templateInjector.createEditTemplateAction(dataContext);
                    if (editTemplateAction != null) {
                        result.add(new GenerateWrappingGroup(action, editTemplateAction));
                        continue;
                    }
                }
                result.add(action instanceof ActionGroup g ? new GenerateWrapperGroup(g) : action);
            }
            return result;
        }
    }

    private static class GenerateWrappingGroup extends ActionGroup {
        private final AnAction myAction;
        private final AnAction myEditTemplateAction;

        public GenerateWrappingGroup(AnAction action, AnAction editTemplateAction) {
            myAction = action;
            myEditTemplateAction = editTemplateAction;
            copyFrom(action);

            getTemplatePresentation().setPopupGroup(true);
            getTemplatePresentation().setPerformGroup(true);
        }

        
        @Override
        public AnAction[] getChildren(@Nullable AnActionEvent e) {
            return new AnAction[]{myEditTemplateAction};
        }

        @Override
        @RequiredUIAccess
        public void actionPerformed(AnActionEvent e) {
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