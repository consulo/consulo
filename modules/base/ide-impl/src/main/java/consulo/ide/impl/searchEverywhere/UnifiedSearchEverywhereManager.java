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
package consulo.ide.impl.searchEverywhere;

import consulo.annotation.component.ComponentProfiles;
import consulo.annotation.component.ServiceImpl;
import consulo.ide.impl.idea.ide.actions.searcheverywhere.SearchEverywhereManagerImpl;
import consulo.project.Project;
import consulo.searchEverywhere.SearchEverywhereManager;
import consulo.ui.Window;
import consulo.ui.WindowOptions;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.layout.VerticalLayout;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

/**
 * Stands in for the swing search everywhere on the frontends that cannot show it. The window is empty for now -
 * what it is here for is the way in: the action is reached through a double shift, and that flow is worth having
 * end to end before there is anything to put inside.
 *
 * @author VISTALL
 * @since 2026-08-01
 */
@Singleton
@ServiceImpl(profiles = ComponentProfiles.UNIFIED)
public class UnifiedSearchEverywhereManager implements SearchEverywhereManager {
    private final Project myProject;

    private @Nullable Window myWindow;
    private String myContributorID = SearchEverywhereManagerImpl.ALL_CONTRIBUTORS_GROUP_ID;
    private boolean myEverywhere;

    @Inject
    public UnifiedSearchEverywhereManager(Project project) {
        myProject = project;
    }

    @Override
    public boolean isShown() {
        return myWindow != null;
    }

    @Override
    @RequiredUIAccess
    public void show(String contributorID, @Nullable String searchText, AnActionEvent initEvent) {
        myContributorID = contributorID;

        if (myWindow != null) {
            return;
        }

        Window window = Window.create("Search Everywhere", WindowOptions.builder().owner(myProject.getWindow()).build());
        window.setContent(VerticalLayout.create());
        window.addCloseListener(event -> myWindow = null);

        myWindow = window;

        window.show();
    }

    @Override
    public String getSelectedContributorID() {
        return myContributorID;
    }

    @Override
    public void setSelectedContributor(String contributorID) {
        myContributorID = contributorID;
    }

    @Override
    public void toggleEverywhereFilter() {
        myEverywhere = !myEverywhere;
    }

    @Override
    public boolean isEverywhere() {
        return myEverywhere;
    }

    @Override
    public String getCurrentSearchText() {
        return "";
    }
}
