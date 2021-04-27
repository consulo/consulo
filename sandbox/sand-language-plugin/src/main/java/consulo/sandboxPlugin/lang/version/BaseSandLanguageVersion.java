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

import com.intellij.lang.PsiParser;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.NotNullLazyValue;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;
import consulo.lang.LanguageVersion;
import consulo.lang.LanguageVersionWithDefinition;
import consulo.lang.LanguageVersionWithParsing;
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
