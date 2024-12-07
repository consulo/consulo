// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.editor.hint;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.application.Application;
import consulo.codeEditor.Editor;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.RelativePoint;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.intellij.lang.annotations.MagicConstant;

import javax.swing.*;
import javax.swing.event.HyperlinkListener;

/**
 * @author cdr
 */
@ServiceAPI(ComponentScope.APPLICATION)
public interface HintManager {
     static HintManager getInstance() {
        return Application.get().getInstance(HintManager.class);
    }

    // Constants for 'constraint' parameter of showErrorHint()
    short ABOVE = 1;
    short UNDER = 2;
    short LEFT = 3;
    short RIGHT = 4;
    short RIGHT_UNDER = 5;
    short DEFAULT = 6;

    @MagicConstant(intValues = {ABOVE, UNDER, LEFT, RIGHT, RIGHT_UNDER, DEFAULT})
    public @interface PositionFlags {
    }

    // Constants for 'flags' parameters
    int HIDE_BY_ESCAPE = 0x01;
    int HIDE_BY_ANY_KEY = 0x02;
    int HIDE_BY_LOOKUP_ITEM_CHANGE = 0x04;
    int HIDE_BY_TEXT_CHANGE = 0x08;
    int HIDE_BY_OTHER_HINT = 0x10;
    int HIDE_BY_SCROLLING = 0x20;
    int HIDE_IF_OUT_OF_EDITOR = 0x40;
    int UPDATE_BY_SCROLLING = 0x80;
    int HIDE_BY_MOUSEOVER = 0x100;
    int DONT_CONSUME_ESCAPE = 0x200;
    int HIDE_BY_CARET_MOVE = 0x400;

    @MagicConstant(flags = {HIDE_BY_ESCAPE, HIDE_BY_ANY_KEY, HIDE_BY_LOOKUP_ITEM_CHANGE, HIDE_BY_TEXT_CHANGE, HIDE_BY_OTHER_HINT, HIDE_BY_SCROLLING, HIDE_IF_OUT_OF_EDITOR, UPDATE_BY_SCROLLING,
        HIDE_BY_MOUSEOVER, DONT_CONSUME_ESCAPE, HIDE_BY_CARET_MOVE})
    public @interface HideFlags {
    }

    @RequiredUIAccess
    void showHint(@Nonnull JComponent component, @Nonnull RelativePoint p, @HideFlags int flags, int timeout);

    @RequiredUIAccess
    void showErrorHint(@Nonnull Editor editor, @Nonnull String text);

    @RequiredUIAccess
    void showErrorHint(@Nonnull Editor editor, @Nonnull String text, @PositionFlags short position);

    @RequiredUIAccess
    default void showInformationHint(@Nonnull Editor editor, @Nonnull String text) {
        showInformationHint(editor, text, ABOVE);
    }

    @RequiredUIAccess
    void showInformationHint(@Nonnull Editor editor, @Nonnull String text, @PositionFlags short position);

    @RequiredUIAccess
    void showInformationHint(@Nonnull Editor editor, @Nonnull String text, @Nullable HyperlinkListener listener);

    @RequiredUIAccess
    void showInformationHint(@Nonnull Editor editor, @Nonnull JComponent component);

    @RequiredUIAccess
    void showQuestionHint(@Nonnull Editor editor, @Nonnull String hintText, int offset1, int offset2, @Nonnull QuestionAction action);

    @RequiredUIAccess
    boolean hideHints(@HideFlags int mask, boolean onlyOne, boolean editorChanged);

    @RequiredUIAccess
    void showErrorHint(@Nonnull Editor editor, @Nonnull String hintText, int offset1, int offset2, @PositionFlags short constraint, @HideFlags int flags, int timeout);

    @RequiredUIAccess
    void hideAllHints();

    @RequiredUIAccess
    boolean hasShownHintsThatWillHideByOtherHint(boolean willShowTooltip);

    @RequiredUIAccess
    void setRequestFocusForNextHint(boolean requestFocus);
}
