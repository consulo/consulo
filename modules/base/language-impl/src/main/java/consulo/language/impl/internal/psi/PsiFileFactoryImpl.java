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

import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.language.Language;
import consulo.language.ast.ASTNode;
import consulo.language.ast.IElementType;
import consulo.language.file.FileViewProvider;
import consulo.language.file.FileViewProviderFactory;
import consulo.language.file.LanguageFileType;
import consulo.language.file.LanguageFileViewProviderFactory;
import consulo.language.file.light.LightVirtualFile;
import consulo.language.impl.ast.FileElement;
import consulo.language.impl.ast.TreeElement;
import consulo.language.impl.file.SingleRootFileViewProvider;
import consulo.language.impl.plain.PsiPlainTextFileImpl;
import consulo.language.impl.psi.CodeEditUtil;
import consulo.language.impl.psi.DummyHolder;
import consulo.language.impl.psi.DummyHolderFactory;
import consulo.language.lexer.Lexer;
import consulo.language.parser.ParserDefinition;
import consulo.language.parser.PsiBuilder;
import consulo.language.parser.PsiBuilderFactory;
import consulo.language.psi.*;
import consulo.language.version.LanguageVersion;
import consulo.language.version.LanguageVersionUtil;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.util.lang.CharSequenceSubSequence;
import consulo.util.lang.LocalTimeCounter;
import consulo.virtualFileSystem.RawFileLoaderHelper;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.fileType.FileType;
import consulo.virtualFileSystem.fileType.FileTypeRegistry;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

/**
 * @author max
 */
@Singleton
@ServiceImpl
public class PsiFileFactoryImpl implements PsiFileFactory {
  private static final Logger LOG = Logger.getInstance(PsiFileFactoryImpl.class);

  private final Application myApplication;
  private final PsiManager myManager;

  @Inject
  public PsiFileFactoryImpl(Application application, final PsiManager manager) {
      myApplication = application;
      myManager = manager;
  }

  @Override
  @Nonnull
  public PsiFile createFileFromText(@Nonnull String name,
                                    @Nonnull FileType fileType,
                                    @Nonnull CharSequence text,
                                    long modificationStamp,
                                    final boolean physical) {
    return createFileFromText(name, fileType, text, modificationStamp, physical, true);
  }

  @Override
  public PsiFile createFileFromText(@Nonnull String name, @Nonnull Language language, @Nonnull CharSequence text) {
    return createFileFromText(name, language, text, true, true);
  }

  @Override
  public PsiFile createFileFromText(@Nonnull String name,
                                    @Nonnull Language language,
                                    @Nonnull LanguageVersion languageVersion,
                                    @Nonnull CharSequence text) {
    return createFileFromText(name, language, languageVersion, text, true, true, false);
  }

  @Override
  public PsiFile createFileFromText(@Nonnull String name,
                                    @Nonnull Language language,
                                    @Nonnull CharSequence text,
                                    boolean physical,
                                    final boolean markAsCopy) {
    return createFileFromText(name, language, text, physical, markAsCopy, false);
  }

  @Override
  public PsiFile createFileFromText(@Nonnull String name,
                                    @Nonnull Language language,
                                    @Nonnull CharSequence text,
                                    boolean physical,
                                    boolean markAsCopy,
                                    boolean noSizeLimit) {
    return createFileFromText(name, language, LanguageVersionUtil.findDefaultVersion(language),
                              text, physical, markAsCopy, noSizeLimit);
  }

  @Nullable
  @Override
  public PsiFile createFileFromText(@Nonnull String name,
                                    @Nonnull Language language,
                                    @Nonnull LanguageVersion languageVersion,
                                    @Nonnull CharSequence text,
                                    boolean physical,
                                    boolean markAsCopy,
                                    boolean noSizeLimit) {
    LightVirtualFile virtualFile = new LightVirtualFile(name, language, text);
    virtualFile.putUserData(LanguageVersion.KEY, languageVersion);
    if (noSizeLimit) {
      RawFileLoaderHelper.doNotCheckFileSizeLimit(virtualFile);
    }
    return trySetupPsiForFile(virtualFile, language, languageVersion, physical, markAsCopy);
  }

  @Nullable
  @Override
  public PsiFile createFileFromText(@Nonnull String name,
                                    @Nonnull LanguageVersion languageVersion,
                                    @Nonnull CharSequence text,
                                    boolean physical,
                                    boolean markAsCopy,
                                    boolean noSizeLimit,
                                    @Nullable VirtualFile original) {
    LightVirtualFile virtualFile = new LightVirtualFile(name, languageVersion.getLanguage(), text);
    if (original != null) virtualFile.setOriginalFile(original);
    virtualFile.putUserData(LanguageVersion.KEY, languageVersion);
    if (noSizeLimit) {
      RawFileLoaderHelper.doNotCheckFileSizeLimit(virtualFile);
    }
    return trySetupPsiForFile(virtualFile, languageVersion.getLanguage(), languageVersion, physical, markAsCopy);
  }

  @Override
  @Nonnull
  public PsiFile createFileFromText(@Nonnull String name,
                                    @Nonnull FileType fileType,
                                    @Nonnull CharSequence text,
                                    long modificationStamp,
                                    final boolean physical,
                                    boolean markAsCopy) {
    final LightVirtualFile virtualFile = new LightVirtualFile(name, fileType, text, modificationStamp);
    if (fileType instanceof LanguageFileType) {
      final Language language =
        LanguageSubstitutors.substituteLanguage(((LanguageFileType)fileType).getLanguage(), virtualFile, myManager.getProject());
      final PsiFile file = trySetupPsiForFile(virtualFile, language, LanguageVersionUtil.findDefaultVersion(language), physical, markAsCopy);
      if (file != null) return file;
    }
    final SingleRootFileViewProvider singleRootFileViewProvider = new SingleRootFileViewProvider(myManager, virtualFile, physical);
    final PsiPlainTextFileImpl plainTextFile = new PsiPlainTextFileImpl(singleRootFileViewProvider);
    if (markAsCopy) CodeEditUtil.setNodeGenerated(plainTextFile.getNode(), true);
    return plainTextFile;
  }

  @Nullable
  public PsiFile trySetupPsiForFile(final LightVirtualFile virtualFile,
                                    @Nonnull Language language,
                                    @Nonnull LanguageVersion languageVersion,
                                    final boolean physical,
                                    final boolean markAsCopy) {
    final FileViewProviderFactory factory = LanguageFileViewProviderFactory.forLanguage(language);
    FileViewProvider viewProvider = factory != null ? factory.createFileViewProvider(virtualFile, language, myManager, physical) : null;
    if (viewProvider == null) viewProvider = new SingleRootFileViewProvider(myManager, virtualFile, physical);

    language = viewProvider.getBaseLanguage();
    final ParserDefinition parserDefinition = ParserDefinition.forLanguage(myApplication, language);
    if (parserDefinition != null) {
      final PsiFile psiFile = viewProvider.getPsi(language);
      if (psiFile != null) {
        psiFile.putUserData(LanguageVersion.KEY, languageVersion);
        if (markAsCopy) {
          markGenerated(psiFile);
        }
        return psiFile;
      }
    }
    return null;
  }

  @Nonnull
  public PsiFile createFileFromText(@Nonnull String name,
                                    @Nonnull FileType fileType,
                                    final Language language,
                                    @Nonnull Language targetLanguage,
                                    @Nonnull CharSequence text,
                                    long modificationStamp,
                                    final boolean physical,
                                    boolean markAsCopy) {
    final LightVirtualFile virtualFile = new LightVirtualFile(name, fileType, text, modificationStamp);

    final ParserDefinition parserDefinition = ParserDefinition.forLanguage(myApplication, language);
    final FileViewProviderFactory factory = LanguageFileViewProviderFactory.forLanguage(language);
    FileViewProvider viewProvider = factory != null ? factory.createFileViewProvider(virtualFile, language, myManager, physical) : null;
    if (viewProvider == null) viewProvider = new SingleRootFileViewProvider(myManager, virtualFile, physical);

    if (parserDefinition != null) {
      final PsiFile psiFile = viewProvider.getPsi(targetLanguage);
      if (psiFile != null) {
        if (markAsCopy) {
          markGenerated(psiFile);
        }
        return psiFile;
      }
    }

    final SingleRootFileViewProvider singleRootFileViewProvider = new SingleRootFileViewProvider(myManager, virtualFile, physical);
    final PsiPlainTextFileImpl plainTextFile = new PsiPlainTextFileImpl(singleRootFileViewProvider);
    if (markAsCopy) CodeEditUtil.setNodeGenerated(plainTextFile.getNode(), true);
    return plainTextFile;
  }

  @Override
  @Nonnull
  public PsiFile createFileFromText(@Nonnull String name, @Nonnull FileType fileType, @Nonnull CharSequence text) {
    return createFileFromText(name, fileType, text, LocalTimeCounter.currentTime(), false);
  }

  @Override
  @Nonnull
  public PsiFile createFileFromText(@Nonnull String name, @Nonnull String text) {
    FileType type = FileTypeRegistry.getInstance().getFileTypeByFileName(name);
    if (type.isBinary()) {
      throw new RuntimeException("Cannot create binary files from text: name " + name + ", file type " + type);
    }

    return createFileFromText(name, type, text);
  }

  @Override
  public PsiFile createFileFromText(FileType fileType, final String fileName, CharSequence chars, int startOffset, int endOffset) {
    LOG.assertTrue(!fileType.isBinary());
    final CharSequence text =
      startOffset == 0 && endOffset == chars.length() ? chars : new CharSequenceSubSequence(chars, startOffset, endOffset);
    return createFileFromText(fileName, fileType, text);
  }

  @Nullable
  @Override
  public PsiFile createFileFromText(@Nonnull CharSequence chars, @Nonnull PsiFile original) {
    final PsiFile file = createFileFromText(original.getName(), original.getLanguage(), chars, false, true);
    if (file != null) {
      file.putUserData(ORIGINAL_FILE, original);
    }
    return file;
  }

  @Override
  @Nullable
  public PsiElement createElementFromText(@Nullable final String text,
                                          @Nonnull final Language language,
                                          @Nonnull final LanguageVersion languageVersion,
                                          @Nonnull final IElementType type,
                                          @Nullable final PsiElement context) {
    if (text == null) return null;
    final DummyHolder result = DummyHolderFactory.createHolder(myManager, language, context);
    final FileElement holder = result.getTreeElement();

    final ParserDefinition parserDefinition = ParserDefinition.forLanguage(myApplication, language);
    if (parserDefinition == null) {
      throw new AssertionError("No parser definition for " + language);
    }
    final Project project = myManager.getProject();
    final Lexer lexer = parserDefinition.createLexer(languageVersion);
    final PsiBuilder builder = PsiBuilderFactory.getInstance().createBuilder(project, holder, lexer, language, languageVersion, text);
    final ASTNode node = parserDefinition.createParser(languageVersion).parse(type, builder, languageVersion);
    holder.rawAddChildren((TreeElement)node);
    markGenerated(result);
    return node.getPsi();
  }


  public static void markGenerated(PsiElement element) {
    final TreeElement node = (TreeElement)element.getNode();
    assert node != null;
    node.acceptTree(new GeneratedMarkerVisitor());
  }
}
