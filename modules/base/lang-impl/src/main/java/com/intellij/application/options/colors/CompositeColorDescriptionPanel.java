/*
 * Copyright 2000-2017 JetBrains s.r.o.
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
package com.intellij.application.options.colors;

import com.intellij.application.options.colors.OptionsPanelImpl.ColorDescriptionPanel;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.util.Condition;
import javax.annotation.Nonnull;

import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class CompositeColorDescriptionPanel extends JPanel implements ColorDescriptionPanel {
  @Nonnull
  protected final List<ColorDescriptionPanel> myDescriptionPanels = new ArrayList<>();
  @Nonnull
  protected final List<Condition<? super EditorSchemeAttributeDescriptor>> myConditions = new ArrayList<>();

  @Nonnull
  private final List<Listener> myListeners = new ArrayList<>();

  private ColorDescriptionPanel myActive;

  public void addDescriptionPanel(@Nonnull ColorDescriptionPanel descriptionPanel, @Nonnull Condition<? super EditorSchemeAttributeDescriptor> condition) {
    myDescriptionPanels.add(descriptionPanel);
    myConditions.add(condition);

    for (Listener listener : myListeners) {
      descriptionPanel.addListener(listener);
    }

    updatePreferredSize();
  }

  private void updatePreferredSize() {
    Dimension preferredSize = new Dimension();
    for (ColorDescriptionPanel panel : myDescriptionPanels) {
      Dimension size = panel.getPanel().getPreferredSize();
      preferredSize.setSize(Math.max(size.getWidth(), preferredSize.getWidth()), Math.max(size.getHeight(), preferredSize.getHeight()));
    }
    setPreferredSize(preferredSize);
  }

  @Nonnull
  @Override
  public JComponent getPanel() {
    return this;
  }

  @Override
  public void resetDefault() {
    if (myActive != null) {
      final PaintLocker locker = new PaintLocker(this);
      try {
        setPreferredSize(getSize());// froze [this] size
        remove(myActive.getPanel());
        myActive = null;
      }
      finally {
        locker.release();
      }
    }
  }

  @Override
  public void reset(@Nonnull EditorSchemeAttributeDescriptor descriptor) {
    JComponent oldPanel = myActive == null ? null : myActive.getPanel();
    myActive = getPanelForDescriptor(descriptor);
    JComponent newPanel = myActive == null ? null : myActive.getPanel();

    if (oldPanel != newPanel) {
      final PaintLocker locker = new PaintLocker(this);
      try {
        if (oldPanel != null) {
          remove(oldPanel);
        }
        if (newPanel != null) {
          setPreferredSize(null);// make [this] resizable
          add(newPanel);
        }
      }
      finally {
        locker.release();
      }
    }
    if (myActive != null) {
      myActive.reset(descriptor);
    }
  }

  @Nullable
  private ColorDescriptionPanel getPanelForDescriptor(@Nonnull EditorSchemeAttributeDescriptor descriptor) {
    for (int i = myConditions.size() - 1; i >= 0; i--) {
      Condition<? super EditorSchemeAttributeDescriptor> condition = myConditions.get(i);
      ColorDescriptionPanel panel = myDescriptionPanels.get(i);
      if (condition.value(descriptor)) return panel;
    }
    return null;
  }


  @Override
  public void apply(@Nonnull EditorSchemeAttributeDescriptor descriptor, EditorColorsScheme scheme) {
    if (myActive != null) {
      myActive.apply(descriptor, scheme);
    }
  }

  @Override
  public void addListener(@Nonnull Listener listener) {
    for (ColorDescriptionPanel panel : myDescriptionPanels) {
      panel.addListener(listener);
    }
    myListeners.add(listener);
  }

  private static class PaintLocker {
    private final Container myPaintHolder;
    private final boolean myPaintState;

    PaintLocker(@Nonnull JComponent component) {
      myPaintHolder = component.getParent();
      myPaintState = myPaintHolder.getIgnoreRepaint();
      myPaintHolder.setIgnoreRepaint(true);
    }

    public void release() {
      myPaintHolder.validate();
      myPaintHolder.setIgnoreRepaint(myPaintState);
      myPaintHolder.repaint();
    }
  }
}
