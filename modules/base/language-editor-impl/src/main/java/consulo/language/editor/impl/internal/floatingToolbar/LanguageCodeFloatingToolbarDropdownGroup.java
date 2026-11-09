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
package consulo.language.editor.impl.internal.floatingToolbar;

import consulo.codeEditor.toolbar.floating.CodeFloatingToolbarDropdownGroup;
import consulo.language.Language;
import consulo.language.editor.floatingToolbar.CodeFloatingToolbarLanguageSupport;
import consulo.localize.LocalizeValue;
import consulo.ui.ex.action.AnActionEvent;

/**
 * A refactoring dropdown, left out of the toolbar of a language with a minimal one.
 *
 * @author VISTALL
 * @since 2026-11-09
 */
public abstract class LanguageCodeFloatingToolbarDropdownGroup extends CodeFloatingToolbarDropdownGroup {
    protected LanguageCodeFloatingToolbarDropdownGroup(LocalizeValue text) {
        super(text);
    }

    @Override
    public void update(AnActionEvent e) {
        super.update(e);

        Language language = e.getData(Language.KEY);
        CodeFloatingToolbarLanguageSupport support = language == null ? null : CodeFloatingToolbarLanguageSupport.forLanguage(language);
        e.getPresentation().setEnabledAndVisible(support == null || !support.isMinimal());
    }
}
