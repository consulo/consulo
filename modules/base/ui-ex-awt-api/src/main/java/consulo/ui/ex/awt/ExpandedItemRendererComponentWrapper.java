/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package consulo.ui.ex.awt;

import consulo.ui.ex.awt.accessibility.AbstractAccessibleContextDelegate;

import javax.accessibility.Accessible;
import javax.accessibility.AccessibleContext;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.MouseEvent;

public class ExpandedItemRendererComponentWrapper extends JComponent {
  JComponent owner;

  /**
   * @deprecated use {@link #wrap(Component)}} instead to create an instance
   */
  @Deprecated
  private ExpandedItemRendererComponentWrapper(@Nonnull final Component rendererComponent) {
    add(rendererComponent);
    setOpaque(false);
    setLayout(new AbstractLayoutManager() {
      @Override
      public Dimension preferredLayoutSize(Container parent) {
        return rendererComponent.getPreferredSize();
      }

      @Override
      public void layoutContainer(Container parent) {
        Dimension size = parent.getSize();
        if (owner != null) {
          Rectangle visible = owner.getVisibleRect();
          size.width = visible.x + visible.width;
        }
        Insets i = parent.getInsets();
        Dimension pref = rendererComponent.getPreferredSize();
        rendererComponent.setBounds(i.left, i.top, Math.max(pref.width, size.width - i.left - i.right), size.height - i.top - i.bottom);
      }
    });
  }

  private ExpandedItemRendererComponentWrapper() {
  }

  @Nonnull
  public static ExpandedItemRendererComponentWrapper wrap(@Nonnull Component rendererComponent) {
    if (rendererComponent instanceof Accessible) {
      return new MyComponent(rendererComponent, (Accessible)rendererComponent);
    }
    return new ExpandedItemRendererComponentWrapper(rendererComponent);
  }

  @Nonnull
  public static Component unwrap(@Nonnull Component rendererComponent) {
    if (rendererComponent instanceof ExpandedItemRendererComponentWrapper) {
      Component component = ((ExpandedItemRendererComponentWrapper)rendererComponent).getDelegate();
      return component;
    }
    return rendererComponent;
  }

  private static class MyComponent extends ExpandedItemRendererComponentWrapper implements Accessible {
    private final Accessible myAccessible;
    private AccessibleContext myDefaultAccessibleContext;

    MyComponent(@Nonnull Component comp, @Nonnull Accessible accessible) {
      super(comp);
      myAccessible = accessible;
    }

    @Override
    public AccessibleContext getAccessibleContext() {
      if (accessibleContext == null) {
        accessibleContext = new AccessibleMyComponent();
      }
      return accessibleContext;
    }

    public AccessibleContext getDefaultAccessibleContext() {
      if (myDefaultAccessibleContext == null) {
        myDefaultAccessibleContext = new AccessibleJComponent() {
        };
      }
      return myDefaultAccessibleContext;
    }

    /**
     * Wraps the accessible context of {@link #myAccessible}, except for parent, which
     * needs to come from the default implementation to avoid infinite parent/child cycle.
     */
    protected class AccessibleMyComponent extends AbstractAccessibleContextDelegate {
      @Nonnull
      @Override
      protected AccessibleContext getDelegate() {
        return myAccessible.getAccessibleContext();
      }

      @Override
      public Accessible getAccessibleParent() {
        return getDefaultAccessibleContext().getAccessibleParent();
      }

      @Override
      public int getAccessibleIndexInParent() {
        return getDefaultAccessibleContext().getAccessibleIndexInParent();
      }
    }
  }

  @Override
  public void setBorder(Border border) {
    Component c = getDelegate();
    if (c instanceof JComponent) {
      ((JComponent)c).setBorder(border);
      return;
    }
    super.setBorder(border);
  }

  @Override
  public String getToolTipText() {
    Component c = getDelegate();
    if (c instanceof JComponent) {
      return ((JComponent)c).getToolTipText();
    }
    return super.getToolTipText();
  }

  @Override
  public String getToolTipText(MouseEvent event) {
    Component c = getDelegate();
    if (c instanceof JComponent) {
      return ((JComponent)c).getToolTipText(event);
    }
    return super.getToolTipText(event);
  }

  @Override
  public Point getToolTipLocation(MouseEvent event) {
    Component c = getDelegate();
    if (c instanceof JComponent) {
      return ((JComponent)c).getToolTipLocation(event);
    }
    return super.getToolTipLocation(event);
  }

  @Nullable
  private Component getDelegate() {
    if (getComponentCount() == 1) {
      return getComponent(0);
    }
    return null;
  }
}
