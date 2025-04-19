/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

package consulo.language.editor.testIntegration;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.application.Application;
import consulo.codeEditor.Editor;
import consulo.component.extension.ExtensionPoint;
import consulo.component.extension.ExtensionPointCacheKey;
import consulo.language.Language;
import consulo.language.extension.ByLanguageValue;
import consulo.language.extension.LanguageExtension;
import consulo.language.extension.LanguageOneToOne;
import consulo.language.psi.PsiFile;
import consulo.project.Project;

import jakarta.annotation.Nullable;

@ExtensionAPI(ComponentScope.APPLICATION)
public interface TestCreator extends LanguageExtension {
    ExtensionPointCacheKey<TestCreator, ByLanguageValue<TestCreator>> KEY =
        ExtensionPointCacheKey.create("TestCreator", LanguageOneToOne.build());

    @Nullable
    static TestCreator forLanguage(Language language) {
        ExtensionPoint<TestCreator> extensionPoint = Application.get().getExtensionPoint(TestCreator.class);
        ByLanguageValue<TestCreator> map = extensionPoint.getOrBuildCache(KEY);
        return map.get(language);
    }

    boolean isAvailable(Project project, Editor editor, PsiFile file);

    void createTest(Project project, Editor editor, PsiFile file);
}
