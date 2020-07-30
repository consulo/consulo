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
import consulo.awt.TargetAWT;
import consulo.localize.LocalizeManager;
import consulo.localize.LocalizeValue;
import consulo.ui.image.Image;
import consulo.ui.migration.SwingImageRef;
import consulo.util.dataholder.Key;
import consulo.util.lang.StringUtil;
import kava.beans.PropertyChangeListener;
import kava.beans.PropertyChangeSupport;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
   * value: Icon
   */
  public static final String PROP_ICON = "icon";
  /**
   * value: Icon
   */
  public static final String PROP_DISABLED_ICON = "disabledIcon";
  /**
   * value: Icon
   */
  public static final String PROP_SELECTED_ICON = "selectedIcon";
  /**
   * value: Icon
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
  private double myWeight = DEFAULT_WEIGHT;

  // TODO [VISTALL] migrate to UI image
  private Icon myIcon;
  private Icon myDisabledIcon;
  private Icon myHoveredIcon;
  private Icon mySelectedIcon;

  private static final Pattern MNEMONIC = Pattern.compile(" ?\\(_?[A-Z]\\)");

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
    String text = StringUtil.nullize(myTextValue.getValue());
    if(text == null) {
      return null;
    }

    Matcher matcher = MNEMONIC.matcher(text);
    return matcher.replaceAll("");
  }

  @Nullable
  public String getTextWithMnemonic() {
    return StringUtil.nullize(myTextValue.getValue());
  }

  public void setTextValue(@Nonnull LocalizeValue textValue) {
    myTextValue = textValue;
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

      if(mayContainMnemonic) {
        setTextValue(LocalizeValue.of(text));
      }
      else {
        Matcher matcher = MNEMONIC.matcher(text);
        String textNoMnemonic = matcher.replaceAll("");

        setTextValue(LocalizeValue.of(textNoMnemonic));
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

  public void setDescriptionValue(@Nonnull LocalizeValue descriptionValue) {
    LocalizeValue oldDescription = myDescriptionValue;
    myDescriptionValue = descriptionValue;
    fireObjectPropertyChange(PROP_DESCRIPTION, oldDescription, myDescriptionValue);
  }

  public Icon getIcon() {
    return myIcon;
  }

  @Nullable
  public Image getUIIcon() {
    return TargetAWT.from(myIcon);
  }

  public void setIcon(@Nullable Image image) {
    setIcon(TargetAWT.to(image));
  }

  public Icon getDisabledIcon() {
    return myDisabledIcon;
  }

  public void setDisabledIcon(@Nullable Icon icon) {
    Icon oldDisabledIcon = myDisabledIcon;
    myDisabledIcon = icon;
    fireObjectPropertyChange(PROP_DISABLED_ICON, oldDisabledIcon, myDisabledIcon);
  }

  public Icon getHoveredIcon() {
    return myHoveredIcon;
  }

  public void setHoveredIcon(@Nullable final Image hoveredIcon) {
    setHoveredIcon(TargetAWT.to(hoveredIcon));
  }

  public Icon getSelectedIcon() {
    return mySelectedIcon;
  }

  public void setSelectedIcon(@Nullable final Image hoveredIcon) {
    setSelectedIcon(TargetAWT.to(hoveredIcon));
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
    setVisible(presentation.isVisible());
    setEnabled(presentation.isEnabled());

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
  public <T> T getClientProperty(@Nonnull Key<T> key) {
    //noinspection unchecked
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

  @Override
  public String toString() {
    return getText() + " (" + myDescriptionValue + ")";
  }

  public boolean isEnabledAndVisible() {
    return isEnabled() && isVisible();
  }

  // region obsolete stuff
  public void setSelectedIcon(@Nullable final SwingImageRef hoveredIcon) {
    setSelectedIcon((Icon)hoveredIcon);
  }

  @Deprecated
  @DeprecationInfo("Use setSelectedIcon with ui image")
  public void setSelectedIcon(Icon selectedIcon) {
    Icon old = mySelectedIcon;
    mySelectedIcon = selectedIcon;
    fireObjectPropertyChange(PROP_SELECTED_ICON, old, mySelectedIcon);
  }

  public void setHoveredIcon(@Nullable final SwingImageRef hoveredIcon) {
    setHoveredIcon((Icon)hoveredIcon);
  }

  @Deprecated
  @DeprecationInfo("Use setHoveredIcon with ui image")
  public void setHoveredIcon(@Nullable final Icon hoveredIcon) {
    Icon old = myHoveredIcon;
    myHoveredIcon = hoveredIcon;
    fireObjectPropertyChange(PROP_HOVERED_ICON, old, myHoveredIcon);
  }


  public void setIcon(@Nullable SwingImageRef image) {
    setIcon((Icon)image);
  }

  @Deprecated
  @DeprecationInfo("Use setIcon with ui image")
  public void setIcon(@Nullable Icon icon) {
    Icon oldIcon = myIcon;
    if (oldIcon == icon) return;

    myIcon = icon;
    fireObjectPropertyChange(PROP_ICON, oldIcon, myIcon);
  }

  // endregion
}