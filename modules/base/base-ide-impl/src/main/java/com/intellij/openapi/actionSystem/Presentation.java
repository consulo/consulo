/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package com.intellij.openapi.actionSystem;

import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.text.TextWithMnemonic;
import com.intellij.util.SmartFMap;
import com.intellij.util.ui.UIUtil;
import consulo.annotation.DeprecationInfo;
import consulo.localize.LocalizeManager;
import consulo.localize.LocalizeValue;
import consulo.ui.image.Image;
import consulo.util.dataholder.Key;
import consulo.util.lang.StringUtil;
import kava.beans.PropertyChangeListener;
import kava.beans.PropertyChangeSupport;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiFunction;

/**
 * The presentation of an action in a specific place in the user interface.
 *
 * @see AnAction
 * @see ActionPlaces
 */
public final class Presentation implements Cloneable {
  public static final BiFunction<LocalizeManager, String, String> NO_MNEMONIC = (localizeManager, text) -> TextWithMnemonic.parse(text).getText();

  /**
   * Defines tool tip for button at tool bar or text for element at menu
   * value: LocalizeValue
   */
  public static final String PROP_TEXT = "text";
  /**
   * value: LocalizeValue
   */
  public static final String PROP_DESCRIPTION = "description";
  /**
   * value: Image
   */
  public static final String PROP_ICON = "icon";
  /**
   * value: Image
   */
  public static final String PROP_DISABLED_ICON = "disabledIcon";
  /**
   * value: Image
   */
  public static final String PROP_SELECTED_ICON = "selectedIcon";
  /**
   * value: Image
   */
  public static final String PROP_HOVERED_ICON = "hoveredIcon";
  /**
   * value: Boolean
   */
  public static final String PROP_VISIBLE = "visible";
  /**
   * The actual value is a Boolean.
   */
  public static final String PROP_ENABLED = "enabled";
  /**
   * value: Boolean
   */
  public static final String PROP_DISABLED_MNEMONIC = "disabledMnemonic";

  public static final double DEFAULT_WEIGHT = 0;
  public static final double HIGHER_WEIGHT = 42;
  public static final double EVEN_HIGHER_WEIGHT = 239;

  private SmartFMap<String, Object> myUserMap = SmartFMap.emptyMap();

  @Nullable
  private PropertyChangeSupport myChangeSupport;

  private LocalizeValue myTextValue = LocalizeValue.empty();
  private LocalizeValue myDescriptionValue = LocalizeValue.empty();

  private boolean myVisible = true;
  private boolean myEnabled = true;
  private boolean myDisabledMnemonic;
  private double myWeight = DEFAULT_WEIGHT;

  private Image myIcon;
  private Image myDisabledIcon;
  private Image myHoveredIcon;
  private Image mySelectedIcon;

  public Presentation() {
  }

  public Presentation(@Nonnull LocalizeValue textValue) {
    myTextValue = textValue;
  }

  @Deprecated
  @DeprecationInfo("Use #Presentation(LocalizeValue)")
  public Presentation(String text) {
    this(LocalizeValue.of(text));
  }

  public void addPropertyChangeListener(PropertyChangeListener l) {
    PropertyChangeSupport support = myChangeSupport;
    if (support == null) {
      support = myChangeSupport = new PropertyChangeSupport(this);
    }
    support.addPropertyChangeListener(l);
  }

  public void removePropertyChangeListener(PropertyChangeListener l) {
    PropertyChangeSupport support = myChangeSupport;
    if (support != null) {
      support.removePropertyChangeListener(l);
    }
  }

  /**
   * Fire listeners about current client properties. This need when presentation created and initialized before component added
   */
  public void fireAllProperties() {
    PropertyChangeSupport changeSupport = myChangeSupport;
    if (changeSupport == null) {
      return;
    }

    Set<String> strings = myUserMap.keySet();
    for (String key : strings) {
      Object clientProperty = getClientProperty(key);
      changeSupport.firePropertyChange(key, null, clientProperty);
    }
  }

  @Nullable
  public String getText() {
    if(myDisabledMnemonic) {
      return StringUtil.nullize(myTextValue.getValue());
    }
    return StringUtil.nullize(myTextValue.map(NO_MNEMONIC).getValue());
  }

  @Nullable
  public String getTextWithMnemonic() {
    return StringUtil.nullize(myTextValue.getValue());
  }

  public void setTextValue(@Nonnull LocalizeValue newTextValue) {
    LocalizeValue oldValue = myTextValue;
    myTextValue = newTextValue;
    if(oldValue != newTextValue) {
      fireObjectPropertyChange(PROP_TEXT, oldValue, newTextValue);
    }
  }

  @Nonnull
  public LocalizeValue getTextValue() {
    return myTextValue;
  }

  @Deprecated
  @DeprecationInfo("Use #setTextValue() with localize value parameter")
  public void setText(@Nullable String text,  boolean mayContainMnemonic) {
    if(text != null) {
      if (text.indexOf(UIUtil.MNEMONIC) >= 0) {
        text = text.replace(UIUtil.MNEMONIC, '&');
      }

      LocalizeValue value = LocalizeValue.of(text);
      if(mayContainMnemonic) {
        setTextValue(value);
      }
      else {
        setTextValue(value.map(NO_MNEMONIC));
      }
    }
    else {
      setTextValue(LocalizeValue.empty());
    }
  }

  @Deprecated
  @DeprecationInfo("Use #setTextValue() with localize value parameter")
  public void setText(String text) {
    setText(text, true);
  }

  @Deprecated
  @DeprecationInfo("Must be reviewed usage of this method, since changed logic for action presentation mnemonic")
  public static String restoreTextWithMnemonic(@Nullable String text, final int mnemonic) {
    if (text == null) {
      return null;
    }
    for (int i = 0; i < text.length(); i++) {
      if (Character.toUpperCase(text.charAt(i)) == mnemonic) {
        return text.substring(0, i) + "_" + text.substring(i);
      }
    }
    return text;
  }

  @Nonnull
  public String getDescription() {
    return myDescriptionValue.getValue();
  }

  @Nonnull
  public LocalizeValue getDescriptionValue() {
    return myDescriptionValue;
  }

  @Deprecated
  @DeprecationInfo("Use #setDescriptionValue() with localize value parameter")
  public void setDescription(@Nullable String description) {
    LocalizeValue value = StringUtil.isEmpty(description) ? LocalizeValue.empty() : LocalizeValue.of(description);
    setDescriptionValue(value);
  }

  public void setDescriptionValue(@Nonnull LocalizeValue newDescriptionValue) {
    LocalizeValue oldDescription = myDescriptionValue;
    myDescriptionValue = newDescriptionValue;
    if(oldDescription != newDescriptionValue) {
      fireObjectPropertyChange(PROP_DESCRIPTION, oldDescription, myDescriptionValue);
    }
  }

  @Nullable
  public Image getIcon() {
    return myIcon;
  }

  public void setIcon(@Nullable Image icon) {
    Image oldIcon = myIcon;
    if (oldIcon == icon) return;
    myIcon = icon;
    fireObjectPropertyChange(PROP_ICON, oldIcon, myIcon);
  }

  @Nullable
  public Image getDisabledIcon() {
    return myDisabledIcon;
  }

  public void setDisabledIcon(@Nullable Image icon) {
    Image oldDisabledIcon = myDisabledIcon;
    myDisabledIcon = icon;
    fireObjectPropertyChange(PROP_DISABLED_ICON, oldDisabledIcon, myDisabledIcon);
  }

  @Nullable
  public Image getHoveredIcon() {
    return myHoveredIcon;
  }

  public void setHoveredIcon(@Nullable final Image hoveredIcon) {
    Image old = myHoveredIcon;
    myHoveredIcon = hoveredIcon;
    fireObjectPropertyChange(PROP_HOVERED_ICON, old, myHoveredIcon);
  }

  @Nullable
  public Image getSelectedIcon() {
    return mySelectedIcon;
  }

  public void setSelectedIcon(@Nullable final Image selectedIcon) {
    Image old = mySelectedIcon;
    mySelectedIcon = selectedIcon;
    fireObjectPropertyChange(PROP_SELECTED_ICON, old, mySelectedIcon);
  }

  public boolean isVisible() {
    return myVisible;
  }

  public void setVisible(boolean visible) {
    boolean oldVisible = myVisible;
    myVisible = visible;
    fireBooleanPropertyChange(PROP_VISIBLE, oldVisible, myVisible);
  }

  /**
   * Returns the state of this action.
   *
   * @return <code>true</code> if action is enabled, <code>false</code> otherwise
   */
  public boolean isEnabled() {
    return myEnabled;
  }

  /**
   * Sets whether the action enabled or not. If an action is disabled, {@link AnAction#actionPerformed}
   * won't be called. In case when action represents a button or a menu item, the
   * representing button or item will be greyed out.
   *
   * @param enabled <code>true</code> if you want to enable action, <code>false</code> otherwise
   */
  public void setEnabled(boolean enabled) {
    boolean oldEnabled = myEnabled;
    myEnabled = enabled;
    fireBooleanPropertyChange(PROP_ENABLED, oldEnabled, myEnabled);
  }

  public final void setEnabledAndVisible(boolean enabled) {
    setEnabled(enabled);
    setVisible(enabled);
  }

  public void setDisabledMnemonic(boolean disabledMnemonic) {
    boolean oldDisabledMnemonic = myDisabledMnemonic;
    myDisabledMnemonic = disabledMnemonic;
    fireBooleanPropertyChange(PROP_DISABLED_MNEMONIC, oldDisabledMnemonic, myDisabledMnemonic);
  }

  public boolean isDisabledMnemonic() {
    return myDisabledMnemonic;
  }

  private void fireBooleanPropertyChange(String propertyName, boolean oldValue, boolean newValue) {
    PropertyChangeSupport support = myChangeSupport;
    if (oldValue != newValue && support != null) {
      support.firePropertyChange(propertyName, oldValue, newValue);
    }
  }

  private void fireObjectPropertyChange(String propertyName, Object oldValue, Object newValue) {
    PropertyChangeSupport support = myChangeSupport;
    if (support != null && !Objects.equals(oldValue, newValue)) {
      support.firePropertyChange(propertyName, oldValue, newValue);
    }
  }

  @Override
  public Presentation clone() {
    Presentation copy = new Presentation();
    copy.copyFrom(this);
    return copy;
  }

  public void copyFrom(Presentation presentation) {
    setTextValue(presentation.getTextValue());
    setDescriptionValue(presentation.getDescriptionValue());
    setIcon(presentation.getIcon());
    setDisabledIcon(presentation.getDisabledIcon());
    setHoveredIcon(presentation.getHoveredIcon());
    setSelectedIcon(presentation.getSelectedIcon());
    setVisible(presentation.isVisible());
    setEnabled(presentation.isEnabled());
    setDisabledMnemonic(presentation.isDisabledMnemonic());

    if (!myUserMap.equals(presentation.myUserMap)) {
      Set<String> allKeys = new HashSet<>(presentation.myUserMap.keySet());
      allKeys.addAll(myUserMap.keySet());
      if (!allKeys.isEmpty()) {
        for (String key : allKeys) {
          putClientProperty(key, presentation.getClientProperty(key));
        }
      }
    }
  }

  @Nullable
  @SuppressWarnings("unchecked")
  public <T> T getClientProperty(@Nonnull Key<T> key) {
    return (T)myUserMap.get(key.toString());
  }

  public <T> void putClientProperty(@Nonnull Key<T> key, @Nullable T value) {
    putClientProperty(key.toString(), value);
  }

  @Nullable
  public Object getClientProperty(@Nonnull String key) {
    return myUserMap.get(key);
  }

  public void putClientProperty(@Nonnull String key, @Nullable Object value) {
    Object oldValue = myUserMap.get(key);
    if (Comparing.equal(oldValue, value)) return;
    myUserMap = value == null ? myUserMap.minus(key) : myUserMap.plus(key, value);
    fireObjectPropertyChange(key, oldValue, value);
  }

  public double getWeight() {
    return myWeight;
  }

  /**
   * Some action groups (like 'New...') may filter out actions with non-highest priority.
   *
   * @param weight please use {@link #HIGHER_WEIGHT} or {@link #EVEN_HIGHER_WEIGHT}
   */
  public void setWeight(double weight) {
    myWeight = weight;
  }

  public boolean isEnabledAndVisible() {
    return isEnabled() && isVisible();
  }

  @Override
  public String toString() {
    return getText() + " (" + myDescriptionValue + ")";
  }
}