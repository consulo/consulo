// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.auth.ui;

import com.intellij.collaboration.auth.Account;
import com.intellij.collaboration.auth.AccountManager;
import com.intellij.collaboration.auth.DefaultAccountHolder;
import com.intellij.collaboration.ui.CollaborationToolsUIUtil;
import com.intellij.collaboration.ui.HorizontalListPanel;
import com.intellij.collaboration.ui.ListModelExtKt;
import com.intellij.collaboration.ui.util.JListHoveredRowMaterialiser;
import com.intellij.credentialStore.PasswordSafeConfigurable;
import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.keymap.KeymapUtil;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.options.ex.Settings;
import com.intellij.openapi.progress.CoroutinesKt;
import com.intellij.platform.ide.progress.ModalTaskOwner;
import com.intellij.ui.LayeredIcon;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.ui.dsl.builder.*;
import com.intellij.util.ui.StatusText;
import com.intellij.util.ui.UIUtil;
import consulo.collaboration.localize.CollaborationToolsLocalize;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.ui.ex.awt.JBList;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import kotlinx.coroutines.CoroutineScope;
import kotlinx.coroutines.flow.Flow;
import kotlinx.coroutines.flow.FlowKt;
import kotlinx.coroutines.flow.SharingStarted;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

public final class AccountsPanelFactory<A extends Account, Cred> {
    private final AccountManager<A, Cred> accountManager;
    @Nullable
    private final DefaultAccountHolder<A> defaultAccountHolder;
    private final AccountsListModel<A, Cred> accountsModel;
    private final CoroutineScope scope;

    private AccountsPanelFactory(
        @Nonnull AccountManager<A, Cred> accountManager,
        @Nullable DefaultAccountHolder<A> defaultAccountHolder,
        @Nonnull AccountsListModel<A, Cred> accountsModel,
        @Nonnull CoroutineScope scope
    ) {
        this.accountManager = accountManager;
        this.defaultAccountHolder = defaultAccountHolder;
        this.accountsModel = accountsModel;
        this.scope = scope;
    }

    public AccountsPanelFactory(
        @Nonnull CoroutineScope scope,
        @Nonnull AccountManager<A, Cred> accountManager,
        @Nonnull DefaultAccountHolder<A> defaultAccountHolder,
        @Nonnull AccountsListModel.WithDefault<A, Cred> accountsModel
    ) {
        this(accountManager, defaultAccountHolder, accountsModel, scope);
    }

    public AccountsPanelFactory(
        @Nonnull CoroutineScope scope,
        @Nonnull AccountManager<A, Cred> accountManager,
        @Nonnull AccountsListModel<A, Cred> accountsModel
    ) {
        this(accountManager, null, accountsModel, scope);
    }

    @Nonnull
    public Cell<JComponent> accountsPanelCell(
        @Nonnull Row row,
        @Nonnull LoadingAccountsDetailsProvider<A, ?> detailsProvider,
        @Nonnull AccountsPanelActionsController<A> actionsController
    ) {
        JBList<A> accountsList = createList(actionsController, detailsProvider);
        JPanel component = wrapWithToolbar(accountsList, actionsController);

        return row.cell(component)
            .onIsModified(c -> isModified())
            .onReset(c -> reset())
            .onApply(c -> apply(component));
    }

    private boolean isModified() {
        boolean defaultModified = false;
        if (defaultAccountHolder != null && accountsModel instanceof AccountsListModel.WithDefault<A, Cred> withDefault) {
            defaultModified = withDefault.getDefaultAccount() != defaultAccountHolder.getAccount();
        }

        return !accountsModel.getNewCredentials().isEmpty()
            || !accountsModel.getAccounts().equals(accountManager.getAccountsState().getValue())
            || defaultModified;
    }

    private void reset() {
        accountsModel.setAccounts(accountManager.getAccountsState().getValue());
        if (defaultAccountHolder != null && accountsModel instanceof AccountsListModel.WithDefault<A, Cred> withDefault) {
            withDefault.setDefaultAccount(defaultAccountHolder.getAccount());
        }
        accountsModel.clearNewCredentials();
    }

    private void apply(@Nonnull JComponent component) {
        try {
            Map<A, Cred> newTokensMap = new LinkedHashMap<>();
            newTokensMap.putAll(accountsModel.getNewCredentials());
            for (A account : accountsModel.getAccounts()) {
                newTokensMap.putIfAbsent(account, null);
            }
            CoroutinesKt.runBlockingModalWithRawProgressReporter(
                ModalTaskOwner.component(component),
                CollaborationToolsLocalize.accountsSavingCredentials().get(),
                (cont) -> accountManager.updateAccounts(newTokensMap, cont)
            );
            accountsModel.clearNewCredentials();

            if (defaultAccountHolder != null && accountsModel instanceof AccountsListModel.WithDefault<A, Cred> withDefault) {
                A defaultAccount = withDefault.getDefaultAccount();
                defaultAccountHolder.setAccount(defaultAccount);
            }
        }
        catch (Exception ignored) {
        }
    }

    @Nonnull
    @SuppressWarnings("unchecked")
    private JBList<A> createList(
        @Nonnull AccountsPanelActionsController<A> actionsController,
        @Nonnull LoadingAccountsDetailsProvider<A, ?> detailsLoadingVm
    ) {
        SimpleAccountsListCellRenderer<A, ?> renderer = new SimpleAccountsListCellRenderer<>(
            account -> (accountsModel instanceof AccountsListModel.WithDefault<?, ?> wd) && account.equals(((AccountsListModel.WithDefault<A, Cred>) wd).getDefaultAccount()),
            detailsLoadingVm, actionsController
        );

        JBList<A> accountsList = new JBList<>(accountsModel.getAccountsListModel());
        accountsList.setCellRenderer(renderer);
        UIUtil.putClientProperty(accountsList, UIUtil.NOT_IN_HIERARCHY_COMPONENTS, List.of(renderer));
        accountsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        SimpleAccountsListCellRenderer<A, ?> materialiserRenderer = new SimpleAccountsListCellRenderer<>(
            account -> (accountsModel instanceof AccountsListModel.WithDefault<?, ?> wd) && account.equals(((AccountsListModel.WithDefault<A, Cred>) wd).getDefaultAccount()),
            detailsLoadingVm, actionsController
        );
        JListHoveredRowMaterialiser<A> rowMaterialiser = JListHoveredRowMaterialiser.install(accountsList, materialiserRenderer);

        kotlinx.coroutines.BuildersKt.launch(scope, null, null, (s, cont) -> {
            return detailsLoadingVm.getLoadingState().collect(loading -> {
                accountsList.setPaintBusy((Boolean) loading);
                return kotlin.Unit.INSTANCE;
            }, cont);
        });

        kotlinx.coroutines.BuildersKt.launch(
            scope,
            null,
            null,
            (s, cont) -> detailsLoadingVm.getLoadingCompletionFlow().collect(
                account -> {
                    repaint(accountsList, (A) account);
                    rowMaterialiser.update();
                    return kotlin.Unit.INSTANCE;
                },
                cont
            )
        );

        accountsList.addListSelectionListener(e -> accountsModel.setSelectedAccount(accountsList.getSelectedValue()));

        accountsList.getEmptyText()
            .appendText(CollaborationToolsLocalize.accountsNoneAdded())
            .appendSecondaryText(
                CollaborationToolsLocalize.accountsAddLink().get(),
                SimpleTextAttributes.LINK_PLAIN_ATTRIBUTES,
                e -> {
                    RelativePoint relativePoint = (e instanceof MouseEvent me) ? new RelativePoint(me) : null;
                    actionsController.addAccount(accountsList, relativePoint);
                }
            )
            .appendSecondaryText(
                " (" + KeymapUtil.getFirstKeyboardShortcutText(CommonShortcuts.getNew()) + ")",
                StatusText.DEFAULT_ATTRIBUTES,
                null
            );

        return accountsList;
    }

    private boolean repaint(@Nonnull JList<A> list, @Nonnull A account) {
        int idx = ListModelExtKt.findIndex(list.getModel(), account);
        if (idx < 0) {
            return true;
        }
        Rectangle cellBounds = list.getCellBounds(idx, idx);
        list.repaint(cellBounds);
        return false;
    }

    @Nonnull
    private JPanel wrapWithToolbar(@Nonnull JBList<A> accountsList, @Nonnull AccountsPanelActionsController<A> actionsController) {
        Icon addIcon = actionsController.isAddActionWithPopup() ? LayeredIcon.ADD_WITH_DROPDOWN : PlatformIconGroup.generalAdd();

        ToolbarDecorator toolbar = ToolbarDecorator.createDecorator(accountsList)
            .disableUpDownActions()
            .setAddAction(button -> actionsController.addAccount(accountsList, button.getPreferredPopupPoint()))
            .setAddIcon(addIcon)
            .setRemoveAction(button -> {
                A selected = accountsList.getSelectedValue();
                if (selected != null && accountsModel instanceof MutableAccountsListModel<A, Cred> mutable) {
                    mutable.remove(selected);
                }
            })
            .setRemoveActionUpdater(e -> accountsModel instanceof MutableAccountsListModel && accountsList.getSelectedValue() != null);

        if (accountsModel instanceof AccountsListModel.WithDefault<A, Cred> withDefault) {
            toolbar.addExtraAction(new DumbAwareAction(
                CollaborationToolsLocalize.accountsSetDefault(),
                LocalizeValue.empty(),
                PlatformIconGroup.actionsChecked()
            ) {
                @Override
                @RequiredUIAccess
                public void actionPerformed(@Nonnull AnActionEvent e) {
                    A selected = accountsList.getSelectedValue();
                    if (selected == null || selected.equals(withDefault.getDefaultAccount())) {
                        return;
                    }
                    withDefault.setDefaultAccount(selected);
                }

                @Override
                public void update(@Nonnull AnActionEvent e) {
                    e.getPresentation()
                        .setEnabled(!Objects.equals(withDefault.getDefaultAccount(), accountsList.getSelectedValue()));
                }

                @Nonnull
                @Override
                public ActionUpdateThread getActionUpdateThread() {
                    return ActionUpdateThread.EDT;
                }
            });
        }

        return toolbar.createPanel();
    }

    /**
     * Adds a warning to a panel that tells the user that passwords and other credentials
     * are currently not persisted to disk.
     */
    @Nonnull
    public static CellBase<Panel> addWarningForPersistentCredentials(
        @Nonnull CoroutineScope cs,
        @Nonnull Flow<Boolean> canPersistCredentials,
        @Nonnull Function<PanelConsumer, Panel> panel,
        @Nullable Consumer<DataContext> solution
    ) {
        return panel.apply(p -> {
            p.row(r -> {
                HorizontalListPanel hp = new HorizontalListPanel(4);
                LocalizeValue warning = solution != null
                    ? CollaborationToolsLocalize.accountsErrorPasswordNotSavedColon()
                    : CollaborationToolsLocalize.accountsErrorPasswordNotSaved();
                hp.add(new JLabel(warning.get(), PlatformIconGroup.generalWarning(), SwingConstants.LEFT));

                if (solution != null) {
                    ActionLink link = new ActionLink(CollaborationToolsLocalize.accountsErrorPasswordNotSavedLink().get());
                    link.addActionListener(e -> {
                        if (e.getSource() != link) {
                            return;
                        }
                        solution.accept(DataManager.getInstance().getDataContext(link));
                    });
                    hp.add(link);
                }
                r.cell(hp);
                return kotlin.Unit.INSTANCE;
            });
            return kotlin.Unit.INSTANCE;
        }).visibleIf(CollaborationToolsUIUtil.asObservableIn(
            FlowKt.stateIn(FlowKt.map(canPersistCredentials, v -> !v), cs, SharingStarted.Companion.getLazily(), false),
            cs
        ));
    }

    @Nonnull
    public static CellBase<Panel> addWarningForMemoryOnlyPasswordSafeAndGet(
        @Nonnull CoroutineScope cs,
        @Nonnull Flow<Boolean> canPersistCredentials,
        @Nonnull Function<PanelConsumer, Panel> panel
    ) {
        return addWarningForPersistentCredentials(
            cs,
            canPersistCredentials,
            panel,
            dataContext -> {
                Settings settings = Settings.KEY.getData(dataContext);
                com.intellij.openapi.project.Project project = CommonDataKeys.PROJECT.getData(dataContext);
                if (settings != null) {
                    settings.select(settings.find(PasswordSafeConfigurable.class));
                }
                else {
                    ShowSettingsUtil.getInstance().showSettingsDialog(project, PasswordSafeConfigurable.class);
                }
            }
        );
    }

    /**
     * Adds a warning to a panel that tells the user that password safe settings are
     * currently not set to persistent storage, meaning no passwords or tokens are
     * persisted to storage.
     * <p>
     * This specific function also adds a link to the settings page to solve it.
     */
    public static void addWarningForMemoryOnlyPasswordSafe(
        @Nonnull CoroutineScope cs,
        @Nonnull Flow<Boolean> canPersistCredentials,
        @Nonnull Function<PanelConsumer, Panel> panel
    ) {
        addWarningForMemoryOnlyPasswordSafeAndGet(cs, canPersistCredentials, panel);
    }

    @FunctionalInterface
    public interface PanelConsumer {
        @Nonnull
        kotlin.Unit accept(@Nonnull Panel panel);
    }
}
