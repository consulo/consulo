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
package consulo.project.ui.internal;

import consulo.application.Application;
import consulo.project.ui.wm.ToolWindowFactory;
import consulo.ui.Rectangle2D;
import consulo.ui.ex.toolWindow.ToolWindowAnchor;
import consulo.ui.ex.toolWindow.ToolWindowContentUiType;
import consulo.ui.ex.toolWindow.ToolWindowType;
import consulo.ui.ex.toolWindow.WindowInfo;
import consulo.util.lang.Comparing;
import org.jdom.Element;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author Anton Katilin
 * @author Vladimir Kondratyev
 */
public final class WindowInfoImpl implements Cloneable, WindowInfo {
  public static float normalizeWeigh(float weight) {
    if (weight <= 0) return WindowInfoImpl.DEFAULT_WEIGHT;
    if (weight >= 1) return 1 - WindowInfoImpl.DEFAULT_WEIGHT;
    return weight;
  }

  /**
   * XML tag.
   */
  static final String TAG = "window_info";
  /**
   * Default window weight.
   */
  public static final float DEFAULT_WEIGHT = 0.33f;
  private static final float DEFAULT_SIDE_WEIGHT = 0.5f;

  private boolean myActive;
  @Nonnull
  private ToolWindowAnchor myAnchor = ToolWindowAnchor.LEFT;
  private boolean myAutoHide;
  /**
   * Bounds of window in "floating" mode. It equals to <code>null</code> if
   * floating bounds are undefined.
   */
  private Rectangle2D myFloatingBounds;
  private String myId;
  private ToolWindowType myInternalType;
  private ToolWindowType myType;
  private boolean myVisible;
  private boolean myShowStripeButton = true;
  private float myWeight = DEFAULT_WEIGHT;
  private float mySideWeight = DEFAULT_SIDE_WEIGHT;
  private boolean mySplitMode;

  @Nonnull
  private ToolWindowContentUiType myContentUiType = ToolWindowContentUiType.TABBED;
  /**
   * Defines order of tool window button inside the stripe.
   * The default value is <code>-1</code>.
   */
  private int myOrder = -1;
  private static final String ID_ATTR = "id";
  private static final String ACTIVE_ATTR = "active";
  private static final String ANCHOR_ATTR = "anchor";
  private static final String AUTOHIDE_ATTR = "auto_hide";
  private static final String INTERNAL_TYPE_ATTR = "internal_type";
  private static final String TYPE_ATTR = "type";
  private static final String VISIBLE_ATTR = "visible";
  private static final String WEIGHT_ATTR = "weight";
  private static final String SIDE_WEIGHT_ATTR = "sideWeight";
  private static final String ORDER_ATTR = "order";
  private static final String X_ATTR = "x";
  private static final String Y_ATTR = "y";
  private static final String WIDTH_ATTR = "width";
  private static final String HEIGHT_ATTR = "height";
  private static final String SIDE_TOOL_ATTR = "side_tool";
  private static final String CONTENT_UI_ATTR = "content_ui";
  private static final String SHOW_STRIPE_BUTTON = "show_stripe_button";

  private boolean myWasRead;

  /**
   * Creates <code>WindowInfo</code> for tool window with specified <code>ID</code>.
   */
  WindowInfoImpl(@Nonnull String id) {
    myId = id;
    setType(ToolWindowType.DOCKED);
  }

  /**
   * Creates copy of <code>WindowInfo</code> object.
   */
  @Nonnull
  public WindowInfoImpl copy() {
    try {
      WindowInfoImpl info = (WindowInfoImpl)clone();
      if (myFloatingBounds != null) {
        info.myFloatingBounds = myFloatingBounds;
      }
      return info;
    }
    catch (CloneNotSupportedException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Copies all data from the passed <code>WindowInfo</code> into itself.
   */
  void copyFrom(@Nonnull WindowInfoImpl info) {
    myActive = info.myActive;
    myAnchor = info.myAnchor;
    myAutoHide = info.myAutoHide;
    myFloatingBounds = info.myFloatingBounds;
    myId = info.myId;
    myType = info.myType;
    myInternalType = info.myInternalType;
    myVisible = info.myVisible;
    myWeight = info.myWeight;
    mySideWeight = info.mySideWeight;
    myOrder = info.myOrder;
    mySplitMode = info.mySplitMode;
    myContentUiType = info.myContentUiType;
  }

  /**
   * @return tool window's anchor in internal mode.
   */
  @Nonnull
  @Override
  public ToolWindowAnchor getAnchor() {
    return myAnchor;
  }

  @Nonnull
  @Override
  public ToolWindowContentUiType getContentUiType() {
    return myContentUiType;
  }

  public void setContentUiType(@Nonnull ToolWindowContentUiType type) {
    myContentUiType = type;
  }

  /**
   * @return bound of tool window in floating mode.
   */
  @Override
  public Rectangle2D getFloatingBounds() {
    return myFloatingBounds;
  }

  /**
   * @return <code>ID</code> of the tool window.
   */
  @Override
  @Nonnull
  public String getId() {
    return myId;
  }

  /**
   * @return type of the tool window in internal (docked or sliding) mode. Actually the tool
   * window can be in floating mode, but this method has sense if you want to know what type
   * tool window had when it was internal one. The method never returns <code>null</code>.
   */
  @Nonnull
  public ToolWindowType getInternalType() {
    return myInternalType;
  }

  /**
   * @return current type of tool window.
   * @see ToolWindowType#DOCKED
   * @see ToolWindowType#FLOATING
   * @see ToolWindowType#SLIDING
   */
  @Override
  public ToolWindowType getType() {
    return myType;
  }

  /**
   * @return internal weight of tool window. "weigth" means how much of internal desktop
   * area the tool window is occupied. The weight has sense if the tool window is docked or
   * sliding.
   */
  public float getWeight() {
    return myWeight;
  }

  public float getSideWeight() {
    return mySideWeight;
  }

  public int getOrder() {
    return myOrder;
  }

  public void setOrder(int order) {
    myOrder = order;
  }

  @Override
  public boolean isActive() {
    return myActive;
  }

  @Override
  public boolean isAutoHide() {
    return myAutoHide;
  }

  @Override
  public boolean isDocked() {
    return ToolWindowType.DOCKED == myType;
  }

  @Override
  public boolean isFloating() {
    return ToolWindowType.FLOATING == myType;
  }

  @Override
  public boolean isWindowed() {
    return ToolWindowType.WINDOWED == myType;
  }

  @Override
  public boolean isSliding() {
    return ToolWindowType.SLIDING == myType;
  }

  @Override
  public boolean isVisible() {
    return myVisible;
  }

  @Override
  public boolean isShowStripeButton() {
    return myShowStripeButton;
  }

  public void setShowStripeButton(boolean showStripeButton) {
    myShowStripeButton = showStripeButton;
  }

  @Override
  public boolean isSplit() {
    return mySplitMode;
  }

  public void setSplit(boolean sideTool) {
    mySplitMode = sideTool;
  }

  public void readExternal(Element element) {
    myId = element.getAttributeValue(ID_ATTR);
    myWasRead = true;
    try {
      myActive = Boolean.valueOf(element.getAttributeValue(ACTIVE_ATTR)).booleanValue() && canActivateOnStart(myId);
    }
    catch (NumberFormatException ignored) {
    }
    try {
      myAnchor = ToolWindowAnchor.fromText(element.getAttributeValue(ANCHOR_ATTR));
    }
    catch (IllegalArgumentException ignored) {
    }
    myAutoHide = Boolean.valueOf(element.getAttributeValue(AUTOHIDE_ATTR)).booleanValue();
    try {
      myInternalType = ToolWindowType.valueOf(element.getAttributeValue(INTERNAL_TYPE_ATTR));
    }
    catch (IllegalArgumentException ignored) {
    }
    try {
      myType = ToolWindowType.valueOf(element.getAttributeValue(TYPE_ATTR));
    }
    catch (IllegalArgumentException ignored) {
    }
    myVisible = Boolean.valueOf(element.getAttributeValue(VISIBLE_ATTR)).booleanValue() && canActivateOnStart(myId);
    if (element.getAttributeValue(SHOW_STRIPE_BUTTON) != null) {
      myShowStripeButton = Boolean.valueOf(element.getAttributeValue(SHOW_STRIPE_BUTTON)).booleanValue();
    }
    try {
      myWeight = Float.parseFloat(element.getAttributeValue(WEIGHT_ATTR));
    }
    catch (NumberFormatException ignored) {
    }
    try {
      String value = element.getAttributeValue(SIDE_WEIGHT_ATTR);
      if (value != null) {
        mySideWeight = Float.parseFloat(value);
      }
    }
    catch (NumberFormatException ignored) {
    }
    try {
      myOrder = Integer.valueOf(element.getAttributeValue(ORDER_ATTR)).intValue();
    }
    catch (NumberFormatException ignored) {
    }
    try {
      int x = Integer.parseInt(element.getAttributeValue(X_ATTR));
      int y = Integer.parseInt(element.getAttributeValue(Y_ATTR));
      int width = Integer.parseInt(element.getAttributeValue(WIDTH_ATTR));
      int height = Integer.parseInt(element.getAttributeValue(HEIGHT_ATTR));
      myFloatingBounds = new Rectangle2D(x, y, width, height);
    }
    catch (NumberFormatException ignored) {
    }
    mySplitMode = Boolean.parseBoolean(element.getAttributeValue(SIDE_TOOL_ATTR));

    myContentUiType = ToolWindowContentUiType.getInstance(element.getAttributeValue(CONTENT_UI_ATTR));
  }

  public void writeExternal(Element element) {
    element.setAttribute(ID_ATTR, myId);
    element.setAttribute(ACTIVE_ATTR, Boolean.toString(myActive));
    element.setAttribute(ANCHOR_ATTR, myAnchor.toString());
    element.setAttribute(AUTOHIDE_ATTR, Boolean.toString(myAutoHide));
    element.setAttribute(INTERNAL_TYPE_ATTR, myInternalType.toString());
    element.setAttribute(TYPE_ATTR, myType.toString());
    element.setAttribute(VISIBLE_ATTR, Boolean.toString(myVisible));
    element.setAttribute(SHOW_STRIPE_BUTTON, Boolean.toString(myShowStripeButton));
    element.setAttribute(WEIGHT_ATTR, Float.toString(myWeight));
    element.setAttribute(SIDE_WEIGHT_ATTR, Float.toString(mySideWeight));
    element.setAttribute(ORDER_ATTR, Integer.toString(myOrder));
    element.setAttribute(SIDE_TOOL_ATTR, Boolean.toString(mySplitMode));
    element.setAttribute(CONTENT_UI_ATTR, myContentUiType.getName());
    if (myFloatingBounds != null) {
      element.setAttribute(X_ATTR, Integer.toString(myFloatingBounds.minX()));
      element.setAttribute(Y_ATTR, Integer.toString(myFloatingBounds.minY()));
      element.setAttribute(WIDTH_ATTR, Integer.toString(myFloatingBounds.width()));
      element.setAttribute(HEIGHT_ATTR, Integer.toString(myFloatingBounds.height()));
    }
  }

  private static boolean canActivateOnStart(String id) {
    for (ToolWindowFactory factory : Application.get().getExtensionPoint(ToolWindowFactory.class).getExtensionList()) {
      if (id.equals(factory.getId())) {
        return !factory.isDoNotActivateOnStart();
      }
    }
    return true;
  }

  /**
   * Sets new anchor.
   */
  public void setAnchor(@Nonnull ToolWindowAnchor anchor) {
    myAnchor = anchor;
  }

  public void setActive(boolean active) {
    myActive = active;
  }

  public void setAutoHide(boolean autoHide) {
    myAutoHide = autoHide;
  }

  public void setFloatingBounds(@Nullable Rectangle2D floatingBounds) {
    myFloatingBounds = floatingBounds;
  }

  public void setType(@Nonnull ToolWindowType type) {
    if (ToolWindowType.DOCKED == type || ToolWindowType.SLIDING == type) {
      myInternalType = type;
    }
    myType = type;
  }

  public void setVisible(boolean visible) {
    myVisible = visible;
  }

  /**
   * Sets window weight and adjust it to [0..1] range if necessary.
   */
  public void setWeight(float weight) {
    myWeight = Math.max(0, Math.min(1, weight));
  }

  public void setSideWeight(float weight) {
    mySideWeight = Math.max(0, Math.min(1, weight));
  }

  public boolean equals(Object obj) {
    if (!(obj instanceof WindowInfoImpl)) {
      return false;
    }
    WindowInfoImpl info = (WindowInfoImpl)obj;
    return myActive == info.myActive &&
           myAnchor == info.myAnchor &&
           myId.equals(info.myId) &&
           myAutoHide == info.myAutoHide &&
           Comparing.equal(myFloatingBounds, info.myFloatingBounds) &&
           myInternalType == info.myInternalType &&
           myType == info.myType &&
           myVisible == info.myVisible &&
           myShowStripeButton == info.myShowStripeButton &&
           myWeight == info.myWeight &&
           mySideWeight == info.mySideWeight &&
           myOrder == info.myOrder &&
           mySplitMode == info.mySplitMode &&
           myContentUiType == info.myContentUiType;
  }

  public int hashCode() {
    return myAnchor.hashCode() + myId.hashCode() + myType.hashCode() + myOrder;
  }

  @SuppressWarnings("HardCodedStringLiteral")
  public String toString() {
    return getClass().getName() +
           "[myId=" +
           myId +
           "; myVisible=" +
           myVisible +
           "; myShowStripeButton=" +
           myShowStripeButton +
           "; myActive=" +
           myActive +
           "; myAnchor=" +
           myAnchor +
           "; myOrder=" +
           myOrder +
           "; myAutoHide=" +
           myAutoHide +
           "; myWeight=" +
           myWeight +
           "; mySideWeight=" +
           mySideWeight +
           "; myType=" +
           myType +
           "; myInternalType=" +
           myInternalType +
           "; myFloatingBounds=" +
           myFloatingBounds +
           "; mySplitMode=" +
           mySplitMode +
           "; myContentUiType=" +
           myContentUiType.getName() +
           ']';
  }

  public boolean wasRead() {
    return myWasRead;
  }
}
