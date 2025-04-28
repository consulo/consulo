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

import consulo.ide.impl.idea.ide.favoritesTreeView.FavoritesManagerImpl;
import consulo.ide.impl.idea.ide.favoritesTreeView.FavoritesViewSettingsImpl;
import consulo.ide.impl.idea.ide.favoritesTreeView.FavoritesViewTreeBuilder;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.Toggleable;
import consulo.project.Project;
import consulo.ui.ex.awt.AnActionButton;
import consulo.disposer.Disposer;
import consulo.localize.LocalizeValue;
import consulo.ui.image.Image;
import jakarta.annotation.Nonnull;

/**
 * @author Konstantin Bulenkov
 */
public abstract class FavoritesToolbarButtonAction extends AnActionButton implements Toggleable {
    private FavoritesViewTreeBuilder myBuilder;
    private FavoritesViewSettingsImpl mySettings;

    @Deprecated
    public FavoritesToolbarButtonAction(Project project, FavoritesViewTreeBuilder builder, String name, Image icon) {
        this(project, builder, LocalizeValue.of(name), icon);
    }

    public FavoritesToolbarButtonAction(Project project, FavoritesViewTreeBuilder builder, LocalizeValue name, Image icon) {
        super(name, LocalizeValue.empty(), icon);
        myBuilder = builder;
        mySettings = FavoritesManagerImpl.getInstance(project).getViewSettings();
        setContextComponent(myBuilder.getTree());
        Disposer.register(
            project,
            () -> {
                myBuilder = null;
                mySettings = null;
            }
        );
    }

    public abstract boolean isOptionEnabled();

    public abstract void setOption(boolean enabled);

    public FavoritesViewSettingsImpl getViewSettings() {
        return mySettings;
    }

    public FavoritesViewTreeBuilder getBuilder() {
        return myBuilder;
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(@Nonnull AnActionEvent e) {
        setOption(!isOptionEnabled());
        myBuilder.updateFromRootCB();
    }

    @Override
    public void updateButton(AnActionEvent e) {
        super.updateButton(e);
        e.getPresentation().putClientProperty(SELECTED_PROPERTY, isOptionEnabled());
    }
}
