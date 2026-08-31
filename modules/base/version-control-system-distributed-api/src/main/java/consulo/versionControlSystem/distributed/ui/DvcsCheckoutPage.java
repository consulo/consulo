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
package consulo.versionControlSystem.distributed.ui;

import consulo.annotation.UsedInPlugin;
import consulo.container.boot.ContainerPathManager;
import consulo.disposer.Disposable;
import consulo.fileChooser.FileChooserDescriptorFactory;
import consulo.fileChooser.FileChooserTextBoxBuilder;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.Alerts;
import consulo.ui.Button;
import consulo.ui.Component;
import consulo.ui.Label;
import consulo.ui.TextBox;
import consulo.ui.TextBoxWithHistory;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.DockLayout;
import consulo.ui.style.StandardColors;
import consulo.ui.util.FormBuilder;
import consulo.util.lang.StringUtil;
import consulo.versionControlSystem.distributed.DvcsRememberedInputs;
import consulo.versionControlSystem.distributed.localize.DistributedVcsLocalize;
import consulo.versionControlSystem.checkout.CheckoutCallback;
import consulo.versionControlSystem.checkout.CheckoutPage;
import org.jspecify.annotations.Nullable;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Pattern;

/**
 * @author VISTALL
 * @since 2026-08-25
 */
@UsedInPlugin
public abstract class DvcsCheckoutPage implements CheckoutPage {
    private static final Pattern SSH_URL_PATTERN;

    static {
        String ch = "[\\p{ASCII}&&[\\p{Graph}]&&[^@:/]]";
        String host = ch + "+(?:\\." + ch + "+)*";
        String path = "/?" + ch + "+(?:/" + ch + "+)*/?";
        String all = "(?:" + ch + "+@)?" + host + ":" + path;
        SSH_URL_PATTERN = Pattern.compile(all);
    }

    protected final Project myProject;
    protected final Disposable myUiDisposable;
    private final LocalizeValue myVcsName;
    private final String myVcsDirName;

    private @Nullable TextBoxWithHistory myRepositoryUrlBox;
    private FileChooserTextBoxBuilder.@Nullable Controller myParentDirectoryController;
    private @Nullable TextBox myDirectoryNameBox;
    private @Nullable Label myErrorLabel;
    private @Nullable Button myTestButton;

    private String myDefaultDirectoryName = "";

    private @Nullable String myTestUrl;
    private @Nullable Boolean myTestResult;

    protected DvcsCheckoutPage(Project project, Disposable uiDisposable, LocalizeValue vcsName, String vcsDirName) {
        myProject = project;
        myUiDisposable = uiDisposable;
        myVcsName = vcsName;
        myVcsDirName = vcsDirName;
    }

    protected abstract DvcsRememberedInputs getRememberedInputs();

    @RequiredUIAccess
    protected abstract void test(String url, @RequiredUIAccess Consumer<Boolean> result);

    @RequiredUIAccess
    protected void addCustomRows(FormBuilder builder, Context context) {
    }

    protected void rememberCustomSettings() {
    }

    @RequiredUIAccess
    protected abstract void doClone(CheckoutCallback callback, String sourceRepositoryUrl, String parentDirectory, String directoryName);

    @Override
    @RequiredUIAccess
    public void doCheckout(CheckoutCallback callback) {
        String sourceRepositoryUrl = getSourceRepositoryUrl();
        String parentDirectory = getParentDirectory();
        String directoryName = getDirectoryName();

        rememberSettings();

        doClone(callback, sourceRepositoryUrl, parentDirectory, directoryName);
    }

    @Override
    @RequiredUIAccess
    public Component createComponent(Context context) {
        DvcsRememberedInputs rememberedInputs = getRememberedInputs();

        TextBoxWithHistory repositoryUrlBox = TextBoxWithHistory.create();
        repositoryUrlBox.setHistory(rememberedInputs.getVisitedUrls());
        myRepositoryUrlBox = repositoryUrlBox;

        FileChooserTextBoxBuilder parentDirectoryBuilder = FileChooserTextBoxBuilder.create(myProject);
        parentDirectoryBuilder.uiDisposable(myUiDisposable);
        parentDirectoryBuilder.fileChooserDescriptor(FileChooserDescriptorFactory.createSingleFolderDescriptor()
            .withShowFileSystemRoots(true)
            .withHideIgnored(false));
        parentDirectoryBuilder.dialogTitle(DistributedVcsLocalize.cloneDestinationDirectoryTitle());
        parentDirectoryBuilder.dialogDescription(DistributedVcsLocalize.cloneDestinationDirectoryDescription());

        FileChooserTextBoxBuilder.Controller parentDirectoryController = parentDirectoryBuilder.build();
        parentDirectoryController.setValue(defaultParentDirectory(rememberedInputs));
        myParentDirectoryController = parentDirectoryController;

        TextBox directoryNameBox = TextBox.create();
        myDirectoryNameBox = directoryNameBox;

        Label errorLabel = Label.create();
        errorLabel.setForegroundColor(StandardColors.RED);
        myErrorLabel = errorLabel;

        Button testButton = Button.create(DistributedVcsLocalize.cloneTest(), event -> runTest(context));
        testButton.setEnabled(false);
        myTestButton = testButton;

        repositoryUrlBox.addValueListener(event -> {
            syncDirectoryName();
            testButton.setEnabled(!getSourceRepositoryUrl().isEmpty());
            validate(context);
        });
        parentDirectoryController.getComponent().addValueListener(event -> validate(context));
        directoryNameBox.addValueListener(event -> validate(context));

        DockLayout urlLayout = DockLayout.create();
        urlLayout.center(repositoryUrlBox);
        urlLayout.right(testButton);

        FormBuilder builder = FormBuilder.create();
        builder.addLabeled(DistributedVcsLocalize.cloneRepositoryUrl(myVcsName), urlLayout);
        builder.addLabeled(DistributedVcsLocalize.cloneParentDir(), parentDirectoryController.getComponent());
        builder.addLabeled(DistributedVcsLocalize.cloneDirName(), directoryNameBox);

        addCustomRows(builder, context);

        builder.addBottom(errorLabel);

        validate(context);

        return builder.build();
    }

    private static String defaultParentDirectory(DvcsRememberedInputs rememberedInputs) {
        String parentDir = rememberedInputs.getCloneParentDir();
        if (StringUtil.isEmptyOrSpaces(parentDir)) {
            parentDir = ContainerPathManager.get().getDocumentsDir().getPath();
        }
        return parentDir;
    }

    @RequiredUIAccess
    private void syncDirectoryName() {
        TextBox directoryNameBox = myDirectoryNameBox;
        if (directoryNameBox == null) {
            return;
        }

        String currentName = StringUtil.notNullize(directoryNameBox.getValue());
        if (!myDefaultDirectoryName.equals(currentName) && !currentName.isEmpty()) {
            return;
        }

        myDefaultDirectoryName = defaultDirectoryName(getSourceRepositoryUrl(), myVcsDirName);
        directoryNameBox.setValue(myDefaultDirectoryName);
    }

    @RequiredUIAccess
    private void runTest(Context context) {
        String url = getSourceRepositoryUrl();
        if (url.isEmpty()) {
            return;
        }

        Button testButton = myTestButton;
        if (testButton != null) {
            testButton.setEnabled(false);
        }

        test(url, success -> onTestFinished(context, url, Boolean.TRUE.equals(success)));
    }

    @RequiredUIAccess
    private void onTestFinished(Context context, String url, boolean success) {
        myTestUrl = url;
        myTestResult = success;

        Button testButton = myTestButton;
        if (testButton != null) {
            testButton.setEnabled(!getSourceRepositoryUrl().isEmpty());
        }

        if (success) {
            Alerts.okInfo(DistributedVcsLocalize.cloneTestSuccessMessage(url)).showAsync();
        }

        validate(context);
    }

    @RequiredUIAccess
    protected void validate(Context context) {
        LocalizeValue error = findError();

        Label errorLabel = myErrorLabel;
        if (errorLabel != null) {
            errorLabel.setText(error);
        }

        context.setCheckoutEnabled(error == LocalizeValue.empty() && isFilled());
    }

    @RequiredUIAccess
    private boolean isFilled() {
        return !getSourceRepositoryUrl().isEmpty() && !getParentDirectory().isEmpty() && !getDirectoryName().isEmpty();
    }

    @RequiredUIAccess
    private LocalizeValue findError() {
        String url = getSourceRepositoryUrl();
        if (url.isEmpty()) {
            return LocalizeValue.empty();
        }

        if (myTestResult != null && url.equals(myTestUrl)) {
            if (!myTestResult) {
                return DistributedVcsLocalize.cloneTestFailedError();
            }
        }
        else {
            LocalizeValue urlError = findUrlError(url);
            if (urlError != LocalizeValue.empty()) {
                return urlError;
            }
        }

        String parentDirectory = getParentDirectory();
        String directoryName = getDirectoryName();
        if (parentDirectory.isEmpty() || directoryName.isEmpty()) {
            return LocalizeValue.empty();
        }

        File file = new File(parentDirectory, directoryName);
        if (file.exists()) {
            return DistributedVcsLocalize.cloneDestinationExistsError(file);
        }
        if (!file.getParentFile().exists()) {
            return DistributedVcsLocalize.cloneParentMissingError(file.getParent());
        }
        return LocalizeValue.empty();
    }

    private static LocalizeValue findUrlError(String url) {
        try {
            if (new URI(url).isAbsolute()) {
                return LocalizeValue.empty();
            }
        }
        catch (URISyntaxException ignored) {
        }

        if (SSH_URL_PATTERN.matcher(url).matches()) {
            return LocalizeValue.empty();
        }

        File file = new File(url);
        if (file.exists()) {
            return file.isDirectory() ? LocalizeValue.empty() : DistributedVcsLocalize.cloneUrlIsNotDirectoryError();
        }

        return DistributedVcsLocalize.cloneInvalidUrl();
    }

    @RequiredUIAccess
    @UsedInPlugin
    protected void setUrlHistory(List<String> history) {
        TextBoxWithHistory repositoryUrlBox = myRepositoryUrlBox;
        if (repositoryUrlBox != null) {
            repositoryUrlBox.setHistory(history);
        }
    }

    @RequiredUIAccess
    protected String getSourceRepositoryUrl() {
        return myRepositoryUrlBox == null ? "" : StringUtil.notNullize(myRepositoryUrlBox.getValue()).trim();
    }

    @RequiredUIAccess
    protected String getParentDirectory() {
        return myParentDirectoryController == null ? "" : myParentDirectoryController.getValue();
    }

    @RequiredUIAccess
    protected String getDirectoryName() {
        return myDirectoryNameBox == null ? "" : StringUtil.notNullize(myDirectoryNameBox.getValue());
    }

    @RequiredUIAccess
    protected void rememberSettings() {
        DvcsRememberedInputs rememberedInputs = getRememberedInputs();
        rememberedInputs.addUrl(getSourceRepositoryUrl());
        rememberedInputs.setCloneParentDir(getParentDirectory());

        rememberCustomSettings();
    }

    public static String defaultDirectoryName(String url, String vcsDirName) {
        String nonSystemName;
        if (url.endsWith("/" + vcsDirName) || url.endsWith(File.separator + vcsDirName)) {
            nonSystemName = url.substring(0, url.length() - vcsDirName.length() - 1);
        }
        else if (url.endsWith(vcsDirName)) {
            nonSystemName = url.substring(0, url.length() - vcsDirName.length());
        }
        else {
            nonSystemName = url;
        }

        int i = nonSystemName.lastIndexOf('/');
        if (i == -1 && File.separatorChar != '/') {
            i = nonSystemName.lastIndexOf(File.separatorChar);
        }
        return i >= 0 ? nonSystemName.substring(i + 1) : "";
    }
}
