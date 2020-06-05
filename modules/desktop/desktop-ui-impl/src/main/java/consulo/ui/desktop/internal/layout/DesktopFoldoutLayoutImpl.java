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
package consulo.ui.desktop.internal.layout;

import com.intellij.ui.HideableDecorator;
import consulo.awt.TargetAWT;
import consulo.awt.impl.FromSwingComponentWrapper;
import consulo.disposer.Disposable;
import consulo.localize.LocalizeValue;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.FoldoutLayout;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.*;

/**
 * @author VISTALL
 * @since 2020-05-29
 */
public class DesktopFoldoutLayoutImpl extends DesktopLayoutBase<DesktopFoldoutLayoutImpl.HideableTitledPanel> implements FoldoutLayout {
  public static class HideableTitledPanel extends JPanel implements FromSwingComponentWrapper {
    private final HideableDecorator myDecorator;
    private final DesktopFoldoutLayoutImpl myFoldoutLayout;

    public HideableTitledPanel(String title, JComponent content, DesktopFoldoutLayoutImpl foldoutLayout) {
      super(new BorderLayout());
      myFoldoutLayout = foldoutLayout;
      myDecorator = new HideableDecorator(this, title, false) {
        @Override
        protected void on() {
          super.on();

          myFoldoutLayout.fireStateListeners(true);
        }

        @Override
        protected void off() {
          super.off();

          myFoldoutLayout.fireStateListeners(false);
        }
      };

      setContentComponent(content);
    }

    @Override
    public void updateUI() {
      super.updateUI();

      updateTitle();
    }

    public void updateTitle() {
      // first component initialize
      if (myFoldoutLayout == null) {
        return;
      }

      myDecorator.setTitle(myFoldoutLayout.myTitleValue.getValue());
    }

    public void setContentComponent(@Nullable JComponent content) {
      myDecorator.setContentComponent(content);
    }

    public void setOn(boolean on) {
      myDecorator.setOn(on);
    }

    @Override
    public void setEnabled(boolean enabled) {
      myDecorator.setEnabled(enabled);
    }

    @Nonnull
    @Override
    public Component toUIComponent() {
      return myFoldoutLayout;
    }
  }

  @Nonnull
  private LocalizeValue myTitleValue = LocalizeValue.empty();

  public DesktopFoldoutLayoutImpl(LocalizeValue titleValue, Component component, boolean state) {
    myTitleValue = titleValue;
    initialize(new HideableTitledPanel(titleValue.getValue(), (JComponent)TargetAWT.to(component), this));
    toAWTComponent().setOn(state);
  }

  private void fireStateListeners(boolean state) {
    getListenerDispatcher(StateListener.class).stateChanged(state);
  }

  @RequiredUIAccess
  @Nonnull
  @Override
  public FoldoutLayout setState(boolean showing) {
    toAWTComponent().setOn(showing);
    return this;
  }

  @RequiredUIAccess
  @Nonnull
  @Override
  public FoldoutLayout setTitle(@Nonnull LocalizeValue title) {
    myTitleValue = title;
    toAWTComponent().updateTitle();
    return this;
  }

  @Nonnull
  @Override
  public Disposable addStateListener(@Nonnull StateListener stateListener) {
    return addListener(StateListener.class, stateListener);
  }
}
