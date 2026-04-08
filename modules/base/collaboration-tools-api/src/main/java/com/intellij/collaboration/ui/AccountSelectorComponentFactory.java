// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.ui;

import com.intellij.collaboration.auth.Account;
import com.intellij.collaboration.auth.DefaultAccountHolder;
import com.intellij.collaboration.auth.ServerAccount;
import com.intellij.collaboration.ui.icon.IconsProvider;
import com.intellij.collaboration.ui.util.ActionUtil;
import com.intellij.collaboration.ui.util.SwingBindingsUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopupListener;
import com.intellij.openapi.ui.popup.LightweightWindowEvent;
import com.intellij.ui.popup.list.ComboBoxPopup;
import com.intellij.util.ui.cloneDialog.AccountMenuItem;
import com.intellij.util.ui.cloneDialog.AccountMenuItemRenderer;
import kotlinx.coroutines.CoroutineScope;
import kotlinx.coroutines.flow.MutableStateFlow;
import kotlinx.coroutines.flow.StateFlow;
import org.jetbrains.annotations.Nls;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

public final class AccountSelectorComponentFactory<A extends Account> {
    private final StateFlow<Collection<A>> myAccountsState;
    private final MutableStateFlow<A> mySelectionState;

    public AccountSelectorComponentFactory(
        @Nonnull StateFlow<Collection<A>> accountsState,
        @Nonnull MutableStateFlow<A> selectionState
    ) {
        myAccountsState = accountsState;
        mySelectionState = selectionState;
    }

    public @Nonnull JComponent create(
        @Nonnull CoroutineScope scope,
        @Nullable DefaultAccountHolder<A> defaultAccountHolder,
        @Nonnull IconsProvider<A> avatarIconsProvider,
        int avatarSize,
        int popupAvatarSize,
        @Nls @Nonnull String emptyStateTooltip,
        @Nonnull StateFlow<List<Action>> actions
    ) {
        ComboBoxWithActionsModel<A> comboModel = new ComboBoxWithActionsModel<>();
        SwingBindingsUtil.bindComboBoxWithActionsModel(
            scope, comboModel, myAccountsState, mySelectionState, actions,
            Comparator.comparing(a -> a.getName())
        );

        A defaultAccount = defaultAccountHolder != null ? defaultAccountHolder.getAccount() : null;
        if (comboModel.getSelectedItem() == null && defaultAccount != null) {
            comboModel.setSelectedItem(new ComboBoxWithActionsModel.Item.Wrapper<>(defaultAccount));
        }

        if (comboModel.getSize() > 0 && comboModel.getSelectedItem() == null) {
            for (int i = 0; i < comboModel.getSize(); i++) {
                ComboBoxWithActionsModel.Item<A> item = comboModel.getElementAt(i);
                if (item instanceof ComboBoxWithActionsModel.Item.Wrapper<A>) {
                    comboModel.setSelectedItem(item);
                    break;
                }
            }
        }

        JLabel label = new JLabel();
        label.setOpaque(false);
        label.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        label.setFocusable(true);
        label.setBorder(new SimpleFocusBorder());

        new Controller<>(comboModel, label, avatarIconsProvider, avatarSize, popupAvatarSize, emptyStateTooltip);
        return label;
    }

    public @Nonnull JComponent create(
        @Nonnull CoroutineScope scope,
        @Nullable DefaultAccountHolder<A> defaultAccountHolder,
        @Nonnull IconsProvider<A> avatarIconsProvider,
        int avatarSize,
        int popupAvatarSize,
        @Nls @Nonnull String emptyStateTooltip
    ) {
        return create(scope, defaultAccountHolder, avatarIconsProvider, avatarSize, popupAvatarSize, emptyStateTooltip,
            kotlinx.coroutines.flow.StateFlowKt.MutableStateFlow(List.of())
        );
    }

    private static final class Controller<A extends Account>
        implements ComboBoxPopup.Context<ComboBoxWithActionsModel.Item<A>> {

        private final ComboBoxWithActionsModel<A> accountsModel;
        private final JLabel label;
        private final IconsProvider<A> avatarIconsProvider;
        private final int avatarSize;
        private final int popupAvatarSize;
        private final @Nls String emptyStateTooltip;
        private ComboBoxPopup<?> popup;

        Controller(
            @Nonnull ComboBoxWithActionsModel<A> accountsModel,
            @Nonnull JLabel label,
            @Nonnull IconsProvider<A> avatarIconsProvider,
            int avatarSize,
            int popupAvatarSize,
            @Nls @Nonnull String emptyStateTooltip
        ) {
            this.accountsModel = accountsModel;
            this.label = label;
            this.avatarIconsProvider = avatarIconsProvider;
            this.avatarSize = avatarSize;
            this.popupAvatarSize = popupAvatarSize;
            this.emptyStateTooltip = emptyStateTooltip;

            getModel().addListDataListener(new ListDataListener() {
                @Override
                public void contentsChanged(ListDataEvent e) {
                    updateLabel();
                }

                @Override
                public void intervalAdded(ListDataEvent e) {
                    updateLabel();
                }

                @Override
                public void intervalRemoved(ListDataEvent e) {
                    updateLabel();
                }
            });
            label.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    showPopup();
                }
            });
            registerPopupOnKeyboardShortcut(label, KeyEvent.VK_ENTER);
            registerPopupOnKeyboardShortcut(label, KeyEvent.VK_SPACE);
            updateLabel();
        }

        private void updateLabel() {
            ComboBoxWithActionsModel.Item.Wrapper<A> selected = accountsModel.getSelectedItem();
            A selectedAccount = selected != null ? selected.wrappee() : null;
            label.setIcon(avatarIconsProvider.getIcon(selectedAccount, avatarSize));
            label.setToolTipText(selectedAccount != null ? selectedAccount.getName() : emptyStateTooltip);
        }

        private void showPopup() {
            if (!label.isEnabled()) {
                return;
            }
            popup = new ComboBoxPopup<>(this, accountsModel.getSelectedItem(), item -> accountsModel.setSelectedItem(item)) {
                @Override
                protected ListCellRenderer<ComboBoxWithActionsModel.Item<A>> getListElementRenderer() {
                    return getRenderer();
                }
            };
            popup.addListener(new JBPopupListener() {
                @Override
                public void onClosed(@Nonnull LightweightWindowEvent event) {
                    popup = null;
                }
            });
            popup.showUnderneathOf(label);
        }

        private void registerPopupOnKeyboardShortcut(@Nonnull JComponent component, int keyCode) {
            component.registerKeyboardAction(e -> showPopup(), KeyStroke.getKeyStroke(keyCode, 0), JComponent.WHEN_FOCUSED);
        }

        @Override
        public @Nullable Project getProject() {
            return null;
        }

        @Override
        public @Nonnull ListModel<ComboBoxWithActionsModel.Item<A>> getModel() {
            return accountsModel;
        }

        @Override
        public @Nonnull ListCellRenderer<ComboBoxWithActionsModel.Item<A>> getRenderer() {
            return new PopupItemRenderer();
        }

        private final class PopupItemRenderer implements ListCellRenderer<ComboBoxWithActionsModel.Item<A>> {
            private final AccountMenuItemRenderer delegateRenderer = new AccountMenuItemRenderer();

            @Override
            public Component getListCellRendererComponent(
                JList<? extends ComboBoxWithActionsModel.Item<A>> list,
                ComboBoxWithActionsModel.Item<A> value,
                int index, boolean selected, boolean focused
            ) {
                AccountMenuItem item;
                if (value instanceof ComboBoxWithActionsModel.Item.Wrapper<A> wrapper) {
                    A account = wrapper.wrappee();
                    Icon icon = avatarIconsProvider.getIcon(account, popupAvatarSize);
                    String serverAddress = account instanceof ServerAccount sa ? sa.getServer().toString() : "";
                    item = new AccountMenuItem.Account(account.getName(), serverAddress, icon);
                }
                else if (value instanceof ComboBoxWithActionsModel.Item.Action<A> actionItem) {
                    String name = ActionUtil.getName(actionItem.getAction());
                    item = new AccountMenuItem.Action(
                        name != null ? name : "",
                        () -> {
                        },
                        actionItem.getNeedSeparatorAbove()
                    );
                }
                else {
                    throw new IllegalStateException("Unknown item type: " + value);
                }
                return delegateRenderer.getListCellRendererComponent(null, item, index, selected, focused);
            }
        }
    }
}
