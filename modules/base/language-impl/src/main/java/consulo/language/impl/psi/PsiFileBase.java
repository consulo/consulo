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

package consulo.language.impl.psi;

import consulo.language.Language;
import consulo.language.ast.IFileElementType;
import consulo.language.file.FileViewProvider;
import consulo.language.parser.ParserDefinition;
import consulo.language.psi.PsiElementVisitor;
import consulo.virtualFileSystem.fileType.FileType;

import jakarta.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Set;

/**
 * @author max
 */
public abstract class PsiFileBase extends PsiFileImpl {
  @Nonnull
  private final Language myLanguage;
  @Nonnull
  private final ParserDefinition myParserDefinition;

  protected PsiFileBase(@Nonnull FileViewProvider viewProvider, @Nonnull Language language) {
    super(viewProvider);
    myLanguage = findLanguage(language, viewProvider);
    ParserDefinition parserDefinition = ParserDefinition.forLanguage(myLanguage);
    if (parserDefinition == null) {
      throw new RuntimeException("PsiFileBase: language.getParserDefinition() returned null for: "+myLanguage);
    }
    myParserDefinition = parserDefinition;
    IFileElementType nodeType = parserDefinition.getFileNodeType();
    assert nodeType.getLanguage() == myLanguage: nodeType.getLanguage() + " != " + myLanguage;
    init(nodeType, nodeType);
  }

  private static Language findLanguage(Language baseLanguage, FileViewProvider viewProvider) {
    Set<Language> languages = viewProvider.getLanguages();
    for (Language actualLanguage : languages) {
      if (actualLanguage.isKindOf(baseLanguage)) {
        return actualLanguage;
      }
    }
    throw new AssertionError(
        "Language " + baseLanguage + " doesn't participate in view provider " + viewProvider + ": " + new ArrayList<Language>(languages));
  }

  @Nonnull
  @Override
  public FileType getFileType() {
    return getViewProvider().getFileType();
  }

  @Override
  @Nonnull
  public final Language getLanguage() {
    return myLanguage;
  }

  @Override
  public void accept(@Nonnull PsiElementVisitor visitor) {
    visitor.visitFile(this);
  }

  @Nonnull
  public ParserDefinition getParserDefinition() {
    return myParserDefinition;
  }
}
