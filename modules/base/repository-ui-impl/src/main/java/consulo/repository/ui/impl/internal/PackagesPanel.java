/*
 * Copyright 2013-2026 consulo.io
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
package consulo.repository.ui.impl.internal;

import consulo.application.Application;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.repository.ui.InstalledPackage;
import consulo.repository.ui.PackageInstallScope;
import consulo.repository.ui.PackageManagementFix;
import consulo.repository.ui.PackageManagementProblem;
import consulo.repository.ui.PackageManagementService;
import consulo.repository.ui.PackageManagementServiceEx;
import consulo.repository.ui.RepoPackage;
import consulo.repository.ui.SearchablePackageManagementService;
import consulo.repository.ui.localize.RepositoryUILocalize;
import consulo.ui.Button;
import consulo.ui.Component;
import consulo.ui.HtmlView;
import consulo.ui.Hyperlink;
import consulo.ui.Label;
import consulo.ui.Space;
import consulo.ui.Tab;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.ActionManager;
import consulo.ui.ex.action.ActionToolbar;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.HorizontalLayout;
import consulo.ui.layout.TabbedLayout;
import consulo.ui.layout.TabbedLayoutStyle;
import consulo.ui.style.StandardColors;
import consulo.util.lang.StringUtil;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The packages of a {@link PackageManagementService}: the installed ones, the repository, and its repositories - with the operations
 * on them and their results in place.
 */
public class PackagesPanel implements Disposable {
    private static final Logger LOG = Logger.getInstance(PackagesPanel.class);

    private enum Operation {
        INSTALL,
        UPDATE,
        UNINSTALL
    }

    private final Project myProject;
    private final PackageManagementService myService;
    private final UIAccess myUIAccess;

    private final DockLayout myRoot;
    private final TabbedLayout myTabbedLayout;
    private final PackageListTab myInstalledTab;
    private final PackageListTab myBrowseTab;
    private final @Nullable RepositoriesTab myRepositoriesTab;
    private boolean myBrowseLoaded;

    private final DockLayout myProblemLine;
    private final Label myProblemLabel;
    private final HorizontalLayout myProblemFixes;

    private final DockLayout myStatusLine;
    private final Label myStatusLabel;
    private final Hyperlink myDetailsLink;
    private final HtmlView myErrorDetails;
    private PackageManagementService.@Nullable ErrorDescription myError;

    // the state the rows are decorated by - read in the ui thread, the installed packages are set by a background loading
    private volatile Map<String, InstalledPackage> myInstalled = Map.of();
    private final Map<String, String> myLatestVersions = new ConcurrentHashMap<>();
    private final Set<String> myInProgress = new HashSet<>();
    private final AtomicBoolean myUpdateRowsQueued = new AtomicBoolean();

    @RequiredUIAccess
    public PackagesPanel(Project project, PackageManagementService service) {
        myProject = project;
        myService = service;
        myUIAccess = UIAccess.current();

        SearchablePackageManagementService searchable = service instanceof SearchablePackageManagementService s ? s : null;

        myInstalledTab = new PackageListTab(this, query -> loadInstalled(), false);
        Disposer.register(this, myInstalledTab);
        myBrowseTab = new PackageListTab(this, query -> loadRepository(searchable, query), searchable != null);
        Disposer.register(this, myBrowseTab);

        myTabbedLayout = TabbedLayout.create();
        myTabbedLayout.addStyle(TabbedLayoutStyle.NO_PADDING);
        myTabbedLayout.addTab(RepositoryUILocalize.tabInstalled(), myInstalledTab.getComponent());
        Tab browseTab = myTabbedLayout.addTab(RepositoryUILocalize.tabBrowse(), myBrowseTab.getComponent());
        Tab repositoriesTab = null;
        if (service.canManageRepositories()) {
            myRepositoriesTab = new RepositoriesTab(this);
            repositoriesTab = myTabbedLayout.addTab(RepositoryUILocalize.tabRepositories(), myRepositoriesTab.getComponent());
        }
        else {
            myRepositoriesTab = null;
        }

        // the repository is asked only when it is looked at
        Tab finalRepositoriesTab = repositoriesTab;
        myTabbedLayout.addSelectListener(event -> {
            if (event.getTab() == browseTab && !myBrowseLoaded) {
                myBrowseLoaded = true;
                myBrowseTab.reload();
            }
            else if (event.getTab() == finalRepositoriesTab && myRepositoriesTab != null) {
                myRepositoriesTab.reload();
            }
        });

        ActionToolbar toolbar = ActionManager.getInstance().createActionToolbar(
            "PackagesPanelToolbar",
            ActionGroup.newImmutableBuilder().add(new ReloadAction()).build(),
            true
        );
        toolbar.setTargetUIComponent(myTabbedLayout);
        myTabbedLayout.setSuffixComponent(toolbar.getUIComponent());

        myProblemLabel = Label.create(LocalizeValue.empty());
        myProblemLabel.setImage(PlatformIconGroup.generalBalloonwarning());
        myProblemFixes = HorizontalLayout.create(Space.SMALL);
        myProblemLine = DockLayout.create(Space.SMALL);
        myProblemLine.center(myProblemLabel);
        myProblemLine.right(myProblemFixes);
        myProblemLine.paddingBuilder().allSet(Space.MEDIUM).apply();
        myProblemLine.borderBuilder().bottomSet().apply();
        myProblemLine.setVisible(false);

        myStatusLabel = Label.create(LocalizeValue.empty());
        myDetailsLink = Hyperlink.create(RepositoryUILocalize.errorDetailsLink(), event -> toggleErrorDetails());
        myDetailsLink.setVisible(false);
        myErrorDetails = HtmlView.create();
        myErrorDetails.setVisible(false);

        DockLayout statusText = DockLayout.create(Space.SMALL);
        statusText.center(myStatusLabel);
        statusText.right(myDetailsLink);

        myStatusLine = DockLayout.create(Space.SMALL);
        myStatusLine.top(statusText);
        myStatusLine.center(myErrorDetails);
        myStatusLine.paddingBuilder().allSet(Space.MEDIUM).apply();
        myStatusLine.borderBuilder().topSet().apply();
        myStatusLine.setVisible(false);

        myRoot = DockLayout.create(Space.NONE);
        myRoot.top(myProblemLine);
        myRoot.center(myTabbedLayout);
        myRoot.bottom(myStatusLine);

        myInstalledTab.reload();
        checkProblem();
    }

    public Component getComponent() {
        return myRoot;
    }

    public Component getPreferredFocusedComponent() {
        return myInstalledTab.getPreferredFocusedComponent();
    }

    public PackageManagementService getService() {
        return myService;
    }

    public boolean isInProgress(String packageName) {
        return myInProgress.contains(packageName);
    }

    /**
     * The row with the state of the installed packages and the latest versions known now.
     */
    public PackageRow decorate(PackageRow row) {
        InstalledPackage installed = myInstalled.get(row.name());
        String latestVersion = myLatestVersions.get(row.name());
        if (latestVersion == null) {
            latestVersion = row.repoPackage() != null ? row.repoPackage().getLatestVersion() : row.latestVersion();
        }
        boolean updateAvailable = installed != null && isUpdateAvailable(installed.getVersion(), latestVersion);
        return new PackageRow(row.name(), installed, row.repoPackage(), latestVersion, updateAvailable);
    }

    private boolean isUpdateAvailable(@Nullable String currentVersion, @Nullable String availableVersion) {
        if (StringUtil.isEmpty(availableVersion)) {
            return false;
        }
        return currentVersion == null || myService.compareVersions(currentVersion, availableVersion) < 0;
    }

    private List<PackageRow> loadInstalled() {
        List<? extends InstalledPackage> packages;
        try {
            packages = myService.getInstalledPackagesList();
        }
        catch (IOException e) {
            LOG.warn(e.getMessage());
            packages = List.of();
        }

        Map<String, InstalledPackage> installed = new LinkedHashMap<>();
        for (InstalledPackage pkg : packages) {
            installed.put(pkg.getName(), pkg);
        }
        myInstalled = installed;

        Map<String, RepoPackage> cache = new LinkedHashMap<>();
        for (RepoPackage repoPackage : myService.getAllPackagesCached()) {
            cache.put(repoPackage.getName(), repoPackage);
        }

        List<PackageRow> rows = new ArrayList<>(installed.size());
        for (InstalledPackage pkg : installed.values()) {
            rows.add(new PackageRow(pkg.getName(), pkg, cache.get(pkg.getName()), null, false));
        }

        fetchLatestVersions(installed.values(), cache.isEmpty());
        // the repository rows show the installed state too
        queueUpdateRows();
        return rows;
    }

    private void fetchLatestVersions(Iterable<InstalledPackage> packages, boolean noCache) {
        if (myService instanceof PackageManagementServiceEx serviceEx && serviceEx.shouldFetchLatestVersionsForOnlyInstalledPackages()) {
            for (InstalledPackage pkg : packages) {
                serviceEx.fetchLatestVersion(pkg).doWhenDone(latestVersion -> {
                    if (latestVersion != null) {
                        myLatestVersions.put(pkg.getName(), latestVersion);
                        queueUpdateRows();
                    }
                });
            }
        }
        else if (noCache) {
            Application.get().executeOnPooledThread(() -> {
                try {
                    for (RepoPackage repoPackage : myService.reloadAllPackages()) {
                        String latestVersion = repoPackage.getLatestVersion();
                        if (latestVersion != null) {
                            myLatestVersions.put(repoPackage.getName(), latestVersion);
                        }
                    }
                    queueUpdateRows();
                }
                catch (IOException e) {
                    LOG.warn("Cannot refresh the list of available packages with their latest versions", e);
                }
            });
        }
    }

    private List<PackageRow> loadRepository(@Nullable SearchablePackageManagementService searchable, String query) {
        List<RepoPackage> packages;
        try {
            packages = searchable != null ? searchable.getPackages(query, 0, searchable.getPageSize()) : myService.getAllPackages();
        }
        catch (IOException e) {
            LOG.info("Error loading package list", e);
            myUIAccess.give(() -> showStatus(RepositoryUILocalize.errorLoadingPackageList(StringUtil.notNullize(e.getMessage())), loadError(e)));
            return List.of();
        }
        List<PackageRow> rows = new ArrayList<>(packages.size());
        for (RepoPackage repoPackage : packages) {
            rows.add(new PackageRow(repoPackage.getName(), null, repoPackage, repoPackage.getLatestVersion(), false));
        }
        return rows;
    }

    private static PackageManagementService.ErrorDescription loadError(IOException e) {
        return new PackageManagementService.ErrorDescription(StringUtil.notNullize(e.getMessage(), e.getClass().getName()), null, null, null);
    }

    private void queueUpdateRows() {
        // the latest versions come one by one, the lists are rebuilt once for those come by then
        if (myUpdateRowsQueued.compareAndSet(false, true)) {
            myUIAccess.give(() -> {
                myUpdateRowsQueued.set(false);
                updateRows();
            });
        }
    }

    @RequiredUIAccess
    private void updateRows() {
        myInstalledTab.updateRows();
        myBrowseTab.updateRows();
    }

    @RequiredUIAccess
    private void reloadInstalled() {
        myInstalledTab.reload();
    }

    @RequiredUIAccess
    public void install(RepoPackage repoPackage, @Nullable String version, @Nullable PackageInstallScope scope, @Nullable String extraOptions) {
        myService.installPackage(repoPackage, version, scope, extraOptions, createListener(Operation.INSTALL));
    }

    @RequiredUIAccess
    public void update(InstalledPackage pkg, @Nullable String version) {
        PackageManagementService.Listener listener = createListener(Operation.UPDATE);
        if (myService instanceof PackageManagementServiceEx serviceEx) {
            serviceEx.updatePackage(pkg, version, listener);
        }
        else {
            myService.installPackage(new RepoPackage(pkg.getName(), null), version, true, null, listener, false);
        }
    }

    @RequiredUIAccess
    public void uninstall(InstalledPackage pkg) {
        myService.uninstallPackages(List.of(pkg), createListener(Operation.UNINSTALL));
    }

    @RequiredUIAccess
    public void repositoriesChanged() {
        if (myBrowseLoaded) {
            myBrowseTab.reload();
        }
    }

    private PackageManagementService.Listener createListener(Operation operation) {
        return new PackageManagementService.Listener() {
            @Override
            public void operationStarted(@Nullable String packageName) {
                myUIAccess.give(() -> {
                    if (packageName != null) {
                        myInProgress.add(packageName);
                    }
                    hideStatus();
                    updateRows();
                });
            }

            @Override
            public void operationFinished(@Nullable String packageName, PackageManagementService.@Nullable ErrorDescription errorDescription) {
                myUIAccess.give(() -> {
                    if (packageName != null) {
                        myInProgress.remove(packageName);
                    }
                    showStatus(resultText(operation, packageName, errorDescription == null), errorDescription);
                    reloadInstalled();
                });
            }
        };
    }

    private static LocalizeValue resultText(Operation operation, @Nullable String packageName, boolean success) {
        String name = StringUtil.notNullize(packageName);
        return switch (operation) {
            case INSTALL -> success ? RepositoryUILocalize.packageInstalledSuccessfully(name) : RepositoryUILocalize.packageInstallFailed(name);
            case UPDATE -> success ? RepositoryUILocalize.packageUpdatedSuccessfully(name) : RepositoryUILocalize.packageUpdateFailed(name);
            case UNINSTALL -> {
                if (packageName == null) {
                    yield success ? RepositoryUILocalize.packagesUninstalledSuccessfully() : RepositoryUILocalize.packageUninstallFailed(name);
                }
                yield success ? RepositoryUILocalize.packageUninstalledSuccessfully(name) : RepositoryUILocalize.packageUninstallFailed(name);
            }
        };
    }

    private void checkProblem() {
        Application.get().executeOnPooledThread(() -> {
            PackageManagementProblem problem;
            try {
                problem = myService.findProblem();
            }
            catch (Exception e) {
                LOG.warn(e);
                problem = null;
            }
            PackageManagementProblem finalProblem = problem;
            myUIAccess.give(() -> showProblem(finalProblem));
        });
    }

    @RequiredUIAccess
    private void showProblem(@Nullable PackageManagementProblem problem) {
        myProblemFixes.removeAll();
        if (problem == null) {
            myProblemLine.setVisible(false);
            return;
        }
        myProblemLabel.setText(problem.message());
        for (PackageManagementFix fix : problem.fixes()) {
            myProblemFixes.add(Button.create(fix.getName(), event -> runFix(fix)));
        }
        myProblemLine.setVisible(true);
    }

    @RequiredUIAccess
    private void runFix(PackageManagementFix fix) {
        fix.run(new PackageManagementService.Listener() {
            @Override
            public void operationStarted(@Nullable String packageName) {
                myUIAccess.give(() -> hideStatus());
            }

            @Override
            public void operationFinished(@Nullable String packageName, PackageManagementService.@Nullable ErrorDescription errorDescription) {
                myUIAccess.give(() -> {
                    if (errorDescription != null) {
                        showStatus(LocalizeValue.of(errorDescription.getMessage()), errorDescription);
                    }
                    checkProblem();
                    reloadInstalled();
                });
            }
        });
    }

    @RequiredUIAccess
    private void hideStatus() {
        myError = null;
        myStatusLine.setVisible(false);
        myErrorDetails.setVisible(false);
    }

    @RequiredUIAccess
    private void showStatus(LocalizeValue text, PackageManagementService.@Nullable ErrorDescription error) {
        myError = error;
        myStatusLabel.setText(text);
        boolean failed = error != null;
        myStatusLabel.setImage(failed ? PlatformIconGroup.generalBalloonerror() : PlatformIconGroup.generalInformation());
        myStatusLabel.setForegroundColor(failed ? StandardColors.RED : null);
        myDetailsLink.setVisible(failed);
        myErrorDetails.setVisible(false);
        myStatusLine.setVisible(true);
    }

    @RequiredUIAccess
    private void toggleErrorDetails() {
        PackageManagementService.ErrorDescription error = myError;
        if (error == null) {
            return;
        }
        if (myErrorDetails.isVisible()) {
            myErrorDetails.setVisible(false);
            return;
        }
        StringBuilder html = new StringBuilder("<html><body>");
        html.append("<p>").append(StringUtil.escapeXmlEntities(error.getMessage())).append("</p>");
        String command = error.getCommand();
        if (command != null) {
            html.append("<p><code>").append(StringUtil.escapeXmlEntities(command)).append("</code></p>");
        }
        String output = error.getOutput();
        if (output != null) {
            html.append("<pre>").append(StringUtil.escapeXmlEntities(output)).append("</pre>");
        }
        String solution = error.getSolution();
        if (solution != null) {
            html.append("<p>").append(StringUtil.escapeXmlEntities(solution)).append("</p>");
        }
        html.append("</body></html>");
        myErrorDetails.render(new HtmlView.RenderData(html.toString(), "pre { white-space: pre-wrap; }", new URL[0]));
        myErrorDetails.setVisible(true);
    }

    private class ReloadAction extends DumbAwareAction {
        ReloadAction() {
            super(RepositoryUILocalize.actionReloadText(), LocalizeValue.empty(), PlatformIconGroup.actionsRefresh());
        }

        @RequiredUIAccess
        @Override
        public void actionPerformed(AnActionEvent e) {
            Application.get().executeOnPooledThread(() -> {
                try {
                    myService.reloadAllPackages();
                }
                catch (IOException ex) {
                    LOG.info("Error updating list of repository packages", ex);
                    myUIAccess.give(() -> showStatus(RepositoryUILocalize.errorLoadingPackageList(StringUtil.notNullize(ex.getMessage())), loadError(ex)));
                }
                myUIAccess.give(() -> {
                    myLatestVersions.clear();
                    reloadInstalled();
                    if (myBrowseLoaded) {
                        myBrowseTab.reload();
                    }
                });
            });
        }
    }

    @Override
    public void dispose() {
    }
}
