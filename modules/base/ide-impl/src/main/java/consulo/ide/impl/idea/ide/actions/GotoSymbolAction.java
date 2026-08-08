// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.ide.actions;

import consulo.annotation.access.RequiredReadAction;
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
import jakarta.inject.Inject;

@ActionImpl(id = "GotoSymbol")
public class GotoSymbolAction extends SearchEverywhereBaseAction implements DumbAware {
    private final Application myApplication;

    @Inject
    public GotoSymbolAction(Application application) {
        super(ActionLocalize.actionGotosymbolText(), ActionLocalize.actionGotosymbolDescription());
        myApplication = application;
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(AnActionEvent e) {
        Project project = e.getData(Project.KEY);
        if (project == null) {
            return;
        }

        boolean dumb = DumbService.isDumb(project);
        if (!dumb || new SymbolSearchEverywhereContributor(project, null).isDumbAware()) {
            showInSearchEverywherePopup(SymbolSearchEverywhereContributor.class.getSimpleName(), e, true, true);
        }
        else {
            invokeGoToFile(project, e);
        }
    }

    @Override
    protected boolean hasContributors(DataContext dataContext) {
        return myApplication.getExtensionPoint(GotoSymbolContributor.class).hasAnyExtensions();
    }
}