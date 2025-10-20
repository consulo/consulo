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
package consulo.ui.ex.action;

import consulo.annotation.DeprecationInfo;
import consulo.component.util.localize.BundleBase;
import consulo.localize.LocalizeManager;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.ui.image.Image;
import consulo.ui.util.TextWithMnemonic;
import consulo.util.collection.SmartFMap;
import consulo.util.dataholder.Key;
import consulo.util.lang.BitUtil;
import consulo.util.lang.Comparing;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import kava.beans.PropertyChangeListener;
import kava.beans.PropertyChangeSupport;

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
    private static final Logger LOG = Logger.getInstance(Presentation.class);

    public static final BiFunction<LocalizeManager, String, String> NO_MNEMONIC = (localizeManager, text) -> TextWithMnemonic.parse(text).getText();

    @Nonnull
    public static Presentation newTemplatePresentation() {
        Presentation presentation = new Presentation();
        presentation.myFlags = BitUtil.set(presentation.myFlags, IS_TEMPLATE, true);
        return presentation;
    }

    private static final int IS_ENABLED = 0b1;
    private static final int IS_VISIBLE = 0b10;
    private static final int IS_KEEP_POPUP_IF_REQUESTED = 0b100;
    private static final int IS_KEEP_POPUP_IF_PREFERRED = 0b1000;
    private static final int IS_POPUP_GROUP = 0b10000;
    private static final int IS_PERFORM_GROUP = 0b100000;
    private static final int IS_HIDE_GROUP_IF_EMPTY = 0b1000000;
    private static final int IS_DISABLE_GROUP_IF_EMPTY = 0b10000000;
    private static final int IS_APPLICATION_SCOPE = 0b100000000;
    private static final int IS_PREFER_INJECTED_PSI = 0b1000000000;
    private static final int IS_ENABLED_IN_MODAL_CONTEXT = 0b10000000000;
    private static final int IS_TEMPLATE = 0b1000000000000;

    private static final int IS_DEFAULT_ICON = 0b100000000000000000000;
    private static final int IS_DISABLE_MNEMONIC = 0b1000000000000000000000;

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

    private SmartFMap<String, Object> myUserMap = SmartFMap.emptyMap();

    @Nullable
    private PropertyChangeSupport myChangeSupport;

    private LocalizeValue myTextValue = LocalizeValue.empty();
    private LocalizeValue myDescriptionValue = LocalizeValue.empty();

    private int myFlags = IS_ENABLED | IS_VISIBLE | IS_DISABLE_GROUP_IF_EMPTY | IS_DEFAULT_ICON;

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
    @Deprecated
    @DeprecationInfo("Use #getTextValue()")
    public String getText() {
        if (BitUtil.isSet(myFlags, IS_DISABLE_MNEMONIC)) {
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

        if (!Objects.equals(oldValue, newTextValue)) {
            fireObjectPropertyChange(PROP_TEXT, oldValue, newTextValue);
        }
    }

    @Nonnull
    public LocalizeValue getTextValue() {
        return myTextValue;
    }

    @Deprecated
    @DeprecationInfo("Use #setTextValue() with localize value parameter")
    public void setText(@Nullable String text, boolean mayContainMnemonic) {
        if (text != null) {
            if (text.indexOf(BundleBase.MNEMONIC) >= 0) {
                text = text.replace(BundleBase.MNEMONIC, '&');
            }

            LocalizeValue value = LocalizeValue.of(text);
            if (mayContainMnemonic) {
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

    @Nonnull
    @DeprecationInfo("see #getDescriptionValue")
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

        if (!Objects.equals(oldDescription, newDescriptionValue)) {
            fireObjectPropertyChange(PROP_DESCRIPTION, oldDescription, myDescriptionValue);
        }
    }

    @Nullable
    public Image getIcon() {
        return myIcon;
    }

    public void setIcon(@Nullable Image icon) {
        Image oldIcon = myIcon;
        if (oldIcon == icon) {
            return;
        }
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

    public void setHoveredIcon(@Nullable Image hoveredIcon) {
        Image old = myHoveredIcon;
        myHoveredIcon = hoveredIcon;
        fireObjectPropertyChange(PROP_HOVERED_ICON, old, myHoveredIcon);
    }

    @Nullable
    public Image getSelectedIcon() {
        return mySelectedIcon;
    }

    public void setSelectedIcon(@Nullable Image selectedIcon) {
        Image old = mySelectedIcon;
        mySelectedIcon = selectedIcon;
        fireObjectPropertyChange(PROP_SELECTED_ICON, old, mySelectedIcon);
    }

    public boolean isVisible() {
        return BitUtil.isSet(myFlags, IS_VISIBLE);
    }

    public void setVisible(boolean visible) {
        assertNotTemplatePresentation();
        myFlags = checkAndSetFlag(myFlags, IS_VISIBLE, visible, PROP_VISIBLE);
    }

    /**
     * Returns the state of this action.
     *
     * @return <code>true</code> if action is enabled, <code>false</code> otherwise
     */
    public boolean isEnabled() {
        return BitUtil.isSet(myFlags, IS_ENABLED);
    }

    /**
     * Sets whether the action enabled or not. If an action is disabled, {@link AnAction#actionPerformed}
     * won't be called. In case when action represents a button or a menu item, the
     * representing button or item will be greyed out.
     *
     * @param enabled <code>true</code> if you want to enable action, <code>false</code> otherwise
     */
    public void setEnabled(boolean enabled) {
        assertNotTemplatePresentation();

        myFlags = checkAndSetFlag(myFlags, IS_ENABLED, enabled, PROP_ENABLED);
    }

    public final void setEnabledAndVisible(boolean enabled) {
        setEnabled(enabled);
        setVisible(enabled);
    }

    public void setDisabledMnemonic(boolean disabledMnemonic) {
        myFlags = checkAndSetFlag(myFlags, IS_DISABLE_MNEMONIC, disabledMnemonic, PROP_DISABLED_MNEMONIC);
    }

    public boolean isDisabledMnemonic() {
        return BitUtil.isSet(myFlags, IS_DISABLE_MNEMONIC);
    }

    /**
     * Template presentations must be returned by {@link AnAction#getTemplatePresentation()} only.
     * Template presentations assert that their enabled and visible flags are never updated
     * because menus and shortcut processing use different defaults,
     * so values from template presentations are silently ignored.
     */
    public boolean isTemplate() {
        return BitUtil.isSet(myFlags, IS_TEMPLATE);
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

    private int checkAndSetFlag(int flags, int mask, boolean toSet, String propertyName) {
        boolean oldValue = BitUtil.isSet(flags, mask);

        if (oldValue == toSet) {
            return flags;
        }

        int newFlags = BitUtil.set(flags, mask, toSet);
        fireBooleanPropertyChange(propertyName, oldValue, !oldValue);
        return newFlags;
    }

    @Override
    public Presentation clone() {
        Presentation copy = new Presentation();
        copy.copyFrom(this, null, true);
        copy.myFlags = BitUtil.set(copy.myFlags, IS_TEMPLATE, false);
        copy.myChangeSupport = null;
        return copy;
    }

    public void copyFrom(Presentation presentation) {
        copyFrom(presentation, null, false, false);
    }

    public void copyFrom(@Nonnull Presentation presentation, @Nullable Object customComponent, boolean allFlags) {
        copyFrom(presentation, customComponent, true, allFlags);
    }

    private void copyFrom(@Nonnull Presentation presentation,
                          @Nullable Object customComponent,
                          boolean forceNullComponent,
                          boolean allFlags) {
        if (presentation == this) {
            return;
        }

        boolean oldEnabled = isEnabled(), oldVisible = isVisible();
        if (allFlags) {
            myFlags = BitUtil.set(presentation.myFlags, IS_TEMPLATE, isTemplate());
        }
        else {
            myFlags = BitUtil.set(myFlags, IS_ENABLED, BitUtil.isSet(presentation.myFlags, IS_ENABLED));
            myFlags = BitUtil.set(myFlags, IS_VISIBLE, BitUtil.isSet(presentation.myFlags, IS_VISIBLE));
        }
        fireBooleanPropertyChange(PROP_ENABLED, oldEnabled, isEnabled());
        fireBooleanPropertyChange(PROP_VISIBLE, oldVisible, isVisible());

        setTextValue(presentation.getTextValue());
        setDescriptionValue(presentation.getDescriptionValue());

        setIcon(presentation.getIcon());
        setSelectedIcon(presentation.getSelectedIcon());
        setDisabledIcon(presentation.getDisabledIcon());
        setHoveredIcon(presentation.getHoveredIcon());

        if (!myUserMap.equals(presentation.myUserMap)) {
            Set<String> allKeys = new HashSet<>(presentation.myUserMap.keySet());
            allKeys.addAll(myUserMap.keySet());
            if (!allKeys.isEmpty()) {
                for (String key : allKeys) {
//                    if (key.equals(CustomComponentAction.COMPONENT_KEY.toString()) && (customComponent != null || forceNullComponent)) {
//                        putClientProperty(key, customComponent);
//                    }
//                    else {
                    putClientProperty(key, presentation.getClientProperty(key));
//                    }
                }
            }
        }
    }

    @Nullable
    @SuppressWarnings("unchecked")
    public <T> T getClientProperty(@Nonnull Key<T> key) {
        return (T) myUserMap.get(key.toString());
    }

    public <T> void putClientProperty(@Nonnull Key<T> key, @Nullable T value) {
        putClientProperty(key.toString(), value);
    }

    @Nullable
    public Object getClientProperty(@Nonnull String key) {
        return myUserMap.get(key);
    }

    @Deprecated
    public void putClientProperty(@Nonnull String key, @Nullable Object value) {
        Object oldValue = myUserMap.get(key);
        if (Comparing.equal(oldValue, value)) {
            return;
        }
        myUserMap = value == null ? myUserMap.minus(key) : myUserMap.plus(key, value);
        fireObjectPropertyChange(key, oldValue, value);
    }

    public boolean isEnabledAndVisible() {
        return isEnabled() && isVisible();
    }

    /**
     * @see Presentation#setPerformGroup(boolean)
     */
    public boolean isPerformGroup() {
        return BitUtil.isSet(myFlags, IS_PERFORM_GROUP);
    }

    /**
     * For an action group presentation sets whether the action group is "performable" as an ordinary action or not.
     *
     * @see ActionUtil#SUPPRESS_SUBMENU
     * @see HIDE_DROPDOWN_ICON
     */
    public void setPerformGroup(boolean performing) {
        myFlags = BitUtil.set(myFlags, IS_PERFORM_GROUP, performing);
    }

    /**
     * For an action presentation in a popup sets whether a popup is closed or kept open
     * when the action is performed.
     * <p>
     * {@link ToggleAction} use {@link KeepPopupOnPerform#Always} by default.
     * The behavior is controlled by the {@link UISettings#getKeepPopupsForToggles} property.
     *
     * @see KeepPopupOnPerform
     * @see UISettings#getKeepPopupsForToggles
     */
    public void setKeepPopupOnPerform(@Nonnull KeepPopupOnPerform mode) {
        boolean requestedBit = mode == KeepPopupOnPerform.IfRequested || mode == KeepPopupOnPerform.Always;
        boolean preferredBit = mode == KeepPopupOnPerform.IfPreferred || mode == KeepPopupOnPerform.Always;
        myFlags = BitUtil.set(myFlags, IS_KEEP_POPUP_IF_REQUESTED, requestedBit);
        myFlags = BitUtil.set(myFlags, IS_KEEP_POPUP_IF_PREFERRED, preferredBit);
    }

    /**
     * @see Presentation#setKeepPopupOnPerform(KeepPopupOnPerform)
     */
    @Nonnull
    public KeepPopupOnPerform getKeepPopupOnPerform() {
        boolean requestedBit = BitUtil.isSet(myFlags, IS_KEEP_POPUP_IF_REQUESTED);
        boolean preferedBit = BitUtil.isSet(myFlags, IS_KEEP_POPUP_IF_PREFERRED);
        return requestedBit && preferedBit ? KeepPopupOnPerform.Always :
            requestedBit ? KeepPopupOnPerform.IfRequested :
                preferedBit ? KeepPopupOnPerform.IfPreferred :
                    KeepPopupOnPerform.Never;
    }

    /**
     * @see Presentation#setEnabledInModalContext(boolean)
     */
    public boolean isEnabledInModalContext() {
        return BitUtil.isSet(myFlags, IS_ENABLED_IN_MODAL_CONTEXT);
    }

    /**
     * For an action presentation sets whether the action can be performed in the modal context.
     * The default is {@code false}.
     */
    public void setEnabledInModalContext(boolean enabledInModalContext) {
        myFlags = BitUtil.set(myFlags, IS_ENABLED_IN_MODAL_CONTEXT, enabledInModalContext);
    }

    /**
     * @see Presentation#setPreferInjectedPsi(boolean)
     */
    public boolean isPreferInjectedPsi() {
        return BitUtil.isSet(myFlags, IS_PREFER_INJECTED_PSI);
    }

    /**
     * For an action presentation sets whether the action prefers to be updated and performed with the injected {@code DataContext}.
     * Injected data context returns {@link InjectedDataKeys} data for regular data keys, if present.
     * The default is {@code false}.
     */
    public void setPreferInjectedPsi(boolean preferInjectedPsi) {
        myFlags = BitUtil.set(myFlags, IS_PREFER_INJECTED_PSI, preferInjectedPsi);
    }

    /**
     * Returns true if the action has an internal, not user-customized icon.
     *
     * @return true if the icon is internal, false if the icon is customized by the user.
     */
    public boolean isDefaultIcon() {
        return BitUtil.isSet(myFlags, IS_DEFAULT_ICON);
    }

    /**
     * Sets the flag indicating whether the action has an internal or a user-customized icon.
     *
     * @param isDefaultIconSet true if the icon is internal, false if the icon is customized by the user.
     */
    public void setDefaultIcon(boolean defaultIcon) {
        myFlags = BitUtil.set(myFlags, IS_DEFAULT_ICON, defaultIcon);
    }

    /**
     * @see Presentation#setPopupGroup(boolean)
     */
    public boolean isPopupGroup() {
        return BitUtil.isSet(myFlags, IS_POPUP_GROUP);
    }

    /**
     * For an action group presentation sets whether the action group is a popup group or not.
     * A popup action group is shown as a submenu, a toolbar button that shows a popup when clicked, etc.
     * A non-popup action group child actions are injected into the parent group.
     */
    public void setPopupGroup(boolean popup) {
        myFlags = BitUtil.set(myFlags, IS_POPUP_GROUP, popup);
    }

    void assertNotTemplatePresentation() {
        if (BitUtil.isSet(myFlags, IS_TEMPLATE)) {
            LOG.warn(new Throwable("Template presentations must not be used directly"));
        }
    }

    @Override
    public String toString() {
        return myTextValue + " (" + myDescriptionValue + ")";
    }
}