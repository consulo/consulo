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

import consulo.annotation.component.ExtensionImpl;
import consulo.codeEditor.Editor;
import consulo.language.Language;
import consulo.language.editor.refactoring.inline.InlineActionHandler;
import consulo.language.psi.PsiElement;
import consulo.project.Project;
import consulo.sandboxPlugin.lang.SandLanguage;
import consulo.ui.annotation.RequiredUIAccess;

/**
 * @author VISTALL
 * @since 2024-09-14
 */
@ExtensionImpl
public class SandInlineActionHandler extends InlineActionHandler {
    @Override
    public boolean isEnabledForLanguage(Language l) {
        return l instanceof SandLanguage;
    }

    @Override
    public boolean canInlineElement(PsiElement element) {
        return true;
    }

    @Override
    @RequiredUIAccess
    public void inlineElement(Project project, Editor editor, PsiElement element) {
        SandInlineOptionsDialog dialog = new SandInlineOptionsDialog(project, true, element);
        dialog.showAsync();
    }
}
