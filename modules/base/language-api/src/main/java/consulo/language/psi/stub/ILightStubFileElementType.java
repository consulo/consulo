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
package consulo.language.psi.stub;

import consulo.language.Language;
import consulo.language.ast.ASTNode;
import consulo.language.ast.LighterASTNode;
import consulo.language.parser.*;
import consulo.language.psi.PsiElement;
import consulo.language.util.FlyweightCapableTreeStructure;
import consulo.language.version.LanguageVersion;
import consulo.project.Project;

public class ILightStubFileElementType<T extends PsiFileStub> extends IStubFileElementType<T> {
  public ILightStubFileElementType(Language language) {
    super(language);
  }

  public ILightStubFileElementType(String debugName, Language language) {
    super(debugName, language);
  }

  @Override
  public LightStubBuilder getBuilder() {
    return new LightStubBuilder();
  }

  public FlyweightCapableTreeStructure<LighterASTNode> parseContentsLight(ASTNode chameleon) {
    PsiElement psi = chameleon.getPsi();
    assert psi != null : "Bad chameleon: " + chameleon;

    Project project = psi.getProject();
    PsiBuilderFactory factory = PsiBuilderFactory.getInstance();
    Language language = getLanguage();
    LanguageVersion languageVersion = psi.getLanguageVersion();
    PsiBuilder builder = factory.createBuilder(project, chameleon, languageVersion);
    ParserDefinition parserDefinition = ParserDefinition.forLanguage(language);
    assert parserDefinition != null : this;
    PsiParser parser = parserDefinition.createParser(languageVersion);
    parser.parse(this, builder, languageVersion);
    return builder.getLightTree();
  }
}