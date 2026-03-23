/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package consulo.externalSystem.view;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.externalSystem.model.DataNode;
import consulo.externalSystem.model.Key;
import consulo.externalSystem.model.ProjectSystemId;
import consulo.util.collection.MultiMap;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Extension point for contributing nodes to the external system project tree view.
 * Implementations are filtered by {@link #getSystemId()} — only contributors matching
 * the current system or {@link ProjectSystemId#IDE} are applied.
 *
 * @author Vladislav.Soroka
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public abstract class ExternalSystemViewContributor {
    /**
     * The external system this contributor applies to, or {@link ProjectSystemId#IDE} to apply to all systems.
     */
   
    public abstract ProjectSystemId getSystemId();

    /**
     * The data node keys this contributor handles.
     * Only data nodes with one of these keys will be passed to {@link #createNodes}.
     */
   
    public abstract List<Key<?>> getKeys();

    /**
     * Convert data nodes into view tree nodes.
     *
     * @param view      the current projects view
     * @param dataNodes data nodes grouped by key (only keys from {@link #getKeys()} are included)
     * @return list of tree nodes to insert into the view
     */
   
    public abstract List<ExternalSystemNode<?>> createNodes(ExternalProjectsView view,
                                                            MultiMap<Key<?>, DataNode<?>> dataNodes);

    /**
     * Return a display name for the given data node, or {@code null} to use the default.
     */
    @Nullable
    public String getDisplayName(DataNode<?> node) {
        return null;
    }

    /**
     * Return the error level for the given data node, or {@link ExternalProjectsStructure.ErrorLevel#NONE} if no error.
     */
   
    public ExternalProjectsStructure.ErrorLevel getErrorLevel(DataNode<?> dataNode) {
        return ExternalProjectsStructure.ErrorLevel.NONE;
    }
}
