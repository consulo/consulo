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
package consulo.sandboxPlugin.lang.refactoring;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.Language;
import consulo.language.editor.refactoring.RefactoringSupportProvider;
import consulo.language.psi.PsiElement;
import consulo.sandboxPlugin.lang.SandLanguage;
import consulo.sandboxPlugin.lang.psi.SandClass;

/**
 * @author VISTALL
 * @since 2026-11-10
 */
@ExtensionImpl
public class SandRefactoringSupportProvider extends RefactoringSupportProvider {
    @Override
    public boolean isSafeDeleteAvailable(PsiElement element) {
        return element instanceof SandClass;
    }

    @Override
    public Language getLanguage() {
        return SandLanguage.INSTANCE;
    }
}
