/*
 * Copyright 2013-2016 consulo.io
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
package consulo.sandboxPlugin.lang.version;

import consulo.language.parser.PsiParser;
import consulo.virtualFileSystem.fileType.FileType;
import consulo.project.Project;
import consulo.ide.impl.idea.openapi.util.NotNullLazyValue;
import consulo.util.lang.Pair;
import consulo.virtualFileSystem.VirtualFile;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.ast.IElementType;
import consulo.language.ast.TokenSet;
import consulo.language.version.LanguageVersion;
import consulo.language.version.LanguageVersionWithDefinition;
import consulo.language.version.LanguageVersionWithParsing;
import consulo.sandboxPlugin.lang.SandLanguage;
import consulo.sandboxPlugin.lang.parser.SandParser;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * @author VISTALL
 * @since 19.03.14
 */
public abstract class BaseSandLanguageVersion extends LanguageVersion implements LanguageVersionWithDefinition, LanguageVersionWithParsing {

  private NotNullLazyValue<List<Pair<IElementType, IElementType>>> myValue = new NotNullLazyValue<List<Pair<IElementType, IElementType>>>() {
    @Nonnull
    @Override
    protected List<Pair<IElementType, IElementType>> compute() {
      return createList();
    }
  };
  private NotNullLazyValue<TokenSet> myHighlightKeywords = new NotNullLazyValue<TokenSet>() {
    @Nonnull
    @Override
    protected TokenSet compute() {
      List<Pair<IElementType, IElementType>> value = myValue.getValue();
      List<IElementType> list = new ArrayList<IElementType>(value.size());
      for (Pair<IElementType, IElementType> pair : value) {
        list.add(pair.getFirst());
      }
      return TokenSet.create(list.toArray(new IElementType[list.size()]));
    }
  };

  public BaseSandLanguageVersion(String name) {
    super(name, name, SandLanguage.INSTANCE);
  }

  @Nonnull
  public TokenSet getHighlightKeywords() {
    return myHighlightKeywords.getValue();
  }

  protected abstract List<Pair<IElementType, IElementType>> createList();

  public abstract FileType getFileType();

  @Nonnull
  @Override
  public PsiParser createParser() {
    return new SandParser(myValue.getValue());
  }

  @Override
  public boolean isMyElement(@Nullable PsiElement element) {
    if (element == null) {
      return false;
    }
    PsiFile containingFile = element.getContainingFile();
    if (containingFile == null) {
      return false;
    }
    VirtualFile virtualFile = containingFile.getVirtualFile();
    if (virtualFile == null) {
      return false;
    }
    return isMyFile(element.getProject(), virtualFile);
  }

  @Override
  public boolean isMyFile(@javax.annotation.Nullable Project project, @javax.annotation.Nullable VirtualFile virtualFile) {
    if (virtualFile == null) {
      return false;
    }
    return virtualFile.getFileType() == getFileType();
  }
}
