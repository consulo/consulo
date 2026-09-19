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

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.application.Application;
import consulo.disposer.Disposable;
import consulo.project.Project;

/**
 * Builds the frontend half of a hierarchy session. Each frontend binds its own implementation; a frontend
 * which cannot draw one yet binds a stub answering {@code false} from {@link #isSupported()}, so the
 * hierarchy actions are hidden rather than failing when invoked.
 * <p>
 * Not plugin-facing.
 *
 * @author VISTALL
 * @since 2026-09-19
 */
@ServiceAPI(ComponentScope.APPLICATION)
public interface HierarchyTreeViewFactory {
    static HierarchyTreeViewFactory getInstance() {
        return Application.get().getInstance(HierarchyTreeViewFactory.class);
    }

    /**
     * Whether this frontend can draw a hierarchy at all. Checked while updating an action, so it must not
     * build anything.
     */
    boolean isSupported();

    HierarchyTreeView createView(Project project, HierarchyViewSetup setup, Disposable parent);
}
