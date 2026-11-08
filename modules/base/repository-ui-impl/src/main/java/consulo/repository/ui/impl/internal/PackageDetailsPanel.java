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

import consulo.localize.LocalizeValue;
import consulo.platform.Platform;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.repository.ui.InstalledPackage;
import consulo.repository.ui.PackageInstallScope;
import consulo.repository.ui.PackageManagementService;
import consulo.repository.ui.localize.RepositoryUILocalize;
import consulo.ui.Button;
import consulo.ui.ComboBox;
import consulo.ui.Component;
import consulo.ui.HtmlView;
import consulo.ui.Label;
import consulo.ui.LabelStyle;
import consulo.ui.Space;
import consulo.ui.TextBox;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.HorizontalLayout;
import consulo.ui.layout.VerticalLayout;
import consulo.ui.model.FlatDataModel;
import consulo.ui.model.MutableFlatDataModel;
import consulo.ui.style.ComponentColors;
import consulo.util.lang.StringUtil;
import org.jspecify.annotations.Nullable;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * The selected package: its versions, the description from the repository, and the operations on it.
 */
public class PackageDetailsPanel {
    private static final String DESCRIPTION_CSS = """
        body { margin: 5px 7px 0 7px; font-size: 12px; }
        pre { white-space: pre-wrap; }
        """;

    // the item of the version box which installs the latest version
    private static final String LATEST = "";

    private final PackagesPanel myPanel;
    private final PackageManagementService myService;
    private final UIAccess myUIAccess;

    private final DockLayout myRoot;
    private final VerticalLayout myHeader;
    private final Label myNameLabel;
    private final Label myVersionsLabel;
    private final ComboBox<String> myVersionBox;
    private final HorizontalLayout myScopeLine;
    private final ComboBox<PackageInstallScope> myScopeBox;
    private final HorizontalLayout myOptionsLine;
    private final TextBox myOptionsBox;
    private final Button myInstallButton;
    private final Button myUpdateButton;
    private final Button myUninstallButton;
    private final HtmlView myDescription;

    private final boolean myHasScopes;

    private @Nullable PackageRow myRow;

    @RequiredUIAccess
    public PackageDetailsPanel(PackagesPanel panel) {
        myPanel = panel;
        myService = panel.getService();
        myUIAccess = UIAccess.current();

        myNameLabel = Label.create(LocalizeValue.empty());
        myNameLabel.addStyle(LabelStyle.FONT_LARGE);
        myNameLabel.addStyle(LabelStyle.FONT_BOLD);

        myVersionsLabel = Label.create(LocalizeValue.empty());
        myVersionsLabel.setForegroundColor(ComponentColors.DISABLED_TEXT);

        myVersionBox = ComboBox.create(FlatDataModel.of(List.of(LATEST)));
        myVersionBox.setTextRenderer(version -> version == null || LATEST.equals(version) ? RepositoryUILocalize.versionLatest() : LocalizeValue.of(version));
        myVersionBox.setValue(LATEST);
        myVersionBox.addValueListener(event -> updateButtons());

        List<PackageInstallScope> scopes = myService.getInstallScopes();
        myScopeBox = ComboBox.create(FlatDataModel.of(scopes));
        myScopeBox.setTextRenderer(scope -> scope == null ? LocalizeValue.empty() : scope.name());
        myScopeBox.setValue(myService.getDefaultInstallScope());
        myScopeLine = HorizontalLayout.create(Space.SMALL);
        myScopeLine.add(Label.create(RepositoryUILocalize.labelScope()));
        myScopeLine.add(myScopeBox);
        myHasScopes = !scopes.isEmpty();
        myScopeLine.setVisible(myHasScopes);

        myOptionsBox = TextBox.create("");
        myOptionsLine = HorizontalLayout.create(Space.SMALL);
        myOptionsLine.add(Label.create(RepositoryUILocalize.labelOptions()));
        myOptionsLine.add(myOptionsBox);
        myOptionsLine.setVisible(myService.canUseExtraOptions());

        myInstallButton = Button.create(RepositoryUILocalize.actionInstallText(), event -> install());
        myInstallButton.setIcon(PlatformIconGroup.actionsInstall());
        myUpdateButton = Button.create(RepositoryUILocalize.actionUpdateText(), event -> update());
        myUpdateButton.setIcon(PlatformIconGroup.actionsMoveup());
        myUninstallButton = Button.create(RepositoryUILocalize.actionUninstallText(), event -> uninstall());
        myUninstallButton.setIcon(PlatformIconGroup.generalDelete());

        HorizontalLayout versionLine = HorizontalLayout.create(Space.SMALL);
        versionLine.add(Label.create(RepositoryUILocalize.labelVersion()));
        versionLine.add(myVersionBox);

        HorizontalLayout buttons = HorizontalLayout.create(Space.SMALL);
        buttons.add(myInstallButton);
        buttons.add(myUpdateButton);
        buttons.add(myUninstallButton);

        myHeader = VerticalLayout.create(Space.SMALL);
        myHeader.add(myNameLabel);
        myHeader.add(myVersionsLabel);
        myHeader.add(versionLine);
        myHeader.add(myScopeLine);
        myHeader.add(myOptionsLine);
        myHeader.add(buttons);
        myHeader.paddingBuilder().allSet(Space.MEDIUM).apply();
        myHeader.setVisible(false);

        myDescription = HtmlView.create();
        myDescription.addHyperlinkListener(event -> {
            String url = event.getDescription();
            if (!url.isEmpty()) {
                Platform.current().openInBrowser(url);
            }
        });
        myDescription.setBackgroundColor(ComponentColors.LAYOUT);

        myRoot = DockLayout.create(Space.NONE);
        myRoot.top(myHeader);
        myRoot.center(myDescription);
        myRoot.setBackgroundColor(ComponentColors.LAYOUT);
    }

    public Component getComponent() {
        return myRoot;
    }

    /**
     * Shows the package. The same package again - after its state changed - only updates the versions and the operations,
     * the repository is asked again only for another package.
     */
    @RequiredUIAccess
    public void update(@Nullable PackageRow row) {
        PackageRow previous = myRow;
        myRow = row;
        if (row == null) {
            myHeader.setVisible(false);
            render("");
            return;
        }

        myHeader.setVisible(true);
        myNameLabel.setText(LocalizeValue.of(row.name()));

        List<LocalizeValue> versions = new ArrayList<>(2);
        String installedVersion = row.installedVersion();
        if (installedVersion != null) {
            versions.add(RepositoryUILocalize.labelInstalledVersion(installedVersion));
        }
        String latestVersion = row.latestVersion();
        if (latestVersion != null) {
            versions.add(RepositoryUILocalize.labelLatestVersion(latestVersion));
        }
        myVersionsLabel.setText(LocalizeValue.join("   ", versions.toArray(LocalizeValue[]::new)));

        if (previous == null || !previous.name().equals(row.name())) {
            loadRepositoryInfo(row.name());
        }
        updateButtons();
    }

    @RequiredUIAccess
    private void loadRepositoryInfo(String name) {
        replaceVersions(List.of());
        render(RepositoryUILocalize.loadingPackages().get());

        myService.fetchPackageVersions(name).doWhenDone(releases -> myUIAccess.give(() -> {
            if (isShown(name)) {
                replaceVersions(releases);
            }
        }));

        myService.fetchPackageDetails(name)
            .doWhenDone(details -> myUIAccess.give(() -> {
                if (isShown(name)) {
                    render(StringUtil.isEmptyOrSpaces(details) ? RepositoryUILocalize.noInformationAvailable().get() : details);
                }
            }))
            .doWhenRejected(() -> myUIAccess.give(() -> {
                if (isShown(name)) {
                    render(RepositoryUILocalize.noInformationAvailable().get());
                }
            }));
    }

    private boolean isShown(String name) {
        PackageRow row = myRow;
        return row != null && row.name().equals(name);
    }

    @RequiredUIAccess
    private void replaceVersions(@Nullable List<String> releases) {
        List<String> items = new ArrayList<>();
        items.add(LATEST);
        if (releases != null) {
            items.addAll(releases);
        }
        ((MutableFlatDataModel<String>) myVersionBox.getDataModel()).replaceAll(items);
        myVersionBox.setValue(LATEST);
    }

    @RequiredUIAccess
    private void updateButtons() {
        PackageRow row = myRow;
        if (row == null) {
            return;
        }
        boolean busy = myPanel.isInProgress(row.name());

        String selectedVersion = getSelectedVersion();
        boolean otherVersion = selectedVersion != null && !selectedVersion.equals(row.installedVersion());

        InstalledPackage installedPackage = row.installed();
        myInstallButton.setVisible(installedPackage == null);
        myInstallButton.setEnabled(!busy && myService.canInstall());
        myUpdateButton.setVisible(installedPackage != null && (row.updateAvailable() || otherVersion));
        myUpdateButton.setEnabled(!busy && installedPackage != null && myService.canUpdatePackage(installedPackage));
        myUninstallButton.setVisible(installedPackage != null);
        myUninstallButton.setEnabled(!busy && installedPackage != null && myService.canUninstallPackage(installedPackage));
        // the scope and the options are the ones of an installation
        myScopeLine.setVisible(installedPackage == null && myHasScopes);
        myOptionsLine.setVisible(installedPackage == null && myService.canUseExtraOptions());
    }

    private @Nullable String getSelectedVersion() {
        String value = myVersionBox.getValue();
        return value == null || LATEST.equals(value) ? null : value;
    }

    @RequiredUIAccess
    private void install() {
        PackageRow row = myRow;
        if (row == null) {
            return;
        }
        String options = myService.canUseExtraOptions() ? StringUtil.nullize(myOptionsBox.getValue(), true) : null;
        myPanel.install(row.toRepoPackage(), getSelectedVersion(), myScopeBox.getValue(), options);
    }

    @RequiredUIAccess
    private void update() {
        PackageRow row = myRow;
        if (row == null || row.installed() == null) {
            return;
        }
        String version = getSelectedVersion();
        myPanel.update(row.installed(), version != null ? version : row.latestVersion());
    }

    @RequiredUIAccess
    private void uninstall() {
        PackageRow row = myRow;
        if (row == null || row.installed() == null) {
            return;
        }
        myPanel.uninstall(row.installed());
    }

    @RequiredUIAccess
    private void render(String body) {
        String html = StringUtil.startsWithIgnoreCase(body.trim(), "<html") ? body : "<html><body>" + body + "</body></html>";
        myDescription.render(new HtmlView.RenderData(html, DESCRIPTION_CSS, new URL[0]));
    }
}
