/*
 * Copyright 2000-2013 JetBrains s.r.o.
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

import consulo.application.impl.internal.LaterInvocator;
import consulo.application.ui.UISettings;
import consulo.application.ui.wm.IdeFocusManager;
import consulo.dataContext.DataContext;
import consulo.dataContext.DataManager;
import consulo.ide.impl.actionSystem.ex.TopApplicationMenuUtil;
import consulo.ide.impl.desktop.DesktopIdeFrameUtil;
import consulo.ide.impl.idea.openapi.actionSystem.impl.ActionMenuUtil;
import consulo.ide.impl.idea.openapi.actionSystem.impl.actionholder.ActionRef;
import consulo.localize.LocalizeValue;
import consulo.project.ui.wm.IdeFrame;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.ActionPlaces;
import consulo.ui.ex.action.Presentation;
import consulo.ui.ex.action.PresentationFactory;
import consulo.ui.ex.awt.UIExAWTDataKey;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.ex.internal.LocalizeValueWithMnemonic;
import consulo.ui.image.Image;
import consulo.ui.image.ImageEffects;
import consulo.ui.util.TextWithMnemonic;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import kava.beans.PropertyChangeEvent;
import kava.beans.PropertyChangeListener;

import javax.swing.*;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
import java.awt.*;
import java.awt.event.KeyEvent;

public final class ActionMenu extends JMenu {
    private final String myPlace;
    private DataContext myContext;
    private final ActionRef<ActionGroup> myGroup;
    private final PresentationFactory myPresentationFactory;
    private final Presentation myPresentation;
    private MenuItemSynchronizer myMenuItemSynchronizer;
    private StubItem myStubItem;  // A PATCH!!! Do not remove this code, otherwise you will lose all keyboard navigation in JMenuBar.

    private Component[] myMenuComponents;
    // protector for update inside setMnemonic
    private boolean myMnemonicUpdate;

    private LocalizeValue myTextValue = LocalizeValue.empty();

    private boolean myComponentMnemonicEnabled;
    private boolean myPresentationMnemonicDisabled;

    public ActionMenu(final DataContext context,
                      @Nonnull final String place,
                      final ActionGroup group,
                      final PresentationFactory presentationFactory,
                      final boolean enableMnemonics,
                      final boolean topLevel) {
        myContext = context;
        myPlace = place;
        myGroup = ActionRef.fromAction(group);
        myPresentationFactory = presentationFactory;
        myPresentation = myPresentationFactory.getPresentation(group);
        myComponentMnemonicEnabled = enableMnemonics;

        updateUI();

        init();

        // addNotify won't be called for menus in MacOS system menu
        if (TopApplicationMenuUtil.isMacSystemMenu) {
            installSynchronizer();
        }
    }

    public void updateContext(DataContext context) {
        myContext = context;
    }

    @Override
    public void addNotify() {
        super.addNotify();
        installSynchronizer();
    }

    private void installSynchronizer() {
        if (myMenuItemSynchronizer == null) {
            myMenuItemSynchronizer = new MenuItemSynchronizer();
            myGroup.getAction().addPropertyChangeListener(myMenuItemSynchronizer);
            myPresentation.addPropertyChangeListener(myMenuItemSynchronizer);
        }
    }

    @Override
    public void removeNotify() {
        uninstallSynchronizer();
        super.removeNotify();
    }

    private void uninstallSynchronizer() {
        if (myMenuItemSynchronizer != null) {
            myGroup.getAction().removePropertyChangeListener(myMenuItemSynchronizer);
            myPresentation.removePropertyChangeListener(myMenuItemSynchronizer);
            myMenuItemSynchronizer = null;
        }
    }

    @Override
    public void updateUI() {
        super.updateUI();

        updateTextAndMnemonic(null, myPresentation != null && myPresentation.isDisabledMnemonic());
    }

    private void updateTextAndMnemonic(@Nullable LocalizeValue newTextValue, boolean disableMnemonic) {
        myPresentationMnemonicDisabled = disableMnemonic;

        // first initialization
        if (myTextValue == null) {
            return;
        }

        if (newTextValue != null) {
            myTextValue = newTextValue;
        }

        TextWithMnemonic textWithMnemonic = LocalizeValueWithMnemonic.get(myTextValue);

        setText(textWithMnemonic.getText());
        setDisplayedMnemonicIndex(textWithMnemonic.getMnemonicIndex());

        myMnemonicUpdate = true;
        try {
            setMnemonic(textWithMnemonic.getMnemonic());
        }
        finally {
            myMnemonicUpdate = false;
        }
    }

    private void init() {
        myStubItem = isTopMenuBar() ? null : new StubItem();
        addStubItem();
        addMenuListener(new MenuListenerImpl());

        setVisible(myPresentation.isVisible());
        setEnabled(myPresentation.isEnabled());
        updateTextAndMnemonic(myPresentation.getTextValue(), myPresentation.isDisabledMnemonic());
        updateIcon();
    }

    public boolean isMainMenuPlace() {
        return myPlace.equals(ActionPlaces.MAIN_MENU);
    }

    private void addStubItem() {
        if (myStubItem != null) {
            add(myStubItem);
        }
    }

    public void setMnemonicEnabled(boolean enable) {
        myComponentMnemonicEnabled = enable;

        updateTextAndMnemonic(null, myPresentation.isDisabledMnemonic());
    }

    @Override
    public void setDisplayedMnemonicIndex(final int index) throws IllegalArgumentException {
        super.setDisplayedMnemonicIndex(isMnemonicEnabled() ? index : -1);
    }

    @Override
    public void setMnemonic(int mnemonic) {
        super.setMnemonic(isMnemonicEnabled() ? mnemonic : 0);
    }

    private boolean isMnemonicEnabled() {
        if (myPresentationMnemonicDisabled) {
            return false;
        }
        return myComponentMnemonicEnabled;
    }

    private void updateIcon() {
        if (UISettings.getInstance().SHOW_ICONS_IN_MENUS) {
            final Presentation presentation = myPresentation;
            final Image icon = presentation.getIcon();
            setIcon(TargetAWT.to(icon));
            if (presentation.getDisabledIcon() != null) {
                setDisabledIcon(TargetAWT.to(presentation.getDisabledIcon()));
            }
            else {
                setDisabledIcon(TargetAWT.to(icon == null ? null : ImageEffects.grayed(icon)));
            }
        }
    }

    @Override
    public void menuSelectionChanged(boolean isIncluded) {
        super.menuSelectionChanged(isIncluded);
        ActionMenuUtil.showDescriptionInStatusBar(isIncluded, this, myPresentation.getDescription());
    }

    private class MenuListenerImpl implements MenuListener {
        @Override
        public void menuCanceled(MenuEvent e) {
            if (isTopMenuBarAfterOpenJDKMemLeakFix()) {
                myMenuComponents = new Component[]{myStubItem};
            }
            else {
                clearItems();
            }
        }

        @Override
        public void menuDeselected(MenuEvent e) {
            if (myMnemonicUpdate) {
                return;
            }

            menuCanceled(e);
        }

        @Override
        public void menuSelected(MenuEvent e) {
            if (myMnemonicUpdate) {
                return;
            }

            if (isTopMenuBarAfterOpenJDKMemLeakFix()) {
                myMenuComponents = null;
            }
            else {
                fillMenu(ActionMenu.this);
            }
        }
    }

    @Override
    public Component[] getMenuComponents() {
        if (isTopMenuBarAfterOpenJDKMemLeakFix()) {
            if (myMenuComponents == null) {
                JMenu temp = new JMenu();
                fillMenu(temp);
                myMenuComponents = temp.getMenuComponents();
            }
            return myMenuComponents;
        }
        else {
            return super.getMenuComponents();
        }
    }

    @Override
    public int getMenuComponentCount() {
        if (isTopMenuBarAfterOpenJDKMemLeakFix()) {
            return getMenuComponents().length;
        }
        return super.getMenuComponentCount();
    }

    private boolean isTopMenuBar() {
        return TopApplicationMenuUtil.isMacSystemMenu && isMainMenuPlace();
    }

    private boolean isTopMenuBarAfterOpenJDKMemLeakFix() {
        if (isTopMenuBar()) {
            // jdk 10 have initial change in screen menu
            return true;
        }
        return false;
    }

    private void clearItems() {
        if (isTopMenuBar()) {
            for (Component menuComponent : getMenuComponents()) {
                if (menuComponent instanceof ActionMenu actionMenu) {
                    actionMenu.clearItems();
                    if (TopApplicationMenuUtil.isMacSystemMenu) {
                        // hideNotify is not called on Macs
                        actionMenu.uninstallSynchronizer();
                    }
                }
                else if (menuComponent instanceof ActionMenuItem actionMenuItem) {
                    actionMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F24, 0));
                }
            }
        }

        removeAll();
        addStubItem();

        validate();
    }

    private void fillMenu(JMenu menu) {
        DataContext context;
        boolean mayContextBeInvalid;

        if (myContext != null) {
            context = myContext;
            mayContextBeInvalid = false;
        }
        else {
            @SuppressWarnings("deprecation") DataContext contextFromFocus = DataManager.getInstance().getDataContext();
            context = contextFromFocus;
            if (context.getData(UIExAWTDataKey.CONTEXT_COMPONENT) == null) {
                IdeFrame frame = DesktopIdeFrameUtil.findIdeFrameFromParent(this);
                context = DataManager.getInstance().getDataContext(IdeFocusManager.getGlobalInstance().getLastFocusedFor(frame));
            }
            mayContextBeInvalid = true;
        }

        Utils.fillMenu(myGroup.getAction(), menu, isMnemonicEnabled(), myPresentationFactory, context, myPlace, true, mayContextBeInvalid, LaterInvocator.isInModalContext());
    }

    private class MenuItemSynchronizer implements PropertyChangeListener {
        @Override
        public void propertyChange(PropertyChangeEvent e) {
            String name = e.getPropertyName();
            if (Presentation.PROP_VISIBLE.equals(name)) {
                setVisible(myPresentation.isVisible());
                if (TopApplicationMenuUtil.isMacSystemMenu && myPlace.equals(ActionPlaces.MAIN_MENU)) {
                    validate();
                }
            }
            else if (Presentation.PROP_ENABLED.equals(name)) {
                setEnabled(myPresentation.isEnabled());
            }
            else if (Presentation.PROP_TEXT.equals(name)) {
                updateTextAndMnemonic((LocalizeValue) e.getNewValue(), Boolean.FALSE);
            }
            else if (Presentation.PROP_DISABLED_MNEMONIC.equals(name)) {
                updateTextAndMnemonic(null, (Boolean) e.getNewValue());
            }
            else if (Presentation.PROP_ICON.equals(name) || Presentation.PROP_DISABLED_ICON.equals(name)) {
                updateIcon();
            }
        }
    }
}
