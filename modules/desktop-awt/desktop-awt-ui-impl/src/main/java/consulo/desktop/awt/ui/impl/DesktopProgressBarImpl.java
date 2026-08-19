/*
 * Copyright 2013-2020 consulo.io
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
package consulo.desktop.awt.ui.impl;

import consulo.desktop.awt.ui.impl.facade.FromSwingComponentWrapper;
import consulo.desktop.awt.ui.impl.base.SwingComponentDelegate;
import consulo.desktop.awt.ui.impl.progressBar.SpinnerProgress;
import consulo.ui.Component;
import consulo.ui.ProgressBar;
import consulo.ui.ProgressBarStyle;

import javax.swing.*;
import java.awt.Graphics;
import java.awt.Insets;

/**
 * @author VISTALL
 * @since 2020-05-11
 */
class DesktopProgressBarImpl extends SwingComponentDelegate<JProgressBar> implements ProgressBar {
    /**
     * A layout is free to make the component as tall as it likes, but the line drawn in it keeps the height of a
     * line - {@link com.formdev.flatlaf.ui.FlatProgressBarUI} takes the track from the insets, so the leftover is
     * handed to them while the painting runs, and the line ends up centered in the space the layout gave.
     * Sizing is left alone, or the inflated insets would grow the preferred height they were computed from.
     */
    private class BaseProgressBar extends JProgressBar implements FromSwingComponentWrapper {
        private int myLineHeight;
        private boolean myPainting;

        @Override
        public Component toUIComponent() {
            return DesktopProgressBarImpl.this;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Insets insets = super.getInsets();
            myLineHeight = getPreferredSize().height - (insets.top + insets.bottom);

            myPainting = true;
            try {
                super.paintComponent(g);
            }
            finally {
                myPainting = false;
            }
        }

        @Override
        public Insets getInsets() {
            return inflate(super.getInsets());
        }

        @Override
        public Insets getInsets(Insets insets) {
            return inflate(super.getInsets(insets));
        }

        private Insets inflate(Insets insets) {
            if (!myPainting) {
                return insets;
            }

            Insets inflated = new Insets(insets.top, insets.left, insets.bottom, insets.right);
            int leftover = getHeight() - (inflated.top + inflated.bottom) - myLineHeight;
            if (leftover > 0) {
                inflated.top += leftover / 2;
                inflated.bottom += leftover - leftover / 2;
            }
            return inflated;
        }
    }

    private class SpinnerProgressBar extends SpinnerProgress implements FromSwingComponentWrapper {

        @Override
        public Component toUIComponent() {
            return DesktopProgressBarImpl.this;
        }
    }

    private boolean mySpinner;
    private boolean myTransparent;

    @Override
    protected JProgressBar createComponent() {
        JProgressBar component;
        if (mySpinner) {
            component = new SpinnerProgressBar();
        } else {
            component = new BaseProgressBar();
        }
        if (myTransparent) {
            component.setOpaque(false);
        }
        return component;
    }

    @Override
    public void addStyle(ProgressBarStyle style) {
        if (isInitialized()) {
            throw new IllegalArgumentException("Can't change after initialized");
        }

        switch (style) {
            case SPINNER:
                mySpinner = true;
                break;
            case TRANSPARENT_BACKGROUND:
                myTransparent = true;
                break;
        }
    }

    @Override
    public void setIndeterminate(boolean value) {
        toAWTComponent().setIndeterminate(value);
    }

    @Override
    public boolean isIndeterminate() {
        return toAWTComponent().isIndeterminate();
    }

    @Override
    public void setMinimum(int value) {
        toAWTComponent().setMinimum(value);
    }

    @Override
    public void setMaximum(int value) {
        toAWTComponent().setMaximum(value);
    }

    @Override
    public void setValue(int value) {
        toAWTComponent().setValue(value);
    }
}
