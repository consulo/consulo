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

package consulo.language.editor.template.context;

import consulo.language.editor.highlight.SyntaxHighlighter;
import consulo.language.editor.highlight.SyntaxHighlighterFactory;
import consulo.language.file.LanguageFileType;

import consulo.localize.LocalizeValue;
import jakarta.annotation.Nonnull;

/**
 * @author lesya
 */
public abstract class FileTypeBasedContextType extends BaseTemplateContextType {
  private final LanguageFileType myFileType;

  protected FileTypeBasedContextType(@Nonnull String id, @Nonnull LocalizeValue presentableName, @Nonnull LanguageFileType fileType) {
    super(id, presentableName);
    myFileType = fileType;
  }

  @Override
  public boolean isInContext(@Nonnull TemplateActionContext templateActionContext) {
    return myFileType == templateActionContext.getFile().getFileType();
  }

  @Override
  public SyntaxHighlighter createHighlighter() {
    return SyntaxHighlighterFactory.getSyntaxHighlighter(myFileType, null, null);
  }
}
