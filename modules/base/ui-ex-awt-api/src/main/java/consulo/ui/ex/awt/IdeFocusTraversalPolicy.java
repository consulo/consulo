/*
 * Copyright 2000-2016 JetBrains s.r.o.
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

import consulo.awt.hacking.ContainerHacking;

import jakarta.annotation.Nonnull;
import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.awt.*;

public class IdeFocusTraversalPolicy extends LayoutFocusTraversalPolicy {
  public interface PassThroughComponent {
  }

  @Override
  public Component getDefaultComponent(Container focusCycleRoot) {
    if (!(focusCycleRoot instanceof JComponent)) {
      return super.getDefaultComponent(focusCycleRoot);
    }
    return getPreferredFocusedComponent((JComponent)focusCycleRoot, this);
  }

  public static JComponent getPreferredFocusedComponent(@Nonnull final JComponent component) {
    return getPreferredFocusedComponent(component, null);
  }

  /**
   * @return preferred focused component inside the specified <code>component</code>.
   * Method can return component itself if the <code>component</code> is legal
   * (JTextFiel)focusable
   */
  public static JComponent getPreferredFocusedComponent(@Nonnull final JComponent component, final FocusTraversalPolicy policyToIgnore) {
    if (!component.isVisible()) {
      return null;
    }

    final FocusTraversalPolicy focusTraversalPolicy = ContainerHacking.getFocusTraversalPolicyAwtImpl(component);
    if (focusTraversalPolicy != null && focusTraversalPolicy != policyToIgnore) {
      if (focusTraversalPolicy.getClass().getName().indexOf("LegacyGlueFocusTraversalPolicy") >= 0) {
        return component;
      }

      Component defaultComponent = focusTraversalPolicy.getDefaultComponent(component);

      if (defaultComponent instanceof JComponent) {
        return (JComponent)defaultComponent;
      }
    }

    if (component instanceof JTabbedPane) {
      final JTabbedPane tabbedPane = (JTabbedPane)component;
      final Component selectedComponent = tabbedPane.getSelectedComponent();
      if (selectedComponent instanceof JComponent) {
        return getPreferredFocusedComponent((JComponent)selectedComponent);
      }
      return null;
    }

    if (_accept(component)) {
      return component;
    }

    for (Component ca : component.getComponents()) {
      if (!(ca instanceof JComponent)) {
        continue;
      }
      final JComponent c = getPreferredFocusedComponent((JComponent)ca);
      if (c != null) {
        return c;
      }
    }
    return null;
  }

  @Override
  protected final boolean accept(final Component aComponent) {
    if (aComponent instanceof JComponent) {
      return _accept((JComponent)aComponent);
    }
    return super.accept(aComponent);
  }

  private static boolean _accept(final JComponent component) {
    if (!component.isEnabled() || !component.isVisible() || !component.isFocusable()) {
      return false;
    }

    if (component instanceof PassThroughComponent) {
      return true;
    }

    if (component instanceof JTextComponent) {
      return ((JTextComponent)component).isEditable();
    }

    return component instanceof AbstractButton || component instanceof JList || component instanceof JTree || component instanceof JTable || component instanceof JComboBox;
  }
}
