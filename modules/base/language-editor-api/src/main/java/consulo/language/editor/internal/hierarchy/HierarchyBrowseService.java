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

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.dataContext.DataContext;
import consulo.language.editor.hierarchy.HierarchyKind;
import consulo.language.editor.hierarchy.HierarchyModel;
import consulo.language.editor.hierarchy.HierarchyProvider;
import consulo.language.psi.PsiElement;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;

/**
 * Opens a hierarchy. This is the whole of what opening one involves - provider lookup, model creation and
 * the tool window behind it all live here, so an action only ever names a {@link HierarchyKind} and a
 * context.
 * <p>
 * Not plugin-facing; the way in for a plugin is
 * {@link consulo.language.editor.hierarchy.BrowseHierarchyActionBase}.
 *
 * @author VISTALL
 * @since 2026-09-19
 */
@ServiceAPI(ComponentScope.PROJECT)
public interface HierarchyBrowseService {
    static HierarchyBrowseService getInstance(Project project) {
        return project.getInstance(HierarchyBrowseService.class);
    }

    /**
     * Whether a hierarchy of this kind could ever be shown here - the frontend can draw one, and some
     * {@link HierarchyProvider} offers this kind. Resolves nothing, so it is safe to ask while deciding
     * whether an action should appear at all.
     */
    boolean isSupported(HierarchyKind kind);

    /**
     * Whether a hierarchy of this kind could be opened from the given context. Meant for deciding whether
     * to enable the action, so it resolves the target but builds nothing.
     */
    @RequiredReadAction
    boolean isAvailable(HierarchyKind kind, DataContext dataContext);

    /**
     * Finds the element the context points at, asks the provider registered for its language, and shows
     * the result in the hierarchy tool window. Does nothing where no provider claims the context.
     */
    @RequiredUIAccess
    void browse(HierarchyKind kind, DataContext dataContext);

    /**
     * Shows a hierarchy on an element the caller has already chosen, rather than on whatever the context
     * resolves to. {@code afterInit} is handed the browser once it has been put into the tool window.
     */
    @RequiredUIAccess
    void browse(HierarchyKind kind, PsiElement target, DataContext dataContext, Consumer<HierarchyBrowser> afterInit);

    /**
     * Shows a model the caller built itself, bypassing provider lookup entirely - for a caller that
     * already knows what it wants to show.
     */
    @RequiredUIAccess
    <E extends PsiElement> void browse(HierarchyModel<E> model);

    /**
     * Builds a hierarchy over the given context and hands it back instead of showing it, for a caller
     * which embeds it somewhere of its own. The caller owns the result and must dispose it.
     */
    @RequiredUIAccess
    @Nullable
    HierarchyBrowser createBrowser(HierarchyKind kind, DataContext dataContext);
}
