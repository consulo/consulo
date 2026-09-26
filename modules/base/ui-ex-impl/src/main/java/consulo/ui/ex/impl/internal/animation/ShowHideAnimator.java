/*
 * Copyright 2013-2023 consulo.io
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
package consulo.ui.ex.impl.internal.animation;

import consulo.disposer.Disposable;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.DoubleConsumer;

/**
 * from from kotlin
 *
 * @author VISTALL
 * @since 12/05/2023
 */
public class ShowHideAnimator {
    private int myShowingDelay = 0;
    private int myShowingDuration = 130;
    private int myHidingDelay = 140;
    private int myHidingDuration = 150;

    private final DoubleConsumer myConsumer;

    private final JBAnimator myAnimator = new JBAnimator();
    private final AtomicBoolean myAtomicVisible = new AtomicBoolean();
    private final Easing.Stateful myStatefulEasing;

    public ShowHideAnimator(DoubleConsumer consumer) {
        this(Easing.LINEAR, consumer);
    }

    public ShowHideAnimator(Easing easing, DoubleConsumer consumer) {
        myConsumer = consumer;
        myStatefulEasing = easing.stateful();
    }

    public Disposable getDisposable() {
        return myAnimator;
    }

    public void setVisible(boolean visible, Runnable updateVisibility) {
        setVisible(visible, null, updateVisibility);
    }

    /**
     * @param onCompletion Is called on animation completion or at the end of the call if no animation is needed
     */
    public void setVisible(boolean visible, @Nullable Runnable onCompletion, Runnable updateVisibility) {
        if (visible != myAtomicVisible.getAndSet(visible)) {
            double value = myStatefulEasing.value;

            if (!visible && value > 0.0) {
                Animation animation = createHidingAnimation(value, updateVisibility);
                if (onCompletion != null) {
                    animation.runWhenExpired(onCompletion);
                }
                myAnimator.animate(animation);
            }
            else if (visible && value < 1.0) {
                Animation animation = createShowingAnimation(value, updateVisibility);
                if (onCompletion != null) {
                    animation.runWhenExpired(onCompletion);
                }
                myAnimator.animate(animation);
            }
            else {
                myAnimator.stop();
                updateVisibility.run();
                if (onCompletion != null) {
                    onCompletion.run();
                }
            }
        }
        else if (onCompletion != null) {
            onCompletion.run();
        }
    }

    public int getShowingDelay() {
        return myShowingDelay;
    }

    public void setShowingDelay(int showingDelay) {
        myShowingDelay = showingDelay;
    }

    public int getShowingDuration() {
        return myShowingDuration;
    }

    public void setShowingDuration(int showingDuration) {
        myShowingDuration = showingDuration;
    }

    public int getHidingDelay() {
        return myHidingDelay;
    }

    public void setHidingDelay(int hidingDelay) {
        myHidingDelay = hidingDelay;
    }

    public int getHidingDuration() {
        return myHidingDuration;
    }

    public void setHidingDuration(int hidingDuration) {
        myHidingDuration = hidingDuration;
    }

    private Animation createShowingAnimation(double value, Runnable updateVisibility) {
        Animation animation = new Animation(myConsumer);
        if (value > 0.0) {
            animation.setDuration((int) Math.round(myShowingDuration * (1 - value)));
            animation.setEasing(myStatefulEasing.coerceIn(value, 1.0));
        }
        else {
            animation.setDelay(myShowingDelay);
            animation.setDuration(myShowingDuration);
            animation.setEasing(myStatefulEasing);
        }

        animation.runWhenScheduled(() -> {
            // Most likely not needed, just for consistency with hide. In the worst case we just avoid minor flickering here.
            if (myAtomicVisible.get()) {
                updateVisibility.run();
            }
        });
        return animation;
    }

    private Animation createHidingAnimation(double value, Runnable updateVisibility) {
        Animation animation = new Animation(myConsumer);
        if (value < 1.0) {
            animation.setDuration((int) Math.round(myHidingDuration * value));
            animation.setEasing(myStatefulEasing.coerceIn(0.0, value).reverse());
        }
        else {
            animation.setDelay(myHidingDelay);
            animation.setDuration(myHidingDuration);
            animation.setEasing(myStatefulEasing.reverse());
        }

        animation.runWhenExpiredOrCancelled(() -> {
            // If the animation is cancelled and the component was already made visible, we do NOT want to hide it again!
            if (!myAtomicVisible.get()) {
                updateVisibility.run();
            }
        });
        return animation;
    }

    public void setVisibleImmediately(boolean visible) {
        myAnimator.stop();
        if (visible != myAtomicVisible.getAndSet(visible)) {
            myConsumer.accept(myStatefulEasing.calc(visible ? 1.0 : 0.0));
        }
    }
}
