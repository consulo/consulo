/*
 * Copyright 2013-2020 consulo.io
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
package consulo.wm.impl;

import com.intellij.openapi.wm.StatusBarWidget;
import com.intellij.openapi.wm.impl.status.widget.StatusBarWidgetWrapper;
import consulo.ui.Component;
import consulo.ui.Label;
import consulo.ui.PseudoComponent;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.border.BorderPosition;
import consulo.ui.border.BorderStyle;
import consulo.ui.layout.WrappedLayout;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 2020-11-21
 */
public class UnifiedStatusBarWidgetWrapper {
  @RequiredUIAccess
  public static PseudoComponent wrap(StatusBarWidget widget) {
    StatusBarWidget.WidgetPresentation presentation = widget.getPresentation();

    if (presentation instanceof StatusBarWidget.TextPresentation) {
      return new TextPresentationWidget((StatusBarWidget.TextPresentation)presentation);
    }
    return new DummyWidget(widget);
  }

  private static class DummyWidget implements PseudoComponent {
    private final WrappedLayout myLayout;

    @RequiredUIAccess
    public DummyWidget(StatusBarWidget widget) {
      String id = widget.ID();

      Label label = Label.create(id.substring(0, 2));
      myLayout = WrappedLayout.create(label);
      myLayout.addBorder(BorderPosition.LEFT, BorderStyle.EMPTY, null, 4);
      myLayout.addBorder(BorderPosition.RIGHT, BorderStyle.EMPTY, null, 4);
    }

    @RequiredUIAccess
    @Nonnull
    @Override
    public Component getComponent() {
      return myLayout;
    }
  }

  private static class TextPresentationWidget implements PseudoComponent, StatusBarWidgetWrapper {
    private Label myLabel;

    private StatusBarWidget.TextPresentation myWidgetPresentation;

    private WrappedLayout myWrappedLayout;

    @RequiredUIAccess
    public TextPresentationWidget(StatusBarWidget.TextPresentation widget) {
      myWidgetPresentation = widget;
      myLabel = Label.create("");

      myWrappedLayout = WrappedLayout.create(myLabel);
      myWrappedLayout.addBorder(BorderPosition.LEFT, BorderStyle.EMPTY, null, 4);
      myWrappedLayout.addBorder(BorderPosition.RIGHT, BorderStyle.EMPTY, null, 4);
    }

    @Nonnull
    @Override
    public StatusBarWidget.WidgetPresentation getPresentation() {
      return myWidgetPresentation;
    }

    @RequiredUIAccess
    @Override
    public void beforeUpdate() {
      String text = myWidgetPresentation.getText();
      myLabel.setText(text);
      myLabel.setToolTipText(myWidgetPresentation.getTooltipText());
      myLabel.setVisible(!text.isEmpty());
    }

    @RequiredUIAccess
    @Nonnull
    @Override
    public Component getComponent() {
      return myWrappedLayout;
    }
  }
}
