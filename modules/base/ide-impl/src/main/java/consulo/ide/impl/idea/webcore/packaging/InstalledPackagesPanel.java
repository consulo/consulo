// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.webcore.packaging;

import consulo.application.Application;
import consulo.application.impl.internal.IdeaModalityState;
import consulo.application.impl.internal.performance.ActivityTracker;
import consulo.application.progress.PerformInBackgroundOption;
import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.ProgressManager;
import consulo.application.progress.Task;
import consulo.ide.impl.idea.ui.DumbAwareActionButton;
import consulo.ide.localize.IdeLocalize;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.platform.base.localize.CommonLocalize;
import consulo.project.Project;
import consulo.repository.ui.*;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.CommonShortcuts;
import consulo.ui.ex.awt.*;
import consulo.ui.ex.awt.event.DoubleClickListener;
import consulo.ui.ex.awt.speedSearch.TableSpeedSearch;
import consulo.ui.ex.awt.table.JBTable;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.util.collection.ContainerUtil;
import consulo.util.concurrent.AsyncResult;
import consulo.util.lang.ObjectUtil;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.List;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;

public class InstalledPackagesPanel extends JPanel {
    private static final Logger LOG = Logger.getInstance(InstalledPackagesPanel.class);

    private final AnActionButton myUpgradeButton;
    protected final AnActionButton myInstallButton;
    private final AnActionButton myUninstallButton;

    protected final JBTable myPackagesTable;
    private final DefaultTableModel myPackagesTableModel;
    // can be accessed from any thread
    protected volatile PackageManagementService myPackageManagementService;
    protected final Project myProject;
    protected final PackagesNotificationPanel myNotificationArea;
    private final Set<String> myCurrentlyInstalling = new HashSet<>();
    private final Map<InstalledPackage, String> myWaitingToUpgrade = new HashMap<>();

    public InstalledPackagesPanel(@Nonnull Project project, @Nonnull PackagesNotificationPanel area) {
        super(new BorderLayout());
        myProject = project;
        myNotificationArea = area;

        String[] names = {
            IdeLocalize.packagesSettingsPackage().get(),
            IdeLocalize.packagesSettingsVersion().get(),
            IdeLocalize.packagesSettingsLatestVersion().get()
        };
        myPackagesTableModel = new DefaultTableModel(names, 0) {
            @Override
            public boolean isCellEditable(int i, int i1) {
                return false;
            }
        };
        final TableCellRenderer tableCellRenderer = new MyTableCellRenderer();
        myPackagesTable = new JBTable(myPackagesTableModel) {
            @Override
            public TableCellRenderer getCellRenderer(int row, int column) {
                return tableCellRenderer;
            }
        };
        myPackagesTable.setShowGrid(false);
        myPackagesTable.getTableHeader().setReorderingAllowed(false);
        new TableSpeedSearch(myPackagesTable);

        myUpgradeButton = new DumbAwareActionButton(
            IdeLocalize.actionPackagesUpgradeText(),
            LocalizeValue.absent(),
            PlatformIconGroup.actionsMoveup()
        ) {
            @Override
            @RequiredUIAccess
            public void actionPerformed(@Nonnull AnActionEvent e) {
                //PackageManagementUsageCollector.triggerUpgradePerformed(myProject, myPackageManagementService);
                upgradeAction();
            }
        };
        myInstallButton = new DumbAwareActionButton(
            IdeLocalize.actionPackagesInstallText(),
            LocalizeValue.absent(),
            PlatformIconGroup.generalAdd()
        ) {
            @Override
            @RequiredUIAccess
            public void actionPerformed(@Nonnull AnActionEvent e) {
                //PackageManagementUsageCollector.triggerBrowseAvailablePackagesPerformed(myProject, myPackageManagementService);
                if (myPackageManagementService != null) {
                    ManagePackagesDialog dialog = createManagePackagesDialog();
                    dialog.show();
                }
            }
        };
        myInstallButton.setShortcut(CommonShortcuts.getNew());
        myUninstallButton = new DumbAwareActionButton(
            IdeLocalize.actionPackagesUninstallText(),
            LocalizeValue.absent(),
            PlatformIconGroup.generalRemove()
        ) {
            @Override
            @RequiredUIAccess
            public void actionPerformed(@Nonnull AnActionEvent e) {
                //PackageManagementUsageCollector.triggerUninstallPerformed(myProject, myPackageManagementService);
                uninstallAction();
            }
        };
        myUninstallButton.setShortcut(CommonShortcuts.getDelete());
        ToolbarDecorator decorator = ToolbarDecorator.createDecorator(myPackagesTable)
            .disableUpDownActions()
            .disableAddAction()
            .disableRemoveAction()
            .addExtraAction(myInstallButton)
            .addExtraAction(myUninstallButton)
            .addExtraAction(myUpgradeButton);

        decorator.addExtraActions(getExtraActions());
        add(decorator.createPanel());
        myInstallButton.setEnabled(false);
        myUninstallButton.setEnabled(false);
        myUpgradeButton.setEnabled(false);

        myPackagesTable.getSelectionModel().addListSelectionListener(event -> updateUninstallUpgrade());

        new DoubleClickListener() {
            @Override
            @RequiredUIAccess
            protected boolean onDoubleClick(@Nonnull MouseEvent e) {
                if (myPackageManagementService != null && myInstallButton.isEnabled()) {
                    ManagePackagesDialog dialog = createManagePackagesDialog();
                    Point p = e.getPoint();
                    int row = myPackagesTable.rowAtPoint(p);
                    int column = myPackagesTable.columnAtPoint(p);
                    if (row >= 0 && column >= 0 && myPackagesTable.getValueAt(row, 0) instanceof InstalledPackage pkg) {
                        dialog.selectPackage(pkg);
                    }
                    dialog.show();
                    return true;
                }
                return false;
            }
        }.installOn(myPackagesTable);
    }

    protected AnAction[] getExtraActions() {
        return new AnAction[0];
    }

    @Nonnull
    protected ManagePackagesDialog createManagePackagesDialog() {
        return new ManagePackagesDialog(myProject, myPackageManagementService, new PackageManagementService.Listener() {
            @Override
            public void operationStarted(String packageName) {
                myNotificationArea.hide();
                myPackagesTable.setPaintBusy(true);
            }

            @Override
            public void operationFinished(String packageName, @Nullable PackageManagementService.ErrorDescription errorDescription) {
                myNotificationArea.showResult(packageName, errorDescription);
                myPackagesTable.clearSelection();
                doUpdatePackages(myPackageManagementService);
            }
        });
    }

    private void upgradeAction() {
        int[] rows = myPackagesTable.getSelectedRows();
        if (myPackageManagementService != null) {
            Set<String> upgradedPackages = new HashSet<>();
            Set<String> packagesShouldBePostponed = getPackagesToPostpone();
            for (int row : rows) {
                Object packageObj = myPackagesTableModel.getValueAt(row, 0);
                if (packageObj instanceof InstalledPackage) {
                    InstalledPackage pkg = (InstalledPackage) packageObj;
                    String packageName = pkg.getName();
                    String currentVersion = pkg.getVersion();
                    String availableVersion = (String) myPackagesTableModel.getValueAt(row, 2);

                    if (packagesShouldBePostponed.contains(packageName)) {
                        myWaitingToUpgrade.put(pkg, availableVersion);
                    }
                    else if (isUpdateAvailable(currentVersion, availableVersion)) {
                        upgradePackage(pkg, availableVersion);
                        upgradedPackages.add(packageName);
                    }
                }
            }

            if (myCurrentlyInstalling.isEmpty() && upgradedPackages.isEmpty() && !myWaitingToUpgrade.isEmpty()) {
                upgradePostponedPackages();
            }
        }
    }

    private void upgradePostponedPackages() {
        Iterator<Entry<InstalledPackage, String>> iterator = myWaitingToUpgrade.entrySet().iterator();
        Entry<InstalledPackage, String> toUpgrade = iterator.next();
        iterator.remove();
        upgradePackage(toUpgrade.getKey(), toUpgrade.getValue());
    }

    protected Set<String> getPackagesToPostpone() {
        return Collections.emptySet();
    }

    private void upgradePackage(@Nonnull InstalledPackage pkg, @Nullable String toVersion) {
        PackageManagementService selPackageManagementService = myPackageManagementService;

        AsyncResult<List<String>> result = myPackageManagementService.fetchPackageVersions(pkg.getName());
        Application application = Application.get();
        result.doWhenDone(releases -> {
            if (!releases.isEmpty() && !isUpdateAvailable(pkg.getVersion(), releases.get(0))) {
                return;
            }

            application.invokeLater(
                () -> {
                    IdeaModalityState modalityState = IdeaModalityState.current();
                    PackageManagementService.Listener listener = new PackageManagementService.Listener() {
                        @Override
                        public void operationStarted(String packageName) {
                            application.invokeLater(
                                () -> {
                                    myPackagesTable.setPaintBusy(true);
                                    myCurrentlyInstalling.add(packageName);
                                },
                                modalityState
                            );
                        }

                        @Override
                        public void operationFinished(
                            String packageName,
                            @Nullable PackageManagementService.ErrorDescription errorDescription
                        ) {
                            application.invokeLater(
                                () -> {
                                    myPackagesTable.clearSelection();
                                    updatePackages(selPackageManagementService);
                                    myCurrentlyInstalling.remove(packageName);
                                    myPackagesTable.setPaintBusy(!myCurrentlyInstalling.isEmpty());
                                    if (errorDescription == null) {
                                        myNotificationArea.showSuccess(IdeLocalize.packageSuccessfullyUpgraded(packageName).get());
                                    }
                                    else {
                                        myNotificationArea.showError(
                                            IdeLocalize.upgradePackagesFailed().get(),
                                            IdeLocalize.upgradePackagesFailedDialogTitle().get(),
                                            errorDescription
                                        );
                                    }

                                    if (myCurrentlyInstalling.isEmpty() && !myWaitingToUpgrade.isEmpty()) {
                                        upgradePostponedPackages();
                                    }
                                },
                                modalityState
                            );
                        }
                    };
                    PackageManagementServiceEx serviceEx = getServiceEx();
                    if (serviceEx != null) {
                        serviceEx.updatePackage(pkg, toVersion, listener);
                    }
                    else {
                        myPackageManagementService.installPackage(
                            new RepoPackage(pkg.getName(), null /* TODO? */),
                            null,
                            true,
                            null,
                            listener,
                            false
                        );
                    }
                    myUpgradeButton.setEnabled(false);
                },
                IdeaModalityState.any()
            );
        });
        result.doWhenRejectedWithThrowable(e -> application.invokeLater(
            () -> Messages.showErrorDialog(
                IdeLocalize.errorOccurredPleaseCheckYourInternetConnection().get(),
                IdeLocalize.upgradePackageFailedTitle().get()
            ),
            IdeaModalityState.any()
        ));
    }

    @Nullable
    private PackageManagementServiceEx getServiceEx() {
        return ObjectUtil.tryCast(myPackageManagementService, PackageManagementServiceEx.class);
    }

    protected void updateUninstallUpgrade() {
        int[] selected = myPackagesTable.getSelectedRows();
        boolean upgradeAvailable = false;
        boolean canUninstall = selected.length != 0;
        boolean canInstall = installEnabled();
        boolean canUpgrade = true;
        if (myPackageManagementService != null && selected.length != 0) {
            for (int i = 0; i != selected.length; ++i) {
                int index = selected[i];
                if (index >= myPackagesTable.getRowCount()) {
                    continue;
                }
                Object value = myPackagesTable.getValueAt(index, 0);
                if (value instanceof InstalledPackage pkg) {
                    if (!canUninstallPackage(pkg)) {
                        canUninstall = false;
                    }
                    canInstall = canInstallPackage(pkg);
                    if (!canUpgradePackage(pkg)) {
                        canUpgrade = false;
                    }
                    String pyPackageName = pkg.getName();
                    String availableVersion = (String) myPackagesTable.getValueAt(index, 2);
                    if (!upgradeAvailable) {
                        upgradeAvailable =
                            isUpdateAvailable(pkg.getVersion(), availableVersion) && !myCurrentlyInstalling.contains(pyPackageName);
                    }
                    if (!canUninstall && !canUpgrade) {
                        break;
                    }
                }
            }
        }
        myUninstallButton.setEnabled(canUninstall);
        myInstallButton.setEnabled(canInstall);
        myUpgradeButton.setEnabled(upgradeAvailable && canUpgrade);
    }

    protected boolean canUninstallPackage(InstalledPackage pyPackage) {
        return true;
    }

    protected boolean canInstallPackage(@Nonnull InstalledPackage pyPackage) {
        return true;
    }

    protected boolean installEnabled() {
        return true;
    }

    protected boolean canUpgradePackage(InstalledPackage pyPackage) {
        return true;
    }

    private void uninstallAction() {
        List<InstalledPackage> packages = getSelectedPackages();
        final PackageManagementService selPackageManagementService = myPackageManagementService;
        if (selPackageManagementService != null) {
            IdeaModalityState modalityState = IdeaModalityState.current();
            Application application = Application.get();
            PackageManagementService.Listener listener = new PackageManagementService.Listener() {
                @Override
                public void operationStarted(String packageName) {
                    application.invokeLater(() -> myPackagesTable.setPaintBusy(true), modalityState);
                }

                @Override
                public void operationFinished(
                    String packageName,
                    @Nullable PackageManagementService.ErrorDescription errorDescription
                ) {
                    application.invokeLater(
                        () -> {
                            myPackagesTable.clearSelection();
                            updatePackages(selPackageManagementService);
                            myPackagesTable.setPaintBusy(!myCurrentlyInstalling.isEmpty());
                            if (errorDescription == null) {
                                myNotificationArea.showSuccess(
                                    packageName != null
                                        ? IdeLocalize.packageSuccessfullyUninstalled(packageName).get()
                                        : IdeLocalize.packagesSuccessfullyUninstalled().get()
                                );
                            }
                            else {
                                myNotificationArea.showError(
                                    IdeLocalize.uninstallPackagesFailed().get(),
                                    IdeLocalize.uninstallPackagesFailedDialogTitle().get(),
                                    errorDescription
                                );
                            }
                        },
                        modalityState
                    );
                }
            };
            myPackageManagementService.uninstallPackages(packages, listener);
        }
    }

    @Nonnull
    private List<InstalledPackage> getSelectedPackages() {
        List<InstalledPackage> results = new ArrayList<>();
        int[] rows = myPackagesTable.getSelectedRows();
        for (int row : rows) {
            Object packageName = myPackagesTableModel.getValueAt(row, 0);
            if (packageName instanceof InstalledPackage) {
                results.add((InstalledPackage) packageName);
            }
        }
        return results;
    }

    public void updatePackages(@Nullable PackageManagementService packageManagementService) {
        myPackageManagementService = packageManagementService;
        myPackagesTable.clearSelection();
        myPackagesTableModel.getDataVector().clear();
        myPackagesTableModel.fireTableDataChanged();
        if (packageManagementService != null) {
            doUpdatePackages(packageManagementService);
        }
    }

    private void onUpdateStarted() {
        myPackagesTable.setPaintBusy(true);
        myPackagesTable.getEmptyText().setText(CommonLocalize.treeNodeLoading());
    }

    private void onUpdateFinished() {
        myPackagesTable.setPaintBusy(!myCurrentlyInstalling.isEmpty());
        myPackagesTable.getEmptyText().setText(StatusText.DEFAULT_EMPTY_TEXT_VALUE);
        updateUninstallUpgrade();
        // Action button presentations won't be updated if no events occur (e.g. mouse isn't moving, keys aren't being pressed).
        // In that case emulating activity will help:
        ActivityTracker.getInstance().inc();
    }

    public void doUpdatePackages(@Nonnull final PackageManagementService packageManagementService) {
        onUpdateStarted();
        ProgressManager.getInstance().run(new Task.Backgroundable(
            myProject,
            IdeLocalize.packagesSettingsLoading(),
            true,
            PerformInBackgroundOption.ALWAYS_BACKGROUND
        ) {
            @Override
            public void run(@Nonnull ProgressIndicator indicator) {
                List<? extends InstalledPackage> packages = List.of();
                try {
                    packages = packageManagementService.getInstalledPackagesList();
                }
                catch (IOException e) {
                    LOG.warn(e.getMessage()); // do nothing, we already have an empty list
                }
                finally {
                    Map<String, RepoPackage> cache = buildNameToPackageMap(packageManagementService.getAllPackagesCached());
                    boolean shouldFetchLatestVersionsForOnlyInstalledPackages =
                        shouldFetchLatestVersionsForOnlyInstalledPackages();
                    if (cache.isEmpty()) {
                        if (!shouldFetchLatestVersionsForOnlyInstalledPackages) {
                            refreshLatestVersions(packageManagementService);
                        }
                    }

                    List<Object[]> rows = ContainerUtil.map(
                        packages,
                        pkg -> new Object[]{pkg, pkg.getVersion(), getVersionString(cache.get(pkg.getName()))}
                    );

                    UIUtil.invokeLaterIfNeeded(() -> {
                        if (packageManagementService == myPackageManagementService) {
                            myPackagesTableModel.getDataVector().clear();
                            for (Object[] row : rows) {
                                myPackagesTableModel.addRow(row);
                            }
                            if (!cache.isEmpty()) {
                                onUpdateFinished();
                            }
                            if (shouldFetchLatestVersionsForOnlyInstalledPackages) {
                                setLatestVersionsForInstalledPackages();
                            }
                        }
                    });
                }
            }
        });
    }

    private InstalledPackage getInstalledPackageAt(int index) {
        return (InstalledPackage) myPackagesTableModel.getValueAt(index, 0);
    }

    private void setLatestVersionsForInstalledPackages() {
        PackageManagementServiceEx serviceEx = getServiceEx();
        if (serviceEx == null) {
            return;
        }
        int packageCount = myPackagesTableModel.getRowCount();
        if (packageCount == 0) {
            onUpdateFinished();
        }
        AtomicInteger inProgressPackageCount = new AtomicInteger(packageCount);
        for (int i = 0; i < packageCount; ++i) {
            int finalIndex = i;
            InstalledPackage pkg = getInstalledPackageAt(finalIndex);
            AsyncResult<String> result = serviceEx.fetchLatestVersion(pkg);
            Runnable decrement = () -> {
                if (inProgressPackageCount.decrementAndGet() == 0) {
                    onUpdateFinished();
                }
            };
            result.doWhenDone(latestVersion -> {
                if (latestVersion == null) {
                    return;
                }

                UIUtil.invokeLaterIfNeeded(() -> {
                    if (finalIndex < myPackagesTableModel.getRowCount()) {
                        InstalledPackage p = getInstalledPackageAt(finalIndex);
                        if (pkg == p) {
                            myPackagesTableModel.setValueAt(latestVersion, finalIndex, 2);
                        }
                    }
                    decrement.run();
                });
            });
            result.doWhenRejectedWithThrowable(e -> {
                UIUtil.invokeLaterIfNeeded(decrement::run);
                LOG.warn("Cannot fetch the latest version of the installed package " + pkg, e);
            });
        }
    }

    private boolean shouldFetchLatestVersionsForOnlyInstalledPackages() {
        PackageManagementServiceEx serviceEx = getServiceEx();
        if (serviceEx != null) {
            return serviceEx.shouldFetchLatestVersionsForOnlyInstalledPackages();
        }
        return false;
    }

    private boolean isUpdateAvailable(@Nullable String currentVersion, @Nullable String availableVersion) {
        if (availableVersion == null) {
            return false;
        }
        if (currentVersion == null) {
            return true;
        }
        PackageManagementService service = myPackageManagementService;
        if (service != null) {
            return service.compareVersions(currentVersion, availableVersion) < 0;
        }
        return PackageVersionComparator.VERSION_COMPARATOR.compare(currentVersion, availableVersion) < 0;
    }

    private void refreshLatestVersions(@Nonnull PackageManagementService packageManagementService) {
        Application application = Application.get();
        application.executeOnPooledThread(() -> {
            if (packageManagementService == myPackageManagementService) {
                try {
                    List<RepoPackage> packages = packageManagementService.reloadAllPackages();
                    Map<String, RepoPackage> packageMap = buildNameToPackageMap(packages);
                    application.invokeLater(
                        () -> {
                            for (int i = 0; i != myPackagesTableModel.getRowCount(); ++i) {
                                InstalledPackage pyPackage = (InstalledPackage) myPackagesTableModel.getValueAt(i, 0);
                                RepoPackage repoPackage = packageMap.get(pyPackage.getName());
                                myPackagesTableModel.setValueAt(repoPackage == null ? null : repoPackage.getLatestVersion(), i, 2);
                            }
                            myPackagesTable.setPaintBusy(!myCurrentlyInstalling.isEmpty());
                        },
                        IdeaModalityState.stateForComponent(myPackagesTable)
                    );
                }
                catch (IOException ignored) {
                    LOG.warn("Cannot refresh the list of available packages with their latest versions", ignored);
                    myPackagesTable.setPaintBusy(false);
                }
            }
        });
    }

    private Map<String, RepoPackage> buildNameToPackageMap(List<? extends RepoPackage> packages) {
        try {
            return doBuildNameToPackageMap(packages);
        }
        catch (Exception e) {
            PackageManagementService service = myPackageManagementService;
            LOG.error("Failure in " + getClass().getName() + ", service: " + (service != null ? service.getClass().getName() : null), e);
            return Collections.emptyMap();
        }
    }

    private static Map<String, RepoPackage> doBuildNameToPackageMap(List<? extends RepoPackage> packages) {
        Map<String, RepoPackage> packageMap = new HashMap<>();
        for (RepoPackage aPackage : packages) {
            packageMap.put(aPackage.getName(), aPackage);
        }
        return packageMap;
    }

    @Nonnull
    private static String getVersionString(@Nullable RepoPackage repoPackage) {
        String version = repoPackage != null ? repoPackage.getLatestVersion() : null;
        return version != null ? version : "";
    }

    private class MyTableCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(
            JTable table,
            Object value,
            boolean isSelected,
            boolean hasFocus,
            int row,
            int column
        ) {
            JLabel cell = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            String version = (String) table.getValueAt(row, 1);
            String availableVersion = (String) table.getValueAt(row, 2);
            boolean update = column == 2 && StringUtil.isNotEmpty(availableVersion) && isUpdateAvailable(version, availableVersion);
            cell.setIcon(update ? TargetAWT.to(PlatformIconGroup.actionsMoveup()) : null);
            Object pyPackage = table.getValueAt(row, 0);
            if (pyPackage instanceof InstalledPackage installedPackage) {
                cell.setToolTipText(installedPackage.getTooltipText());
            }
            return cell;
        }
    }
}
