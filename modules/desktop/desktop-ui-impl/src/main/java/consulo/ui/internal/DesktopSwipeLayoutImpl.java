/*
 * Copyright 2013-2018 consulo.io
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
package consulo.ui.internal;

import com.intellij.ui.JBCardLayout;
import consulo.ui.Layout;
import consulo.ui.internal.base.SwingComponentDelegate;
import consulo.ui.layout.SwipeLayout;

import javax.annotation.Nonnull;
import javax.swing.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 2018-07-01
 */
public class DesktopSwipeLayoutImpl extends SwingComponentDelegate<JPanel> implements SwingWrapper, SwipeLayout {
  static class LayoutInfo {
    private String myId;
    private Supplier<Layout> myLayoutSupplier;

    private Layout myLayout;

    LayoutInfo(String id, Supplier<Layout> layoutSupplier) {
      myId = id;
      myLayoutSupplier = layoutSupplier;
    }
  }

  private final List<LayoutInfo> myLayoutInfos = new ArrayList<>();
  private final JBCardLayout myCardLayout = new JBCardLayout();

  public DesktopSwipeLayoutImpl() {
    myComponent = new JPanel(myCardLayout);
  }

  @Nonnull
  @Override
  public SwipeLayout register(@Nonnull String id, @Nonnull Supplier<Layout> layoutSupplier) {
    myLayoutInfos.add(new LayoutInfo(id, layoutSupplier));
    return this;
  }

  @Nonnull
  @Override
  public Layout swipeTo(@Nonnull String id) {
    return null;
  }
}
