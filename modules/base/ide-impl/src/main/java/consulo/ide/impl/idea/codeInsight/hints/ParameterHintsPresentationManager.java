// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.ide.impl.idea.codeInsight.hints;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.application.ApplicationManager;
import consulo.codeEditor.*;
import consulo.colorScheme.EffectType;
import consulo.colorScheme.TextAttributes;
import consulo.colorScheme.TextAttributesEffectsBuilder;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.language.editor.inlay.HintWidthAdjustment;
import consulo.ui.ModalityState;
import consulo.ui.UIAccess;
import consulo.ui.color.ColorValue;
import consulo.ui.ex.awt.util.Alarm;
import consulo.util.dataholder.Key;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Singleton;
import org.jetbrains.annotations.TestOnly;

import java.awt.*;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

@ServiceAPI(ComponentScope.APPLICATION)
@ServiceImpl
@Singleton
public final class ParameterHintsPresentationManager implements Disposable {
    private static final Key<AnimationStep> ANIMATION_STEP = Key.create("ParameterHintAnimationStep");
    private static final Key<Boolean> PREVIEW_MODE = Key.create("ParameterHintsPreviewMode");

    private static final int ANIMATION_STEP_MS = 25;
    private static final int ANIMATION_CHARS_PER_STEP = 3;

    private final Alarm myAlarm = new Alarm(this);

    public static ParameterHintsPresentationManager getInstance() {
        return ApplicationManager.getApplication().getService(ParameterHintsPresentationManager.class);
    }

    public List<Inlay<?>> getParameterHintsInRange(@Nonnull Editor editor, int startOffset, int endOffset) {
        //noinspection unchecked
        return (List) editor.getInlayModel().getInlineElementsInRange(startOffset, endOffset, MyRenderer.class);
    }

    public boolean isParameterHint(@Nonnull Inlay<?> inlay) {
        return inlay.getRenderer() instanceof MyRenderer;
    }

    public String getHintText(@Nonnull Inlay inlay) {
        EditorCustomElementRenderer renderer = inlay.getRenderer();
        return renderer instanceof MyRenderer myRenderer ? myRenderer.getText() : null;
    }

    public Inlay addHint(@Nonnull Editor editor, int offset, boolean relatesToPrecedingText, @Nonnull String hintText,
                         @Nullable HintWidthAdjustment widthAdjuster, boolean useAnimation) {
        MyRenderer renderer = new MyRenderer(editor, hintText, widthAdjuster, useAnimation);
        Inlay inlay = editor.getInlayModel().addInlineElement(offset, relatesToPrecedingText, renderer);
        if (inlay != null) {
            if (useAnimation) {
                scheduleRendererUpdate(editor, inlay);
            }
        }
        return inlay;
    }

    public void deleteHint(@Nonnull Editor editor, @Nonnull Inlay hint, boolean useAnimation) {
        if (useAnimation) {
            updateRenderer(editor, hint, null, null, true);
        }
        else {
            Disposer.dispose(hint);
        }
    }

    public void replaceHint(@Nonnull Editor editor, @Nonnull Inlay hint, @Nonnull String newText, @Nullable HintWidthAdjustment widthAdjuster,
                            boolean useAnimation) {
        updateRenderer(editor, hint, newText, widthAdjuster, useAnimation);
    }

    public void setHighlighted(@Nonnull Inlay hint, boolean highlighted) {
        if (!isParameterHint(hint)) {
            throw new IllegalArgumentException("Not a parameter hint");
        }
        MyRenderer renderer = (MyRenderer) hint.getRenderer();
        boolean oldValue = renderer.highlighted;
        if (highlighted != oldValue) {
            renderer.highlighted = highlighted;
            hint.repaint();
        }
    }

    public boolean isHighlighted(@Nonnull Inlay hint) {
        if (!isParameterHint(hint)) {
            throw new IllegalArgumentException("Not a parameter hint");
        }
        MyRenderer renderer = (MyRenderer) hint.getRenderer();
        return renderer.highlighted;
    }

    public void setCurrent(@Nonnull Inlay hint, boolean current) {
        if (!isParameterHint(hint)) {
            throw new IllegalArgumentException("Not a parameter hint");
        }
        MyRenderer renderer = (MyRenderer) hint.getRenderer();
        boolean oldValue = renderer.current;
        if (current != oldValue) {
            renderer.current = current;
            hint.repaint();
        }
    }

    public boolean isCurrent(@Nonnull Inlay hint) {
        if (!isParameterHint(hint)) {
            throw new IllegalArgumentException("Not a parameter hint");
        }
        MyRenderer renderer = (MyRenderer) hint.getRenderer();
        return renderer.current;
    }

    public void setPreviewMode(Editor editor, boolean b) {
        PREVIEW_MODE.set(editor, b);
    }

    private void updateRenderer(@Nonnull Editor editor, @Nonnull Inlay hint, @Nullable String newText, HintWidthAdjustment widthAdjuster,
                                boolean useAnimation) {
        MyRenderer renderer = (MyRenderer) hint.getRenderer();
        renderer.update(editor, newText, widthAdjuster, useAnimation);
        hint.update();
        if (useAnimation) {
            scheduleRendererUpdate(editor, hint);
        }
    }

    @Override
    public void dispose() {
    }

    private void scheduleRendererUpdate(@Nonnull Editor editor, @Nonnull Inlay inlay) {
        UIAccess.assertIsUIThread(); // to avoid race conditions in "new AnimationStep"
        AnimationStep step = editor.getUserData(ANIMATION_STEP);
        if (step == null) {
            editor.putUserData(ANIMATION_STEP, step = new AnimationStep(editor));
        }
        step.inlays.add(inlay);
        scheduleAnimationStep(step);
    }

    private void scheduleAnimationStep(@Nonnull AnimationStep step) {
        myAlarm.cancelRequest(step);
        myAlarm.addRequest(step, ANIMATION_STEP_MS, ModalityState.any());
    }

    @TestOnly
    public boolean isAnimationInProgress(@Nonnull Editor editor) {
        UIAccess.assertIsUIThread();
        return editor.getUserData(ANIMATION_STEP) != null;
    }

    private static final class MyRenderer extends HintRenderer {
        private int startWidth;
        private int steps;
        private int step;
        private boolean highlighted;
        private boolean current;

        private MyRenderer(Editor editor, String text, HintWidthAdjustment widthAdjustment, boolean animated) {
            super(text);
            updateState(editor, text, widthAdjustment, animated);
        }

        @Override
        public String toString() {
            return "[" + this.getText() + "]";
        }

        public void update(Editor editor, String newText, HintWidthAdjustment widthAdjustment, boolean animated) {
            updateState(editor, newText, widthAdjustment, animated);
        }

        @Override
        protected @Nullable TextAttributes getTextAttributes(@Nonnull Editor editor) {
            if (step > steps || startWidth != 0) {
                TextAttributes attributes = editor.getColorsScheme().getAttributes(current
                    ? DefaultLanguageHighlighterColors.INLINE_PARAMETER_HINT_CURRENT
                    : highlighted
                    ? DefaultLanguageHighlighterColors.INLINE_PARAMETER_HINT_HIGHLIGHTED
                    : DefaultLanguageHighlighterColors.INLINE_PARAMETER_HINT);
                Boolean aBoolean = PREVIEW_MODE.get(editor);
                return aBoolean != null && aBoolean ? strikeOutBuilder(editor).applyTo(attributes.clone()) : attributes;
            }
            return null;
        }

        private static TextAttributesEffectsBuilder strikeOutBuilder(Editor editor) {
            ColorValue color = editor.getColorsScheme().getAttributes(DefaultLanguageHighlighterColors.INLAY_DEFAULT).getForegroundColor();
            return TextAttributesEffectsBuilder.create().coverWith(EffectType.STRIKEOUT, color);
        }

        @Override
        public @Nonnull String getContextMenuGroupId(@Nonnull Inlay inlay) {
            return "ParameterNameHints";
        }

        private void updateState(Editor editor, String text, HintWidthAdjustment widthAdjustment, boolean animated) {
            setWidthAdjustment(widthAdjustment);
            FontMetrics metrics = getFontMetrics(editor, useEditorFont()).metrics;
            startWidth = calcHintTextWidth(getText(), metrics);
            setText(text);
            int endWidth = calcHintTextWidth(getText(), metrics);
            steps = Math.max(1, Math.abs(endWidth - startWidth) / metrics.charWidth('a') / ANIMATION_CHARS_PER_STEP);
            step = animated ? 1 : steps + 1;
        }

        public boolean nextStep() {
            return ++step <= steps;
        }

        @Override
        public int calcWidthInPixels(@Nonnull Inlay inlay) {
            int endWidth = super.calcWidthInPixels(inlay);
            return step <= steps ? Math.max(1, startWidth + (endWidth - startWidth) / steps * step) : endWidth;
        }
    }

    private final class AnimationStep implements Runnable {
        private final Editor myEditor;
        private final Set<Inlay> inlays = new HashSet<>();

        AnimationStep(@Nonnull Editor editor) {
            myEditor = editor;
            Disposer.register(((RealEditor) editor).getDisposable(), () -> myAlarm.cancelRequest(this));
        }

        @Override
        public void run() {
            Iterator<Inlay> it = inlays.iterator();
            while (it.hasNext()) {
                Inlay inlay = it.next();
                if (inlay.isValid()) {
                    MyRenderer renderer = (MyRenderer) inlay.getRenderer();
                    if (!renderer.nextStep()) {
                        it.remove();
                    }
                    if (renderer.calcWidthInPixels(inlay) == 0) {
                        Disposer.dispose(inlay);
                    }
                    else {
                        inlay.update();
                    }
                }
                else {
                    it.remove();
                }
            }
            if (inlays.isEmpty()) {
                myEditor.putUserData(ANIMATION_STEP, null);
            }
            else {
                scheduleAnimationStep(this);
            }
        }
    }
}
