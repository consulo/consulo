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

import consulo.ui.ProgressBarStyle;
import io.qt.core.QRectF;
import io.qt.core.QTimer;
import io.qt.core.Qt;
import io.qt.gui.QColor;
import io.qt.gui.QHideEvent;
import io.qt.gui.QPaintEvent;
import io.qt.gui.QPainter;
import io.qt.gui.QPalette;
import io.qt.gui.QPen;
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
    private static final int ourSpinnerThickness = 2;
    private static final int ourSpinnerFrameDelay = 40;
    private static final int ourSpinnerStep = 12;
    private static final int ourSpinnerArc = 100;
    private static final int ourTrackAlpha = 51;

    /** the angle of a qt arc is a sixteenth of a degree, and the zero of it points at three o'clock */
    private static final int ourDegree = 16;
    private static final int ourTopAngle = 90 * ourDegree;

    /**
     * The ring the awt frontend draws for {@link ProgressBarStyle#SPINNER} and the web frontend turns its bar
     * into with a style sheet. A {@link QProgressBar} of an empty range answers a sliding bar of its own, which
     * is a whole status bar wide - there is no shape of a qt bar which is a ring, so it is painted here.
     */
    private class SpinnerWidget extends QWidget {
        private final QTimer myTimer;

        private int myAngle;

        private SpinnerWidget(QWidget parent) {
            super(parent);

            setFixedSize(ourSpinnerSize, ourSpinnerSize);

            myTimer = new QTimer(this);
            myTimer.setInterval(ourSpinnerFrameDelay);
            myTimer.timeout.connect(() -> {
                myAngle = (myAngle + ourSpinnerStep) % 360;
                update();
            });
        }

        private void syncTimer() {
            if (myIndeterminate && isVisible()) {
                if (!myTimer.isActive()) {
                    myTimer.start();
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

            double inset = ourSpinnerThickness / 2.0;
            QRectF ring = new QRectF(inset, inset, width() - ourSpinnerThickness, height() - ourSpinnerThickness);

            QPainter painter = new QPainter(this);
            try {
                painter.setRenderHint(QPainter.RenderHint.Antialiasing, true);

                QPen pen = new QPen(new QColor(color.red(), color.green(), color.blue(), ourTrackAlpha));
                pen.setWidth(ourSpinnerThickness);
                pen.setCapStyle(Qt.PenCapStyle.RoundCap);

                painter.setPen(pen);
                painter.drawArc(ring, 0, 360 * ourDegree);

                pen.setColor(color);
                painter.setPen(pen);

                if (myIndeterminate) {
                    painter.drawArc(ring, ourTopAngle - myAngle * ourDegree, -ourSpinnerArc * ourDegree);
                }
                else {
                    painter.drawArc(ring, ourTopAngle, -(int) (fraction() * 360 * ourDegree));
                }
            }
            finally {
                painter.end();
            }
        }
    }

    private boolean myIndeterminate;
    private boolean mySpinner;
    private int myMinimum;
    private int myMaximum = 100;
    private int myValue;

    private final Set<ProgressBarStyle> myStyles = EnumSet.noneOf(ProgressBarStyle.class);

    @Override
    protected QWidget createQt(QWidget parent) {
        return mySpinner ? new SpinnerWidget(parent) : new QProgressBar(parent);
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

        switch (style) {
            case SPINNER -> bar.setTextVisible(false);
            case TRANSPARENT_BACKGROUND -> bar.setAutoFillBackground(false);
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
