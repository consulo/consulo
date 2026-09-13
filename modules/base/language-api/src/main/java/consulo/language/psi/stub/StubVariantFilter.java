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
package consulo.language.psi.stub;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.language.Language;
import consulo.language.psi.PsiElement;
import consulo.project.Project;

/**
 * Decides whether an indexed declaration variant is active in the current project state.
 * Languages with condition-annotated stubs index <b>all</b> declaration variants (every
 * conditional branch, guards stored in the stubs) because usage contexts arrive at query
 * and resolve time when reindexing is impossible; this filter is where the language
 * evaluates a variant's guard against the declaring file's contexts.
 *
 * <p>Consumed by {@link StubIndex#getActiveElements} — the correct default view for
 * navigation and search. Resolution paths that carry an explicit requester context filter
 * with it directly instead.</p>
 */
@ExtensionAPI(ComponentScope.PROJECT)
public interface StubVariantFilter {
    Language getLanguage();

    /**
     * @return {@code true} when the variant is active in at least one context of its
     * declaring file — standalone or any recorded usage
     */
    boolean isActive(PsiElement variant);

    static boolean isActiveVariant(Project project, PsiElement variant) {
        Language language = variant.getLanguage();
        return !project.getExtensionPoint(StubVariantFilter.class)
            .anyMatchSafe(filter -> filter.getLanguage() == language && !filter.isActive(variant));
    }
}
