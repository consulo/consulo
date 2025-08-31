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

package consulo.language.impl.internal.psi;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ServiceImpl;
import consulo.language.Commenter;
import consulo.language.Language;
import consulo.language.ast.TokenType;
import consulo.language.file.LanguageFileType;
import consulo.language.impl.ast.ASTFactory;
import consulo.language.impl.ast.FileElement;
import consulo.language.impl.ast.LeafElement;
import consulo.language.impl.ast.TreeElement;
import consulo.language.impl.psi.CodeEditUtil;
import consulo.language.impl.psi.DummyHolderFactory;
import consulo.language.impl.psi.SourceTreeToPsiMap;
import consulo.language.psi.*;
import consulo.language.util.IncorrectOperationException;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import jakarta.annotation.Nonnull;

/**
 * @author yole
 */
@Singleton
@ServiceImpl
public class PsiParserFacadeImpl implements PsiParserFacade {
  private final PsiManager myPsiManager;
  private final PsiFileFactory myPsiFileFactory;

  @Inject
  public PsiParserFacadeImpl(PsiManager psiManager, PsiFileFactory psiFileFactory) {
    myPsiManager = psiManager;
    myPsiFileFactory = psiFileFactory;
  }

  @Override
  @Nonnull
  public PsiElement createWhiteSpaceFromText(@Nonnull String text) throws IncorrectOperationException {
    FileElement holderElement = DummyHolderFactory.createHolder(myPsiManager, null).getTreeElement();
    LeafElement newElement = ASTFactory.leaf(TokenType.WHITE_SPACE, holderElement.getCharTable().intern(text));
    holderElement.rawAddChildren(newElement);
    CodeEditUtil.markGenerated(newElement);
    return newElement.getPsi();
  }

  @Override
  @Nonnull
  public PsiComment createLineCommentFromText(@Nonnull LanguageFileType fileType, @Nonnull String text) throws IncorrectOperationException {
    Commenter commenter = Commenter.forLanguage(fileType.getLanguage());
    assert commenter != null;
    String prefix = commenter.getLineCommentPrefix();
    if (prefix == null) {
      throw new IncorrectOperationException("No line comment prefix defined for language " + fileType.getLanguage().getID());
    }

    PsiFile aFile = createDummyFile(prefix + text, fileType);
    return findPsiCommentChild(aFile);
  }

  @Nonnull
  @Override
  public PsiComment createBlockCommentFromText(@Nonnull Language language, @Nonnull String text) throws IncorrectOperationException {
    Commenter commenter = Commenter.forLanguage(language);
    assert commenter != null : language;
    String blockCommentPrefix = commenter.getBlockCommentPrefix();
    String blockCommentSuffix = commenter.getBlockCommentSuffix();

    PsiFile aFile = myPsiFileFactory.createFileFromText("_Dummy_", language, (blockCommentPrefix + text + blockCommentSuffix));
    return findPsiCommentChild(aFile);
  }

  @Override
  @Nonnull
  public PsiComment createLineOrBlockCommentFromText(@Nonnull Language lang, @Nonnull String text) throws IncorrectOperationException {
    Commenter commenter = Commenter.forLanguage(lang);
    assert commenter != null : lang;
    String prefix = commenter.getLineCommentPrefix();
    String blockCommentPrefix = commenter.getBlockCommentPrefix();
    String blockCommentSuffix = commenter.getBlockCommentSuffix();
    assert prefix != null || (blockCommentPrefix != null && blockCommentSuffix != null);

    PsiFile aFile = myPsiFileFactory.createFileFromText("_Dummy_", lang, prefix != null ? (prefix + text) : (blockCommentPrefix + text + blockCommentSuffix));
    return findPsiCommentChild(aFile);
  }

  @RequiredReadAction
  private PsiComment findPsiCommentChild(PsiFile aFile) {
    PsiElement[] children = aFile.getChildren();
    for (PsiElement aChildren : children) {
      if (aChildren instanceof PsiComment) {
        PsiComment comment = (PsiComment)aChildren;
        DummyHolderFactory.createHolder(myPsiManager, (TreeElement)SourceTreeToPsiMap.psiElementToTree(comment), null);
        return comment;
      }
    }
    throw new IncorrectOperationException("Incorrect comment \"" + aFile.getText() + "\".");
  }

  protected PsiFile createDummyFile(String text, LanguageFileType fileType) {
    String ext = fileType.getDefaultExtension();
    String fileName = "_Dummy_." + ext;

    return myPsiFileFactory.createFileFromText(fileType, fileName, text, 0, text.length());
  }
}
