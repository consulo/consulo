// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.impl.psi.template;

import consulo.annotation.access.RequiredReadAction;
import consulo.application.progress.ProgressManager;
import consulo.document.util.TextRange;
import consulo.language.Language;
import consulo.language.ast.*;
import consulo.language.file.FileViewProvider;
import consulo.language.file.LanguageFileType;
import consulo.language.file.light.LightVirtualFile;
import consulo.language.impl.DebugUtil;
import consulo.language.impl.ast.FileElement;
import consulo.language.impl.ast.SharedImplUtil;
import consulo.language.impl.ast.TreeElement;
import consulo.language.impl.ast.TreeUtil;
import consulo.language.impl.file.SingleRootFileViewProvider;
import consulo.language.impl.psi.PsiFileImpl;
import consulo.language.lexer.Lexer;
import consulo.language.parser.ParserDefinition;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiManager;
import consulo.language.template.ITemplateDataElementType;
import consulo.language.template.TemplateLanguageFileViewProvider;
import consulo.language.util.CharTable;
import consulo.language.version.LanguageVersion;
import consulo.language.version.LanguageVersionResolver;
import consulo.localize.LocalizeValue;
import consulo.ui.image.Image;
import consulo.util.lang.LocalTimeCounter;
import consulo.util.lang.StringUtil;
import consulo.util.lang.lazy.LazyValue;
import consulo.util.lang.reflect.ReflectionUtil;

import jakarta.annotation.Nonnull;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * @author peter
 */
public class TemplateDataElementType extends IFileElementType implements ITemplateDataElementType {
  private static final int CHECK_PROGRESS_AFTER_TOKENS = 1000;

  @Nonnull
  private final IElementType myTemplateElementType;
  @Nonnull
  final IElementType myOuterElementType;

  public TemplateDataElementType(String debugName, Language language, @Nonnull IElementType templateElementType, @Nonnull IElementType outerElementType) {
    super(debugName, language);
    myTemplateElementType = templateElementType;
    myOuterElementType = outerElementType;
  }

  @RequiredReadAction
  @Nonnull
  protected Lexer createBaseLexer(PsiFile file, TemplateLanguageFileViewProvider viewProvider) {
    Language baseLanguage = viewProvider.getBaseLanguage();
    LanguageVersion languageVersion = LanguageVersionResolver.forLanguage(baseLanguage).getLanguageVersion(baseLanguage, file);
    return ParserDefinition.forLanguage(viewProvider.getBaseLanguage()).createLexer(languageVersion);
  }

  protected LanguageFileType createTemplateFakeFileType(Language language) {
    return new TemplateFileType(language);
  }

  @Override
  public ASTNode parseContents(@Nonnull ASTNode chameleon) {
    CharTable charTable = SharedImplUtil.findCharTableByTree(chameleon);
    FileElement fileElement = TreeUtil.getFileElement((TreeElement)chameleon);
    PsiFile psiFile = (PsiFile)fileElement.getPsi();
    PsiFile originalPsiFile = psiFile.getOriginalFile();

    TemplateLanguageFileViewProvider viewProvider = (TemplateLanguageFileViewProvider)originalPsiFile.getViewProvider();

    Language templateLanguage = getTemplateFileLanguage(viewProvider);
    CharSequence sourceCode = chameleon.getChars();

    RangeCollectorImpl collector = new RangeCollectorImpl(this);
    PsiFile templatePsiFile = createTemplateFile(psiFile, templateLanguage, sourceCode, viewProvider, collector);
    FileElement templateFileElement = ((PsiFileImpl)templatePsiFile).calcTreeElement();

    return DebugUtil.performPsiModification("template language parsing", () -> {
      collector.insertOuterElementsAndRemoveRanges(templateFileElement, sourceCode, charTable, templateFileElement.getPsi().getLanguage());

      TreeElement childNode = templateFileElement.getFirstChildNode();

      DebugUtil.checkTreeStructure(templateFileElement);
      DebugUtil.checkTreeStructure(chameleon);
      if (fileElement != chameleon) {
        DebugUtil.checkTreeStructure(psiFile.getNode());
        DebugUtil.checkTreeStructure(originalPsiFile.getNode());
      }

      return childNode;
    });
  }

  protected Language getTemplateFileLanguage(TemplateLanguageFileViewProvider viewProvider) {
    return viewProvider.getTemplateDataLanguage();
  }

  /**
   * Creates psi tree without base language elements. The result PsiFile can contain additional elements.
   * Ranges of the removed tokens/additional elements should be stored in the rangeCollector
   *
   * @param psiFile          chameleon's psi file
   * @param templateLanguage template language to parse
   * @param sourceCode       source code: base language with template language
   * @param rangeCollector   collector for ranges with non-template/additional elements
   * @return template psiFile
   */
  protected PsiFile createTemplateFile(PsiFile psiFile,
                                       Language templateLanguage,
                                       CharSequence sourceCode,
                                       TemplateLanguageFileViewProvider viewProvider,
                                       @Nonnull TemplateDataElementType.RangeCollector rangeCollector) {
    CharSequence templateSourceCode = createTemplateText(sourceCode, createBaseLexer(psiFile, viewProvider), rangeCollector);
    if (rangeCollector instanceof RangeCollectorImpl rc) {
      rc.prepareFileForParsing(templateLanguage, sourceCode, templateSourceCode);
    }
    return createPsiFileFromSource(templateLanguage, templateSourceCode, psiFile.getManager());
  }

  /**
   * Creates source code without template tokens. May add additional pieces of code.
   * Ranges of such additions should be added in rangeCollector using {@link RangeCollector#addRangeToRemove(TextRange)}
   * for later removal from the resulting tree.
   * <p>
   * Consider overriding {@link #collectTemplateModifications(CharSequence, Lexer)} instead.
   *
   * @param sourceCode     source code with base and template languages
   * @param baseLexer      base language lexer
   * @param rangeCollector collector for ranges with non-template/additional symbols
   * @return template source code
   */
  protected CharSequence createTemplateText(@Nonnull CharSequence sourceCode, @Nonnull Lexer baseLexer, @Nonnull TemplateDataElementType.RangeCollector rangeCollector) {
    if (REQUIRES_OLD_CREATE_TEMPLATE_TEXT.get()) {
      return oldCreateTemplateText(sourceCode, baseLexer, rangeCollector);
    }

    TemplateDataModifications modifications = collectTemplateModifications(sourceCode, baseLexer);
    return ((RangeCollectorImpl)rangeCollector).applyTemplateDataModifications(sourceCode, modifications);
  }

  private final Supplier<Boolean> REQUIRES_OLD_CREATE_TEMPLATE_TEXT = LazyValue.notNull(() -> {
    Class<?> implementationClass = ReflectionUtil.getMethodDeclaringClass(getClass(), "appendCurrentTemplateToken", StringBuilder.class, CharSequence.class, Lexer.class, RangeCollector.class);
    return implementationClass != TemplateDataElementType.class;
  });

  private CharSequence oldCreateTemplateText(@Nonnull CharSequence sourceCode, @Nonnull Lexer baseLexer, @Nonnull RangeCollector rangeCollector) {
    StringBuilder result = new StringBuilder(sourceCode.length());
    baseLexer.start(sourceCode);

    TextRange currentRange = TextRange.EMPTY_RANGE;
    int tokenCounter = 0;
    while (baseLexer.getTokenType() != null) {
      if (++tokenCounter % CHECK_PROGRESS_AFTER_TOKENS == 0) {
        ProgressManager.checkCanceled();
      }
      TextRange newRange = TextRange.create(baseLexer.getTokenStart(), baseLexer.getTokenEnd());
      assert currentRange.getEndOffset() == newRange.getStartOffset() : "Inconsistent tokens stream from " +
                                                                        baseLexer +
                                                                        ": " +
                                                                        getRangeDump(currentRange, sourceCode) +
                                                                        " followed by " +
                                                                        getRangeDump(newRange, sourceCode);
      currentRange = newRange;
      if (baseLexer.getTokenType() == myTemplateElementType) {
        appendCurrentTemplateToken(result, sourceCode, baseLexer, rangeCollector);
      }
      else {
        rangeCollector.addOuterRange(currentRange);
      }
      baseLexer.advance();
    }

    return result;
  }

  /**
   * Collects changes to apply to template source code for later parsing by underlying language.
   *
   * @param sourceCode source code with base and template languages
   * @param baseLexer  base language lexer
   */
  protected
  @Nonnull
  TemplateDataModifications collectTemplateModifications(@Nonnull CharSequence sourceCode, @Nonnull Lexer baseLexer) {
    TemplateDataModifications modifications = new TemplateDataModifications();
    baseLexer.start(sourceCode);
    TextRange currentRange = TextRange.EMPTY_RANGE;
    int tokenCounter = 0;
    while (baseLexer.getTokenType() != null) {
      if (++tokenCounter % CHECK_PROGRESS_AFTER_TOKENS == 0) {
        ProgressManager.checkCanceled();
      }
      TextRange newRange = TextRange.create(baseLexer.getTokenStart(), baseLexer.getTokenEnd());
      assert currentRange.getEndOffset() == newRange.getStartOffset() : "Inconsistent tokens stream from " +
                                                                        baseLexer +
                                                                        ": " +
                                                                        getRangeDump(currentRange, sourceCode) +
                                                                        " followed by " +
                                                                        getRangeDump(newRange, sourceCode);
      currentRange = newRange;
      if (baseLexer.getTokenType() == myTemplateElementType) {
        TemplateDataModifications tokenModifications = appendCurrentTemplateToken(baseLexer.getTokenEnd(), baseLexer.getTokenSequence());
        modifications.addAll(tokenModifications);
      }
      else {
        modifications.addOuterRange(currentRange, getTemplateDataInsertionTokens().contains(baseLexer.getTokenType()));
      }
      baseLexer.advance();
    }

    return modifications;
  }


  @Nonnull
  private static String getRangeDump(@Nonnull TextRange range, @Nonnull CharSequence sequence) {
    return "'" + StringUtil.escapeLineBreak(range.subSequence(sequence).toString()) + "' " + range;
  }

  /**
   * @deprecated Override {@link #appendCurrentTemplateToken(int, CharSequence)} instead.
   */
  @Deprecated
  protected void appendCurrentTemplateToken(@Nonnull StringBuilder result, @Nonnull CharSequence buf, @Nonnull Lexer lexer, @Nonnull TemplateDataElementType.RangeCollector collector) {
    result.append(buf, lexer.getTokenStart(), lexer.getTokenEnd());
  }

  /**
   * Collects modifications for tokens having {@link #myTemplateElementType} type.
   *
   * @return modifications need to be applied for the current token
   */
  protected
  @Nonnull
  TemplateDataModifications appendCurrentTemplateToken(int tokenEndOffset, @Nonnull CharSequence tokenText) {
    return TemplateDataModifications.EMPTY;
  }

  /**
   * Returns token types of template elements which are expected to insert some strings into resulting file.
   * It's fine to include only starting token of the whole insertion range. For example, if
   * <code><?=$myVar?></code> has three tokens <code><?=</code>, <code>$myVar</code> and <code>?></code>, only type of <code><?=</code>
   * may be included. Moreover, other tokens shouldn't be included if they can be a part of a non-insertion range like
   * <code><?$myVar?></code>.
   * <p>
   * Override this method when overriding {@link #collectTemplateModifications(CharSequence, Lexer)} is not required.
   *
   * @see RangeCollector#addOuterRange(TextRange, boolean)
   */
  protected
  @Nonnull
  TokenSet getTemplateDataInsertionTokens() {
    return TokenSet.EMPTY;
  }

  protected OuterLanguageElementImpl createOuterLanguageElement(@Nonnull CharSequence internedTokenText, @Nonnull IElementType outerElementType) {
    return new OuterLanguageElementImpl(outerElementType, internedTokenText);
  }

  protected PsiFile createPsiFileFromSource(final Language language, CharSequence sourceCode, PsiManager manager) {
    final LightVirtualFile virtualFile = new LightVirtualFile("foo", createTemplateFakeFileType(language), sourceCode, LocalTimeCounter.currentTime());

    FileViewProvider viewProvider = new SingleRootFileViewProvider(manager, virtualFile, false) {
      @Override
      @Nonnull
      public Language getBaseLanguage() {
        return language;
      }
    };

    // Since we're already inside a template language PSI that was built regardless of the file size (for whatever reason),
    // there should also be no file size checks for template data files.
    SingleRootFileViewProvider.doNotCheckFileSizeLimit(virtualFile);

    return viewProvider.getPsi(language);
  }

  @Nonnull
  public static ASTNode parseWithOuterAndRemoveRangesApplied(@Nonnull ASTNode chameleon, @Nonnull Language language, @Nonnull Function<CharSequence, ASTNode> parser) {
    RangeCollectorImpl collector = chameleon.getUserData(RangeCollectorImpl.OUTER_ELEMENT_RANGES);
    return collector != null ? collector.applyRangeCollectorAndExpandChameleon(chameleon, language, parser) : parser.apply(chameleon.getChars());
  }

  protected static class TemplateFileType extends LanguageFileType {
    private final Language myLanguage;

    protected TemplateFileType(Language language) {
      super(language);
      myLanguage = language;
    }

    @Override
    @Nonnull
    public String getDefaultExtension() {
      return "";
    }

    @Override
    @Nonnull
    public LocalizeValue getDescription() {
      return LocalizeValue.of("fake for language:" + myLanguage.getID());
    }

    @Nonnull
    @Override
    public Image getIcon() {
      throw new UnsupportedOperationException();
    }

    @Override
    @Nonnull
    public String getId() {
      return myLanguage.getID();
    }
  }

  /**
   * This collector is used for storing ranges of outer elements and ranges of artificial elements, that should be stripped from the resulting tree
   * At the time of creating source code for the data language we need to memorize positions with template language elements.
   * For such positions we use {@link RangeCollector#addOuterRange}
   * Sometimes to build a correct tree we need to insert additional symbols into resulting source:
   * e.g. put an identifier instead of the base language fragment: {@code something={% $var %}} => {@code something=dummyidentifier}
   * that must be removed after building the tree.
   * For such additional symbols {@link RangeCollector#addRangeToRemove} must be used
   *
   * @apiNote Please note that all start offsets for the ranges must be in terms of "original source code". So, outer ranges are ranges
   * of outer elements in original source code. Ranges to remove don't correspond to any text range neither in original nor in modified text.
   * But their start offset is the offset in original text, and length is the length of inserted dummy identifier.
   * @implNote Should be interface, but abstract class with empty method bodies for keeping binary compatibility with plugins.
   */
  public static abstract class RangeCollector {

    /**
     * Adds range corresponding to the outer element inside original source code.
     * After building the data template tree these ranges will be used for inserting outer language elements.
     * If it's known whether this template element adds some string to resulting text, consider using {@link #addOuterRange(TextRange, boolean)}.
     */
    public void addOuterRange(@Nonnull TextRange newRange) {
    }

    /**
     * Adds range corresponding to the outer element inside original source code.
     * After building the data template tree these ranges will be used for inserting outer language elements.
     *
     * @param isInsertion <tt>true</tt> if element is expected to insert some text into template data fragment. For example, PHP's
     *                    <code><?= $myVar ?></code> are insertions, while <code><?php foo() ?></code> are not.
     */
    public abstract void addOuterRange(@Nonnull TextRange newRange, boolean isInsertion);

    /**
     * Adds the fragment that must be removed from the tree on the stage inserting outer elements.
     * This method should be called after adding "fake" symbols inside the data language text for building syntax correct tree
     */
    public void addRangeToRemove(@Nonnull TextRange rangeToRemove) {
    }
  }


  /**
   * Marker interface for element types which handle outer language elements themselves in
   * {@link ILazyParseableElementTypeBase#parseContents(ASTNode)} method.
   * <p>
   * To parse lazy parseable element {@link TemplateDataElementType#parseWithOuterAndRemoveRangesApplied(ASTNode, Language, Function)}
   * should be used.
   */
  public interface TemplateAwareElementType extends ILazyParseableElementTypeBase {
    @Nonnull
    TreeElement createTreeElement(@Nonnull CharSequence text);
  }

}
