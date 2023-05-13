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
package consulo.desktop.awt.ui.animation;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.DoubleConsumer;

/**
 * from from kotlin
 *
 * @author VISTALL
 * @since 12/05/2023
 */
public class ShowHideAnimator {
  private final int showingDelay = 0;
  private final int showingDuration = 130;
  private final int hidingDelay = 140;
  private final int hidingDuration = 150;

  private DoubleConsumer consumer;

  private final JBAnimator animator = new JBAnimator();
  private final AtomicBoolean atomicVisible = new AtomicBoolean();
  private final Easing.Stateful statefulEasing;

  public ShowHideAnimator(DoubleConsumer consumer) {
    this(Easing.LINEAR, consumer);
  }

  public ShowHideAnimator(Easing easing, DoubleConsumer consumer) {
    this.consumer = consumer;
    statefulEasing = easing.stateful();
  }

  public void setVisible(boolean visible, Runnable updateVisibility) {
    if (visible != atomicVisible.getAndSet(visible)) {
      double value = statefulEasing.value;

      if (!visible && value > 0.0) {
        animator.animate(createHidingAnimation(value, updateVisibility));
      }
      else if (visible && value < 1.0) {
        animator.animate(createShowingAnimation(value, updateVisibility));
      }
      else {
        animator.stop();
        updateVisibility.run();
      }
    }
  }

  private Animation createShowingAnimation(double value, Runnable updateVisibility) {
    Animation animation = new Animation(consumer);
    if (value > 0.0) {
      animation.setDuration((int)Math.round(showingDuration * (1 - value)));
      animation.setEasing(statefulEasing.coerceIn(value, 1.0));
    }
    else {
      animation.setDelay(showingDelay);
      animation.setDuration(showingDuration);
      animation.setEasing(statefulEasing);
    }

    animation.runWhenScheduled(() -> {
      if (atomicVisible.get()) { // Most likely not needed, just for consistency with hide. In the worst case we just avoid minor flickering here.
        updateVisibility.run();
      }
    });
    return animation;
  }

  private Animation createHidingAnimation(double value, Runnable updateVisibility) {
    Animation animation = new Animation(consumer);
    if (value < 1.0) {
      animation.setDuration((int)Math.round(hidingDuration * value));
      animation.setEasing(statefulEasing.coerceIn(0.0, value).reverse());
    }
    else {
      animation.setDelay(hidingDelay);
      animation.setDuration(hidingDuration);
      animation.setEasing(statefulEasing.reverse());
    }

    animation.runWhenExpiredOrCancelled(() -> {
      if (!atomicVisible.get()) { // If the animation is cancelled and the component was already made visible, we do NOT want to hide it again!
        updateVisibility.run();
      }
    });
    return animation;
  }

  public void setVisibleImmediately(boolean visible) {
    animator.stop();
    if (visible != atomicVisible.getAndSet(visible)) {
      consumer.accept(statefulEasing.calc(visible ? 1.0 : 0.0));
    }
  }
}
