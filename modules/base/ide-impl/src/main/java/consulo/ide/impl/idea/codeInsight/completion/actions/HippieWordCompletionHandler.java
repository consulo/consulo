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

package consulo.ide.impl.idea.codeInsight.completion.actions;

import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorColors;
import consulo.codeEditor.EditorEx;
import consulo.codeEditor.HighlighterIterator;
import consulo.colorScheme.EditorColorsManager;
import consulo.colorScheme.TextAttributesKey;
import consulo.fileEditor.FileEditor;
import consulo.fileEditor.FileEditorManager;
import consulo.fileEditor.TextEditor;
import consulo.language.editor.FileModificationService;
import consulo.language.editor.action.CodeInsightActionHandler;
import consulo.language.editor.completion.CamelHumpMatcher;
import consulo.language.editor.completion.lookup.LookupManager;
import consulo.language.editor.highlight.HighlightManager;
import consulo.language.psi.PsiFile;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.dataholder.Key;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.*;

/**
 * @author mike
 */
public class HippieWordCompletionHandler implements CodeInsightActionHandler {
  private static final Key<CompletionState> KEY_STATE = new Key<CompletionState>("HIPPIE_COMPLETION_STATE");
  private final boolean myForward;

  public HippieWordCompletionHandler(boolean forward) {
    myForward = forward;
  }

  @RequiredUIAccess
  @Override
  public void invoke(@Nonnull Project project, @Nonnull final Editor editor, @Nonnull PsiFile file) {
    if (!FileModificationService.getInstance().prepareFileForWrite(file)) return;

    LookupManager.getInstance(project).hideActiveLookup();

    final CharSequence charsSequence = editor.getDocument().getCharsSequence();

    final CompletionData data = computeData(editor, charsSequence);
    String currentPrefix = data.myPrefix;

    final CompletionState completionState = getCompletionState(editor);

    String oldPrefix = completionState.oldPrefix;
    CompletionVariant lastProposedVariant = completionState.lastProposedVariant;

    if (lastProposedVariant == null || oldPrefix == null || !new CamelHumpMatcher(oldPrefix).isStartMatch(currentPrefix) ||
        !currentPrefix.equals(lastProposedVariant.variant)) {
      //we are starting over
      oldPrefix = currentPrefix;
      completionState.oldPrefix = oldPrefix;
      lastProposedVariant = null;
    }

    CompletionVariant nextVariant = computeNextVariant(editor, oldPrefix, lastProposedVariant, data, file);
    if (nextVariant == null) return;

    int replacementEnd = data.startOffset + data.myWordUnderCursor.length();
    editor.getDocument().replaceString(data.startOffset, replacementEnd, nextVariant.variant);
    editor.getCaretModel().moveToOffset(data.startOffset + nextVariant.variant.length());
    completionState.lastProposedVariant = nextVariant;
    highlightWord(nextVariant, project, data);
  }

  private static void highlightWord(final CompletionVariant variant, final Project project, CompletionData data) {
    int delta = data.startOffset < variant.offset ? variant.variant.length() - data.myWordUnderCursor.length() : 0;

    HighlightManager highlightManager = HighlightManager.getInstance(project);
    TextAttributesKey attributesKey = EditorColors.TEXT_SEARCH_RESULT_ATTRIBUTES;
    highlightManager.addOccurrenceHighlight(variant.editor, variant.offset + delta, variant.offset + variant.variant.length() + delta, attributesKey,
                                            HighlightManager.HIDE_BY_ANY_KEY, null);
  }


  private static class CompletionData {
    public String myPrefix;
    public String myWordUnderCursor;
    public int startOffset;
  }

  @Nullable
  private CompletionVariant computeNextVariant(final Editor editor,
                                               @Nullable final String prefix,
                                               @Nullable CompletionVariant lastProposedVariant,
                                               final CompletionData data, PsiFile file) {
    final List<CompletionVariant> variants = computeVariants(editor, new CamelHumpMatcher(StringUtil.notNullize(prefix)), file);
    if (variants.isEmpty()) return null;

    for (CompletionVariant variant : variants) {
      if (lastProposedVariant != null) {
        if (variant.variant.equals(lastProposedVariant.variant)) {
          if (lastProposedVariant.offset > data.startOffset && variant.offset > data.startOffset) lastProposedVariant = variant;
          if (lastProposedVariant.offset < data.startOffset && variant.offset < data.startOffset) lastProposedVariant = variant;
        }
      }
    }


    if (lastProposedVariant == null) {
      CompletionVariant result = null;

      if (myForward) {
        for (CompletionVariant variant : variants) {
          if (variant.offset < data.startOffset) {
            result = variant;
          }
          else if (result == null) {
            result = variant;
            break;
          }
        }
      }
      else {
        for (CompletionVariant variant : variants) {
          if (variant.offset > data.startOffset) return variant;
        }

        return variants.iterator().next();
      }

      return result;
    }


    if (myForward) {
      CompletionVariant result = null;
      for (CompletionVariant variant : variants) {
        if (variant == lastProposedVariant) {
          if (result == null) return variants.get(variants.size() - 1);
          return result;
        }
        result = variant;
      }

      return variants.get(variants.size() - 1);
    }
    else {
      for (Iterator<CompletionVariant> i = variants.iterator(); i.hasNext();) {
        CompletionVariant variant = i.next();
        if (variant == lastProposedVariant) {
          if (i.hasNext()) {
            return i.next();
          }
          else {
            return variants.iterator().next();
          }
        }
      }

    }

    return null;
  }

  public static class CompletionVariant {
    public final Editor editor;
    public final String variant;
    public final int offset;

    public CompletionVariant(final Editor editor, final String variant, final int offset) {
      this.editor = editor;
      this.variant = variant;
      this.offset = offset;
    }
  }

  private static boolean containsLetters(CharSequence seq, int start, int end) {
    for (int i = start; i < end; i++) {
      if (Character.isLetter(seq.charAt(i))) {
        return true;
      }
    }
    return false;
  }

  private static List<CompletionVariant> computeVariants(@Nonnull final Editor editor, CamelHumpMatcher matcher, PsiFile file) {

    final CharSequence chars = editor.getDocument().getCharsSequence();

    final ArrayList<CompletionVariant> words = new ArrayList<CompletionVariant>();
    final List<CompletionVariant> afterWords = new ArrayList<CompletionVariant>();

    final int caretOffset = editor.getCaretModel().getOffset();

    addWordsForEditor((EditorEx)editor, matcher, chars, words, afterWords, caretOffset);

    for(FileEditor fileEditor: FileEditorManager.getInstance(file.getProject()).getAllEditors()) {
      if (fileEditor instanceof TextEditor textEditor) {
        Editor anotherEditor = textEditor.getEditor();
        if (anotherEditor != editor) {
          addWordsForEditor((EditorEx)anotherEditor, matcher, anotherEditor.getDocument().getCharsSequence(), words, afterWords, 0);
        }
      }
    }

    Set<String> allWords = new HashSet<String>();
    List<CompletionVariant> result = new ArrayList<CompletionVariant>();

    Collections.reverse(words);

    for (CompletionVariant variant : words) {
      if (!allWords.contains(variant.variant)) {
        result.add(variant);
        allWords.add(variant.variant);
      }
    }

    Collections.reverse(result);

    allWords.clear();
    for (CompletionVariant variant : afterWords) {
      if (!allWords.contains(variant.variant)) {
        result.add(variant);
        allWords.add(variant.variant);
      }
    }

    return result;
  }

  private interface TokenProcessor {
    boolean processToken(int start, int end);
  }

  private static void addWordsForEditor(final EditorEx editor,
                                        final CamelHumpMatcher matcher,
                                        final CharSequence chars,
                                        final List<CompletionVariant> words,
                                        final List<CompletionVariant> afterWords, final int caretOffset) {
    int startOffset = 0;
    TokenProcessor processor = new TokenProcessor() {
      @Override
      public boolean processToken(int start, int end) {
        if ((start > caretOffset || end < caretOffset) &&  //skip prefix itself
            end - start > matcher.getPrefix().length()) {
          final String word = chars.subSequence(start, end).toString();
          if (matcher.isStartMatch(word)) {
            CompletionVariant v = new CompletionVariant(editor, word, start);
            if (end > caretOffset) {
              afterWords.add(v);
            }
            else {
              words.add(v);
            }
          }
        }
        return true;
      }
    };
    processWords(editor, startOffset, processor);
  }

  private static void processWords(Editor editor, int startOffset, TokenProcessor processor) {
    CharSequence chars = editor.getDocument().getCharsSequence();
    HighlighterIterator iterator = editor.getHighlighter().createIterator(startOffset);
    while (!iterator.atEnd()) {
      int start = iterator.getStart();
      int end = iterator.getEnd();

      while (start < end) {
        int wordStart = start;
        while (wordStart < end && !isWordPart(chars.charAt(wordStart))) wordStart++;

        int wordEnd = wordStart;
        while (wordEnd < end && isWordPart(chars.charAt(wordEnd))) wordEnd++;

        if (wordEnd > wordStart && containsLetters(chars, wordStart, wordEnd) && !processor.processToken(wordStart, wordEnd)) {
          return;
        }
        start = wordEnd + 1;
      }
      iterator.advance();
    }
  }

  private static boolean isWordPart(final char c) {
    return Character.isJavaIdentifierPart(c) || c == '-' || c == '*' ;
  }

  private static CompletionData computeData(final Editor editor, final CharSequence charsSequence) {
    final int offset = editor.getCaretModel().getOffset();

    final CompletionData data = new CompletionData();

    processWords(editor, Math.max(offset - 1, 0), new TokenProcessor() {
      @Override
      public boolean processToken(int start, int end) {
        if (start > offset) {
          return false;
        }
        if (end >= offset) {
          data.myPrefix = charsSequence.subSequence(start, offset).toString();
          data.myWordUnderCursor = charsSequence.subSequence(start, end).toString();
          data.startOffset = start;
          return false;
        }
        return true;
      }
    });

    if (data.myPrefix == null) {
      data.myPrefix = "";
      data.myWordUnderCursor = "";
      data.startOffset = offset;
    }
    return data;
  }

  @Override
  public boolean startInWriteAction() {
    return true;
  }

  private static CompletionState getCompletionState(Editor editor) {
    CompletionState state = editor.getUserData(KEY_STATE);
    if (state == null) {
      state = new CompletionState();
      editor.putUserData(KEY_STATE, state);
    }

    return state;
  }

  private static class CompletionState {
    public String oldPrefix;
    public CompletionVariant lastProposedVariant;
  }
}
