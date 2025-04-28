/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package consulo.ide.impl.idea.ide.favoritesTreeView.actions;

import consulo.application.AllIcons;
import consulo.ide.impl.idea.ide.favoritesTreeView.FavoritesViewTreeBuilder;
import consulo.project.Project;
import consulo.ui.ex.awt.AutoScrollToSourceHandler;
import consulo.ui.ex.UIBundle;

/**
 * @author Konstantin Bulenkov
 */
public class FavoritesAutoScrollToSourceAction extends FavoritesToolbarButtonAction {
    private final AutoScrollToSourceHandler myAutoScrollToSourceHandler;

    public FavoritesAutoScrollToSourceAction(
        Project project,
        AutoScrollToSourceHandler autoScrollToSourceHandler,
        FavoritesViewTreeBuilder builder
    ) {
        super(project, builder, UIBundle.message("autoscroll.to.source.action.name"), AllIcons.General.AutoscrollToSource);
        myAutoScrollToSourceHandler = autoScrollToSourceHandler;
    }

    @Override
    public boolean isOptionEnabled() {
        return getViewSettings().isAutoScrollToSource();
    }

    @Override
    public void setOption(boolean enabled) {
        getViewSettings().setAutoScrollToSource(enabled);
        if (enabled) {
            myAutoScrollToSourceHandler.onMouseClicked(getBuilder().getTree());
        }
    }
}
