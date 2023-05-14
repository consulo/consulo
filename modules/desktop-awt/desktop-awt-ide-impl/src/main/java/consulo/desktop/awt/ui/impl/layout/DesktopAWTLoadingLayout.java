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
package consulo.desktop.awt.ui.impl.layout;

import consulo.application.util.concurrent.AppExecutorUtil;
import consulo.desktop.awt.ui.impl.base.SwingComponentDelegate;
import consulo.disposer.Disposable;
import consulo.localize.LocalizeValue;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.layout.Layout;
import consulo.ui.layout.LoadingLayout;

import jakarta.annotation.Nonnull;
import javax.swing.*;
import java.util.concurrent.Future;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 23/04/2023
 */
public class DesktopAWTLoadingLayout<L extends Layout> extends SwingComponentDelegate<AWTLoadingPanel> implements LoadingLayout<L> {
  private final L myInnerLayout;

  public DesktopAWTLoadingLayout(L inner, Disposable parent) {
    myInnerLayout = inner;
    initialize(new AWTLoadingPanel(this, (JComponent)TargetAWT.to(inner), parent));
  }

  @RequiredUIAccess
  @Override
  public <Value> Future<Value> startLoading(@Nonnull Supplier<Value> valueGetter, @Nonnull BiConsumer<L, Value> uiSetter) {
    UIAccess uiAccess = UIAccess.current();

    startLoading();
    
    return AppExecutorUtil.getAppScheduledExecutorService().submit(() -> {
      Value value = valueGetter.get();

      uiAccess.give(() -> stopLoading(l -> uiSetter.accept(l, value)));
      return value;
    });
  }

  @RequiredUIAccess
  @Override
  public void startLoading() {
    myInnerLayout.removeAll();
    toAWTComponent().startLoading();
    forceUpdate();
  }

  @RequiredUIAccess
  @Override
  public void startLoading(@Nonnull LocalizeValue loadingText) {
    myInnerLayout.removeAll();
    toAWTComponent().setLoadingText(loadingText.getValue());
    forceUpdate();
  }

  @RequiredUIAccess
  @Override
  public void stopLoading(@Nonnull Consumer<L> consumer) {
    consumer.accept(myInnerLayout);
    toAWTComponent().stopLoading();
    forceUpdate();
  }

  private void forceUpdate() {
    toAWTComponent().invalidate();
    toAWTComponent().repaint();
  }

  @RequiredUIAccess
  @Override
  public void setLoadingText(@Nonnull LocalizeValue loadingText) {
    toAWTComponent().setLoadingText(loadingText.getValue());
  }
}
