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
package consulo.ui.ex.awt;

import consulo.annotation.DeprecationInfo;
import consulo.application.AllIcons;
import consulo.ui.ex.awtUnsafe.TargetAWT;

import jakarta.annotation.Nonnull;
import javax.swing.*;
import java.awt.*;

/**
 * This class represents non resizable, nonfocusable button with the
 * same height and length.
 */
@Deprecated
@DeprecationInfo("Use buttons inside toolbars")
public class FixedSizeButton extends JButton {
  private int mySize;
  private JComponent myComponent;

  public FixedSizeButton() {
    this(-1, null);
  }

  private FixedSizeButton(int size, JComponent component) {
    setIcon(TargetAWT.to(AllIcons.General.Ellipsis));
    mySize = size;
    myComponent = component;
    setMargin(new Insets(0, 0, 0, 0));
    setDefaultCapable(false);
    setFocusable(false);
  }

  /**
   * Creates the <code>FixedSizeButton</code> with specified size.
   *
   * @throws java.lang.IllegalArgumentException
   *          if <code>size</code> isn't
   *          positive integer number.
   */
  public FixedSizeButton(int size) {
    this(size, null);
    if (size <= 0) {
      throw new IllegalArgumentException("wrong size: " + size);
    }
  }

  /**
   * Creates the <code>FixedSizeButton</code> which size is equals to
   * <code>component.getPreferredSize().height</code>. It is very convenient
   * way to create "browse" like button near the text fields.
   */
  public FixedSizeButton(@Nonnull JComponent component) {
    this(-1, component);
  }

  public Dimension getMinimumSize() {
    return getPreferredSize();
  }

  public Dimension getMaximumSize() {
    return getPreferredSize();
  }

  public Dimension getPreferredSize() {
    if (myComponent != null) {
      int size = myComponent.getPreferredSize().height;
      return new Dimension(size, size);
    }
    else if (mySize != -1) {
      return new Dimension(mySize, mySize);
    }
    else {
      return super.getPreferredSize();
    }
  }

  public void setAttachedComponent(JComponent component) {
    myComponent = component;
  }

  public JComponent getAttachedComponent() {
    return myComponent;
  }

  public void setSize(int size) {
    mySize = size;
  }
}

