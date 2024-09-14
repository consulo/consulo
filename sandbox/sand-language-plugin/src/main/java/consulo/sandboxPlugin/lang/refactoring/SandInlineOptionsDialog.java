/*
 * Copyright 2013-2024 consulo.io
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
package consulo.sandboxPlugin.lang.refactoring;

import consulo.language.editor.refactoring.inline.InlineOptionsWithSearchSettingsDialog;
import consulo.language.psi.PsiElement;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import jakarta.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 2024-09-14
 */
public class SandInlineOptionsDialog extends InlineOptionsWithSearchSettingsDialog {
    public SandInlineOptionsDialog(Project project, boolean canBeParent, PsiElement element) {
        super(project, canBeParent, element);

        init();
    }

    @Override
    protected boolean isSearchInCommentsAndStrings() {
        return true;
    }

    @Override
    protected void saveSearchInCommentsAndStrings(boolean searchInComments) {

    }

    @Override
    protected boolean isSearchForTextOccurrences() {
        return true;
    }

    @Override
    protected void saveSearchInTextOccurrences(boolean searchInTextOccurrences) {

    }

    @Nonnull
    @Override
    protected LocalizeValue getNameLabelText() {
        return LocalizeValue.of("Test");
    }

    @Nonnull
    @Override
    protected LocalizeValue getBorderTitle() {
        return LocalizeValue.of("Invocation");
    }

    @Nonnull
    @Override
    protected LocalizeValue getInlineAllText() {
        return LocalizeValue.of("Inline All");
    }

    @Nonnull
    @Override
    protected LocalizeValue getInlineThisText() {
        return LocalizeValue.of("Inline this");
    }

    @Override
    protected boolean isInlineThis() {
        return true;
    }
}
