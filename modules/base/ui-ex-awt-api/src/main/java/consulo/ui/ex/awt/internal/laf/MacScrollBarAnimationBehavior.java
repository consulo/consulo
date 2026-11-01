// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.ui.ex.awt.internal.laf;

import consulo.ui.ex.awt.util.Alarm;

import javax.swing.*;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class MacScrollBarAnimationBehavior extends DefaultScrollBarAnimationBehavior {
    private static final int HIDE_DELAY = 700;

    private final Supplier<? extends JScrollBar> myScrollBar;
    private final Predicate<JScrollBar> myOpaque;
    private final Alarm myHideAlarm = new Alarm();

    private boolean myTrackHovered;

    public MacScrollBarAnimationBehavior(Supplier<? extends JScrollBar> scrollBar,
                                         Predicate<JScrollBar> opaque,
                                         TwoWayAnimator trackAnimator,
                                         TwoWayAnimator thumbAnimator) {
        super(trackAnimator, thumbAnimator);
        myScrollBar = scrollBar;
        myOpaque = opaque;
    }

    @Override
    public void onTrackHover(boolean hovered) {
        myTrackHovered = hovered;
        JScrollBar scrollBar = myScrollBar.get();
        if (scrollBar != null && myOpaque.test(scrollBar)) {
            trackAnimator.start(hovered);
            thumbAnimator.start(hovered);
        }
        else if (hovered) {
            myHideAlarm.cancelAllRequests();
            trackAnimator.start(true);
            thumbAnimator.start(true);
        }
        else {
            thumbAnimator.start(false);
        }
    }

    @Override
    public void onThumbHover(boolean hovered) {
    }

    @Override
    public void onThumbMove() {
        JScrollBar scrollBar = myScrollBar.get();
        if (scrollBar != null && scrollBar.isShowing() && !myOpaque.test(scrollBar)) {
            if (!myTrackHovered && thumbAnimator.getValue() == 0f) {
                trackAnimator.rewind(false);
            }
            thumbAnimator.rewind(true);

            myHideAlarm.cancelAllRequests();
            if (!myTrackHovered) {
                myHideAlarm.addRequest(() -> thumbAnimator.start(false), HIDE_DELAY);
            }
        }
    }

    @Override
    public void onUninstall() {
        myHideAlarm.cancelAllRequests();
        super.onUninstall();
    }
}
