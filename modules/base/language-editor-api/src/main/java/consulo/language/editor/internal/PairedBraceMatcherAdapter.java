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

package consulo.language.editor.internal;

import consulo.codeEditor.HighlighterIterator;
import consulo.language.BracePair;
import consulo.language.Language;
import consulo.language.PairedBraceMatcher;
import consulo.language.ast.IElementType;
import consulo.language.editor.highlight.BraceMatcherTerminationAspect;
import consulo.language.editor.highlight.LanguageBraceMatcher;
import consulo.language.editor.highlight.NontrivialBraceMatcher;
import consulo.language.psi.PsiFile;
import consulo.virtualFileSystem.fileType.FileType;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@SuppressWarnings("ExtensionImplIsNotAnnotatedInspection")
public class PairedBraceMatcherAdapter implements NontrivialBraceMatcher, LanguageBraceMatcher {
  private final PairedBraceMatcher myMatcher;
  private final Language myLanguage;

  public PairedBraceMatcherAdapter(PairedBraceMatcher matcher, Language language) {
    myMatcher = matcher;
    myLanguage = language;
  }

  @Override
  public int getBraceTokenGroupId(IElementType tokenType) {
    BracePair[] pairs = myMatcher.getPairs();
    for (BracePair pair : pairs) {
      if (tokenType == pair.getLeftBraceType() || tokenType == pair.getRightBraceType()) return myLanguage.hashCode();
    }
    return -1;
  }

  @Nullable
  public BracePair findPair(boolean left, HighlighterIterator iterator, CharSequence fileText, FileType fileType) {
    IElementType tokenType = (IElementType)iterator.getTokenType();
    BracePair[] pairs = myMatcher.getPairs();
    for (BracePair pair : pairs) {
      if (tokenType == (left ? pair.getLeftBraceType() : pair.getRightBraceType())) return pair;
    }
    return null;
  }

  @Override
  public boolean isLBraceToken(HighlighterIterator iterator, CharSequence fileText, FileType fileType) {
    return findPair(true, iterator, fileText, fileType) != null;
  }

  @Override
  public boolean isRBraceToken(HighlighterIterator iterator, CharSequence fileText, FileType fileType) {
    return findPair(false, iterator, fileText, fileType) != null;
  }

  @Override
  public IElementType getOppositeBraceTokenType(@Nonnull IElementType type) {
    BracePair[] pairs = myMatcher.getPairs();
    for (BracePair pair : pairs) {
      if (type == pair.getRightBraceType()) return pair.getLeftBraceType();
      if (type == pair.getLeftBraceType()) return pair.getRightBraceType();
    }

    return null;
  }

  @Override
  public boolean isPairBraces(IElementType tokenType, IElementType tokenType2) {
    BracePair[] pairs = myMatcher.getPairs();
    for (BracePair pair : pairs) {
      if (tokenType == pair.getLeftBraceType() && tokenType2 == pair.getRightBraceType() || tokenType == pair.getRightBraceType() && tokenType2 == pair.getLeftBraceType()) {
        return true;
      }
    }
    return false;
  }

  @Override
  public boolean isStructuralBrace(HighlighterIterator iterator, CharSequence text, FileType fileType) {
    IElementType tokenType = (IElementType)iterator.getTokenType();
    BracePair[] pairs = myMatcher.getPairs();
    for (BracePair pair : pairs) {
      if (tokenType == pair.getRightBraceType() || tokenType == pair.getLeftBraceType()) return pair.isStructural();
    }
    return false;
  }

  @Override
  public boolean isPairedBracesAllowedBeforeType(@Nonnull IElementType lbraceType, @Nullable IElementType contextType) {
    return myMatcher.isPairedBracesAllowedBeforeType(lbraceType, contextType);
  }

  @Override
  public int getCodeConstructStart(PsiFile file, int openingBraceOffset) {
    return myMatcher.getCodeConstructStart(file, openingBraceOffset);
  }

  @Override
  @Nonnull
  public List<IElementType> getOppositeBraceTokenTypes(@Nonnull IElementType type) {
    List<IElementType> result = null;

    for (BracePair pair : myMatcher.getPairs()) {
      IElementType match = null;

      if (type == pair.getRightBraceType()) match = pair.getLeftBraceType();
      if (type == pair.getLeftBraceType()) match = pair.getRightBraceType();

      if (match != null) {
        if (result == null) result = new ArrayList<>(2);
        result.add(match);
      }
    }

    return result != null ? result : Collections.<IElementType>emptyList();
  }

  @Override
  public boolean shouldStopMatch(boolean forward, @Nonnull IElementType braceType, @Nonnull HighlighterIterator iterator) {
    if (myMatcher instanceof BraceMatcherTerminationAspect) {
      return ((BraceMatcherTerminationAspect)myMatcher).shouldStopMatch(forward, braceType, iterator);
    }
    return false;
  }

  @Nonnull
  @Override
  public Language getLanguage() {
    return myLanguage;
  }
}
