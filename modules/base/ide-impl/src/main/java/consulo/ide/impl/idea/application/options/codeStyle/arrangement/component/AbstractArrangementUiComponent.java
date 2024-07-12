/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package consulo.ide.impl.idea.application.options.codeStyle.arrangement.component;

import consulo.application.util.NotNullLazyValue;
import consulo.language.codeStyle.arrangement.std.ArrangementSettingsToken;
import consulo.language.codeStyle.arrangement.std.ArrangementUiComponent;
import consulo.ui.ex.awt.GridBag;
import consulo.ui.ex.awt.UIUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Denis Zhdanov
 * @since 3/11/13 10:41 AM
 */
public abstract class AbstractArrangementUiComponent implements ArrangementUiComponent {
  @Nonnull
  private final NotNullLazyValue<JComponent> myComponent = new NotNullLazyValue<>() {
    @Nonnull
    @Override
    protected JComponent compute() {
      JPanel result = new JPanel(new GridBagLayout()) {
        @Override
        protected void paintComponent(Graphics g) {
          Point point = UIUtil.getLocationOnScreen(this);
          if (point != null) {
            Rectangle bounds = getBounds();
            myScreenBounds = new Rectangle(point.x, point.y, bounds.width, bounds.height);
          }
          if (!myEnabled && g instanceof Graphics2D g2d) {
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.3f));
          }
          super.paintComponent(g);
        }

        @Override
        public boolean isFocusOwner() {
          Component[] components = getComponents();
          if (components != null) {
            for (Component component : components) {
              if (component.isFocusOwner()) {
                return true;
              }
            }
          }
          return false;
        }

        @Override
        public boolean requestFocusInWindow() {
          if (getComponentCount() > 0) {
            return getComponent(0).requestFocusInWindow();
          }
          else {
            return super.requestFocusInWindow();
          }
        }
      };
      result.setOpaque(false);
      result.add(doGetUiComponent(), new GridBag().fillCell());
      return result;
    }
  };

  @Nonnull
  private final Set<ArrangementSettingsToken> myAvailableTokens = new HashSet<>();

  @Nullable private Listener  myListener;
  @Nullable
  private Rectangle myScreenBounds;

  private boolean myEnabled = true;

  protected AbstractArrangementUiComponent(@Nonnull ArrangementSettingsToken ... availableTokens) {
    myAvailableTokens.addAll(Arrays.asList(availableTokens));
  }

  protected AbstractArrangementUiComponent(@Nonnull Collection<ArrangementSettingsToken> availableTokens) {
    myAvailableTokens.addAll(availableTokens);
  }

  @Nonnull
  @Override
  public Set<ArrangementSettingsToken> getAvailableTokens() {
    return myAvailableTokens;
  }

  @Nonnull
  @Override
  public final JComponent getUiComponent() {
    return myComponent.getValue();
  }

  protected abstract JComponent doGetUiComponent();

  @Override
  public void setData(@Nonnull Object data) {
    // Do nothing
  }

  @Override
  public void setListener(@Nullable Listener listener) {
    myListener = listener;
  }

  @Nullable
  @Override
  public Rectangle getScreenBounds() {
    return myScreenBounds;
  }

  @Override
  public boolean isEnabled() {
    return myEnabled;
  }

  @Override
  public void setEnabled(boolean enabled) {
    myEnabled = enabled;
  }

  @Nullable
  @Override
  public Rectangle onMouseMove(@Nonnull MouseEvent event) {
    return null;
  }

  @Override
  public void onMouseRelease(@Nonnull MouseEvent event) {
  }

  @Nullable
  @Override
  public Rectangle onMouseExited() {
    return null;
  }

  @Nullable
  @Override
  public Rectangle onMouseEntered(@Nonnull MouseEvent e) {
    return null;
  }

  protected void fireStateChanged() {
    if (myListener != null) {
      myListener.stateChanged();
    }
  }

  @Override
  public final void reset() {
    setEnabled(false);
    setSelected(false);
    doReset();
  }

  protected abstract void doReset();

  @Override
  public boolean alwaysCanBeActive() {
    return false;
  }
}
