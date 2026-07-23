// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

/*
 * Copyright 2013-2026 consulo.io
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
package consulo.desktop.awt.editor.impl;

import consulo.application.Application;
import consulo.application.util.registry.Registry;
import consulo.codeEditor.LogicalPosition;
import consulo.ui.UIAccessScheduler;
import consulo.util.lang.Pair;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

class EditorCaretMoveService {
    private static final long TICK_MS = 4;

    private static final EditorCaretMoveService ourInstance = new EditorCaretMoveService();

    static EditorCaretMoveService getInstance() {
        return ourInstance;
    }

    /**
     * Set the cursor position immediately without animation. This does not go through the
     * coroutine-based logic which can delay the cursor position update. This is required for
     * the ImmediatePainterTest to work.
     */
    void setCursorPositionImmediately(DesktopEditorImpl editor) {
        ScheduledFuture<?> job = editor.caretAnimationHandle;
        if (job != null) {
            job.cancel(false);
            editor.caretAnimationHandle = null;
        }

        List<CaretUpdate> updates = editor.calculateCaretUpdates();
        DesktopEditorImpl.CaretRectangle[] rects = new DesktopEditorImpl.CaretRectangle[updates.size()];
        for (int i = 0; i < updates.size(); i++) {
            CaretUpdate update = updates.get(i);
            editor.lastPosMap.put(update.caret(), Pair.create(update.finalPos(), update.finalLogicalPosition()));
            rects[i] = new DesktopEditorImpl.CaretRectangle(update.finalPos(), update.width(), update.caret(), update.isRtl());
        }
        editor.myCaretCursor.setPositions(rects);
    }

    void setCursorPosition(DesktopEditorImpl editor) {
        ScheduledFuture<?> prev = editor.caretAnimationHandle;
        if (prev != null) {
            prev.cancel(false);
        }

        DesktopEditorImpl.CaretCursor cursor = editor.myCaretCursor;
        int animationDuration = Registry.intValue("editor.smooth.caret.duration");

        cursor.setBlinkOpacity(1.0f);
        cursor.setStartTime(System.currentTimeMillis() + animationDuration);

        List<CaretUpdate> updates = editor.calculateCaretUpdates();
        List<CaretAnimationState> animationStates = new ArrayList<>(updates.size());
        for (CaretUpdate update : updates) {
            Pair<Point2D, LogicalPosition> last =
                editor.lastPosMap.computeIfAbsent(update.caret(), c -> Pair.create(update.finalPos(), update.finalLogicalPosition()));
            animationStates.add(new CaretAnimationState(last.getFirst(), last.getSecond(), update));
        }

        CaretEasing easing = CaretEasing.fromSettings(editor.getSettings());
        Animation animation = new Animation(editor, animationStates, System.currentTimeMillis(), animationDuration, easing);

        UIAccessScheduler scheduler = Application.get().getLastUIAccess().getScheduler();
        editor.caretAnimationHandle = scheduler.scheduleWithFixedDelay(animation, 0, TICK_MS, TimeUnit.MILLISECONDS);
    }

    private static final class Animation implements Runnable {
        private final DesktopEditorImpl myEditor;
        private final List<CaretAnimationState> myStates;
        private final long myStartTime;
        private final int myDuration;
        private final CaretEasing myEasing;

        private Animation(DesktopEditorImpl editor,
                          List<CaretAnimationState> states,
                          long startTime,
                          int duration,
                          CaretEasing easing) {
            myEditor = editor;
            myStates = states;
            myStartTime = startTime;
            myDuration = duration;
            myEasing = easing;
        }

        @Override
        public void run() {
            long now = System.currentTimeMillis();
            long elapsed = now - myStartTime;

            double t = Math.min(1.0 * elapsed / myDuration, 1.0);

            boolean allDone = true;

            DesktopEditorImpl.CaretRectangle[] interpolatedRects = new DesktopEditorImpl.CaretRectangle[myStates.size()];
            for (int i = 0; i < myStates.size(); i++) {
                CaretAnimationState state = myStates.get(i);
                CaretUpdate update = state.update();

                boolean sameLogicalPosition = Objects.equals(state.startLogicalPosition(), update.finalLogicalPosition());
                boolean isInAnimation = !sameLogicalPosition && t < 1;

                if (isInAnimation) {
                    allDone = false;
                }

                Point2D startPos = state.startPos();
                Point2D finalPos = update.finalPos();

                double ease = myEasing.apply(t);
                double x = startPos.getX() + (finalPos.getX() - startPos.getX()) * ease;
                double y = startPos.getY() + (finalPos.getY() - startPos.getY()) * ease;

                Point2D interpolated = isInAnimation ? new Point2D.Double(x, y) : finalPos;
                myEditor.lastPosMap.put(update.caret(), Pair.create(interpolated, isInAnimation ? null : update.finalLogicalPosition()));
                interpolatedRects[i] = new DesktopEditorImpl.CaretRectangle(interpolated, update.width(), update.caret(), update.isRtl());
            }

            DesktopEditorImpl.CaretCursor cursor = myEditor.myCaretCursor;
            cursor.repaint();
            cursor.setPositions(interpolatedRects);
            cursor.repaint();

            if (allDone) {
                ScheduledFuture<?> handle = myEditor.caretAnimationHandle;
                if (handle != null) {
                    handle.cancel(false);
                    myEditor.caretAnimationHandle = null;
                }
            }
        }
    }
}
