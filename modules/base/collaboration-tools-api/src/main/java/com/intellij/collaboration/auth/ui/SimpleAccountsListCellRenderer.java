// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.collaboration.auth.ui;

import com.intellij.collaboration.auth.Account;
import com.intellij.collaboration.auth.AccountDetails;
import com.intellij.collaboration.auth.ServerAccount;
import com.intellij.collaboration.ui.codereview.avatar.Avatar;
import consulo.collaboration.localize.CollaborationToolsLocalize;
import consulo.ui.ex.awt.GridBag;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awt.LinkLabel;
import consulo.ui.ex.awt.UIUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jetbrains.annotations.Nls;

import javax.swing.*;
import java.awt.*;
import java.util.function.Predicate;

final class SimpleAccountsListCellRenderer<A extends Account, D extends AccountDetails>
    extends JPanel implements ListCellRenderer<A> {

    private final Predicate<A> defaultPredicate;
    private final LoadingAccountsDetailsProvider<A, D> detailsProvider;
    private final AccountsPanelActionsController<A> actionsController;

    private final JLabel accountName = new JLabel();
    private final JLabel serverName = new JLabel();
    private final JLabel profilePicture = new JLabel();
    private final JLabel fullName = new JLabel();
    private final JLabel loadingError = new JLabel();
    private final LinkLabel<Object> reloginLink = new LinkLabel<>(CollaborationToolsLocalize.loginLink().get(), null);

    SimpleAccountsListCellRenderer(
        @Nonnull Predicate<A> defaultPredicate,
        @Nonnull LoadingAccountsDetailsProvider<A, D> detailsProvider,
        @Nonnull AccountsPanelActionsController<A> actionsController
    ) {
        this.defaultPredicate = defaultPredicate;
        this.detailsProvider = detailsProvider;
        this.actionsController = actionsController;

        setLayout(ListLayout.horizontal());
        setBorder(JBUI.Borders.empty(5, 8));

        JPanel namesPanel = new JPanel();
        namesPanel.setLayout(new GridBagLayout());
        namesPanel.setBorder(JBUI.Borders.empty(0, 6, 4, 6));

        GridBag bag = new GridBag()
            .setDefaultInsets(JBUI.insetsRight(UIUtil.DEFAULT_HGAP))
            .setDefaultAnchor(GridBagConstraints.WEST)
            .setDefaultFill(GridBagConstraints.VERTICAL);
        namesPanel.add(fullName, bag.nextLine().next());
        namesPanel.add(accountName, bag.next());
        namesPanel.add(loadingError, bag.next());
        namesPanel.add(reloginLink, bag.next());
        namesPanel.add(serverName, bag.nextLine().coverLine());

        add(profilePicture);
        add(namesPanel);
    }

    @Override
    public Component getListCellRendererComponent(
        JList<? extends A> list,
        A account,
        int index,
        boolean isSelected,
        boolean cellHasFocus
    ) {
        UIUtil.setBackgroundRecursively(this, ListUiUtil.WithTallRow.background(list, isSelected, list.hasFocus()));
        Color primaryTextColor = ListUiUtil.WithTallRow.foreground(isSelected, list.hasFocus());
        Color secondaryTextColor = ListUiUtil.WithTallRow.secondaryForeground(isSelected, list.hasFocus());

        D details = getDetails(account);
        accountName.setText(account.getName());
        setBold(accountName, details == null && defaultPredicate.test(account));
        accountName.setForeground(details == null ? primaryTextColor : secondaryTextColor);

        if (account instanceof ServerAccount serverAccount) {
            serverName.setVisible(true);
            serverName.setText(serverAccount.getServer().toString());
        }
        else {
            serverName.setVisible(false);
        }
        serverName.setForeground(secondaryTextColor);

        profilePicture.setIcon(detailsProvider.getIcon(account, Avatar.Sizes.ACCOUNT));

        String detailsName = details != null ? details.getName() : null;
        fullName.setText(detailsName);
        setBold(fullName, defaultPredicate.test(account));
        fullName.setVisible(detailsName != null);
        fullName.setForeground(primaryTextColor);

        loadingError.setText(getError(account));
        loadingError.setForeground(NamedColorUtil.getErrorForeground());

        reloginLink.setVisible(getError(account) != null && needReLogin(account));
        reloginLink.setListener(
            new LinkListener<>() {
                @Override
                public void linkSelected(LinkLabel<Object> source, Object data) {
                    editAccount(list, account);
                }
            },
            null
        );

        return this;
    }

    private void editAccount(@Nonnull JComponent parentComponent, @Nonnull A account) {
        actionsController.editAccount(parentComponent, account);
    }

    @Nullable
    private D getDetails(@Nonnull A account) {
        return detailsProvider.getDetails(account);
    }

    @Nullable
    @Nls
    private String getError(@Nonnull A account) {
        return detailsProvider.getErrorText(account);
    }

    private boolean needReLogin(@Nonnull A account) {
        return detailsProvider.checkErrorRequiresReLogin(account);
    }

    private static void setBold(@Nonnull JLabel label, boolean isBold) {
        Font font = label.getFont();
        if (isBold) {
            label.setFont(font.deriveFont(font.getStyle() | Font.BOLD));
        }
        else {
            label.setFont(font.deriveFont(font.getStyle() & ~Font.BOLD));
        }
    }
}
