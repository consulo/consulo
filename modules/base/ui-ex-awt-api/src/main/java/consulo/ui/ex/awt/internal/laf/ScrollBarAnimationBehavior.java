// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.ui.ex.awt.internal.laf;

public abstract class ScrollBarAnimationBehavior {
    protected final TwoWayAnimator trackAnimator;
    protected final TwoWayAnimator thumbAnimator;

    protected ScrollBarAnimationBehavior(TwoWayAnimator trackAnimator, TwoWayAnimator thumbAnimator) {
        this.trackAnimator = trackAnimator;
        this.thumbAnimator = thumbAnimator;
    }

    public float getTrackFrame() {
        return trackAnimator.getValue();
    }

    public float getThumbFrame() {
        return thumbAnimator.getValue();
    }

    public void onToggle(Boolean isOn) {
    }

    public abstract void onTrackHover(boolean hovered);

    public abstract void onThumbHover(boolean hovered);

    public abstract void onThumbMove();

    public abstract void onUninstall();

    public abstract void onReset();
}
