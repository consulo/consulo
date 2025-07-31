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
import consulo.externalService.statistic.FeatureUsageTracker;
import consulo.platform.base.localize.ActionLocalize;
import consulo.project.ui.view.CompositeSelectInTarget;
import consulo.ide.localize.IdeLocalize;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.project.ui.view.SelectInContext;
import consulo.project.ui.view.SelectInManager;
import consulo.project.ui.view.SelectInTarget;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DefaultActionGroup;
import consulo.ui.ex.action.Presentation;
import consulo.ui.ex.popup.BaseListPopupStep;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.ui.ex.popup.ListPopup;
import consulo.ui.ex.popup.PopupStep;
import jakarta.annotation.Nonnull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

@ActionImpl(id = "SelectIn")
public class SelectInAction extends AnAction implements DumbAware {
    public SelectInAction() {
        super(ActionLocalize.actionSelectinText(), ActionLocalize.actionSelectinText());
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(@Nonnull AnActionEvent e) {
        FeatureUsageTracker.getInstance().triggerFeatureUsed("navigation.select.in");
        SelectInContext context = SelectInContextImpl.createContext(e);
        if (context == null) {
            return;
        }
        invoke(e.getDataContext(), context);
    }

    @Override
    public void update(@Nonnull AnActionEvent event) {
        event.getPresentation().setEnabledAndVisible(SelectInContextImpl.createContext(event) != null);
    }

    private static void invoke(DataContext dataContext, SelectInContext context) {
        List<SelectInTarget> targetVector = getSelectInManager(context.getProject()).getTargets();
        ListPopup popup;
        if (targetVector.isEmpty()) {
            DefaultActionGroup group = new DefaultActionGroup();
            group.add(new NoTargetsAction());
            popup = JBPopupFactory.getInstance().createActionGroupPopup(
                IdeLocalize.titlePopupSelectTarget().get(),
                group,
                dataContext,
                JBPopupFactory.ActionSelectionAid.MNEMONICS,
                true
            );
        }
        else {
            popup = JBPopupFactory.getInstance().createListPopup(new SelectInActionsStep(targetVector, context));
        }

        popup.showInBestPositionFor(dataContext);
    }

    private static class SelectInActionsStep extends BaseListPopupStep<SelectInTarget> {
        private final SelectInContext mySelectInContext;
        private final List<SelectInTarget> myVisibleTargets;

        public SelectInActionsStep(@Nonnull Collection<SelectInTarget> targetVector, SelectInContext selectInContext) {
            mySelectInContext = selectInContext;
            myVisibleTargets = new ArrayList<>();
            for (SelectInTarget target : targetVector) {
                myVisibleTargets.add(target);
            }
            init(IdeLocalize.titlePopupSelectTarget().get(), myVisibleTargets, null);
        }

        @Override
        @Nonnull
        public String getTextFor(SelectInTarget value) {
            LocalizeValue text = value.getActionText();
            int n = myVisibleTargets.indexOf(value);
            return numberingText(n, text.get());
        }

        @Override
        public PopupStep onChosen(SelectInTarget target, boolean finalChoice) {
            if (finalChoice) {
                target.selectIn(mySelectInContext, true);
                return FINAL_CHOICE;
            }
            if (target instanceof CompositeSelectInTarget compositeSelectInTarget) {
                ArrayList<SelectInTarget> subTargets = new ArrayList<>(compositeSelectInTarget.getSubTargets(mySelectInContext));
                if (subTargets.size() > 0) {
                    Collections.sort(subTargets, new SelectInManager.SelectInTargetComparator());
                    return new SelectInActionsStep(subTargets, mySelectInContext);
                }
            }
            return FINAL_CHOICE;
        }

        @Override
        public boolean hasSubstep(SelectInTarget selectedValue) {
            return selectedValue instanceof CompositeSelectInTarget compositeSelectInTarget
                && !compositeSelectInTarget.getSubTargets(mySelectInContext).isEmpty();
        }

        @Override
        public boolean isSelectable(SelectInTarget target) {
            return target.canSelect(mySelectInContext);
        }

        @Override
        public boolean isMnemonicsNavigationEnabled() {
            return true;
        }
    }

    private static String numberingText(int n, String text) {
        if (n < 9) {
            text = "&" + (n + 1) + ". " + text;
        }
        else if (n == 9) {
            text = "&" + 0 + ". " + text;
        }
        else {
            text = "&" + (char) ('A' + n - 10) + ". " + text;
        }
        return text;
    }

    private static SelectInManager getSelectInManager(Project project) {
        return SelectInManager.getInstance(project);
    }

    private static class NoTargetsAction extends AnAction {
        public NoTargetsAction() {
            super(IdeLocalize.messageNoTargetsAvailable());
        }

        @Override
        @RequiredUIAccess
        public void actionPerformed(@Nonnull AnActionEvent e) {
        }
    }
}