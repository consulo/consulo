// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.auth.ui;

import com.intellij.collaboration.auth.Account;
import com.intellij.collaboration.auth.ServerAccount;
import com.intellij.collaboration.ui.CollaborationToolsUIUtil;
import com.intellij.collaboration.ui.icon.IconsProvider;
import consulo.application.util.HtmlChunk;
import consulo.ui.ex.awt.ClickListener;
import consulo.ui.ex.awt.ClientProperty;
import consulo.ui.ex.awt.UIUtil;
import jakarta.annotation.Nonnull;

import javax.swing.*;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class CompactAccountsPanelFactory<A extends Account> {
    private final ListModel<A> accountsListModel;

    public CompactAccountsPanelFactory(@Nonnull ListModel<A> accountsListModel) {
        this.accountsListModel = accountsListModel;
    }

    @Nonnull
    public JComponent create(
        @Nonnull LoadingAccountsDetailsProvider<A, ?> detailsProvider,
        int listAvatarSize,
        @Nonnull PopupConfig<A> popupConfig
    ) {
        IconCellRenderer<A> iconRenderer = new IconCellRenderer<>(detailsProvider, listAvatarSize);

        @SuppressWarnings("UndesirableClassUsage")
        JList<A> accountsList = new JList<>(accountsListModel);
        accountsList.setOpaque(false);
        accountsList.setCellRenderer(iconRenderer);
        ClientProperty.put(accountsList, UIUtil.NOT_IN_HIERARCHY_COMPONENTS, List.of(iconRenderer));

        accountsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        accountsList.setVisibleRowCount(1);
        accountsList.setLayoutOrientation(JList.HORIZONTAL_WRAP);

        LaunchOnShowKt.launchOnShow(accountsList, "AccountsListUpdate", (scope, cont) -> {
            ListDataListener listener = new ListDataListener() {
                @Override
                public void contentsChanged(ListDataEvent e) {
                    updateAccountsTooltip();
                }

                @Override
                public void intervalAdded(ListDataEvent e) {
                    updateAccountsTooltip();
                }

                @Override
                public void intervalRemoved(ListDataEvent e) {
                    updateAccountsTooltip();
                }

                private void updateAccountsTooltip() {
                    accountsList.setToolTipText(buildTooltipHtml());
                }
            };
            accountsListModel.addListDataListener(listener);
            listener.contentsChanged(null); // initial tooltip update
            try {
                kotlinx.coroutines.AwaitCancellationKt.awaitCancellation(cont);
            }
            finally {
                accountsListModel.removeListDataListener(listener);
            }
            return kotlin.Unit.INSTANCE;
        });

        new PopupMenuListener<>(accountsListModel, detailsProvider, popupConfig).installOn(accountsList);
        return accountsList;
    }

    @Nonnull
    private String buildTooltipHtml() {
        List<HtmlChunk> chunks = new ArrayList<>();
        for (int i = 0; i < accountsListModel.getSize(); i++) {
            chunks.add(HtmlChunk.text(accountsListModel.getElementAt(i).getName()));
        }
        return new HtmlBuilder().appendWithSeparators(HtmlChunk.br(), chunks).toString();
    }

    public interface PopupConfig<A extends Account> {
        int getAvatarSize();

        @Nonnull
        Collection<AccountMenuItem.Action> createActions();
    }

    private static final class IconCellRenderer<A extends Account> extends JLabel implements ListCellRenderer<A> {
        private final IconsProvider<A> iconsProvider;
        private final int avatarSize;

        IconCellRenderer(@Nonnull IconsProvider<A> iconsProvider, int avatarSize) {
            this.iconsProvider = iconsProvider;
            this.avatarSize = avatarSize;
        }

        @Override
        public Component getListCellRendererComponent(
            JList<? extends A> list,
            A value,
            int index,
            boolean isSelected,
            boolean cellHasFocus
        ) {
            setIcon(iconsProvider.getIcon(value, avatarSize));
            return this;
        }
    }

    private static final class PopupMenuListener<A extends Account> extends ClickListener {
        private final ListModel<A> model;
        private final LoadingAccountsDetailsProvider<A, ?> detailsProvider;
        private final PopupConfig<A> popupConfig;

        PopupMenuListener(
            @Nonnull ListModel<A> model,
            @Nonnull LoadingAccountsDetailsProvider<A, ?> detailsProvider,
            @Nonnull PopupConfig<A> popupConfig
        ) {
            this.model = model;
            this.detailsProvider = detailsProvider;
            this.popupConfig = popupConfig;
        }

        @Override
        public boolean onClick(@Nonnull MouseEvent event, int clickCount) {
            Object parentComponent = event.getSource();
            if (!(parentComponent instanceof JComponent jComponent)) {
                return false;
            }
            showPopupMenu(jComponent);
            return true;
        }

        private void showPopupMenu(@Nonnull JComponent parentComponent) {
            List<AccountMenuItem> menuItems = new ArrayList<>();

            for (int index = 0; index < model.getSize(); index++) {
                A account = model.getElementAt(index);
                var details = detailsProvider.getDetails(account);
                String accountTitle = details != null ? details.getName() : account.getName();
                String serverInfo =
                    (account instanceof ServerAccount sa) ? CollaborationToolsUIUtil.cleanupUrl(sa.getServer().toString()) : "";
                Icon avatar = detailsProvider.getIcon(account, popupConfig.getAvatarSize());
                boolean showSeparatorAbove = index != 0;

                menuItems.add(new AccountMenuItem.Account(accountTitle, serverInfo, avatar, List.of(), showSeparatorAbove));
            }
            menuItems.addAll(popupConfig.createActions());

            new AccountsMenuListPopup(null, new AccountMenuPopupStep(menuItems)).showUnderneathOf(parentComponent);
        }
    }
}
