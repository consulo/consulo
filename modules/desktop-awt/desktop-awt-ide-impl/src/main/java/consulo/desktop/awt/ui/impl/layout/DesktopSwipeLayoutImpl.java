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
package consulo.desktop.awt.ui.impl.layout;

import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.JBCardLayout;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.layout.Layout;
import consulo.ui.layout.LayoutConstraint;
import consulo.ui.layout.SwipeLayout;
import org.jspecify.annotations.Nullable;

import javax.swing.*;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 2018-07-01
 */
public class DesktopSwipeLayoutImpl extends DesktopLayoutBase<JPanel, LayoutConstraint> implements SwipeLayout {
  static class LayoutInfo {
    private String myId;
    private Supplier<Layout> myLayoutSupplier;

    private Layout myLayout;

    private boolean myIsAdded;

    LayoutInfo(String id, Supplier<Layout> layoutSupplier) {
      myId = id;
      myLayoutSupplier = layoutSupplier;
    }

   
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

  private Layout show(LayoutInfo layoutInfo, JBCardLayout.@Nullable SwipeDirection swipeDirection) {
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

 
  @Override
  public SwipeLayout register(String id, Supplier<Layout> layoutSupplier) {
    LayoutInfo layoutInfo = new LayoutInfo(id, layoutSupplier);

    myLayoutInfos.put(id, layoutInfo);

    if (toAWTComponent().getComponentCount() == 0) {
      show(layoutInfo, null);
    }
    return this;
  }

 
  @Override
  public Layout swipeLeftTo(String id) {
    LayoutInfo info = myLayoutInfos.get(id);
    if (info == null) {
      throw new IllegalArgumentException(id + " is not registered");
    }
    return show(info, JBCardLayout.SwipeDirection.FORWARD);
  }

 
  @Override
  public Layout swipeRightTo(String id) {
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
    super.removeAll();
  }

  @Override
  public void remove(Component component) {
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
