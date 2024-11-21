/*
 * Copyright 2000-2017 JetBrains s.r.o.
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

import consulo.dataContext.DataContext;
import consulo.dataContext.DataManager;
import consulo.ide.impl.idea.ide.HelpTooltipImpl;
import consulo.ide.impl.idea.openapi.actionSystem.ex.ActionUtil;
import consulo.ide.impl.idea.openapi.actionSystem.ex.TooltipDescriptionProvider;
import consulo.ide.impl.idea.openapi.actionSystem.impl.ActionManagerImpl;
import consulo.ide.impl.idea.openapi.actionSystem.impl.ActionMenuUtil;
import consulo.ide.impl.idea.openapi.actionSystem.impl.MenuItemPresentationFactory;
import consulo.ide.impl.idea.openapi.keymap.KeymapUtil;
import consulo.localize.LocalizeValue;
import consulo.ui.Size;
import consulo.ui.ex.action.*;
import consulo.ui.ex.awt.UIExAWTDataKey;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awt.action.CustomComponentAction;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.ex.internal.ActionButtonEx;
import consulo.ui.ex.internal.ActionManagerEx;
import consulo.ui.ex.internal.HelpTooltip;
import consulo.ui.ex.internal.LocalizeValueWithMnemonic;
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
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class ActionButtonImpl extends JButton implements ActionButtonEx {
    // Contains actions IDs which descriptions are permitted for displaying in the ActionButtonImpl tooltip
    private static final Set<String> WHITE_LIST = Set.of();

    private PropertyChangeListener myPresentationListener;
    protected final Presentation myPresentation;
    protected final AnAction myAction;
    protected final String myPlace;
    private boolean myMouseDown;
    private boolean myRollover;
    private static boolean ourGlobalMouseDown = false;

    private boolean myNoIconsInPopup = false;

    private boolean myMinimalMode;
    private boolean myDecorateButtons;

    protected String myLastComputedText = "";

    @Nullable
    private Function<ActionButton, Image> myImageCalculator;

    @Nullable
    private BiConsumer<HelpTooltip, Presentation> myTooltipPresentationBuilder;

    private Supplier<String> myShortcutBuilder;

    public ActionButtonImpl(AnAction action, Presentation presentation, String place, @Nonnull Size minimumSize) {
        myRollover = false;
        myMouseDown = false;
        myAction = action;
        myPresentation = presentation;
        myPlace = place;

        setMinimumSize(TargetAWT.to(minimumSize));

        putClientProperty(UIUtil.CENTER_TOOLTIP_DEFAULT, Boolean.TRUE);

        updateTextAndMnemonic(presentation.getTextValue());
    }

    @Nonnull
    @Override
    public JComponent getComponent() {
        return this;
    }

    @Nonnull
    @Override
    public consulo.ui.Component getUIComponent() {
        throw new UnsupportedOperationException("unsupported platform");
    }

    @Override
    public void setCustomShortcutBuilder(Supplier<String> shortcutBuilder) {
        myShortcutBuilder = shortcutBuilder;
    }

    public boolean shallPaintDownArrow() {
        if (!(myAction instanceof ActionGroup && ((ActionGroup) myAction).isPopup())) {
            return false;
        }
        if (Boolean.TRUE == myAction.getTemplatePresentation().getClientProperty(HIDE_DROPDOWN_ICON)) {
            return false;
        }
        if (Boolean.TRUE == myPresentation.getClientProperty(HIDE_DROPDOWN_ICON)) {
            return false;
        }
        return true;
    }

    public void setMinimalMode(boolean minimalMode) {
        myMinimalMode = minimalMode;
    }

    public void setDecorateButtons(boolean decorateButtons) {
        myDecorateButtons = decorateButtons;
    }

    public boolean isMinimalMode() {
        return myMinimalMode;
    }

    public boolean isDecorateButtons() {
        return myDecorateButtons;
    }

    @Override
    public void setCustomTooltipBuilder(BiConsumer<HelpTooltip, Presentation> builder) {
        myTooltipPresentationBuilder = builder;
    }

    @Override
    public void setNoIconsInPopup(boolean noIconsInPopup) {
        myNoIconsInPopup = noIconsInPopup;
    }

    @Override
    public int getPopState() {
        if (myAction instanceof Toggleable) {
            return getPopState(Toggleable.isSelected(myPresentation));
        }
        else {
            return getPopState(false);
        }
    }

    @Nonnull
    @Override
    public Presentation getPresentation() {
        return myPresentation;
    }

    @Override
    public void setIconOverrider(@Nullable Function<ActionButton, Image> imageCalculator) {
        myImageCalculator = imageCalculator;
    }

    @Override
    public boolean isEnabled() {
        // TODO move to settings via timer action
        return super.isEnabled() && myPresentation != null && myPresentation.isEnabled();
    }

    public boolean isButtonEnabled() {
        return isEnabled();
    }

    private void onMousePresenceChanged(boolean setInfo) {
        ActionMenuUtil.showDescriptionInStatusBar(setInfo, this, myPresentation.getDescription());
    }

    @Override
    public void click() {
        performAction(new MouseEvent(this, MouseEvent.MOUSE_CLICKED, System.currentTimeMillis(), 0, 0, 0, 1, false));
    }

    private void performAction(MouseEvent e) {
        AnActionEvent event = AnActionEvent.createFromInputEvent(e, myPlace, myPresentation, getDataContext(), false, true);
        if (!ActionUtil.lastUpdateAndCheckDumb(myAction, event, false)) {
            return;
        }

        if (isButtonEnabled()) {
            final ActionManagerEx manager = ActionManagerEx.getInstanceEx();
            final DataContext dataContext = event.getDataContext();
            manager.fireBeforeActionPerformed(myAction, dataContext, event);
            Component component = dataContext.getData(UIExAWTDataKey.CONTEXT_COMPONENT);
            if (component != null && !component.isShowing()) {
                return;
            }
            actionPerformed(event);
            manager.queueActionPerformedEvent(myAction, dataContext, event);
            if (event.getInputEvent() instanceof MouseEvent) {
                //FIXME [VISTALL] we need that ?ToolbarClicksCollector.record(myAction, myPlace);
            }
        }
    }

    protected DataContext getDataContext() {
        ActionToolbar actionToolbar = UIUtil.getParentOfType(ActionToolbar.class, this);
        return actionToolbar != null ? actionToolbar.getToolbarDataContext() : DataManager.getInstance().getDataContext();
    }

    private void actionPerformed(final AnActionEvent event) {
        if (myAction instanceof ActionGroup && !(myAction instanceof CustomComponentAction) && ((ActionGroup) myAction).isPopup() && !((ActionGroup) myAction).canBePerformed(event.getDataContext())) {
            final ActionManagerImpl am = (ActionManagerImpl) ActionManager.getInstance();
            DesktopActionPopupMenuImpl popupMenu = (DesktopActionPopupMenuImpl) am.createActionPopupMenu(event.getPlace(), (ActionGroup) myAction, new MenuItemPresentationFactory() {
                @Override
                protected void processPresentation(Presentation presentation) {
                    if (myNoIconsInPopup) {
                        presentation.setIcon(null);
                        presentation.setHoveredIcon(null);
                    }
                }
            });
            popupMenu.setDataContextProvider(this::getDataContext);
            if (event.isFromActionToolbar()) {
                popupMenu.getComponent().show(this, 0, getHeight());
            }
            else {
                popupMenu.getComponent().show(this, getWidth(), 0);
            }

        }
        else {
            ActionUtil.performActionDumbAware(myAction, event);
        }
    }

    @Override
    public void removeNotify() {
        if (myPresentationListener != null) {
            myPresentation.removePropertyChangeListener(myPresentationListener);
            myPresentationListener = null;
        }
        super.removeNotify();
    }

    @Override
    public void addNotify() {
        super.addNotify();
        if (myPresentationListener == null) {
            myPresentation.addPropertyChangeListener(myPresentationListener = this::presentationPropertyChanded);
        }
        AnActionEvent e = AnActionEvent.createFromInputEvent(null, myPlace, myPresentation, getDataContext(), false, true);
        ActionUtil.performDumbAwareUpdate(myAction, e, false);
        updateToolTipText();
        updateIcon();
    }

    @Override
    public void setToolTipText(String s) {
        String tooltipText = KeymapUtil.createTooltipText(s, myAction);
        super.setToolTipText(tooltipText.length() > 0 ? tooltipText : null);
    }

    @Override
    public void updateToolTipText() {
        LocalizeValue textValue = myPresentation.getTextValue();
        LocalizeValue descriptionValue = myPresentation.getDescriptionValue();
        HelpTooltipImpl.dispose(this);

        if (textValue != LocalizeValue.of() || descriptionValue != LocalizeValue.of()) {
            HelpTooltipImpl ht = new HelpTooltipImpl();
            if (myTooltipPresentationBuilder != null) {
                myTooltipPresentationBuilder.accept(ht, myPresentation);
            }
            else {
                ht.setTitle(textValue.map(Presentation.NO_MNEMONIC).getValue());
                ht.setShortcut(getShortcutText());

                String id = ActionManager.getInstance().getId(myAction);
                if (!textValue.equals(descriptionValue) && (id != null && WHITE_LIST.contains(id) || myAction instanceof TooltipDescriptionProvider)) {
                    ht.setDescription(descriptionValue.getValue());
                }
            }
            ht.installOn(this);
        }
    }

    @Nullable
    protected String getShortcutText() {
        if (myShortcutBuilder != null) {
            return myShortcutBuilder.get();
        }
        return KeymapUtil.getFirstKeyboardShortcutText(myAction);
    }

//    /**
//     * @return button's icon. Icon depends on action's state. It means that the method returns
//     * disabled icon if action is disabled. If the action's icon is {@code null} then it returns
//     * an empty icon.
//     */
//    public Image getIconImpl() {
//        if (myImageCalculator != null) {
//            return myImageCalculator.apply(this);
//        }
//
//        boolean enabled = isButtonEnabled();
//        int popState = getPopState();
//        Image hoveredIcon = (popState == POPPED || popState == PUSHED) ? myPresentation.getHoveredIcon() : null;
//        Image icon = enabled ? hoveredIcon != null ? hoveredIcon : myIcon : myDisabledIcon;
//        return icon == null ? getFallbackIcon(enabled) : icon;
//    }

    @Nonnull
    protected Image getFallbackIcon(boolean enabled) {
        return Image.empty(18);
    }

    @Override
    public void updateIcon() {
        Image icon = myPresentation.getIcon();
        setIcon(TargetAWT.to(icon));

        Image disabledIcon;
        if (myPresentation.getDisabledIcon() != null) { // set disabled icon if it is specified
            disabledIcon = myPresentation.getDisabledIcon();
        }
        else {
            disabledIcon = icon == null ? null : ImageEffects.grayed(icon);
        }

        setDisabledIcon(TargetAWT.to(disabledIcon));
    }

    @Override
    protected void processMouseEvent(MouseEvent e) {
        super.processMouseEvent(e);
        if (e.isConsumed()) {
            return;
        }
        boolean skipPress = e.isMetaDown() || e.getButton() != MouseEvent.BUTTON1;
        switch (e.getID()) {
            case MouseEvent.MOUSE_PRESSED:
                if (skipPress || !isButtonEnabled()) {
                    return;
                }
                myMouseDown = true;
                ourGlobalMouseDown = true;
                repaint();
                break;

            case MouseEvent.MOUSE_RELEASED:
                if (skipPress || !isButtonEnabled()) {
                    return;
                }
                myMouseDown = false;
                ourGlobalMouseDown = false;
                if (myRollover) {
                    performAction(e);
                }
                repaint();
                break;

            case MouseEvent.MOUSE_ENTERED:
                if (!myMouseDown && ourGlobalMouseDown) {
                    break;
                }
                myRollover = true;
                repaint();
                onMousePresenceChanged(true);
                break;

            case MouseEvent.MOUSE_EXITED:
                myRollover = false;
                if (!myMouseDown && ourGlobalMouseDown) {
                    break;
                }
                repaint();
                onMousePresenceChanged(false);
                break;
        }
    }

    private int getPopState(boolean isPushed) {
        if (isPushed || myRollover && myMouseDown && isButtonEnabled()) {
            return PUSHED;
        }
        else if (myRollover && isButtonEnabled()) {
            return POPPED;
        }
        else if (isFocusOwner()) {
            return SELECTED;
        }
        else {
            return NORMAL;
        }
    }

    public void update() {
        AnActionEvent e = AnActionEvent.createFromInputEvent(null, myPlace, myPresentation, getDataContext(), false, true);
        ActionUtil.performDumbAwareUpdate(myAction, e, false);
        updateToolTipText();
        updateIcon();
    }

    @Override
    public final boolean isSelected() {
        return myAction instanceof Toggleable && Toggleable.isSelected(myPresentation);
    }

    @Override
    public AnAction getIdeAction() {
        return myAction;
    }

    protected void updateTextAndMnemonic(@Nonnull LocalizeValue localizeValue) {
        boolean disabledMnemonic = myPresentation.isDisabledMnemonic();

        if (disabledMnemonic) {
            myLastComputedText = localizeValue.getValue();
            setDisplayedMnemonicIndex(-1);
        }
        else {
            TextWithMnemonic textWithMnemonic = LocalizeValueWithMnemonic.get(localizeValue);
            myLastComputedText = textWithMnemonic.getText();
            setDisplayedMnemonicIndex(textWithMnemonic.getMnemonicIndex());
        }
    }

    protected void presentationPropertyChanded(PropertyChangeEvent e) {
        String propertyName = e.getPropertyName();
        if (Presentation.PROP_TEXT.equals(propertyName)) {
            updateTextAndMnemonic((LocalizeValue) e.getNewValue());
            updateToolTipText();
        }
        else if (Presentation.PROP_ENABLED.equals(propertyName)) {
            updateIcon();
            repaint();
        }
        else if (Presentation.PROP_ICON.equals(propertyName)) {
            updateIcon();
            repaint();
        }
        else if (Presentation.PROP_DISABLED_ICON.equals(propertyName)) {
            updateIcon();
            repaint();
        }
        else if (Presentation.PROP_VISIBLE.equals(propertyName)) {
        }
        else if ("selected".equals(propertyName)) {
            repaint();
        }
    }
}
