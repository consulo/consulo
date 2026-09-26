// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.codeEditor.impl.internal.floating;

import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.ui.ModalityState;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.impl.internal.animation.ShowHideAnimator;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class TransparentComponentAnimator {
    public static final int SHOWING_TIME_MS = 500;
    public static final int HIDING_TIME_MS = 1000;
    public static final int RETENTION_TIME_MS = 1500;

    private final TransparentComponent myComponent;

    private final Disposable myDisposable;

    private final ShowHideAnimator myAnimator;

    private boolean myAutoHideable = false;

    private long myClockGeneration;

    private @Nullable ScheduledFuture<?> myClockFuture;

    @RequiredUIAccess
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

        Disposer.register(myDisposable, this::stopTimerIfNeeded);

        myComponent.hideComponent();
    }

    public boolean isAutoHideable() {
        return myAutoHideable;
    }

    public void setAutoHideable(boolean autoHideable) {
        myAutoHideable = autoHideable;
    }

    @RequiredUIAccess
    private void startTimerIfNeeded() {
        if (!Disposer.isDisposed(myDisposable)) {
            if (myClockFuture == null) {
                long generation = myClockGeneration;
                myClockFuture = UIAccess.current().getScheduler().schedule(
                    () -> onClockTick(generation),
                    ModalityState.any(),
                    RETENTION_TIME_MS,
                    TimeUnit.MILLISECONDS
                );
            }
        }
    }

    private void stopTimerIfNeeded() {
        ScheduledFuture<?> clockFuture = myClockFuture;
        if (clockFuture != null) {
            myClockFuture = null;
            myClockGeneration++;
            clockFuture.cancel(false);
        }
    }

    @RequiredUIAccess
    private void onClockTick(long generation) {
        if (generation != myClockGeneration || Disposer.isDisposed(myDisposable)) {
            return;
        }
        myClockFuture = null;
        if (!myComponent.isComponentOnHold()) {
            scheduleHide();
        }
    }

    @RequiredUIAccess
    public void scheduleShow() {
        stopTimerIfNeeded();
        @Nullable Runnable onCompletion = myAutoHideable ? this::startTimerIfNeeded : null;
        myAnimator.setVisible(true, onCompletion, myComponent::showComponent);
    }

    @RequiredUIAccess
    public void scheduleHide() {
        stopTimerIfNeeded();
        myAnimator.setVisible(false, null, myComponent::hideComponent);
    }

    @RequiredUIAccess
    public void hideImmediately() {
        stopTimerIfNeeded();
        myComponent.hideComponent();
        myAnimator.setVisibleImmediately(false);
    }
}
