/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package consulo.bookmark.ui.view;

import consulo.localize.LocalizeValue;
import consulo.project.ui.view.tree.AbstractTreeNode;
import consulo.annotation.access.RequiredReadAction;
import consulo.project.Project;
import consulo.ui.ex.awt.tree.ColoredTreeCellRenderer;
import consulo.ui.ex.awt.CommonActionsPanel;
import jakarta.annotation.Nonnull;

import jakarta.annotation.Nullable;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * @author Vassiliy.Kudryashov
 */
public abstract class AbstractFavoritesListProvider<T> implements FavoritesListProvider {
    public static final int BOOKMARKS_WEIGHT = 100;
    public static final int BREAKPOINTS_WEIGHT = 200;
    public static final int TASKS_WEIGHT = 300;
    protected final Project myProject;
    private final String myListName;
    protected final List<AbstractTreeNode<T>> myChildren = new ArrayList<>();
    protected final FavoritesListNode myNode;

    protected AbstractFavoritesListProvider(@Nonnull Project project, String listName) {
        this(project, listName, null);
    }

    protected AbstractFavoritesListProvider(@Nonnull Project project, final String listName, @Nullable String description) {
        myProject = project;
        myListName = listName;
        myNode = new FavoritesListNode(project, listName, description) {
            @Nonnull
            @Override
            @RequiredReadAction
            public Collection<? extends AbstractTreeNode> getChildren() {
                return myChildren;
            }

            @Override
            public FavoritesListProvider getProvider() {
                return AbstractFavoritesListProvider.this;
            }
        };
    }

    @Override
    public String getListName(Project project) {
        return myListName;
    }

    @Nullable
    @Override
    public FavoritesListNode createFavoriteListNode(Project project) {
        return myNode;
    }

    @Override
    public int compare(FavoritesTreeNodeDescriptor o1, FavoritesTreeNodeDescriptor o2) {
        return o1.getIndex() - o2.getIndex();
    }

    @Nonnull
    @Override
    public LocalizeValue getCustomName(@Nonnull CommonActionsPanel.Buttons type) {
        return LocalizeValue.empty();
    }

    @Override
    public boolean willHandle(@Nonnull CommonActionsPanel.Buttons type, Project project, @Nonnull Set<Object> selectedObjects) {
        return false;
    }

    @Override
    public void handle(
        @Nonnull CommonActionsPanel.Buttons type,
        Project project,
        @Nonnull Set<Object> selectedObjects,
        JComponent component
    ) {
    }

    @Override
    public int compareTo(FavoritesListProvider o) {
        return Integer.compare(getWeight(), o.getWeight());
    }

    @Override
    public void customizeRenderer(
        ColoredTreeCellRenderer renderer,
        JTree tree,
        @Nonnull Object value,
        boolean selected,
        boolean expanded,
        boolean leaf,
        int row,
        boolean hasFocus
    ) {
    }
}
