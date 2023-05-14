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

import jakarta.annotation.Nullable;
import java.awt.*;
import java.util.function.Consumer;

/**
 * from from kotlin
 *
 * @author VISTALL
 * @since 12/05/2023
 */
public class AlphaAnimationContext {
  private Component component;

  @Nullable
  private AlphaComposite composite;

  private ShowHideAnimator animator;

  public AlphaAnimationContext(Consumer<AlphaComposite> consumer) {
    this(AlphaComposite.SrcOver, consumer);
  }

  public AlphaAnimationContext(AlphaComposite base, Consumer<AlphaComposite> consumer) {
    animator = new ShowHideAnimator(it -> {
      if (it <= 0.0) {
        composite = null;
      }
      else if (it >= 1.0) {
        composite = base;
      }
      else {
        composite = base.derive((float)it);
      }
      consumer.accept(composite);
    });
  }

  public AlphaAnimationContext(Component component) {
    this(alphaComposite -> {
      if (component.isShowing()) component.repaint();
    });

    this.component = component;
  }

  public boolean isVisible() {
    return composite != null;
  }

  public void setVisible(boolean visible) {
    animator.setVisible(visible, () -> {
      if (component != null) {
        component.setVisible(visible);
      }
    });
  }

  public void paint(Graphics g, Runnable paint) {
    if (g instanceof Graphics2D graphics2D) {
      paintWithComposite(graphics2D, paint);
    } else if (composite != null) {
      paint.run();
    }
  }

  public void paintWithComposite(Graphics2D g, Runnable paint) {
    if (composite == null) {
      return;
    }

    Composite old = g.getComposite();

    try {
      g.setComposite(composite);
      paint.run();
    }
    finally {
      g.setComposite(old);
    }
  }

  public ShowHideAnimator getAnimator() {
    return animator;
  }
}
