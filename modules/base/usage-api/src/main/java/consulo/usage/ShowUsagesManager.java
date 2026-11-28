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
package consulo.usage;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.codeEditor.Editor;
import consulo.language.psi.PsiElement;
import consulo.project.Project;
import consulo.ui.RelativePoint2D;
import consulo.ui.annotation.RequiredUIAccess;
import org.jspecify.annotations.Nullable;

/**
 * Opens the Show Usages popup - the one of the Show Usages action - for an element.
 *
 * @author VISTALL
 * @since 2026-11-16
 */
@ServiceAPI(ComponentScope.PROJECT)
public interface ShowUsagesManager {
    static ShowUsagesManager getInstance(Project project) {
        return project.getInstance(ShowUsagesManager.class);
    }

    /**
     * Shows the usages popup of {@code element} at {@code point}, the way Go To Declaration does when invoked on a
     * declaration. While indices are not ready, a dumb mode notification is shown instead.
     *
     * @param point where to show the popup; if {@code null}, the best location in {@code editor} is used
     */
    @RequiredUIAccess
    void startFindUsages(Editor editor, PsiElement element, @Nullable RelativePoint2D point);

    /**
     * Shows the usages popup of {@code element} at {@code popupPosition}.
     *
     * @param editor the editor the search is started from, if any
     */
    @RequiredUIAccess
    void startFindUsages(PsiElement element, RelativePoint2D popupPosition, @Nullable Editor editor);

    /**
     * Shows the usages popup of {@code target} at {@code popupPosition}.
     *
     * @param editor the editor the search is started from, if any
     * @return {@code false} if usages of such a target cannot be shown in the popup
     */
    @RequiredUIAccess
    boolean showUsages(UsageTarget target, RelativePoint2D popupPosition, @Nullable Editor editor);
}
