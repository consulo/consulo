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
package consulo.project.ui.view.tree;

import consulo.annotation.DeprecationInfo;
import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.component.extension.ExtensionPointName;
import consulo.dataContext.DataSink;
import consulo.util.dataholder.Key;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Collection;

/**
 * Allows a plugin to modify the structure of a project as displayed in the project view.
 */
@ExtensionAPI(ComponentScope.PROJECT)
public interface TreeStructureProvider {
    ExtensionPointName<TreeStructureProvider> EP_NAME = ExtensionPointName.create(TreeStructureProvider.class);

    /**
     * Allows a plugin to modify the list of children displayed for the specified node in the
     * project view.
     *
     * @param parent   the parent node.
     * @param children the list of child nodes according to the default project structure.
     *                 Elements of the collection are of type {@link ProjectViewNode}.
     * @param settings the current project view settings.
     * @return the modified collection of child nodes, or <code>children</code> if no modifications
     * are required.
     */
    Collection<AbstractTreeNode> modify(AbstractTreeNode parent, Collection<AbstractTreeNode> children, ViewSettings settings);

    /**
     * Override to populate UI data snapshot for the selection.
     *
     * @param sink      the data sink to populate
     * @param selection the currently selected nodes
     */
    default void uiDataSnapshot(@Nonnull DataSink sink,
                                @Nonnull Collection<? extends AbstractTreeNode> selection) {
        // default: delegate to legacy getData for backward compatibility
    }

    /**
     * Returns a user data object of the specified type for the specified selection in the
     * project view.
     *
     * @param selected the list of nodes currently selected in the project view.
     * @param dataKey  the identifier of the requested data object
     * @return the data object, or null if no data object can be returned by this provider.
     * @deprecated Use {@link #uiDataSnapshot(DataSink, Collection)} instead.
     */
    @Deprecated
    @DeprecationInfo("Use #uiDataSnapshot(DataSink, Collection)")
    @Nullable
    default Object getData(Collection<AbstractTreeNode> selected, Key<?> dataKey) {
        return null;
    }
}
