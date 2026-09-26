// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.desktop.awt.editor.impl.internal.floating;

import consulo.desktop.awt.ui.impl.animation.ShowHideAnimator;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.ui.ex.awt.util.TimerUtil;
import org.jspecify.annotations.Nullable;

import javax.swing.*;

public class TransparentComponentAnimator {
    public static final int SHOWING_TIME_MS = 500;
    public static final int HIDING_TIME_MS = 1000;
    public static final int RETENTION_TIME_MS = 1500;

    private final TransparentComponent myComponent;

    private final Disposable myDisposable;

    private final Timer myClk = TimerUtil.createNamedTimer("CLK", RETENTION_TIME_MS);

    private final ShowHideAnimator myAnimator;

    private boolean myAutoHideable = false;

    public TransparentComponentAnimator(TransparentComponent component, Disposable parentDisposable) {
        myComponent = component;
        myDisposable = Disposable.newDisposable();
        Disposer.register(parentDisposable, myDisposable);

        myAnimator = new ShowHideAnimator(progress -> {
            myComponent.setOpacity((float) progress);
            myComponent.repaintComponent();
        });

        Disposer.register(parentDisposable, myAnimator.getDisposable());
        myAnimator.setHidingDelay(0);
        myAnimator.setShowingDelay(0);
        myAnimator.setHidingDuration(HIDING_TIME_MS);
        myAnimator.setShowingDuration(SHOWING_TIME_MS);

        myClk.setRepeats(false);
        Disposer.register(myDisposable, this::stopTimerIfNeeded);
        myClk.addActionListener(e -> {
            if (!myComponent.isComponentOnHold()) {
                scheduleHide();
            }
        });

        myComponent.hideComponent();
    }

    public boolean isAutoHideable() {
        return myAutoHideable;
    }

    public void setAutoHideable(boolean autoHideable) {
        myAutoHideable = autoHideable;
    }

    private void startTimerIfNeeded() {
        if (!Disposer.isDisposed(myDisposable)) {
            if (!myClk.isRunning()) {
                myClk.start();
            }
        }
    }

    private void stopTimerIfNeeded() {
        if (myClk.isRunning()) {
            myClk.stop();
        }
    }

    public void scheduleShow() {
        stopTimerIfNeeded();
        @Nullable Runnable onCompletion = myAutoHideable ? this::startTimerIfNeeded : null;
        myAnimator.setVisible(true, onCompletion, myComponent::showComponent);
    }

    public void scheduleHide() {
        stopTimerIfNeeded();
        myAnimator.setVisible(false, null, myComponent::hideComponent);
    }

    public void hideImmediately() {
        stopTimerIfNeeded();
        myComponent.hideComponent();
        myAnimator.setVisibleImmediately(false);
    }
}
