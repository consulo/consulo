/*
 * Copyright 2013-2022 consulo.io
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
package consulo.language.editor;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.application.Application;
import consulo.codeEditor.Editor;
import consulo.codeEditor.LineWrapPositionStrategy;
import consulo.component.extension.ExtensionPointCacheKey;
import consulo.language.Language;
import consulo.language.editor.internal.LanguageDefaultLineWrapPositionStrategy;
import consulo.language.extension.ByLanguageValue;
import consulo.language.extension.LanguageExtension;
import consulo.language.extension.LanguageOneToOne;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiFile;
import consulo.project.Project;
import consulo.util.lang.ObjectUtil;

import jakarta.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 01-Aug-22
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public interface LanguageLineWrapPositionStrategy extends LineWrapPositionStrategy, LanguageExtension {
  ExtensionPointCacheKey<LanguageLineWrapPositionStrategy, ByLanguageValue<LanguageLineWrapPositionStrategy>> KEY =
          ExtensionPointCacheKey.create("LanguageLineWrapPositionStrategy", LanguageOneToOne.build(LanguageDefaultLineWrapPositionStrategy.INSTANCE));

  @Nonnull
  static LanguageLineWrapPositionStrategy forLanguage(@Nonnull Language language) {
    return Application.get().getExtensionPoint(LanguageLineWrapPositionStrategy.class).getOrBuildCache(KEY).requiredGet(language);
  }

  /**
   * Asks to get wrap position strategy to use for the document managed by the given editor.
   *
   * @param editor editor that manages document which text should be processed by wrap position strategy
   * @return line wrap position strategy to use for the lines from the document managed by the given editor
   */
  @Nonnull
  @RequiredReadAction
  static LineWrapPositionStrategy forEditor(@Nonnull Editor editor) {
    LineWrapPositionStrategy result = null;
    Project project = editor.getProject();
    if (project != null && !project.isDisposed()) {
      PsiFile psiFile = PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument());
      if (psiFile != null) {
        result = forLanguage(psiFile.getLanguage());
      }
    }
    return ObjectUtil.notNull(result, getDefaultImplementation());
  }

  @Nonnull
  static LineWrapPositionStrategy getDefaultImplementation() {
    return LanguageDefaultLineWrapPositionStrategy.INSTANCE;
  }
}
