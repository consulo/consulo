/*
 * Copyright 2013-2025 consulo.io
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
package consulo.ide.impl.idea.ui.popup.actionPopup;

import consulo.ide.impl.idea.openapi.actionSystem.impl.MenuItemPresentationFactory;
import consulo.localize.LocalizeValue;
import consulo.ui.ex.action.*;
import consulo.ui.ex.action.util.ActionUtil;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.image.Image;
import consulo.ui.image.ImageEffects;
import consulo.util.dataholder.Key;
import consulo.util.dataholder.UserDataHolder;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ActionPopupItem implements ShortcutProvider, UserDataHolder {
    // do not expose myPresentation
    private final Presentation myPresentation = Presentation.newTemplatePresentation();

    private final AnAction myAction;
    @Nullable
    private final Character myMnemonicChar;
    private final boolean myMnemonicsEnabled;
    private final boolean myHonorActionMnemonics;
    private final int myMaxIconWidth;
    private final int myMaxIconHeight;

    private LocalizeValue myTextValue = LocalizeValue.absent();
    private LocalizeValue myDescription = LocalizeValue.absent();

    private Image myIcon;
    private Image mySelectedIcon;

    private boolean myIsEnabled;

    @Nonnull
    private List<ActionPopupItem> myInlineActions;

    ActionPopupItem(
        @Nonnull AnAction action,
        @Nonnull LocalizeValue actionText
    ) {
        myAction = action;
        myTextValue = actionText;
        myIsEnabled = false;
        myMnemonicChar = null;
        myHonorActionMnemonics = false;
        myMnemonicsEnabled = false;
        myMaxIconWidth = -1;
        myMaxIconHeight = -1;
        myInlineActions = Collections.emptyList();
    }

    ActionPopupItem(
        @Nonnull AnAction action,
        @Nullable Character mnemonicChar,
        boolean mnemonicsEnabled,
        boolean honorActionMnemonics,
        int maxIconWidth,
        int maxIconHeight
    ) {
        myAction = action;
        myMnemonicChar = mnemonicChar;
        myMnemonicsEnabled = mnemonicsEnabled;
        myHonorActionMnemonics = honorActionMnemonics;
        myMaxIconWidth = maxIconWidth;
        myMaxIconHeight = maxIconHeight;
        myAction.getTemplatePresentation().addPropertyChangeListener(evt -> {
            if (evt.getPropertyName().equals(Presentation.PROP_TEXT)) {
                myTextValue = myAction.getTemplatePresentation().getTextValue();
            }
        });
        myInlineActions = Collections.emptyList();
    }

    @Nonnull
    public List<ActionPopupItem> getInlineItems() {
        return myInlineActions;
    }

    void updateFromPresentation(@Nonnull PresentationFactory presentationFactory, @Nonnull String actionPlace) {
        Presentation presentation = presentationFactory.getPresentation(myAction);
        updateFromPresentation(presentation, actionPlace);

        List<? extends AnAction> inlineActions = presentation.getClientProperty(ActionUtil.INLINE_ACTIONS);
        myInlineActions = createInlineItems(presentationFactory, actionPlace, inlineActions);
    }

    void updateFromPresentation(@Nonnull Presentation presentation, @Nonnull String actionPlace) {
        myPresentation.copyFrom(presentation, null, true);

        if (myAction instanceof AnSeparator anSeparator) {
            myTextValue = anSeparator.getTextValue();
            return;
        }

        myIsEnabled = presentation.isEnabled();

        boolean enabled = presentation.isEnabled();
        LocalizeValue textValue = presentation.getTextValue();
        if (myMnemonicsEnabled) {
            if (myMnemonicChar != null) {
                textValue = textValue.map(text -> "&" + myMnemonicChar + ". " + text);
            }
        }
        else if (presentation.isDisabledMnemonic()) {
            // do nothing, in this case we will show '_' as part of action item
        }
        else if (!myHonorActionMnemonics) {
            textValue = presentation.getTextValue().map(Presentation.NO_MNEMONIC);
        }

        boolean hideIcon = Boolean.TRUE.equals(presentation.getClientProperty(MenuItemPresentationFactory.HIDE_ICON));
        Image icon = hideIcon ? null : presentation.getIcon();
        Image selectedIcon = hideIcon ? null : presentation.getSelectedIcon();
        Image disabledIcon = hideIcon ? null : presentation.getDisabledIcon();

        if (icon == null && selectedIcon == null) {
            if (myAction instanceof Toggleable && Toggleable.isSelected(presentation)) {
                selectedIcon = TargetAWT.wrap(UIManager.getIcon("Menu.selectedCheckboxIcon"));
                disabledIcon = null;
            }
        }
        if (!enabled) {
            icon = disabledIcon != null || icon == null ? disabledIcon : ImageEffects.grayed(icon);
            selectedIcon = disabledIcon != null || selectedIcon == null ? disabledIcon : ImageEffects.grayed(selectedIcon);
        }

        if (myMaxIconWidth != -1 && myMaxIconHeight != -1) {
            if (icon != null) {
                icon = ImageEffects.resize(icon, myMaxIconWidth, myMaxIconHeight);
            }
            if (selectedIcon != null) {
                selectedIcon = ImageEffects.resize(selectedIcon, myMaxIconWidth, myMaxIconHeight);
            }
        }

        if (icon == null) {
            icon = selectedIcon != null ? selectedIcon : getEmptyIconIfCan();
        }
        assert textValue.isPresent() : myAction + " has no presentation";

        myTextValue = textValue;
        myDescription = presentation.getDescriptionValue();
        myIcon = icon;
        mySelectedIcon = selectedIcon;
    }

    private Image getEmptyIconIfCan() {
        if (myMaxIconHeight != -1 && myMaxIconWidth != -1) {
            return Image.empty(myMaxIconWidth, myMaxIconHeight);
        }
        return null;
    }

    @Nonnull
    private List<ActionPopupItem> createInlineItems(@Nonnull PresentationFactory presentationFactory,
                                                    @Nonnull String actionPlace,
                                                    @Nullable List<? extends AnAction> inlineActions) {
        if (inlineActions == null) {
            return Collections.emptyList();
        }
        else {
            List<ActionPopupItem> res = new ArrayList<>();
            for (AnAction a : inlineActions) {
                Presentation p = presentationFactory.getPresentation(a);
                if (!p.isVisible()) {
                    continue;
                }
                ActionPopupItem item = new ActionPopupItem(a, null, false, false, myMaxIconWidth, myMaxIconHeight);
                item.updateFromPresentation(p, actionPlace);
                res.add(item);
            }
            return res.isEmpty() ? Collections.emptyList() : res;
        }
    }

    @Nonnull
    Presentation clonePresentation() {
        return myPresentation.clone();
    }

    @Nonnull
    public AnAction getAction() {
        return myAction;
    }

    @Nonnull
    public LocalizeValue getText() {
        return myTextValue;
    }

    @Nullable
    public Image getIcon(boolean selected) {
        return selected && mySelectedIcon != null ? mySelectedIcon : myIcon;
    }

    public boolean isSeparator() {
        return myAction instanceof AnSeparator;
    }

    public boolean isEnabled() {
        return myIsEnabled;
    }

    public LocalizeValue getDescription() {
        return myDescription;
    }

    @Nullable
    @Override
    public ShortcutSet getShortcut() {
        return myAction.getShortcutSet();
    }

    @Nullable
    public <T> T getClientProperty(@Nonnull Key<T> key) {
        return myPresentation.getClientProperty(key);
    }

    @Nonnull
    public KeepPopupOnPerform getKeepPopupOnPerform() {
        return myPresentation.getKeepPopupOnPerform();
    }

    public boolean isPerformGroup() {
        return myAction instanceof ActionGroup && myPresentation.isPerformGroup();
    }

    @Override
    public String toString() {
        if (myAction instanceof AnSeparator) {
            return "separator: " + myTextValue.get();
        }
        return myTextValue.get();
    }

    @Nullable
    @Override
    public <T> T getUserData(@Nonnull Key<T> key) {
        return myPresentation.getClientProperty(key);
    }

    @Override
    public <T> void putUserData(@Nonnull Key<T> key, @Nullable T value) {
        myPresentation.putClientProperty(key, value);
    }
}
