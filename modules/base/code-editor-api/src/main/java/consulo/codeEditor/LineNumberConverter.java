// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.codeEditor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Defines a mapping between document line numbers and the numbers displayed in the gutter.
 *
 * @see EditorGutter#setLineNumberConverter(LineNumberConverter, LineNumberConverter)
 * @see Increasing
 */
public interface LineNumberConverter {
  /**
   * Defines the number to be displayed in the gutter for the given document line.
   *
   * @param lineNumber 1-based line number
   * @return number to be displayed in gutter, {@code null} means no number is displayed
   */
  @Nullable
  Integer convert(@Nonnull Editor editor, int lineNumber);

  /**
   * Number which should be used to calculate the width of the line number area in the gutter.
   * This should be the largest number that will be displayed.
   * {@code null} means no width will be allocated to the line number area.
   */
  @Nullable
  Integer getMaxLineNumber(@Nonnull Editor editor);

  /**
   * Returns text to be displayed in the gutter for the given document line.
   */
  @Nullable
  default String convertLineNumberToString(@Nonnull Editor editor, int lineNumber) {
    Integer converted = convert(editor, lineNumber);
    return converted == null ? null : String.valueOf(converted);
  }

  /**
   * Returns text of the maximum line number in document which should be used
   * to calculate the width of the line number area in the gutter.
   */
  @Nullable
  default String getMaxLineNumberString(@Nonnull Editor editor) {
    Integer maxLineNumber = getMaxLineNumber(editor);
    return maxLineNumber == null ? null : String.valueOf(maxLineNumber);
  }

  LineNumberConverter DEFAULT = new LineNumberConverter() {
    @Override
    public Integer convert(@Nonnull Editor editor, int lineNumber) {
      return lineNumber;
    }

    @Override
    public Integer getMaxLineNumber(@Nonnull Editor editor) {
      return editor.getDocument().getLineCount();
    }
  };

  /**
   * Specialization of {@link LineNumberConverter} whose {@link #convert(Editor, int)} method
   * always produces monotonically increasing numbers.
   */
  interface Increasing extends LineNumberConverter {
    @Nullable
    @Override
    default Integer getMaxLineNumber(@Nonnull Editor editor) {
      for (int i = editor.getDocument().getLineCount(); i > 0; i--) {
        Integer number = convert(editor, i);
        if (number != null) return number;
      }
      return null;
    }
  }
}
