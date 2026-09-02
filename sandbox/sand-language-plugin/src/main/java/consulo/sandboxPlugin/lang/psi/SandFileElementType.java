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
package consulo.sandboxPlugin.lang.psi;

import consulo.annotation.access.RequiredReadAction;
import consulo.language.Language;
import consulo.language.ast.ASTNode;
import consulo.language.file.FileViewProvider;
import consulo.language.parser.ParserDefinition;
import consulo.language.parser.PsiBuilder;
import consulo.language.parser.PsiBuilderFactory;
import consulo.language.parser.PsiParser;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.stub.IStubFileElementType;
import consulo.language.psi.stub.IndexingDataKeys;
import consulo.language.psi.stub.PsiFileStub;
import consulo.language.version.LanguageVersion;
import consulo.project.Project;
import consulo.sandboxPlugin.lang.SandLanguage;
import consulo.sandboxPlugin.lang.moduleAware.SandSeedEnv;
import consulo.sandboxPlugin.lang.parser.SandBuilderWrapper;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.light.LightVirtualFileBase;
import org.jspecify.annotations.Nullable;

/**
 * The parse entry seeds the preprocessor variables (module flags + include-derived entry
 * environments) into the builder — the consulo-csharp {@code CSharpFileStubElementType} shape.
 * Disabled branches never become real tokens; the same seeded tree serves the editor and the
 * stubs, so a seed change is a reparse + reindex of the file.
 */
public class SandFileElementType extends IStubFileElementType<PsiFileStub<PsiFile>> {
  public static final SandFileElementType INSTANCE = new SandFileElementType();

  private SandFileElementType() {
    super(SandLanguage.INSTANCE);
  }

  /**
   * Any grammar or stub-content change must bump this — persisted stub trees from an
   * older parse otherwise reconcile against the new AST and bind PSI to wrong
   * declarations (headless tests never see it: they always index fresh).
   */
  @Override
  public int getStubVersion() {
    return 8;
  }

  @RequiredReadAction
  @Override
  protected ASTNode doParseContents(ASTNode chameleon, PsiElement psi) {
    Project project = psi.getProject();
    Language languageForParser = getLanguageForParser(psi);
    LanguageVersion tempLanguageVersion = chameleon.getUserData(LanguageVersion.KEY);
    LanguageVersion languageVersion = tempLanguageVersion == null ? psi.getLanguageVersion() : tempLanguageVersion;

    PsiBuilder builder =
      PsiBuilderFactory.getInstance().createBuilder(project, chameleon, null, languageForParser, languageVersion, chameleon.getChars());
    builder.putUserData(SandBuilderWrapper.SEED_VARIABLES, SandSeedEnv.seedFor(project, stableFile(psi)));

    PsiParser parser = ParserDefinition.forLanguage(languageForParser).createParser(languageVersion);
    return parser.parse(this, builder, languageVersion).getFirstChildNode();
  }

  @RequiredReadAction
  private static @Nullable VirtualFile stableFile(PsiElement psi) {
    PsiFile psiFile = psi.getContainingFile();
    FileViewProvider viewProvider = psiFile.getViewProvider();
    VirtualFile virtualFile = viewProvider.getVirtualFile();
    if (virtualFile instanceof LightVirtualFileBase lightVirtualFile) {
      virtualFile = lightVirtualFile.getOriginalFile();
      if (virtualFile instanceof LightVirtualFileBase innerLight) {
        virtualFile = innerLight.getOriginalFile();
      }
    }
    if (virtualFile == null) {
      virtualFile = psi.getUserData(IndexingDataKeys.VIRTUAL_FILE);
    }
    return virtualFile;
  }
}
