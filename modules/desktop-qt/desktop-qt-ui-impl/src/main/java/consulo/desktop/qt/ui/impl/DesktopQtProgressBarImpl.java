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
package consulo.desktop.qt.ui.impl;

import consulo.desktop.qt.ui.impl.image.DesktopQtSpinnerPainter;
import consulo.ui.ProgressBarStyle;
import consulo.ui.impl.ProgressBarPresentation;
import consulo.ui.style.StyleColorValue;
import io.qt.core.QTimer;
import io.qt.core.Qt;
import io.qt.gui.QColor;
import io.qt.gui.QHideEvent;
import io.qt.gui.QPaintEvent;
import io.qt.gui.QPainter;
import io.qt.gui.QPalette;
import io.qt.gui.QShowEvent;
import io.qt.widgets.QProgressBar;
import io.qt.widgets.QWidget;

import java.util.EnumSet;
import java.util.Set;

/**
 * @author VISTALL
 * @since 2026-08-16
 */
public class DesktopQtProgressBarImpl extends QtComponentDelegate<QWidget> implements consulo.ui.ProgressBar {
    private static final int ourSpinnerSize = 16;

    /**
     * The ring the awt frontend draws for {@link ProgressBarStyle#SPINNER} and the web frontend turns its bar
     * into with a style sheet. A {@link QProgressBar} of an empty range answers a sliding bar of its own, which
     * is a whole status bar wide - there is no shape of a qt bar which is a ring, so it is painted here.
     */
    private class SpinnerWidget extends QWidget {
        private final QTimer myTimer;

        private SpinnerWidget(QWidget parent) {
            super(parent);

            setFixedSize(ourSpinnerSize, ourSpinnerSize);

            myTimer = new QTimer(this);
            myTimer.setSingleShot(true);
            myTimer.setTimerType(Qt.TimerType.PreciseTimer);
            myTimer.timeout.connect(this::syncTimer);
        }

        private void syncTimer() {
            if (myIndeterminate && isVisible()) {
                if (!myTimer.isActive()) {
                    myTimer.start((int) DesktopQtSpinnerPainter.millisToNextStep());
                }
            }
            else if (myTimer.isActive()) {
                myTimer.stop();
            }

            update();
        }

        @Override
        protected void showEvent(QShowEvent event) {
            super.showEvent(event);

            syncTimer();
        }

        @Override
        protected void hideEvent(QHideEvent event) {
            super.hideEvent(event);

            syncTimer();
        }

        @Override
        protected void paintEvent(QPaintEvent event) {
            QPalette.ColorGroup group = isEnabled() ? QPalette.ColorGroup.Active : QPalette.ColorGroup.Disabled;
            QColor color = palette().color(group, QPalette.ColorRole.WindowText);

            QPainter painter = new QPainter(this);
            try {
                if (myIndeterminate) {
                    DesktopQtSpinnerPainter.paintBusy(painter, rect(), color);
                }
                else {
                    DesktopQtSpinnerPainter.paintProgress(painter, rect(), color, fraction());
                }
            }
            finally {
                painter.end();
            }
        }
    }

    /** Qt's QWIDGETSIZE_MAX: no limit on the height. */
    private static final int QWIDGETSIZE_MAX = 16777215;

    private boolean myIndeterminate;
    private boolean mySpinner;
    private int myMinimum;
    private int myMaximum = 100;
    private int myValue;

    private final Set<ProgressBarStyle> myStyles = EnumSet.noneOf(ProgressBarStyle.class);

    @Override
    protected QWidget createQt(QWidget parent) {
        if (mySpinner) {
            return new SpinnerWidget(parent);
        }

        QProgressBar bar = new QProgressBar(parent);
        bar.setTextVisible(false);
        return bar;
    }

    @Override
    protected void initialize(QWidget component) {
        super.initialize(component);

        updateProgress();

        for (ProgressBarStyle style : myStyles) {
            applyStyle(style);
        }
    }

    private double fraction() {
        int range = myMaximum - myMinimum;
        if (range <= 0) {
            return 0;
        }

        return Math.clamp((myValue - myMinimum) / (double) range, 0, 1);
    }

    @Override
    public void setIndeterminate(boolean value) {
        myIndeterminate = value;

        updateProgress();
    }

    @Override
    public boolean isIndeterminate() {
        return myIndeterminate;
    }

    @Override
    public void setMinimum(int value) {
        myMinimum = value;

        updateProgress();
    }

    @Override
    public void setMaximum(int value) {
        myMaximum = value;

        updateProgress();
    }

    @Override
    public void setValue(int value) {
        myValue = value;

        updateProgress();
    }

    @Override
    public void addStyle(ProgressBarStyle style) {
        if (style.isTone() || style.isHeight()) {
            ProgressBarPresentation.add(myStyles, style);
            applyToneAndHeight();
            return;
        }

        myStyles.add(style);

        if (style == ProgressBarStyle.SPINNER) {
            mySpinner = true;
        }

        applyStyle(style);
    }

    private void applyStyle(ProgressBarStyle style) {
        if (!(myComponent instanceof QProgressBar bar)) {
            return;
        }

        if (style == ProgressBarStyle.TRANSPARENT_BACKGROUND) {
            bar.setAutoFillBackground(false);
        }
        else if (style.isTone() || style.isHeight()) {
            applyToneAndHeight();
        }
    }

    /**
     * A tone is a style sheet: once the chunk is styled qt draws the whole bar from the sheet, so the groove is given a
     * look of its own too, taken from the palette. A height fixes the height of the bar.
     */
    private void applyToneAndHeight() {
        if (!(myComponent instanceof QProgressBar bar)) {
            return;
        }

        StyleColorValue tone = ProgressBarPresentation.valueColor(myStyles);
        if (tone != null) {
            QColor fill = DesktopQtStyleApplier.themeColor(tone, bar.palette().color(QPalette.ColorRole.Highlight));
            QColor groove = bar.palette().color(QPalette.ColorRole.Mid);
            bar.setStyleSheet("QProgressBar { border: none; border-radius: 3px; background-color: " + groove.name() + "; }"
                + " QProgressBar::chunk { border-radius: 3px; background-color: " + fill.name() + "; }");
        }
        else {
            bar.setStyleSheet("");
        }

        int height = ProgressBarPresentation.barHeight(myStyles);
        if (height > 0) {
            bar.setFixedHeight(height);
        }
        else {
            bar.setMinimumHeight(0);
            bar.setMaximumHeight(QWIDGETSIZE_MAX);
        }
    }

    /** a range of zero to zero is how qt is told to show the busy animation instead of a filled bar */
    private void updateProgress() {
        if (myComponent instanceof QProgressBar bar) {
            if (myIndeterminate) {
                bar.setRange(0, 0);
            }
            else {
                bar.setRange(myMinimum, myMaximum);
                bar.setValue(myValue);
            }
        }
        else if (myComponent instanceof SpinnerWidget spinner) {
            spinner.syncTimer();
        }
    }
}
