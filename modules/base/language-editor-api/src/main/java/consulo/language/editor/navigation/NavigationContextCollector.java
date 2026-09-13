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
package consulo.language.editor.navigation;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.codeEditor.Editor;
import consulo.language.psi.PsiFile;
import org.jspecify.annotations.Nullable;

/**
 * Collects a language-defined context at the start of a navigation gesture — the only
 * moment the requester side is fully known. The collected contexts travel with the whole
 * navigation via {@link NavigationContexts}: available arbitrarily deep on the executing
 * thread, restorable on other threads, and stamped onto the opened target editor as user
 * data.
 *
 * <p>Typical payload: the flag/option environment of the source file, so the target can be
 * presented as the navigation origin sees it (variant banners, inactive-branch dimming).</p>
 */
@ExtensionAPI(ComponentScope.PROJECT)
public interface NavigationContextCollector {
    /**
     * @return a context object for this navigation, or {@code null} when this collector
     * has nothing to say about the given source
     */
    @Nullable
    Object collectContext(Editor editor, PsiFile file, int offset);
}
