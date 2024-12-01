/*
 * Copyright 2013-2024 consulo.io
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
package consulo.desktop.awt.action.toolbar;

import consulo.dataContext.DataContext;
import consulo.desktop.awt.action.DesktopActionPopupMenuImpl;
import consulo.ide.impl.idea.ide.HelpTooltipImpl;
import consulo.ide.impl.idea.openapi.actionSystem.ex.ActionUtil;
import consulo.ide.impl.idea.openapi.actionSystem.ex.TooltipDescriptionProvider;
import consulo.ide.impl.idea.openapi.actionSystem.impl.ActionManagerImpl;
import consulo.ide.impl.idea.openapi.actionSystem.impl.MenuItemPresentationFactory;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.ex.action.*;
import consulo.ui.ex.awt.UIExAWTDataKey;
import consulo.ui.ex.awt.action.CustomComponentAction;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.ex.internal.ActionManagerEx;
import consulo.ui.ex.internal.CustomShortcutBuilder;
import consulo.ui.ex.internal.CustomTooltipBuilder;
import consulo.ui.ex.internal.LocalizeValueWithMnemonic;
import consulo.ui.ex.keymap.util.KeymapUtil;
import consulo.ui.image.Image;
import consulo.ui.image.ImageEffects;
import consulo.ui.util.TextWithMnemonic;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import kava.beans.PropertyChangeEvent;
import kava.beans.PropertyChangeListener;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.Set;
import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 2024-11-21
 */
public class ActionToolbarButtonEngine {
    // Contains actions IDs which descriptions are permitted for displaying in the ActionButtonImpl tooltip
    private static final Set<String> WHITE_LIST = Set.of();

    private PropertyChangeListener myPresentationListener;

    private final AbstractButton myButton;
    private final AnAction myIdeAction;
    private final Presentation myPresentation;
    private final String myPlace;
    private final Supplier<DataContext> myGetDataContext;

    private boolean myDisplayText;

    private boolean myNoIconsInPopup = false;

    public ActionToolbarButtonEngine(AbstractButton button, AnAction ideAction, Presentation presentation, String place, Supplier<DataContext> getDataContext) {
        myButton = button;
        myIdeAction = ideAction;
        myPresentation = presentation;
        myPlace = place;
        myGetDataContext = getDataContext;
        myDisplayText = ideAction.displayTextInToolbar();
    }

    public Presentation getPresentation() {
        return myPresentation;
    }

    public String getPlace() {
        return myPlace;
    }

    public AnAction getIdeAction() {
        return myIdeAction;
    }

    public void repaint() {
        myButton.repaint();
    }

    public void updateToolTipText() {
        LocalizeValue textValue = myPresentation.getTextValue();
        LocalizeValue descriptionValue = myPresentation.getDescriptionValue();
        HelpTooltipImpl.dispose(myButton);

        if (textValue != LocalizeValue.of() || descriptionValue != LocalizeValue.of()) {
            HelpTooltipImpl ht = new HelpTooltipImpl();

            CustomTooltipBuilder customTooltipBuilder = myPresentation.getClientProperty(CustomTooltipBuilder.KEY);
            if (customTooltipBuilder != null) {
                customTooltipBuilder.build(ht, myPresentation);
            }
            else {
                ht.setTitle(textValue.map(Presentation.NO_MNEMONIC).getValue());
                ht.setShortcut(getShortcutText());

                String id = ActionManager.getInstance().getId(myIdeAction);
                if (!textValue.equals(descriptionValue) && (id != null && WHITE_LIST.contains(id) || myIdeAction instanceof TooltipDescriptionProvider)) {
                    if (descriptionValue != LocalizeValue.of()) {
                        ht.setDescription(descriptionValue.getValue());
                    }
                }
            }
            ht.installOn(myButton);
        }
    }

    @Nullable
    protected String getShortcutText() {
        CustomShortcutBuilder shortcutBuilder = myPresentation.getClientProperty(CustomShortcutBuilder.KEY);
        if (shortcutBuilder != null) {
            LocalizeValue shortcutText = shortcutBuilder.build();
            if (shortcutBuilder != LocalizeValue.empty()) {
                return shortcutText.get();
            }
        }

        return KeymapUtil.getFirstKeyboardShortcutText(myIdeAction);
    }

    public void updateIcon() {
        Image icon = myPresentation.getIcon();
        myButton.setIcon(TargetAWT.to(wrapWithArrow(icon)));

        Image disabledIcon;
        if (myPresentation.getDisabledIcon() != null) { // set disabled icon if it is specified
            disabledIcon = wrapWithArrow(myPresentation.getDisabledIcon());
        }
        else {
            Image original = wrapWithArrow(icon);
            disabledIcon = original == null ? null : ImageEffects.grayed(original);
        }

        myButton.setDisabledIcon(TargetAWT.to(disabledIcon));

        Image selectedIcon = wrapWithArrow(myPresentation.getSelectedIcon());
        myButton.setSelectedIcon(TargetAWT.to(selectedIcon));

        Image hoveredIcon = wrapWithArrow(myPresentation.getHoveredIcon());
        myButton.setRolloverEnabled(hoveredIcon != null);
        myButton.setRolloverIcon(TargetAWT.to(hoveredIcon));
    }

    @Nullable
    private Image wrapWithArrow(@Nullable Image icon) {
        if (!(myIdeAction instanceof ActionGroup group)) {
            return icon;
        }

        if (!group.showBelowArrow()) {
            return icon;
        }

        if (icon == null) {
            return null;
        }

        return ImageEffects.layered(icon, PlatformIconGroup.generalDropdown());
    }

    protected void updateTextAndMnemonic(@Nonnull LocalizeValue localizeValue) {
        if (!myDisplayText) {
            myButton.setText("");
            myButton.setDisplayedMnemonicIndex(-1);
            return;
        }

        boolean disabledMnemonic = myPresentation.isDisabledMnemonic();

        if (disabledMnemonic) {
            myButton.setDisplayedMnemonicIndex(-1);
        }
        else {
            TextWithMnemonic textWithMnemonic = LocalizeValueWithMnemonic.get(localizeValue);
            myButton.setText(textWithMnemonic.getText());
            myButton.setDisplayedMnemonicIndex(textWithMnemonic.getMnemonicIndex());
        }
    }

    private void performAction(MouseEvent e) {
        AnActionEvent event = AnActionEvent.createFromInputEvent(e, myPlace, myPresentation, myGetDataContext.get(), false, true);
        if (!ActionUtil.lastUpdateAndCheckDumb(myIdeAction, event, false)) {
            return;
        }

        if (myButton.isEnabled()) {
            final ActionManagerEx manager = ActionManagerEx.getInstanceEx();
            final DataContext dataContext = event.getDataContext();
            manager.fireBeforeActionPerformed(myIdeAction, dataContext, event);
            Component component = dataContext.getData(UIExAWTDataKey.CONTEXT_COMPONENT);
            if (component != null && !component.isShowing()) {
                return;
            }
            actionPerformed(event);
            manager.queueActionPerformedEvent(myIdeAction, dataContext, event);
            if (event.getInputEvent() instanceof MouseEvent) {
                //FIXME [VISTALL] we need that ?ToolbarClicksCollector.record(myAction, myPlace);
            }
        }
    }

    public void removeNotify() {
        if (myPresentationListener != null) {
            myPresentation.removePropertyChangeListener(myPresentationListener);
            myPresentationListener = null;
        }
    }

    public void addNotify() {
        if (myPresentationListener == null) {
            myPresentation.addPropertyChangeListener(myPresentationListener = this::presentationPropertyChanded);
        }
        AnActionEvent e = AnActionEvent.createFromInputEvent(null, myPlace, myPresentation, myGetDataContext.get(), false, true);
        ActionUtil.performDumbAwareUpdate(myIdeAction, e, false);
        updateToolTipText();
        updateIcon();
    }

    public void setNoIconsInPopup(boolean noIconsInPopup) {
        myNoIconsInPopup = noIconsInPopup;
    }

    public void click() {
        performAction(new MouseEvent(myButton, MouseEvent.MOUSE_CLICKED, System.currentTimeMillis(), 0, 0, 0, 1, false));
    }

    public void actionPerformed(final AnActionEvent event) {
        if (myIdeAction instanceof ActionGroup && !(myIdeAction instanceof CustomComponentAction) && ((ActionGroup) myIdeAction).isPopup() && !((ActionGroup) myIdeAction).canBePerformed(event.getDataContext())) {
            final ActionManagerImpl am = (ActionManagerImpl) ActionManager.getInstance();
            DesktopActionPopupMenuImpl popupMenu = (DesktopActionPopupMenuImpl) am.createActionPopupMenu(event.getPlace(), (ActionGroup) myIdeAction, new MenuItemPresentationFactory() {
                @Override
                protected void processPresentation(Presentation presentation) {
                    if (myNoIconsInPopup) {
                        presentation.setIcon(null);
                        presentation.setHoveredIcon(null);
                    }
                }
            });
            popupMenu.setDataContextProvider(myGetDataContext::get);
            if (event.isFromActionToolbar()) {
                popupMenu.getComponent().show(myButton, 0, myButton.getHeight());
            }
            else {
                popupMenu.getComponent().show(myButton, myButton.getWidth(), 0);
            }

        }
        else {
            ActionUtil.performActionDumbAware(myIdeAction, event);
        }
    }

    public void updateEnabled() {
        myButton.setEnabled(myPresentation.isEnabled());
        updateIcon();
    }

    protected void presentationPropertyChanded(PropertyChangeEvent e) {
        String propertyName = e.getPropertyName();
        switch (propertyName) {
            case Presentation.PROP_TEXT:
                updateTextAndMnemonic((LocalizeValue) e.getNewValue());
                updateToolTipText();
                break;
            case Presentation.PROP_ENABLED:
                updateEnabled();
                break;
            case Presentation.PROP_ICON:
                updateIcon();
                break;
            case Presentation.PROP_DISABLED_ICON:
                updateIcon();
                break;
            case Presentation.PROP_VISIBLE:
                myButton.setVisible(myPresentation.isVisible());
                break;
            case "selected":
                repaint();
                break;
        }
    }
}
