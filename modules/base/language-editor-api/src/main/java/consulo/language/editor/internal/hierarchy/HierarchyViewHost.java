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

import consulo.dataContext.DataSink;
import consulo.language.editor.hierarchy.HierarchyNodeDescriptor;
import consulo.ui.annotation.RequiredUIAccess;

/**
 * What a view calls back into. Every decision behind these methods belongs to the session, so a frontend
 * only reports what the user did and never decides what it means.
 * <p>
 * Not plugin-facing.
 *
 * @author VISTALL
 * @since 2026-09-19
 */
public interface HierarchyViewHost {
    /**
     * A node was opened - double-clicked, or selected while auto-scroll is on.
     */
    @RequiredUIAccess
    void activated(HierarchyNodeDescriptor descriptor, boolean requestFocus);

    boolean isAutoScrollToSource();

    /**
     * Publishes the session's data keys. The frontend forwards its own component-scoped keys and then
     * hands the sink here, so everything not tied to a widget is answered in one place.
     */
    @RequiredUIAccess
    void uiDataSnapshot(DataSink sink);
}
