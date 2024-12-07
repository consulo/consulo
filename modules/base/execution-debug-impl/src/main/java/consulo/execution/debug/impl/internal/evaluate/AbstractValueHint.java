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
package consulo.execution.debug.impl.internal.evaluate;

import consulo.application.Application;
import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorColors;
import consulo.codeEditor.EditorHolder;
import consulo.codeEditor.event.EditorMouseEvent;
import consulo.codeEditor.internal.TextAttributesPatcher;
import consulo.codeEditor.markup.HighlighterLayer;
import consulo.codeEditor.markup.HighlighterTargetArea;
import consulo.codeEditor.markup.RangeHighlighter;
import consulo.colorScheme.EditorColorsManager;
import consulo.colorScheme.EditorColorsScheme;
import consulo.colorScheme.TextAttributes;
import consulo.document.util.TextRange;
import consulo.execution.debug.XDebuggerActions;
import consulo.language.editor.hint.HintManager;
import consulo.language.editor.ui.awt.HintUtil;
import consulo.language.editor.ui.internal.HintManagerEx;
import consulo.logging.Logger;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.RelativePoint;
import consulo.ui.ex.SimpleColoredText;
import consulo.ui.ex.awt.ClickListener;
import consulo.ui.ex.awt.hint.HintHint;
import consulo.ui.ex.awt.hint.HintListener;
import consulo.ui.ex.awt.hint.LightweightHint;
import consulo.ui.ex.awt.hint.LightweightHintFactory;
import consulo.ui.ex.keymap.KeymapManager;
import consulo.ui.ex.keymap.util.KeymapUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.intellij.lang.annotations.JdkConstants;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.EventObject;

/**
 * @author nik
 */
public abstract class AbstractValueHint {
    private static final Logger LOG = Logger.getInstance(AbstractValueHint.class);

    private final KeyListener myEditorKeyListener = new KeyAdapter() {
        @Override
        public void keyReleased(KeyEvent e) {
            if (!isAltMask(e.getModifiers())) {
                ValueLookupManager.getInstance(myProject).hideHint();
            }
        }
    };

    private RangeHighlighter myHighlighter;
    private Cursor myStoredCursor;
    private final Project myProject;
    private final Editor myEditor;
    private final ValueHintType myType;
    protected final Point myPoint;
    private LightweightHint myCurrentHint;
    private boolean myHintHidden;
    private TextRange myCurrentRange;
    private Runnable myHideRunnable;

    public AbstractValueHint(@Nonnull Project project, @Nonnull Editor editor, @Nonnull Point point, @Nonnull ValueHintType type,
                             final TextRange textRange) {
        myPoint = point;
        myProject = project;
        myEditor = editor;
        myType = type;
        myCurrentRange = textRange;
    }

    protected abstract boolean canShowHint();

    protected abstract void evaluateAndShowHint();

    public boolean isKeepHint(Editor editor, Point point) {
        if (myCurrentHint != null && myCurrentHint.canControlAutoHide()) {
            return true;
        }

        if (myType == ValueHintType.MOUSE_ALT_OVER_HINT) {
            return false;
        }
        else if (myType == ValueHintType.MOUSE_CLICK_HINT) {
            if (myCurrentHint != null && myCurrentHint.isVisible()) {
                return true;
            }
        }
        else {
            if (isInsideCurrentRange(editor, point)) {
                return true;
            }
        }
        return false;
    }

    boolean isInsideCurrentRange(Editor editor, Point point) {
        return myCurrentRange != null && myCurrentRange.contains(calculateOffset(editor, point));
    }

    public static int calculateOffset(@Nonnull Editor editor, @Nonnull Point point) {
        return editor.logicalPositionToOffset(editor.xyToLogicalPosition(point));
    }

    public void hideHint() {
        myHintHidden = true;
        myCurrentRange = null;
        if (myStoredCursor != null) {
            Component internalComponent = myEditor.getContentComponent();
            internalComponent.setCursor(myStoredCursor);
            if (LOG.isDebugEnabled()) {
                LOG.debug("internalComponent.setCursor(myStoredCursor)");
            }
            internalComponent.removeKeyListener(myEditorKeyListener);
        }

        if (myCurrentHint != null) {
            myCurrentHint.hide();
            myCurrentHint = null;
        }
        if (myHighlighter != null) {
            myHighlighter.dispose();
            myHighlighter = null;
        }
    }

    public void invokeHint() {
        invokeHint(null);
    }

    public void invokeHint(Runnable hideRunnable) {
        myHideRunnable = hideRunnable;

        if (!canShowHint()) {
            hideHint();
            return;
        }

        if (myType == ValueHintType.MOUSE_ALT_OVER_HINT) {
            EditorColorsScheme scheme = EditorColorsManager.getInstance().getGlobalScheme();
            TextAttributes attributes = scheme.getAttributes(EditorColors.REFERENCE_HYPERLINK_COLOR);
            attributes = TextAttributesPatcher.patchAttributesColor(attributes, myCurrentRange, myEditor);

            myHighlighter = myEditor.getMarkupModel().addRangeHighlighter(myCurrentRange.getStartOffset(), myCurrentRange.getEndOffset(),
                HighlighterLayer.SELECTION + 1, attributes,
                HighlighterTargetArea.EXACT_RANGE);
            Component internalComponent = myEditor.getContentComponent();
            myStoredCursor = internalComponent.getCursor();
            internalComponent.addKeyListener(myEditorKeyListener);
            internalComponent.setCursor(hintCursor());
            if (LOG.isDebugEnabled()) {
                LOG.debug("internalComponent.setCursor(hintCursor())");
            }
        }
        else {
            evaluateAndShowHint();
        }
    }

    private static Cursor hintCursor() {
        return Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);
    }

    public Project getProject() {
        return myProject;
    }

    @Nonnull
    protected Editor getEditor() {
        return myEditor;
    }

    protected ValueHintType getType() {
        return myType;
    }

    private boolean myInsideShow = false;

    @RequiredUIAccess
    protected boolean showHint(final JComponent component) {
        myInsideShow = true;
        if (myCurrentHint != null) {
            myCurrentHint.hide();
        }
        LightweightHintFactory factory = Application.get().getInstance(LightweightHintFactory.class);

        myCurrentHint = factory.create(component);
        myCurrentHint.setAutoHideTester(event -> {
            InputEvent inputEvent = event.getInputEvent();
            if (inputEvent instanceof MouseEvent) {
                Component comp = inputEvent.getComponent();
                if (comp instanceof EditorHolder) {
                    Editor editor = ((EditorHolder) comp).getEditor();
                    return !isInsideCurrentRange(editor, ((MouseEvent) inputEvent).getPoint());
                }
            }
            return true;
        });
        myCurrentHint.addHintListener(new HintListener() {
            @Override
            public void hintHidden(EventObject event) {
                if (myHideRunnable != null && !myInsideShow) {
                    myHideRunnable.run();
                }
                onHintHidden();
            }
        });

        // editor may be disposed before later invokator process this action
        if (myEditor.isDisposed() || myEditor.getComponent().getRootPane() == null) {
            return false;
        }

        HintManagerEx hintManager = (HintManagerEx) HintManager.getInstance();

        Point p = hintManager.getHintPosition(myCurrentHint, myEditor, myEditor.xyToLogicalPosition(myPoint), HintManager.UNDER);
        HintHint hint = hintManager.createHintHint(myEditor, p, myCurrentHint, HintManager.UNDER, true);
        hint.setShowImmediately(true);
        hintManager.showEditorHint(myCurrentHint, myEditor, p,
            HintManager.HIDE_BY_ANY_KEY |
                HintManager.HIDE_BY_TEXT_CHANGE |
                HintManager.HIDE_BY_SCROLLING, 0, false,
            hint);
        myInsideShow = false;
        return true;
    }

    protected void onHintHidden() {

    }

    protected boolean isHintHidden() {
        return myHintHidden;
    }

    protected JComponent createExpandableHintComponent(final SimpleColoredText text, final Runnable expand) {
        final JComponent component = HintUtil.createInformationLabel(text, PlatformIconGroup.generalAdd());
        addClickListenerToHierarchy(component, new ClickListener() {
            @Override
            public boolean onClick(@Nonnull MouseEvent event, int clickCount) {
                if (myCurrentHint != null) {
                    myCurrentHint.hide();
                }
                expand.run();
                return true;
            }
        });
        return component;
    }

    private static void addClickListenerToHierarchy(Component c, ClickListener l) {
        l.installOn(c);
        if (c instanceof Container) {
            Component[] children = ((Container) c).getComponents();
            for (Component child : children) {
                addClickListenerToHierarchy(child, l);
            }
        }
    }

    @Nullable
    protected TextRange getCurrentRange() {
        return myCurrentRange;
    }

    private static boolean isAltMask(@JdkConstants.InputEventMask int modifiers) {
        return KeymapUtil.matchActionMouseShortcutsModifiers(KeymapManager.getInstance().getActiveKeymap(),
            modifiers,
            XDebuggerActions.QUICK_EVALUATE_EXPRESSION);
    }

    @Nullable
    public static ValueHintType getHintType(final EditorMouseEvent e) {
        int modifiers = e.getMouseEvent().getModifiers();
        if (modifiers == 0) {
            return ValueHintType.MOUSE_OVER_HINT;
        }
        else if (isAltMask(modifiers)) {
            return ValueHintType.MOUSE_ALT_OVER_HINT;
        }
        return null;
    }

    public boolean isInsideHint(Editor editor, Point point) {
        return myCurrentHint != null && myCurrentHint.isInsideHint(new RelativePoint(editor.getContentComponent(), point));
    }

    protected <D> void showTreePopup(@Nonnull DebuggerTreeCreator<D> creator, @Nonnull D descriptor) {
        DebuggerTreeWithHistoryPopup.showTreePopup(creator, descriptor, getEditor(), myPoint, getProject(), myHideRunnable);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        AbstractValueHint hint = (AbstractValueHint) o;

        if (!myProject.equals(hint.myProject)) {
            return false;
        }
        if (!myEditor.equals(hint.myEditor)) {
            return false;
        }
        if (myType != hint.myType) {
            return false;
        }
        if (myCurrentRange != null ? !myCurrentRange.equals(hint.myCurrentRange) : hint.myCurrentRange != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = myProject.hashCode();
        result = 31 * result + myEditor.hashCode();
        result = 31 * result + myType.hashCode();
        result = 31 * result + (myCurrentRange != null ? myCurrentRange.hashCode() : 0);
        return result;
    }
}
