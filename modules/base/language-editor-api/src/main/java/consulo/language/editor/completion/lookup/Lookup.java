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

package consulo.language.editor.completion.lookup;

import consulo.codeEditor.Editor;
import consulo.application.util.matcher.PrefixMatcher;
import consulo.language.editor.completion.lookup.event.LookupListener;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.project.Project;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.awt.*;
import java.util.List;

/**
 * Represents list with suggestions shown in code completion, refactorings, live templates etc.
 */
public interface Lookup {
  char NORMAL_SELECT_CHAR = '\n';
  char REPLACE_SELECT_CHAR = '\t';
  char COMPLETE_STATEMENT_SELECT_CHAR = '\r';
  char AUTO_INSERT_SELECT_CHAR = (char) 0;

   static boolean shouldAddCompletionChar(char completionChar) {
    return completionChar != Lookup.AUTO_INSERT_SELECT_CHAR && completionChar != Lookup.REPLACE_SELECT_CHAR && completionChar != Lookup.NORMAL_SELECT_CHAR;
  }

  /**
   * @return the offset in {@link #getTopLevelEditor()} which this lookup's left side should be aligned with. Note that if the lookup doesn't fit
   * the screen due to its dimensions, the actual position might differ from this editor offset.
   */
  int getLookupStart();

  @Nullable
  LookupElement getCurrentItem();

  void addLookupListener(LookupListener listener);
  void removeLookupListener(LookupListener listener);

  /**
   * @return bounds in layered pane coordinate system
   */
  Rectangle getBounds();

  /**
   * @return bounds of the current item in the layered pane coordinate system.
   */
  Rectangle getCurrentItemBounds();
  boolean isPositionedAboveCaret();

  /**
   * @return leaf PSI element at this lookup's start position (see {@link #getLookupStart()}) in {@link #getPsiFile()} result.
   */
  @Nullable
  PsiElement getPsiElement();

  /**
   * Consider using {@link #getTopLevelEditor()} if you don't need injected editor.
   * @return editor, possibly injected, where this lookup is shown
   */
  @Nonnull
  Editor getEditor();

  /**
   * @return the non-injected editor where this lookup is shown
   */
  @Nonnull
  Editor getTopLevelEditor();

  @Nonnull
  Project getProject();

  /**
   * @return PSI file, possibly injected, associated with this lookup's editor
   * @see #getEditor()
   */
  @Nullable
  PsiFile getPsiFile();

  boolean isCompletion();

  List<LookupElement> getItems();

  boolean isFocused();

  @Nonnull
  String itemPattern(@Nonnull LookupElement element);

  @Nonnull
  PrefixMatcher itemMatcher(@Nonnull LookupElement item);

  boolean isSelectionTouched();

  List<String> getAdvertisements();
}
