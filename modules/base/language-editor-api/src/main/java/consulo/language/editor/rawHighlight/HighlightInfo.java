/*
 * Copyright 2013-2022 consulo.io
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
package consulo.language.editor.rawHighlight;

import consulo.codeEditor.markup.GutterIconRenderer;
import consulo.codeEditor.markup.GutterMark;
import consulo.codeEditor.markup.RangeHighlighter;
import consulo.colorScheme.EditorColorsScheme;
import consulo.colorScheme.TextAttributes;
import consulo.colorScheme.TextAttributesKey;
import consulo.document.util.Segment;
import consulo.document.util.TextRange;
import consulo.language.ast.ASTNode;
import consulo.language.editor.annotation.HighlightSeverity;
import consulo.language.editor.annotation.ProblemGroup;
import consulo.language.editor.intention.IntentionAction;
import consulo.language.editor.rawHighlight.internal.HighlightInfoFactory;
import consulo.language.psi.PsiElement;
import consulo.localize.LocalizeValue;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Predicate;

/**
 * @author VISTALL
 * @since 13-Feb-22
 */
public interface HighlightInfo extends Segment {
  public interface Builder {
    // only one 'range' call allowed
    @Nonnull
    Builder range(@Nonnull TextRange textRange);

    @Nonnull
    Builder range(@Nonnull ASTNode node);

    @Nonnull
    Builder range(@Nonnull PsiElement element);

    @Nonnull
    Builder range(@Nonnull PsiElement element, @Nonnull TextRange rangeInElement);

    @Nonnull
    Builder range(@Nonnull PsiElement element, int start, int end);

    @Nonnull
    Builder range(int start, int end);

    @Nonnull
    Builder gutterIconRenderer(@Nonnull GutterIconRenderer gutterIconRenderer);

    @Nonnull
    Builder problemGroup(@Nonnull ProblemGroup problemGroup);

    @Nonnull
    Builder inspectionToolId(@Nonnull String inspectionTool);

    // only one allowed
    @Nonnull
    Builder description(@Nonnull String description);

    @Nonnull
    Builder descriptionAndTooltip(@Nonnull String description);

    @Nonnull
    default Builder descriptionAndTooltip(@Nonnull LocalizeValue description) {
      return descriptionAndTooltip(description.get());
    }

    // only one allowed
    @Nonnull
    Builder textAttributes(@Nonnull TextAttributes attributes);

    @Nonnull
    Builder textAttributes(@Nonnull TextAttributesKey attributesKey);

    // only one allowed
    @Nonnull
    Builder unescapedToolTip(@Nonnull String unescapedToolTip);

    @Nonnull
    Builder escapedToolTip(@Nonnull String escapedToolTip);

    @Nonnull
    Builder endOfLine();

    @Nonnull
    Builder needsUpdateOnTyping(boolean update);

    @Nonnull
    Builder severity(@Nonnull HighlightSeverity severity);

    @Nonnull
    Builder fileLevelAnnotation();

    @Nonnull
    Builder navigationShift(int navigationShift);

    @Nonnull
    Builder group(int group);

    /**
     * @return null means filtered out
     */
    @Nullable
    HighlightInfo create();

    @Nonnull
    HighlightInfo createUnconditionally();
  }

  @Nonnull
  static Builder newHighlightInfo(@Nonnull HighlightInfoType type) {
    return HighlightInfoFactory.getInstance().createBuilder(type);
  }

  @Nullable
  static HighlightInfo fromRangeHighlighter(@Nonnull RangeHighlighter highlighter) {
    Object errorStripeTooltip = highlighter.getErrorStripeTooltip();
    return errorStripeTooltip instanceof HighlightInfo ? (HighlightInfo)errorStripeTooltip : null;
  }

  @Nonnull
  HighlightSeverity getSeverity();

  @Nullable
  RangeHighlighter getHighlighter();

  int getActualStartOffset();

  int getActualEndOffset();

  String getDescription();

  HighlightInfoType getType();

  @Nullable
  TextAttributes getTextAttributes(@Nullable PsiElement element, @Nullable EditorColorsScheme editorColorsScheme);

  @Nullable
  GutterMark getGutterIconRenderer();

  void registerFix(@Nullable IntentionAction action, @Nullable List<IntentionAction> options, @Nullable String displayName, @Nullable TextRange fixRange, @Nullable HighlightDisplayKey key);

  void unregisterQuickFix(@Nonnull Predicate<? super IntentionAction> condition);
}
