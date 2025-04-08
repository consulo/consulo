/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package consulo.ide.impl.idea.ide.favoritesTreeView;

import consulo.annotation.component.ExtensionImpl;
import consulo.ide.impl.idea.ide.impl.SelectInTargetPsiWrapper;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFileSystemItem;
import consulo.language.psi.PsiUtilCore;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.project.ui.view.StandardTargetWeights;
import consulo.project.ui.view.localize.ProjectUIViewLocalize;
import consulo.project.ui.wm.ToolWindowId;
import consulo.project.ui.wm.ToolWindowManager;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.toolWindow.ToolWindow;
import consulo.util.concurrent.ActionCallback;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;

/**
 * @author anna
 * @author Konstantin Bulenkov
 */
@ExtensionImpl
public class FavoritesViewSelectInTarget extends SelectInTargetPsiWrapper {
    @Inject
    public FavoritesViewSelectInTarget(final Project project) {
        super(project);
    }

    @Nonnull
    @Override
    public LocalizeValue getActionText() {
        return ProjectUIViewLocalize.selectInFavorites();
    }

    @Override
    public String getToolWindowId() {
        return ProjectUIViewLocalize.selectInFavorites().get();
    }

    @Override
    protected void select(Object selector, VirtualFile virtualFile, boolean requestFocus) {
        select(myProject, selector, virtualFile, requestFocus);
    }

    @Override
    protected void select(PsiElement element, boolean requestFocus) {
        PsiElement toSelect = findElementToSelect(element, null);
        if (toSelect != null) {
            VirtualFile virtualFile = PsiUtilCore.getVirtualFile(toSelect);
            select(toSelect, virtualFile, requestFocus);
        }
    }

    private static ActionCallback select(@Nonnull Project project, Object toSelect, VirtualFile virtualFile, boolean requestFocus) {
        final ActionCallback result = new ActionCallback();

        ToolWindowManager windowManager = ToolWindowManager.getInstance(project);
        final ToolWindow favoritesToolWindow = windowManager.getToolWindow(ToolWindowId.BOOKMARKS);

        if (favoritesToolWindow != null) {
            final Runnable runnable = () -> {
                final FavoritesTreeViewPanel panel = UIUtil.findComponentOfType(favoritesToolWindow.getComponent(), FavoritesTreeViewPanel.class);
                if (panel != null) {
                    panel.selectElement(toSelect, virtualFile, requestFocus);
                    result.setDone();
                }
            };

            if (requestFocus) {
                favoritesToolWindow.activate(runnable, false);
            }
            else {
                favoritesToolWindow.show(runnable);
            }
        }

        return result;
    }

    @Override
    protected boolean canSelect(final PsiFileSystemItem file) {
        return findSuitableFavoritesList(file.getVirtualFile(), myProject, null) != null;
    }

    public static String findSuitableFavoritesList(VirtualFile file, Project project, final String currentSubId) {
        return FavoritesManagerImpl.getInstance(project).getFavoriteListName(currentSubId, file);
    }

    @Override
    public String getMinorViewId() {
        return FavoritesViewTreeBuilder.ID;
    }

    @Override
    public float getWeight() {
        return StandardTargetWeights.FAVORITES_WEIGHT;
    }

    @Override
    protected boolean canWorkWithCustomObjects() {
        return false;
    }
}