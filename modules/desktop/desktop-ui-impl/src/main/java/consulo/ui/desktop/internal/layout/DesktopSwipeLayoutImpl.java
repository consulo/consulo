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
package consulo.ui.desktop.internal.layout;

import com.intellij.ui.JBCardLayout;
import consulo.awt.TargetAWT;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.Layout;
import consulo.ui.layout.SwipeLayout;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 2018-07-01
 */
public class DesktopSwipeLayoutImpl extends DesktopLayoutBase<JPanel> implements SwipeLayout {
  static class LayoutInfo {
    private String myId;
    private Supplier<Layout> myLayoutSupplier;

    private Layout myLayout;

    private boolean myIsAdded;

    LayoutInfo(String id, Supplier<Layout> layoutSupplier) {
      myId = id;
      myLayoutSupplier = layoutSupplier;
    }

    @Nonnull
    Layout get() {
      if (myLayout == null) {
        myLayout = myLayoutSupplier.get();
      }
      return myLayout;
    }
  }

  private final Map<String, LayoutInfo> myLayoutInfos = new HashMap<>();

  private JBCardLayout myCardLayout;

  public DesktopSwipeLayoutImpl() {
    initDefaultPanel(myCardLayout = new JBCardLayout());
  }

  private Layout show(LayoutInfo layoutInfo, @Nullable JBCardLayout.SwipeDirection swipeDirection) {
    if (!layoutInfo.myIsAdded) {
      layoutInfo.myIsAdded = true;

      toAWTComponent().add(layoutInfo.myId, TargetAWT.to(layoutInfo.get()));
    }

    if (swipeDirection == null) {
      myCardLayout.show(toAWTComponent(), layoutInfo.myId);
    }
    else {
      myCardLayout.swipe(toAWTComponent(), layoutInfo.myId, swipeDirection);
    }

    return layoutInfo.get();
  }

  @Nonnull
  @Override
  public SwipeLayout register(@Nonnull String id, @Nonnull Supplier<Layout> layoutSupplier) {
    LayoutInfo layoutInfo = new LayoutInfo(id, layoutSupplier);

    myLayoutInfos.put(id, layoutInfo);

    if (toAWTComponent().getComponentCount() == 0) {
      show(layoutInfo, null);
    }
    return this;
  }

  @Nonnull
  @Override
  public Layout swipeLeftTo(@Nonnull String id) {
    LayoutInfo info = myLayoutInfos.get(id);
    if (info == null) {
      throw new IllegalArgumentException(id + " is not registered");
    }
    return show(info, JBCardLayout.SwipeDirection.FORWARD);
  }

  @Nonnull
  @Override
  public Layout swipeRightTo(@Nonnull String id) {
    LayoutInfo info = myLayoutInfos.get(id);
    if (info == null) {
      throw new IllegalArgumentException(id + " is not registered");
    }
    return show(info, JBCardLayout.SwipeDirection.BACKWARD);
  }

  @RequiredUIAccess
  @Override
  public void removeAll() {
    myLayoutInfos.clear();
    toAWTComponent().removeAll();
  }

  @Override
  public void remove(@Nonnull Component component) {
    String id = null;
    for (LayoutInfo info : myLayoutInfos.values()) {
      if (info.get() == component) {
        id = info.myId;
        break;
      }
    }

    if (id != null) {
      LayoutInfo info = myLayoutInfos.remove(id);
      assert info != null;
      toAWTComponent().remove(TargetAWT.to(info.get()));
    }
  }
}
