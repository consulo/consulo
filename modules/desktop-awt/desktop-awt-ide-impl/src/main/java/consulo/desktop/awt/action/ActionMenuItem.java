/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package consulo.desktop.awt.action;

import consulo.application.ui.UISettings;
import consulo.application.ui.wm.IdeFocusManager;
import consulo.dataContext.DataContext;
import consulo.desktop.awt.ui.IconLookup;
import consulo.desktop.awt.ui.JBCheckBoxMenuItem;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.externalService.statistic.FeatureUsageTracker;
import consulo.ide.impl.actionSystem.ex.TopApplicationMenuUtil;
import consulo.ide.impl.idea.openapi.actionSystem.ex.ActionUtil;
import consulo.ide.impl.idea.openapi.actionSystem.impl.ActionMenuUtil;
import consulo.ide.impl.idea.openapi.actionSystem.impl.actionholder.ActionRef;
import consulo.ide.impl.idea.openapi.keymap.KeymapUtil;
import consulo.localize.LocalizeValue;
import consulo.ui.ex.action.*;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.ex.internal.ActionManagerEx;
import consulo.ui.ex.internal.LocalizeValueWithMnemonic;
import consulo.ui.ex.keymap.KeymapManager;
import consulo.ui.image.IconLibraryManager;
import consulo.ui.image.Image;
import consulo.ui.image.ImageEffects;
import consulo.ui.style.Style;
import consulo.ui.style.StyleManager;
import consulo.ui.util.TextWithMnemonic;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import kava.beans.PropertyChangeEvent;
import kava.beans.PropertyChangeListener;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.HashSet;
import java.util.Set;

public class ActionMenuItem extends JBCheckBoxMenuItem {
    private static final Image ourDefaultIcon = Image.empty(Image.DEFAULT_ICON_SIZE);

    private final ActionRef<AnAction> myAction;
    private final Presentation myPresentation;
    private final String myPlace;
    private final boolean myInsideCheckedGroup;
    private final boolean myUseDarkIcons;
    private DataContext myContext;
    private AnActionEvent myEvent;
    private MenuItemSynchronizer myMenuItemSynchronizer;
    private final boolean myEnableMnemonics;
    private final boolean myToggleable;
    private boolean myToggled;

    private LocalizeValue myTextValue = LocalizeValue.empty();

    public ActionMenuItem(final AnAction action,
                          final Presentation presentation,
                          @Nonnull final String place,
                          @Nonnull DataContext context,
                          final boolean enableMnemonics,
                          final boolean prepareNow,
                          final boolean insideCheckedGroup,
                          boolean useDarkIcons) {
        myUseDarkIcons = useDarkIcons;
        myAction = ActionRef.fromAction(action);
        myPresentation = presentation;
        myPlace = place;
        myContext = context;
        myEnableMnemonics = enableMnemonics;
        myToggleable = action instanceof Toggleable;
        myInsideCheckedGroup = insideCheckedGroup;

        myEvent = new AnActionEvent(null, context, place, myPresentation, ActionManager.getInstance(), 0, true, false);

        addActionListener(new ActionTransmitter());

        if (prepareNow) {
            init();
        }
        else {
            setText("loading...");
        }
    }

    public void prepare() {
        init();
        installSynchronizer();
    }

    @Override
    public void addNotify() {
        super.addNotify();
        installSynchronizer();
        init();
    }

    @Override
    public void removeNotify() {
        uninstallSynchronizer();
        super.removeNotify();
    }

    private void installSynchronizer() {
        if (myMenuItemSynchronizer == null) {
            myMenuItemSynchronizer = new MenuItemSynchronizer();
        }
    }

    private void uninstallSynchronizer() {
        if (myMenuItemSynchronizer != null) {
            Disposer.dispose(myMenuItemSynchronizer);
            myMenuItemSynchronizer = null;
        }
    }

    private void init() {
        setVisible(myPresentation.isVisible());
        setEnabled(myPresentation.isEnabled());
        updateTextAndMnemonic(myPresentation.getTextValue(), myPresentation.isDisabledMnemonic());

        AnAction action = myAction.getAction();
        updateIcon(action);
        String id = ActionManager.getInstance().getId(action);
        if (id != null) {
            Shortcut[] shortcuts = KeymapManager.getInstance().getActiveKeymap().getShortcuts(id);
            setAcceleratorFromShortcuts(shortcuts);
        }
        else {
            final ShortcutSet shortcutSet = action.getShortcutSet();
            if (shortcutSet != null) {
                setAcceleratorFromShortcuts(shortcutSet.getShortcuts());
            }
        }
    }

    private void updateTextAndMnemonic(@Nullable LocalizeValue textValue, boolean disableMnemonics) {
        // first initialization
        if (myTextValue == null) {
            return;
        }

        if (textValue != null) {
            myTextValue = textValue;
        }

        if (disableMnemonics) {
            setText(myTextValue.getValue());
            setDisplayedMnemonicIndex(-1);
            setMnemonic(0);
        }
        else {
            TextWithMnemonic textWithMnemonic = LocalizeValueWithMnemonic.get(myTextValue);
            setText(textWithMnemonic.getText());
            setDisplayedMnemonicIndex(myEnableMnemonics ? textWithMnemonic.getMnemonicIndex() : -1);
            setMnemonic(myEnableMnemonics ? textWithMnemonic.getMnemonic() : 0);
        }
    }

    private void setAcceleratorFromShortcuts(final Shortcut[] shortcuts) {
        for (Shortcut shortcut : shortcuts) {
            if (shortcut instanceof KeyboardShortcut keyboardShortcut) {
                final KeyStroke firstKeyStroke = keyboardShortcut.getFirstKeyStroke();
                //If action has Enter shortcut, do not add it. Otherwise, user won't be able to chose any ActionMenuItem other than that
                if (!isEnterKeyStroke(firstKeyStroke)) {
                    setAccelerator(firstKeyStroke);
                }
                break;
            }
        }
    }

    private static boolean isEnterKeyStroke(KeyStroke keyStroke) {
        return keyStroke.getKeyCode() == KeyEvent.VK_ENTER && keyStroke.getModifiers() == 0;
    }

    /**
     * Updates long description of action at the status bar.
     */
    @Override
    public void menuSelectionChanged(boolean isIncluded) {
        super.menuSelectionChanged(isIncluded);
        ActionMenuUtil.showDescriptionInStatusBar(isIncluded, this, myPresentation.getDescription());
    }

    public String getFirstShortcutText() {
        return KeymapUtil.getFirstKeyboardShortcutText(myAction.getAction());
    }

    public void updateContext(@Nonnull DataContext context) {
        myContext = context;
        myEvent = new AnActionEvent(null, context, myPlace, myPresentation, ActionManager.getInstance(), 0, true, false);
    }

    private final class ActionTransmitter implements ActionListener {
        /**
         * @param component component
         * @return whether the component in Swing tree or not. This method is more
         * weak then {@link Component#isShowing() }
         */
        private boolean isInTree(final Component component) {
            if (component instanceof Window) {
                return component.isShowing();
            }
            else {
                Window windowAncestor = SwingUtilities.getWindowAncestor(component);
                return windowAncestor != null && windowAncestor.isShowing();
            }
        }

        @Override
        public void actionPerformed(final ActionEvent e) {
            final IdeFocusManager fm = IdeFocusManager.findInstanceByContext(myContext);
            final String id = ActionManager.getInstance().getId(myAction.getAction());
            if (id != null) {
                FeatureUsageTracker.getInstance().triggerFeatureUsed("context.menu.click.stats." + id.replace(' ', '.'));
            }
            fm.runOnOwnContext(myContext, () -> {
                final AnActionEvent event =
                    new AnActionEvent(new MouseEvent(ActionMenuItem.this,
                        MouseEvent.MOUSE_PRESSED,
                        0,
                        e.getModifiers(),
                        getWidth() / 2,
                        getHeight() / 2,
                        1,
                        false),
                        myContext, myPlace, myPresentation, ActionManager.getInstance(), e.getModifiers(), true, false);
                final AnAction menuItemAction = myAction.getAction();
                if (ActionUtil.lastUpdateAndCheckDumb(menuItemAction, event, false)) {
                    ActionManagerEx actionManager = ActionManagerEx.getInstanceEx();
                    actionManager.fireBeforeActionPerformed(menuItemAction, myContext, event);
                    ActionUtil.performActionDumbAware(menuItemAction, event);
                    actionManager.queueActionPerformedEvent(menuItemAction, myContext, event);
                }
            });
        }
    }

    private void updateIcon(AnAction action) {
        if (isToggleable() && (myPresentation.getIcon() == null || myInsideCheckedGroup)) {
            action.update(myEvent);
            myToggled = Boolean.TRUE.equals(myEvent.getPresentation().getClientProperty(Toggleable.SELECTED_PROPERTY));
            if (ActionPlaces.MAIN_MENU.equals(myPlace) && TopApplicationMenuUtil.isMacSystemMenu) {
                setState(myToggled);
            }
            else {
                if (myToggled) {
                    IconLookup lookup = IconLookup.get();

                    Icon checkmark = lookup.getIcon("checkmark", false, false, true);
                    Icon selectedCheckmark = lookup.getIcon("checkmark", true, false, true);
                    Icon disabledCheckmark = lookup.getIcon("checkmark", false, false, false);

                    setIcon(checkmark);
                    setSelectedIcon(selectedCheckmark);
                    setDisabledIcon(disabledCheckmark);
                }
                else {
                    setIcon(TargetAWT.to(ourDefaultIcon));
                    setSelectedIcon(TargetAWT.to(ourDefaultIcon));
                    setDisabledIcon(TargetAWT.to(ourDefaultIcon));
                }
            }
        }
        else {
            if (UISettings.getInstance().SHOW_ICONS_IN_MENUS) {
                Image icon = myPresentation.getIcon();

                Style currentStyle = StyleManager.get().getCurrentStyle();
                IconLibraryManager iconLibraryManager = IconLibraryManager.get();

                Image selectedIcon = null;

                if (icon != null && myUseDarkIcons && !currentStyle.isDark()) {
                    icon = iconLibraryManager.inverseIcon(icon);
                }

                if (icon != null && !myUseDarkIcons && shouldConvertIconToDarkVariant(currentStyle)) {
                    selectedIcon = iconLibraryManager.inverseIcon(icon);
                }

                if (action instanceof ToggleAction toggleAction && toggleAction.isSelected(myEvent)) {
                    icon = new PoppedIcon(icon, JBUI.scale(Image.DEFAULT_ICON_SIZE), JBUI.scale(Image.DEFAULT_ICON_SIZE));
                }

                setIcon(TargetAWT.to(icon));

                setSelectedIcon(selectedIcon != null ? TargetAWT.to(selectedIcon) : TargetAWT.to(icon));

                if (myPresentation.getDisabledIcon() != null) {
                    setDisabledIcon(TargetAWT.to(myPresentation.getDisabledIcon()));
                }
                else {
                    setDisabledIcon(icon == null ? null : TargetAWT.to(ImageEffects.grayed(icon)));
                }
            }
        }
    }

    private static boolean shouldConvertIconToDarkVariant(Style currentStyle) {
        return currentStyle.isLight() /*&& ColorUtil.isDark(JBColor.namedColor("MenuItem.background", 0xffffff))*/;
    }

    public boolean isToggleable() {
        return myToggleable;
    }

    @Override
    public boolean isSelected() {
        return myToggled;
    }

    private final class MenuItemSynchronizer implements PropertyChangeListener, Disposable {
        private static final String SELECTED = "selected";

        private final Set<String> mySynchronized = new HashSet<>();

        private MenuItemSynchronizer() {
            myPresentation.addPropertyChangeListener(this);
        }

        @Override
        public void dispose() {
            myPresentation.removePropertyChangeListener(this);
        }

        @Override
        public void propertyChange(PropertyChangeEvent e) {
            boolean queueForDispose = getParent() == null;

            String name = e.getPropertyName();
            if (mySynchronized.contains(name)) {
                return;
            }

            mySynchronized.add(name);

            try {
                if (Presentation.PROP_VISIBLE.equals(name)) {
                    final boolean visible = myPresentation.isVisible();
                    if (!visible && TopApplicationMenuUtil.isMacSystemMenu && myPlace.equals(ActionPlaces.MAIN_MENU)) {
                        setEnabled(false);
                    }
                    else {
                        setVisible(visible);
                    }
                }
                else if (Presentation.PROP_ENABLED.equals(name)) {
                    setEnabled(myPresentation.isEnabled());
                    updateIcon(myAction.getAction());
                }
                else if (Presentation.PROP_TEXT.equals(name)) {
                    updateTextAndMnemonic(myPresentation.getTextValue(), myPresentation.isDisabledMnemonic());
                }
                else if (Presentation.PROP_ICON.equals(name) || Presentation.PROP_DISABLED_ICON.equals(name) || SELECTED.equals(name)) {
                    updateIcon(myAction.getAction());
                }
            }
            finally {
                mySynchronized.remove(name);
                if (queueForDispose) {
                    // later since we cannot remove property listeners inside event processing
                    //noinspection SSBasedInspection
                    SwingUtilities.invokeLater(() -> {
                        if (getParent() == null) {
                            uninstallSynchronizer();
                        }
                    });
                }
            }
        }
    }
}
