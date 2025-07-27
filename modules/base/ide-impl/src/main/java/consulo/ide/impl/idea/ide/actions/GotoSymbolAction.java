// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.ide.actions;

import consulo.annotation.component.ActionImpl;
import consulo.application.Application;
import consulo.application.dumb.DumbAware;
import consulo.dataContext.DataContext;
import consulo.externalService.statistic.FeatureUsageTracker;
import consulo.ide.impl.idea.ide.actions.searcheverywhere.SymbolSearchEverywhereContributor;
import consulo.ide.impl.idea.ide.util.gotoByName.*;
import consulo.ide.navigation.GotoSymbolContributor;
import consulo.language.Language;
import consulo.language.psi.PsiDocumentManager;
import consulo.platform.base.localize.ActionLocalize;
import consulo.project.DumbService;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import jakarta.annotation.Nonnull;

@ActionImpl(id = "GotoSymbol")
public class GotoSymbolAction extends GotoActionBase implements DumbAware {
    public GotoSymbolAction() {
        super(ActionLocalize.actionGotosymbolText(), ActionLocalize.actionGotosymbolDescription());
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(@Nonnull AnActionEvent e) {
        Project project = e.getData(Project.KEY);
        if (project == null) {
            return;
        }

        boolean dumb = DumbService.isDumb(project);
        if (!dumb || new SymbolSearchEverywhereContributor(project, null).isDumbAware()) {
            showInSearchEverywherePopup(SymbolSearchEverywhereContributor.class.getSimpleName(), e, true, true);
        }
        else {
            GotoClassAction.invokeGoToFile(project, e);
        }
    }

    @Override
    public void gotoActionPerformed(@Nonnull AnActionEvent e) {
        FeatureUsageTracker.getInstance().triggerFeatureUsed("navigation.popup.symbol");

        Project project = e.getData(Project.KEY);
        if (project == null) {
            return;
        }

        GotoSymbolModel2 model = new GotoSymbolModel2(project);
        PsiDocumentManager.getInstance(project).commitAllDocuments();
        showNavigationPopup(
            e,
            model,
            new GotoActionCallback<Language>() {
                @Override
                protected ChooseByNameFilter<Language> createFilter(@Nonnull ChooseByNamePopup popup) {
                    return new ChooseByNameLanguageFilter(popup, model, GotoClassSymbolConfiguration.getInstance(project), project);
                }

                @Override
                public void elementChosen(ChooseByNamePopup popup, Object element) {
                    GotoClassAction.handleSubMemberNavigation(popup, element);
                }
            },
            "Symbols matching patterns",
            true
        );
    }

    @Override
    protected boolean hasContributors(DataContext dataContext) {
        return Application.get().getExtensionPoint(GotoSymbolContributor.class).hasAnyExtensions();
    }
}