/*
 * Copyright 2000-2012 JetBrains s.r.o.
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

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.component.extension.ExtensionPointName;
import consulo.project.Project;
import consulo.ui.ex.awt.tree.ColoredTreeCellRenderer;
import consulo.ui.ex.awt.CommonActionsPanel;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.util.Comparator;
import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 * User: Irina.Chernushina
 * Date: 6/7/12
 * Time: 4:17 PM
 */
@ExtensionAPI(ComponentScope.PROJECT)
public interface FavoritesListProvider extends Comparator<FavoritesTreeNodeDescriptor>, Comparable<FavoritesListProvider> {
    ExtensionPointName<FavoritesListProvider> EP_NAME = ExtensionPointName.create(FavoritesListProvider.class);

    String getListName(final Project project);

    @Nullable
    String getCustomName(@Nonnull CommonActionsPanel.Buttons type);

    boolean willHandle(@Nonnull CommonActionsPanel.Buttons type, Project project, @Nonnull Set<Object> selectedObjects);

    void handle(@Nonnull CommonActionsPanel.Buttons type, Project project, @Nonnull Set<Object> selectedObjects, JComponent component);

    int getWeight();

    @Nullable
    FavoritesListNode createFavoriteListNode(Project project);

    void customizeRenderer(
        ColoredTreeCellRenderer renderer,
        JTree tree,
        Object value,
        boolean selected,
        boolean expanded,
        boolean leaf,
        int row,
        boolean hasFocus
    );
}
