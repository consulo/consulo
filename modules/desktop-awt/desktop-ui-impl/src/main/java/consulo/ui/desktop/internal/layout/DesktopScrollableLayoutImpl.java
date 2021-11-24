/*
 * Copyright 2013-2019 consulo.io
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
package consulo.ui.desktop.internal.layout;

import com.intellij.ui.IdeBorderFactory;
import com.intellij.ui.components.JBScrollPane;
import consulo.awt.TargetAWT;
import consulo.awt.impl.FromSwingComponentWrapper;
import consulo.ui.Component;
import consulo.ui.desktop.internal.base.SwingComponentDelegate;
import consulo.ui.layout.ScrollableLayout;
import consulo.ui.layout.ScrollableLayoutOptions;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;

/**
 * @author VISTALL
 * @since 2019-02-16
 */
public class DesktopScrollableLayoutImpl extends SwingComponentDelegate<JScrollPane> implements ScrollableLayout {
  class MyJBScrollPane extends JBScrollPane implements FromSwingComponentWrapper {
    MyJBScrollPane(java.awt.Component view) {
      super(view);
    }

    @Nonnull
    @Override
    public Component toUIComponent() {
      return DesktopScrollableLayoutImpl.this;
    }
  }

  public DesktopScrollableLayoutImpl(@Nullable Component component, ScrollableLayoutOptions options) {
    MyJBScrollPane pane = new MyJBScrollPane(TargetAWT.to(component));
    initialize(pane);

    pane.setBorder(IdeBorderFactory.createEmptyBorder());
    pane.setViewportBorder(IdeBorderFactory.createEmptyBorder());

    switch (options.getHorizontalScrollPolicy()) {
      case ALWAYS:
        pane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
        break;
      case IF_NEED:
        pane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        break;
      case NEVER:
        pane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        break;
    }

    switch (options.getVerticalScrollPolicy()) {
      case ALWAYS:
        pane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        break;
      case IF_NEED:
        pane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        break;
      case NEVER:
        pane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
        break;
    }
  }
}
