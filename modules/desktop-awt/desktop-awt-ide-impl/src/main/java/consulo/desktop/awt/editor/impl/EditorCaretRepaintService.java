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
import consulo.ui.UIAccessScheduler;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

class EditorCaretRepaintService implements Runnable {
    private static final long TICK_MS = 4;

    private long mySleepTime = 500;
    private boolean myIsBlinkCaret = true;

    protected @Nullable DesktopEditorImpl myEditor;
    protected ScheduledFuture<?> mySchedulerHandle;

    private long myPhaseStart;
    private boolean myFadingOut = true;

    public void start() {
        if (mySchedulerHandle != null) {
            mySchedulerHandle.cancel(false);
        }

        myPhaseStart = System.currentTimeMillis();
        myFadingOut = true;

        long tick = isSmoothBlinking() ? TICK_MS : mySleepTime;

        UIAccessScheduler scheduler = Application.get().getLastUIAccess().getScheduler();
        mySchedulerHandle = scheduler.scheduleWithFixedDelay(this, tick, tick, TimeUnit.MILLISECONDS);
    }

    protected void setBlinkPeriod(int blinkPeriod) {
        mySleepTime = Math.max(blinkPeriod, 10);
        start();
    }

    protected void setBlinkCaret(boolean value) {
        myIsBlinkCaret = value;
    }

    @Override
    public void run() {
        if (isSmoothBlinking()) {
            blinkSmooth();
        }
        else {
            blinkNormal();
        }
    }

    private boolean isSmoothBlinking() {
        DesktopEditorImpl editor = myEditor;
        return editor != null && editor.getSettings().isSmoothCaretBlinking();
    }

    private void blinkNormal() {
        DesktopEditorImpl editor = myEditor;
        if (editor == null) {
            return;
        }

        DesktopEditorImpl.CaretCursor cursor = editor.myCaretCursor;
        cursor.setBlinkOpacity(1.0f);

        long time = System.currentTimeMillis() - cursor.getStartTime();
        if (time > mySleepTime) {
            boolean toRepaint = true;
            if (myIsBlinkCaret) {
                cursor.setActive(!cursor.isActive());
            }
            else {
                toRepaint = !cursor.isActive();
                cursor.setActive(true);
            }
            if (toRepaint) {
                cursor.repaint();
            }
        }
    }

    private void blinkSmooth() {
        DesktopEditorImpl editor = myEditor;
        if (editor == null) {
            return;
        }

        double visualBlinkPeriod = 1.2 * mySleepTime;
        double phaseDuration = visualBlinkPeriod / 2.0;
        double holdDuration = visualBlinkPeriod - phaseDuration;

        DesktopEditorImpl.CaretCursor cursor = editor.myCaretCursor;

        long now = System.currentTimeMillis();
        if (!myIsBlinkCaret || now - cursor.getStartTime() < mySleepTime) {
            cursor.setFullOpacity();
            cursor.repaint();
            myPhaseStart = now;
            myFadingOut = true;
            return;
        }

        long elapsed = now - myPhaseStart;
        double opacity;
        if (elapsed < phaseDuration) {
            double t = Math.min(Math.max(elapsed / phaseDuration, 0.0), 1.0);
            opacity = myFadingOut ? 1.0 - easeInOutCubic(t) : easeOutQuint(t);
        }
        else if (elapsed < phaseDuration + holdDuration) {
            opacity = myFadingOut ? 0.0 : 1.0;
        }
        else {
            myFadingOut = !myFadingOut;
            myPhaseStart = now;
            opacity = myFadingOut ? 1.0 : 0.0;
        }

        cursor.setActive(opacity >= 1e-2);
        cursor.setBlinkOpacity((float) opacity);
        cursor.repaint();
    }

    private static double easeOutQuint(double t) {
        double inv = 1 - t;
        return 1 - inv * inv * inv * inv * inv;
    }

    private static double easeInOutCubic(double t) {
        if (t < 0.5) {
            return 4 * t * t * t;
        }
        double f = -2 * t + 2;
        return 1 - (f * f * f) / 2;
    }
}
