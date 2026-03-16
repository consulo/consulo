// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.searchEverywhere;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.project.Project;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.popup.JBPopup;
import consulo.util.dataholder.Key;
import org.jspecify.annotations.Nullable;

/**
 * @author Mikhail.Sokolov
 */
@ServiceAPI(ComponentScope.PROJECT)
public interface SearchEverywhereManager {
    public static final Key<JBPopup> SEARCH_EVERYWHERE_POPUP = Key.create("SearchEverywherePopup");

    static SearchEverywhereManager getInstance(Project project) {
        return project.getInstance(SearchEverywhereManager.class);
    }

    boolean isShown();

    void show(
        String contributorID,
        @Nullable String searchText,
        AnActionEvent initEvent
    );

    
    String getSelectedContributorID();

    void setSelectedContributor(String contributorID);

    void toggleEverywhereFilter();

    // todo remove
    boolean isEverywhere();

    String getCurrentSearchText();
}
