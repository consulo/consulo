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
import consulo.language.editor.hierarchy.HierarchyModel;
import consulo.language.psi.PsiElement;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import org.jspecify.annotations.Nullable;

/**
 * Builds the frontend half of a hierarchy. This is the only seam between the neutral side - provider
 * lookup, tool-window wiring, actions, persisted state - and the widgets, so a frontend binds one
 * implementation and nothing else.
 * <p>
 * Application scoped on purpose: which implementation is bound follows the frontend profile alone, the
 * factory itself holds no state, and the project a browser belongs to is handed to
 * {@link #createBrowser(Project, HierarchyModel)} per call.
 * <p>
 * A frontend which cannot draw a hierarchy yet binds a stub answering {@code false} from
 * {@link #isSupported()}, so the hierarchy actions are hidden rather than failing when invoked.
 * <p>
 * Not plugin-facing.
 *
 * @author VISTALL
 * @since 2026-09-19
 */
@ServiceAPI(ComponentScope.APPLICATION)
public interface HierarchyBrowserFactory {
    static HierarchyBrowserFactory getInstance() {
        return Application.get().getInstance(HierarchyBrowserFactory.class);
    }

    /**
     * Whether this frontend can draw a hierarchy at all. Checked while updating an action, so it must not
     * build anything.
     */
    boolean isSupported();

    /**
     * Builds a browser over the given model, showing it nowhere. Answers null where this frontend cannot
     * draw one.
     */
    @RequiredUIAccess
    @Nullable
    HierarchyBrowser createBrowser(Project project, HierarchyModel<? extends PsiElement> model);
}
