/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package consulo.codeEditor;

import consulo.codeEditor.event.FocusChangeListener;
import consulo.codeEditor.markup.MarkupModelEx;
import consulo.colorScheme.EditorColorsScheme;
import consulo.colorScheme.TextAttributes;
import consulo.dataContext.DataContext;
import consulo.disposer.Disposable;
import consulo.document.Document;
import consulo.document.util.TextRange;
import consulo.project.Project;
import consulo.ui.color.ColorValue;
import consulo.ui.ex.CopyProvider;
import consulo.ui.ex.CutProvider;
import consulo.ui.ex.DeleteProvider;
import consulo.ui.ex.PasteProvider;
import consulo.util.dataholder.Key;
import consulo.util.lang.function.Condition;
import consulo.virtualFileSystem.VirtualFile;
import kava.beans.PropertyChangeListener;
import org.intellij.lang.annotations.MagicConstant;
import org.jetbrains.annotations.NonNls;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.Collection;
import java.util.function.Consumer;
import java.util.function.IntFunction;

public interface EditorEx extends Editor {
    Key<Boolean> DISABLE_CARET_SHIFT_ON_WHITESPACE_INSERTION = Key.create("editor.disable.caret.shift.on.whitespace.insertion");

    String PROP_INSERT_MODE = "insertMode";
    String PROP_COLUMN_MODE = "columnMode";
    String PROP_FONT_SIZE = "fontSize";
    Key<TextRange> LAST_PASTED_REGION = Key.create("LAST_PASTED_REGION");

    @Override
    @Nonnull
    MarkupModelEx getMarkupModel();

    /**
     * Returns the markup model for the underlying Document.
     * <p>
     * This model differs from the one from DocumentMarkupModel#forDocument,
     * as it does not contain highlighters that should not be visible in this Editor.
     * (for example, debugger breakpoints in a diff viewer editors)
     *
     * @return the markup model instance.
     * @see MarkupEditorFilter
     * @see consulo.ide.impl.idea.openapi.editor.impl.DesktopEditorImpl#setHighlightingFilter(Condition<RangeHighlighter>)
     * @see consulo.ide.impl.idea.openapi.editor.impl.DocumentMarkupModel#forDocument(Document, Project, boolean)
     */
    @Nonnull
    MarkupModelEx getFilteredDocumentMarkupModel();

    @Nonnull
    EditorGutterComponentEx getGutterComponentEx();

    @Nonnull
    EditorHighlighter getHighlighter();

    void setHighlighter(@Nonnull EditorHighlighter highlighter);

    default JComponent getPermanentHeaderComponent() {
        throw new UnsupportedOperationException("Unsupported platform");
    }

    /**
     * shouldn't be called during Document update
     */
    void setViewer(boolean isViewer);

    default void setPermanentHeaderComponent(JComponent component) {
        throw new UnsupportedOperationException("Unsupported platform");
    }

    void setColorsScheme(@Nonnull EditorColorsScheme scheme);

    void setInsertMode(boolean val);

    void setColumnMode(boolean val);

    int VERTICAL_SCROLLBAR_LEFT = 0;
    int VERTICAL_SCROLLBAR_RIGHT = 1;

    void setVerticalScrollbarOrientation(@MagicConstant(intValues = {VERTICAL_SCROLLBAR_LEFT, VERTICAL_SCROLLBAR_RIGHT}) int type);

    @MagicConstant(intValues = {VERTICAL_SCROLLBAR_LEFT, VERTICAL_SCROLLBAR_RIGHT})
    int getVerticalScrollbarOrientation();

    void setVerticalScrollbarVisible(boolean b);

    void setHorizontalScrollbarVisible(boolean b);

    CutProvider getCutProvider();

    CopyProvider getCopyProvider();

    PasteProvider getPasteProvider();

    DeleteProvider getDeleteProvider();

    default void repaint(int startOffset, int endOffset) {
        repaint(startOffset, endOffset, true);
    }

    void repaint(int startOffset, int endOffset, boolean invalidateTextLayout);

    void reinitSettings();

    void addPropertyChangeListener(@Nonnull PropertyChangeListener listener, @Nonnull Disposable parentDisposable);

    void addPropertyChangeListener(@Nonnull PropertyChangeListener listener);

    void removePropertyChangeListener(@Nonnull PropertyChangeListener listener);

    int getMaxWidthInRange(int startOffset, int endOffset);

    boolean setCaretVisible(boolean b);

    boolean setCaretEnabled(boolean enabled);

    void addFocusListener(@Nonnull FocusChangeListener listener);

    void addFocusListener(@Nonnull FocusChangeListener listener, @Nonnull Disposable parentDisposable);

    void setOneLineMode(boolean b);

    @Nonnull
    default JScrollPane getScrollPane() {
        throw new UnsupportedOperationException("Unsupported platform");
    }

    boolean isRendererMode();

    void setRendererMode(boolean isRendererMode);

    void setFile(VirtualFile vFile);

    @Nonnull
    DataContext getDataContext();

    default boolean processKeyTyped(@Nonnull KeyEvent e) {
        throw new UnsupportedOperationException("Unsupported platform");
    }

    void setFontSize(int fontSize);

    default ColorValue getBackgroundColor() {
        throw new AbstractMethodError();
    }

    default void setBackgroundColor(ColorValue color) {
        throw new AbstractMethodError();
    }

    default Dimension getContentSize() {
        throw new UnsupportedOperationException("Unsupported platform");
    }

    boolean isEmbeddedIntoDialogWrapper();

    void setEmbeddedIntoDialogWrapper(boolean b);

    TextDrawingCallback getTextDrawingCallback();

    @Nonnull
    @Override
    FoldingModelEx getFoldingModel();

    @Nonnull
    @Override
    SoftWrapModelEx getSoftWrapModel();

    @Nonnull
    @Override
    ScrollingModelEx getScrollingModel();

    /**
     * Creates color scheme delegate which is bound to current editor. E.g. all schema changes will update editor state.
     */
    @Nonnull
    EditorColorsScheme createBoundColorSchemeDelegate(@Nullable EditorColorsScheme customGlobalScheme);

    /**
     * Allows to define <code>'placeholder text'</code> for the current editor, i.e. virtual text that will be represented until
     * any user data is entered.
     * <p>
     * Feel free to see the detailed feature
     * definition <a href="http://dev.w3.org/html5/spec/Overview.html#the-placeholder-attribute">here</a>.
     *
     * @param text virtual text to show until user data is entered or the editor is focused
     */
    void setPlaceholder(@Nullable CharSequence text);

    /**
     * Sets text attributes for a placeholder. Font style and color are currently supported.
     * <code>null</code> means default values should be used.
     *
     * @see #setPlaceholder(CharSequence)
     */
    void setPlaceholderAttributes(@Nullable TextAttributes attributes);

    /**
     * Controls whether <code>'placeholder text'</code> is visible when editor is focused.
     *
     * @param show flag indicating whether placeholder is visible when editor is focused.
     * @see EditorEx#setPlaceholder(CharSequence)
     */
    void setShowPlaceholderWhenFocused(boolean show);

    /**
     * Allows to answer if 'sticky selection' is active for the current editor.
     * <p/>
     * 'Sticky selection' means that every time caret position changes, selection end offset is automatically set to the same position.
     * Selection start is always caret offset on {@link #setStickySelection(boolean)} call with <code>'true'</code> argument.
     *
     * @return <code>true</code> if 'sticky selection' mode is active at the current editor; <code>false</code> otherwise
     */
    boolean isStickySelection();

    /**
     * Allows to set current {@link #isStickySelection() sticky selection} mode.
     *
     * @param enable flag that identifies if <code>'sticky selection'</code> mode should be enabled
     */
    void setStickySelection(boolean enable);

    /**
     * @return width in pixels of the {@link #setPrefixTextAndAttributes(String, TextAttributes) prefix} used with the current editor if any;
     * zero otherwise
     */
    int getPrefixTextWidthInPixels();

    /**
     * Allows to define prefix to be displayed on every editor line and text attributes to use for its coloring.
     *
     * @param prefixText target prefix text
     * @param attributes text attributes to use during given prefix painting
     */
    void setPrefixTextAndAttributes(@Nullable String prefixText, @Nullable TextAttributes attributes);

    /**
     * @return current 'pure painting mode' status
     * @see #setPurePaintingMode(boolean)
     */
    boolean isPurePaintingMode();

    /**
     * We often re-use the logic encapsulated at the editor. For example, every time we show editor fragment (folding, preview etc) we
     * create a dedicated graphics object and ask the editor to paint into it.
     * <p>
     * The thing is that the editor itself may change its state if any postponed operation is triggered by the painting request
     * (e.g. soft wraps recalculation is triggered by the paint request and newly calculated soft wraps cause caret to change its position).
     * <p>
     * This method allows to inform the editor that all subsequent painting request should not change the editor state.
     * <p>
     * In 'pure painting mode' editor also behaves as if soft wraps were not enabled.
     *
     * @param enabled 'pure painting mode' status to use
     */
    void setPurePaintingMode(boolean enabled);

    /**
     * Registers a function which will be applied to a line number to obtain additional text fragments. The fragments returned by the
     * function will be drawn in the editor after end of the line (together with fragments returned by {@link consulo.ide.impl.idea.openapi.editor.EditorLinePainter} extensions).
     */
    void registerLineExtensionPainter(IntFunction<Collection<LineExtensionInfo>> lineExtensionPainter);

    /**
     * Allows to register a callback that will be called one each repaint of the editor vertical scrollbar.
     * This is needed to allow a parent component draw above the scrollbar components (e.g. in the merge tool),
     * otherwise the drawings are cleared once the scrollbar gets repainted (which may happen suddenly, because the scrollbar UI uses the
     * {@link Animator} to draw itself.
     *
     * @param callback callback which will be called from the {@link JComponent#paint(Graphics)} method of
     *                 the editor vertical scrollbar.
     */
    default void registerScrollBarRepaintCallback(@Nullable Consumer<Graphics> callback) {
        throw new UnsupportedOperationException("Unsupported platform");
    }

    /**
     * Sets id of action group what will be used to construct context menu displayed on mouse right button's click. Setting this to
     * <code>null</code> disables built-in logic for showing context menu (it can still be achieved by implementing corresponding mouse
     * event listener).
     *
     * @see #getContextMenuGroupId()
     */
    void setContextMenuGroupId(@Nullable String groupId);

    /**
     * Returns id of action group what will be used to construct context menu displayed on mouse right button's click. <code>null</code>
     * value means built-in logic for showing context menu is disabled.
     *
     * @see #setContextMenuGroupId(String)
     */
    @Nullable
    String getContextMenuGroupId();

    /**
     * Allows to override default editor's context popup logic.
     * <p>
     * Default handler shows a context menu corresponding to a certain action group
     * registered in {@link ActionManager}. Group's id can be changed using {@link #setContextMenuGroupId(String)}. For inline custom visual
     * elements (inlays) action group id is obtained from {@link EditorCustomElementRenderer#getContextMenuGroupId(Inlay)}.
     * <p>
     * If multiple handlers are installed, they are processed in order, starting from the most recently installed one. Processing stops when
     * some handler returns {@code true} from {@link EditorPopupHandler#handlePopup(EditorMouseEvent)} method.
     *
     * @see #uninstallPopupHandler(EditorPopupHandler)
     */
    void installPopupHandler(@Nonnull EditorPopupHandler popupHandler);

    /**
     * Removes previously installed {@link EditorPopupHandler}.
     *
     * @see #installPopupHandler(EditorPopupHandler)
     */
    void uninstallPopupHandler(@Nonnull EditorPopupHandler popupHandler);

    /**
     * If {@code cursor} parameter value is not {@code null}, sets custom cursor to {@link #getContentComponent() editor's content component},
     * otherwise restores default editor cursor management logic ({@code requestor} parameter value should be the same in both setting and
     * restoring requests). 'Restoring' call for a requestor, which hasn't set a cursor previously, has no effect. If multiple requestors have
     * currently set custom cursors, one of them will be used (it is unspecified, which one).
     */
    void setCustomCursor(@Nonnull Object requestor, @Nullable Cursor cursor);

    default void setStatusComponent(@Nonnull JComponent component) {
        // nothing
    }

    default boolean isRightAligned() {
        return false;
    }

    default void setPaintSelection(boolean paintSelection) {
    }

    default void setUseEditorAntialiasing(boolean value) {
    }

    default void resetSizes() {
    }
}
