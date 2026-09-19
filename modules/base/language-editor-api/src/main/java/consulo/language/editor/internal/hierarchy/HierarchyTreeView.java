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
package consulo.language.editor.internal.hierarchy;

import consulo.disposer.Disposable;
import consulo.language.editor.hierarchy.HierarchyLegendEntry;
import consulo.language.editor.hierarchy.HierarchyNodeDescriptor;
import consulo.language.editor.hierarchy.HierarchyTreeStructure;
import consulo.ui.Component;
import consulo.ui.ex.TreeExpander;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * A tree of hierarchy nodes, as the frontend draws it. This is deliberately the whole of what differs
 * between frontends - it knows nothing about view types, scopes, actions or data keys, so the session
 * driving it stays in one place no matter how many frontends exist.
 * <p>
 * Not plugin-facing.
 *
 * @author VISTALL
 * @since 2026-09-19
 */
public interface HierarchyTreeView extends Disposable {
    /**
     * Shows a freshly built structure, keeping nothing from the one before it.
     *
     * @return a future which is done once the tree has been rebuilt and its base node selected
     */
    CompletableFuture<?> setStructure(HierarchyTreeStructure structure);

    /**
     * An opaque token describing what is open and what is selected, for replay after a rebuild. A view
     * which cannot capture its own state answers null and is simply left unrestored.
     */
    @Nullable Object saveState();

    void restoreState(@Nullable Object state);

    /**
     * Re-reads every node of the current structure without rebuilding it.
     */
    void invalidate();

    @Nullable HierarchyNodeDescriptor getSelectedDescriptor();

    List<HierarchyNodeDescriptor> getSelectedDescriptors();

    /**
     * Every descriptor the tree has actually built so far. Unlike walking the structure this loads
     * nothing, so it is safe to answer navigation questions from.
     */
    List<HierarchyNodeDescriptor> getMaterializedDescriptors();

    void select(HierarchyNodeDescriptor descriptor, boolean requestFocus);

    void requestFocus();

    void setLegend(List<HierarchyLegendEntry> legend);

    @Nullable TreeExpander getTreeExpander();

    Component getUIComponent();
}
