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

import consulo.language.psi.PsiElement;
import consulo.project.Project;
import org.jspecify.annotations.Nullable;

/**
 * Everything a model is given when it is asked to build one view of a hierarchy. The element is the live
 * one, re-resolved for this build, so it is safe to use after a refresh has moved the target.
 *
 * @author VISTALL
 * @since 2026-09-19
 */
public interface HierarchyRequest<E extends PsiElement> {
    Project getProject();

    E getTarget();

    HierarchyViewType getViewType();

    /**
     * The scope the user has chosen, or null where the model declared it takes none.
     */
    @Nullable HierarchyScope getScope();

    boolean isEnabled(HierarchyOption option);
}
