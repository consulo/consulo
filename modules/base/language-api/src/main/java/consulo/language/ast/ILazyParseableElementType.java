/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package consulo.language.ast;

import consulo.language.Language;
import consulo.language.parser.ParserDefinition;
import consulo.language.parser.PsiBuilder;
import consulo.language.parser.PsiBuilderFactory;
import consulo.language.parser.PsiParser;
import consulo.language.psi.PsiElement;
import consulo.language.version.LanguageVersion;
import consulo.project.Project;
import consulo.util.dataholder.Key;

import org.jspecify.annotations.Nullable;

/**
 * A token type which represents a fragment of text (possibly in a different language)
 * which is not parsed during the current lexer or parser pass and can be parsed later when
 * its contents is requested.
 *
 * @author max
 */
public class ILazyParseableElementType extends IElementType implements ILazyParseableElementTypeBase {

  public static final Key<Language> LANGUAGE_KEY = Key.create("LANGUAGE_KEY");

  public ILazyParseableElementType(String debugName) {
    this(debugName, null);
  }

  public ILazyParseableElementType(String debugName, @Nullable Language language) {
    super(debugName, language);
  }

  public ILazyParseableElementType(String debugName, @Nullable Language language, boolean register) {
    super(debugName, language, register);
  }

  /**
   * Parses the contents of the specified chameleon node and returns the AST tree
   * representing the parsed contents.
   *
   * @param chameleon the node to parse.
   * @return the parsed contents of the node.
   */
  public ASTNode parseContents(ASTNode chameleon) {
    PsiElement parentElement = chameleon.getTreeParent().getPsi();
    assert parentElement != null : "parent psi is null: " + chameleon;
    return doParseContents(chameleon, parentElement);
  }

  protected ASTNode doParseContents(ASTNode chameleon, PsiElement psi) {
    Project project = psi.getProject();
    Language languageForParser = getLanguageForParser(psi);
    LanguageVersion tempLanguageVersion = chameleon.getUserData(LanguageVersion.KEY);
    LanguageVersion languageVersion = tempLanguageVersion == null ? psi.getLanguageVersion() : tempLanguageVersion;
    PsiBuilder builder = PsiBuilderFactory.getInstance().createBuilder(project, chameleon, null, languageForParser, languageVersion, chameleon.getChars());
    PsiParser parser = ParserDefinition.forLanguage(languageForParser).createParser(languageVersion);
    return parser.parse(this, builder, languageVersion).getFirstChildNode();
  }

  protected Language getLanguageForParser(PsiElement psi) {
    return getLanguage();
  }

  public @Nullable ASTNode createNode(CharSequence text) {
    return null;
  }

  @Override
  public boolean reuseCollapsedTokens() {
    return false;
  }
}
