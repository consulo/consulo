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
package consulo.language.editor.hierarchy;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.dataContext.DataContext;
import consulo.language.extension.LanguageExtension;
import consulo.language.psi.PsiElement;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import org.jspecify.annotations.Nullable;

/**
 * Offers one kind of hierarchy for one language. A provider finds what the user meant and hands back a
 * {@link HierarchyModel} describing it; the platform owns the tool window, the tree, the toolbar and the
 * state, so nothing here touches a component.
 *
 * @author yole
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public interface HierarchyProvider<E extends PsiElement> extends LanguageExtension {
    /**
     * Which family this provider serves - see {@link StandardHierarchyKinds}.
     */
    HierarchyKind getKind();

    /**
     * The element a hierarchy should be shown for, or null where this provider has nothing to offer in
     * the given context.
     */
    @RequiredReadAction
    @Nullable E getTarget(DataContext dataContext);

    @RequiredReadAction
    HierarchyModel<E> createModel(Project project, E target);

    /**
     * Called once the hierarchy has been opened, for a provider that wants to react - moving the caret to
     * the element it resolved, say.
     */
    @RequiredUIAccess
    default void targetSelected(E target) {
    }
}
