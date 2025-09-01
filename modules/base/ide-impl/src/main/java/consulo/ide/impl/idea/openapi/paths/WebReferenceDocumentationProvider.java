/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package consulo.ide.impl.idea.openapi.paths;

import consulo.annotation.component.ExtensionImpl;
import consulo.ide.localize.IdeLocalize;
import consulo.language.editor.documentation.UnrestrictedDocumentationProvider;
import consulo.language.impl.psi.path.WebReference;
import consulo.language.psi.PsiElement;

/**
 * @author Eugene.Kudelevsky
 */
@ExtensionImpl
public class WebReferenceDocumentationProvider implements UnrestrictedDocumentationProvider {
    @Override
    public String getQuickNavigateInfo(PsiElement element, PsiElement originalElement) {
        if (element instanceof WebReference.MyFakePsiElement) {
            return IdeLocalize.openUrlInBrowserTooltip().get();
        }
        return null;
    }
}
