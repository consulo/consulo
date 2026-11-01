// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.ui.ex.awt.internal.laf;

public class DefaultScrollBarAnimationBehavior extends ScrollBarAnimationBehavior {
    public DefaultScrollBarAnimationBehavior(TwoWayAnimator trackAnimator, TwoWayAnimator thumbAnimator) {
        super(trackAnimator, thumbAnimator);
    }

    @Override
    public void onTrackHover(boolean hovered) {
        trackAnimator.start(hovered);
    }

    @Override
    public void onThumbHover(boolean hovered) {
        thumbAnimator.start(hovered);
    }

    @Override
    public void onThumbMove() {
    }

    @Override
    public void onUninstall() {
        trackAnimator.stop();
        thumbAnimator.stop();
    }

    @Override
    public void onReset() {
        trackAnimator.rewind(false);
        thumbAnimator.rewind(false);
    }
}
