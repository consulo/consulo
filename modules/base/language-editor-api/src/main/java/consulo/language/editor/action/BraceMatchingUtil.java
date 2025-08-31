/*
 * Copyright 2000-2013 JetBrains s.r.o.
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

package consulo.language.editor.action;

import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorHighlighter;
import consulo.codeEditor.HighlighterIterator;
import consulo.document.Document;
import consulo.language.Language;
import consulo.language.ast.IElementType;
import consulo.language.editor.highlight.BraceMatcher;
import consulo.language.editor.highlight.LanguageBraceMatcher;
import consulo.language.editor.highlight.VirtualFileBraceMatcher;
import consulo.language.editor.highlight.XmlAwareBraceMatcher;
import consulo.language.editor.internal.BraceMatcherInternal;
import consulo.language.file.LanguageFileType;
import consulo.language.psi.PsiFile;
import consulo.util.collection.Stack;
import consulo.util.lang.Comparing;
import consulo.virtualFileSystem.fileType.FileType;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jetbrains.annotations.TestOnly;

public class BraceMatchingUtil {
  public static final int UNDEFINED_TOKEN_GROUP = -1;

  private BraceMatchingUtil() {
  }

  public static boolean isPairedBracesAllowedBeforeTypeInFileType(@Nonnull IElementType lbraceType,
                                                                  IElementType tokenType,
                                                                  @Nonnull FileType fileType) {
    try {
      return getBraceMatcher(fileType, lbraceType).isPairedBracesAllowedBeforeType(lbraceType, tokenType);
    }
    catch (AbstractMethodError incompatiblePluginThatWeDoNotCare) {
      // Do nothing
    }
    return true;
  }

  @TestOnly
  public static int getMatchedBraceOffset(@Nonnull Editor editor, boolean forward, @Nonnull PsiFile file) {
    Document document = editor.getDocument();
    int offset = editor.getCaretModel().getOffset();
    EditorHighlighter editorHighlighter = editor.getHighlighter();
    HighlighterIterator iterator = editorHighlighter.createIterator(offset);
    boolean matched = matchBrace(document.getCharsSequence(), file.getFileType(), iterator, forward);
    assert matched;
    return iterator.getStart();
  }

  private static class MatchBraceContext {
    private final CharSequence fileText;
    private final FileType fileType;
    private final HighlighterIterator iterator;
    private final boolean forward;

    private final IElementType brace1Token;
    private final int group;
    private final String brace1TagName;
    private final boolean isStrict;
    private final boolean isCaseSensitive;
    @Nonnull
    private final BraceMatcher myMatcher;

    private final Stack<IElementType> myBraceStack = new Stack<>();
    private final Stack<String> myTagNameStack = new Stack<>();

    MatchBraceContext(@Nonnull CharSequence fileText, @Nonnull FileType fileType, @Nonnull HighlighterIterator iterator, boolean forward) {
      this(fileText, fileType, iterator, forward,isStrictTagMatching(getBraceMatcher(fileType, iterator), fileType, getTokenGroup((IElementType)iterator.getTokenType(), fileType)));
    }

    MatchBraceContext(@Nonnull CharSequence fileText, @Nonnull FileType fileType, @Nonnull HighlighterIterator iterator, boolean forward, boolean strict) {
      this.fileText = fileText;
      this.fileType = fileType;
      this.iterator = iterator;
      this.forward = forward;

      myMatcher = getBraceMatcher(fileType, iterator);
      brace1Token = (IElementType)this.iterator.getTokenType();
      group = getTokenGroup(brace1Token, this.fileType);
      brace1TagName = getTagName(myMatcher, this.fileText, this.iterator);

      isCaseSensitive = areTagsCaseSensitive(myMatcher, this.fileType, group);
      isStrict = strict;
    }

    boolean doBraceMatch() {
      myBraceStack.clear();
      myTagNameStack.clear();
      myBraceStack.push(brace1Token);
      if (isStrict) {
        myTagNameStack.push(brace1TagName);
      }
      boolean matched = false;
      while (true) {
        if (!forward) {
          iterator.retreat();
        }
        else {
          iterator.advance();
        }
        if (iterator.atEnd()) {
          break;
        }

        IElementType tokenType = (IElementType)iterator.getTokenType();

        if (getTokenGroup(tokenType, fileType) != group) {
          continue;
        }
        String tagName = getTagName(myMatcher, fileText, iterator);
        if (!isStrict && !Comparing.equal(brace1TagName, tagName, isCaseSensitive)) continue;
        if (forward ? isLBraceToken(iterator, fileText, fileType) : isRBraceToken(iterator, fileText, fileType)) {
          myBraceStack.push(tokenType);
          if (isStrict) {
            myTagNameStack.push(tagName);
          }
        }
        else if (forward ? isRBraceToken(iterator, fileText, fileType) : isLBraceToken(iterator, fileText, fileType)) {
          IElementType topTokenType = myBraceStack.pop();
          String topTagName = null;
          if (isStrict) {
            topTagName = myTagNameStack.pop();
          }

          if (!isStrict) {
            IElementType baseType = myMatcher.getOppositeBraceTokenType(tokenType);
            if (myBraceStack.contains(baseType)) {
              while (!isPairBraces(topTokenType, tokenType, fileType) && !myBraceStack.empty()) {
                topTokenType = myBraceStack.pop();
              }
            }
            else if ((brace1TagName == null || !brace1TagName.equals(tagName)) && !isPairBraces(topTokenType, tokenType, fileType)) {
              // Ignore non-matched opposite-direction brace for non-strict processing.
              myBraceStack.push(topTokenType);
              continue;
            }
          }

          if (!isPairBraces(topTokenType, tokenType, fileType)
              || isStrict && !Comparing.equal(topTagName, tagName, isCaseSensitive))
          {
            matched = false;
            break;
          }

          if (myBraceStack.isEmpty()) {
            matched = true;
            break;
          }
        }
      }
      return matched;
    }
  }

  public static synchronized boolean matchBrace(@Nonnull CharSequence fileText,
                                                @Nonnull FileType fileType,
                                                @Nonnull HighlighterIterator iterator,
                                                boolean forward) {
    return new MatchBraceContext(fileText, fileType, iterator, forward).doBraceMatch();
  }


  public static synchronized boolean matchBrace(@Nonnull CharSequence fileText,
                                                @Nonnull FileType fileType,
                                                @Nonnull HighlighterIterator iterator,
                                                boolean forward,
                                                boolean isStrict) {
    return new MatchBraceContext(fileText, fileType, iterator, forward, isStrict).doBraceMatch();
  }

  public static boolean findStructuralLeftBrace(@Nonnull FileType fileType, @Nonnull HighlighterIterator iterator, @Nonnull CharSequence fileText) {
    Stack<IElementType> braceStack = new Stack<>();
    Stack<String> tagNameStack = new Stack<>();

    BraceMatcher matcher = getBraceMatcher(fileType, iterator);

    while (!iterator.atEnd()) {
      if (isStructuralBraceToken(fileType, iterator, fileText)) {
        if (isRBraceToken(iterator, fileText, fileType)) {
          braceStack.push((IElementType)iterator.getTokenType());
          tagNameStack.push(getTagName(matcher, fileText, iterator));
        }
        if (isLBraceToken(iterator, fileText, fileType)) {
          if (braceStack.isEmpty()) return true;

          int group = matcher.getBraceTokenGroupId((IElementType)iterator.getTokenType());

          IElementType topTokenType = braceStack.pop();
          IElementType tokenType = (IElementType)iterator.getTokenType();

          boolean isStrict = isStrictTagMatching(matcher, fileType, group);
          boolean isCaseSensitive = areTagsCaseSensitive(matcher, fileType, group);

          String topTagName = null;
          String tagName = null;
          if (isStrict) {
            topTagName = tagNameStack.pop();
            tagName = getTagName(matcher, fileText, iterator);
          }

          if (!isPairBraces(topTokenType, tokenType, fileType)
              || isStrict && !Comparing.equal(topTagName, tagName, isCaseSensitive)) {
            return false;
          }
        }
      }

      iterator.retreat();
    }

    return false;
  }

  public static boolean isStructuralBraceToken(@Nonnull FileType fileType, @Nonnull HighlighterIterator iterator, @Nonnull CharSequence text) {
    BraceMatcher matcher = getBraceMatcher(fileType, iterator);
    return matcher.isStructuralBrace(iterator, text, fileType);
  }

  public static boolean isLBraceToken(@Nonnull HighlighterIterator iterator, @Nonnull CharSequence fileText, @Nonnull FileType fileType) {
    BraceMatcher braceMatcher = getBraceMatcher(fileType, iterator);

    return braceMatcher.isLBraceToken(iterator, fileText, fileType);
  }

  public static boolean isRBraceToken(@Nonnull HighlighterIterator iterator, @Nonnull CharSequence fileText, @Nonnull FileType fileType) {
    BraceMatcher braceMatcher = getBraceMatcher(fileType, iterator);

    return braceMatcher.isRBraceToken(iterator, fileText, fileType);
  }

  public static boolean isPairBraces(@Nonnull IElementType tokenType1, @Nonnull IElementType tokenType2, @Nonnull FileType fileType) {
    BraceMatcher matcher = getBraceMatcher(fileType, tokenType1);
    return matcher.isPairBraces(tokenType1, tokenType2);
  }

  private static int getTokenGroup(IElementType tokenType, FileType fileType) {
    BraceMatcher matcher = getBraceMatcher(fileType, tokenType);
    return matcher.getBraceTokenGroupId(tokenType);
  }

  // TODO: better name for this method
  public static int findLeftmostLParen(HighlighterIterator iterator,
                                       IElementType lparenTokenType,
                                       CharSequence fileText,
                                       FileType fileType) {
    int lastLbraceOffset = -1;

    Stack<IElementType> braceStack = new Stack<>();
    for (; !iterator.atEnd(); iterator.retreat()) {
      IElementType tokenType = (IElementType)iterator.getTokenType();

      if (isLBraceToken(iterator, fileText, fileType)) {
        if (!braceStack.isEmpty()) {
          IElementType topToken = braceStack.pop();
          if (!isPairBraces(tokenType, topToken, fileType)) {
            break; // unmatched braces
          }
        }
        else {
          if (tokenType == lparenTokenType) {
            lastLbraceOffset = iterator.getStart();
          }
          else {
            break;
          }
        }
      }
      else if (isRBraceToken(iterator, fileText, fileType)) {
        braceStack.push((IElementType)iterator.getTokenType());
      }
    }

    return lastLbraceOffset;
  }

  public static int findLeftLParen(HighlighterIterator iterator,
                                   IElementType lparenTokenType,
                                   CharSequence fileText,
                                   FileType fileType) {
    int lastLbraceOffset = -1;

    Stack<IElementType> braceStack = new Stack<>();
    for (; !iterator.atEnd(); iterator.retreat()) {
      IElementType tokenType = (IElementType)iterator.getTokenType();

      if (isLBraceToken(iterator, fileText, fileType)) {
        if (!braceStack.isEmpty()) {
          IElementType topToken = braceStack.pop();
          if (!isPairBraces(tokenType, topToken, fileType)) {
            break; // unmatched braces
          }
        }
        else {
          if (tokenType == lparenTokenType) {
            return iterator.getStart();
          }
          else {
            break;
          }
        }
      }
      else if (isRBraceToken(iterator, fileText, fileType)) {
        braceStack.push((IElementType)iterator.getTokenType());
      }
    }

    return lastLbraceOffset;
  }

  // TODO: better name for this method
  public static int findRightmostRParen(HighlighterIterator iterator,
                                        IElementType rparenTokenType,
                                        CharSequence fileText,
                                        FileType fileType) {
    int lastRbraceOffset = -1;

    Stack<IElementType> braceStack = new Stack<>();
    for (; !iterator.atEnd(); iterator.advance()) {
      IElementType tokenType = (IElementType)iterator.getTokenType();

      if (isRBraceToken(iterator, fileText, fileType)) {
        if (!braceStack.isEmpty()) {
          IElementType topToken = braceStack.pop();
          if (!isPairBraces(tokenType, topToken, fileType)) {
            break; // unmatched braces
          }
        }
        else {
          if (tokenType == rparenTokenType) {
            lastRbraceOffset = iterator.getStart();
          }
          else {
            break;
          }
        }
      }
      else if (isLBraceToken(iterator, fileText, fileType)) {
        braceStack.push((IElementType)iterator.getTokenType());
      }
    }

    return lastRbraceOffset;
  }

  private static class BraceMatcherHolder {
    private static final BraceMatcher ourDefaultBraceMatcher = new DefaultBraceMatcher();
  }

  @Nonnull
  public static BraceMatcher getBraceMatcher(@Nonnull FileType fileType, @Nonnull HighlighterIterator iterator) {
    return getBraceMatcher(fileType, (IElementType) iterator.getTokenType());
  }

  @Nonnull
  public static BraceMatcher getBraceMatcher(@Nonnull FileType fileType, @Nonnull IElementType type) {
    return getBraceMatcher(fileType, type.getLanguage());
  }

  @Nonnull
  public static BraceMatcher getBraceMatcher(@Nonnull FileType fileType, @Nonnull Language lang) {
    BraceMatcher matcher = LanguageBraceMatcher.forLanguage(lang);
    if (matcher != null) {
     return matcher;
    }

    BraceMatcher byFileType = getBraceMatcherByFileType(fileType);
    if (byFileType != null) return byFileType;

    if (fileType instanceof LanguageFileType) {
      Language language = ((LanguageFileType)fileType).getLanguage();
      if (lang != language) {
        FileType type1 = lang.getAssociatedFileType();
        if (type1 != null) {
          BraceMatcher braceMatcher = getBraceMatcherByFileType(type1);
          if (braceMatcher != null) {
            return braceMatcher;
          }
        }

        matcher = LanguageBraceMatcher.forLanguage(language);
        if (matcher != null) {
          return matcher;
        }
      }
    }

    return BraceMatcherHolder.ourDefaultBraceMatcher;
  }

  @Nullable
  private static BraceMatcher getBraceMatcherByFileType(@Nonnull FileType fileType) {
    BraceMatcher braceMatcher = BraceMatcherInternal.getMatcher(fileType);
    if (braceMatcher != null) return braceMatcher;

    BraceMatcher matcher = VirtualFileBraceMatcher.forFileType(fileType);
    if (matcher != null) {
      return matcher;
    }
    return null;
  }

  private static boolean isStrictTagMatching(@Nonnull BraceMatcher matcher, @Nonnull FileType fileType, int group) {
    return matcher instanceof XmlAwareBraceMatcher && ((XmlAwareBraceMatcher)matcher).isStrictTagMatching(fileType, group);
  }

  private static boolean areTagsCaseSensitive(@Nonnull BraceMatcher matcher, @Nonnull FileType fileType, int tokenGroup) {
    return matcher instanceof XmlAwareBraceMatcher && ((XmlAwareBraceMatcher)matcher).areTagsCaseSensitive(fileType, tokenGroup);
  }

  @Nullable
  private static String getTagName(@Nonnull BraceMatcher matcher, @Nonnull CharSequence fileText, @Nonnull HighlighterIterator iterator) {
    if (matcher instanceof XmlAwareBraceMatcher) return ((XmlAwareBraceMatcher)matcher).getTagName(fileText, iterator);
    return null;
  }

  private static class DefaultBraceMatcher implements BraceMatcher {
    @Override
    public int getBraceTokenGroupId(IElementType tokenType) {
      return UNDEFINED_TOKEN_GROUP;
    }

    @Override
    public boolean isLBraceToken(HighlighterIterator iterator, CharSequence fileText, FileType fileType) {
      return false;
    }

    @Override
    public boolean isRBraceToken(HighlighterIterator iterator, CharSequence fileText, FileType fileType) {
      return false;
    }

    @Override
    public boolean isPairBraces(IElementType tokenType, IElementType tokenType2) {
      return false;
    }

    @Override
    public boolean isStructuralBrace(HighlighterIterator iterator, CharSequence text, FileType fileType) {
      return false;
    }

    @Override
    public IElementType getOppositeBraceTokenType(@Nonnull IElementType type) {
      return null;
    }

    @Override
    public boolean isPairedBracesAllowedBeforeType(@Nonnull IElementType lbraceType, @Nullable IElementType contextType) {
      return true;
    }

    @Override
    public int getCodeConstructStart(PsiFile file, int openingBraceOffset) {
      return openingBraceOffset;
    }
  }
}
