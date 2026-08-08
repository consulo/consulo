// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.ide.impl.idea.ide.actions;

import consulo.dataContext.DataContext;
import consulo.externalService.statistic.FeatureUsageTracker;
import consulo.ide.impl.idea.openapi.ui.playback.commands.ActionCommand;
import consulo.ide.localize.IdeLocalize;
import consulo.localize.LocalizeValue;
import consulo.project.DumbService;
import consulo.project.Project;
import consulo.searchEverywhere.SearchEverywhereManager;
import consulo.ui.ex.action.*;
import consulo.ui.ex.awt.UIExAWTDataKey;
import consulo.ui.ex.awt.internal.IdeEventQueueProxy;
import consulo.ui.image.Image;
import consulo.util.lang.StringUtil;

import java.awt.*;
import java.awt.event.InputEvent;

import static consulo.ide.impl.idea.ide.actions.GotoActionBase.getInitialText;

public abstract class SearchEverywhereBaseAction extends AnAction implements AnActionWithSyncUpdate {
    protected SearchEverywhereBaseAction() {
    }

    protected SearchEverywhereBaseAction(Image icon) {
        super(icon);
    }

    protected SearchEverywhereBaseAction(LocalizeValue text) {
        super(text);
    }

    protected SearchEverywhereBaseAction(LocalizeValue text, LocalizeValue description) {
        super(text, description);
    }

    protected SearchEverywhereBaseAction(LocalizeValue text, LocalizeValue description, Image icon) {
        super(text, description, icon);
    }

    @Override
    public void update(AnActionEvent event) {
        Presentation presentation = event.getPresentation();
        DataContext dataContext = event.getDataContext();
        Project project = dataContext.getData(Project.KEY);
        boolean hasContributors = hasContributors(dataContext);
        presentation.setEnabled((!requiresProject() || project != null) && hasContributors);
        presentation.setVisible(hasContributors);
    }

    protected boolean requiresProject() {
        return true;
    }

    protected boolean hasContributors(DataContext context) {
        return true;
    }

    public static void invokeGoToFile(Project project, AnActionEvent e) {
        String actionTitle = e.getPresentation().getTextValue().get();
        DumbService.getInstance(project).showDumbModeNotification(IdeLocalize.goToClassDumbModeMessage(actionTitle));
        AnAction action = ActionManager.getInstance().getAction(GotoFileAction.ID);
        InputEvent event = ActionCommand.getInputEvent(GotoFileAction.ID);
        Component component = e.getData(UIExAWTDataKey.CONTEXT_COMPONENT);
        ActionManager.getInstance().tryToExecute(action, event, component, e.getPlace(), true);
    }

    protected void showInSearchEverywherePopup(String searchProviderID, AnActionEvent event, boolean useEditorSelection) {
        showInSearchEverywherePopup(searchProviderID, event, useEditorSelection, false);
    }

    protected void showInSearchEverywherePopup(
        String searchProviderID,
        AnActionEvent event,
        boolean useEditorSelection,
        boolean sendStatistics
    ) {
        Project project = event.getData(Project.KEY);
        if (project == null) {
            return;
        }
        SearchEverywhereManager seManager = SearchEverywhereManager.getInstance(project);
        FeatureUsageTracker.getInstance().triggerFeatureUsed(IdeActions.ACTION_SEARCH_EVERYWHERE);

        if (seManager.isShown()) {
            if (searchProviderID.equals(seManager.getSelectedContributorID())) {
                seManager.toggleEverywhereFilter();
            }
            else {
                seManager.setSelectedContributor(searchProviderID);
            }
            return;
        }

        IdeEventQueueProxy.getInstance().closeAllPopups(false);
        String searchText = StringUtil.nullize(getInitialText(useEditorSelection, event).first);
        seManager.show(searchProviderID, searchText, event);
    }
}