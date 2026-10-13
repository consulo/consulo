/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package consulo.ide.impl.bundle;

import consulo.application.Application;
import consulo.application.content.impl.internal.bundle.SdkImpl;
import consulo.configurable.ConfigurationException;
import consulo.configurable.UnnamedConfigurable;
import consulo.configurable.internal.ConfigurableUIMigrationUtil;
import consulo.content.bundle.AdditionalDataConfigurable;
import consulo.content.bundle.PlatformAwareSdkType;
import consulo.content.bundle.Sdk;
import consulo.content.bundle.SdkAdditionalData;
import consulo.content.bundle.SdkModel;
import consulo.content.bundle.SdkModificator;
import consulo.content.bundle.SdkType;
import consulo.content.bundle.SdkUtil;
import consulo.content.bundle.UnknownSdkType;
import consulo.content.platform.PlatformTarget;
import consulo.content.platform.PlatformTargetManager;
import consulo.disposer.Disposable;
import consulo.ide.ui.OrderRootTypeUIFactory;
import consulo.ide.ui.PathEditor;
import consulo.ide.ui.SdkPathEditor;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.platform.Platform;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.platform.base.localize.CommonLocalize;
import consulo.project.localize.ProjectLocalize;
import consulo.ui.Component;
import consulo.ui.Label;
import consulo.ui.MessageBoxes;
import consulo.ui.Space;
import consulo.ui.Tab;
import consulo.ui.TextBox;
import consulo.ui.TextBoxWithExtensions;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.color.ColorValue;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.TabbedLayout;
import consulo.ui.layout.VerticalLayout;
import consulo.ui.style.StandardColors;
import consulo.ui.util.FormBuilder;
import consulo.util.io.FileUtil;
import consulo.util.lang.Comparing;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

public class SdkEditor implements UnnamedConfigurable {
    private static final Logger LOG = Logger.getInstance(SdkEditor.class);

    private final Sdk mySdk;
    private final SdkModel mySdkModel;
    private final Map<String, SdkPathEditor> myPathEditors = new LinkedHashMap<>();
    private final Map<SdkType, AdditionalDataConfigurable> myAdditionalDataConfigurables = new HashMap<>();
    private final Map<AdditionalDataConfigurable, Component> myAdditionalDataComponents = new HashMap<>();
    private final SdkModificator myEditedSdkModificator = new EditedSdkModificator();
    private final Runnable myNameChanged;

    private @Nullable DockLayout myMainPanel;
    private @Nullable TextBox myNameComponent;
    private @Nullable TextBoxWithExtensions myHomeComponent;
    private @Nullable DockLayout myAdditionalDataPanel;
    private @Nullable Disposable myUIDisposable;

    private @Nullable String myVersionString;

    private String myInitialName;
    private @Nullable String myInitialPath;

    @RequiredUIAccess
    public SdkEditor(SdkModel sdkModel, SdkImpl sdk, Runnable nameChanged) {
        mySdkModel = sdkModel;
        mySdk = sdk;
        myNameChanged = nameChanged;
        myInitialName = sdk.getName();
        myInitialPath = sdk.getHomePath();

        Application.get().getExtensionPoint(OrderRootTypeUIFactory.class).forEach(factory -> {
            String orderRootTypeId = factory.getOrderRootTypeId();
            if (showTabForType(orderRootTypeId)) {
                SdkPathEditor pathEditor = factory.createPathEditor(mySdk);
                if (pathEditor != null) {
                    pathEditor.setAddBaseDir(mySdk.getHomeDirectory());
                    myPathEditors.put(orderRootTypeId, pathEditor);
                }
            }
        });

        AdditionalDataConfigurable additionalDataConfigurable = getAdditionalDataConfigurable();
        if (additionalDataConfigurable != null) {
            additionalDataConfigurable.setSdk(sdk);
        }
    }

    @RequiredUIAccess
    @Override
    public Component createUIComponent(Disposable parentUIDisposable) {
        DockLayout mainPanel = myMainPanel;
        if (mainPanel == null) {
            mainPanel = createMainPanel(parentUIDisposable);
            myMainPanel = mainPanel;
        }
        return mainPanel;
    }

    @RequiredUIAccess
    private DockLayout createMainPanel(Disposable parentUIDisposable) {
        myUIDisposable = parentUIDisposable;

        TextBox nameComponent = TextBox.create(mySdk.getName());
        nameComponent.setEditable(!mySdk.isPredefined());
        nameComponent.addValueListener(event -> {
            String name = event.getValue();
            if (name != null && !name.equals(mySdk.getName())) {
                ((SdkImpl) mySdk).setName(name);
                myNameChanged.run();
            }
        });
        myNameComponent = nameComponent;

        TextBoxWithExtensions homeComponent = TextBoxWithExtensions.create();
        homeComponent.withEditable(false);

        boolean changePathSupported =
            !mySdk.isPredefined() && ((SdkType) mySdk.getSdkType()).supportsUserAdd() && !(mySdk.getSdkType() instanceof UnknownSdkType);
        if (changePathSupported) {
            homeComponent.setExtensions(new TextBoxWithExtensions.Extension(
                false,
                PlatformIconGroup.nodesFolderopened(),
                null,
                e -> doSelectHomePath()
            ));
        }
        myHomeComponent = homeComponent;

        FormBuilder formBuilder = FormBuilder.create();
        if (mySdk.getSdkType() instanceof PlatformAwareSdkType) {
            PlatformTarget target = findPlatformTarget();
            Label platformLabel = Label.create(
                target == null ? ProjectLocalize.platformTargetMissing(mySdk.getPlatformTargetId()) : target.getDisplayName()
            );
            platformLabel.setImage(target == null ? null : target.getIcon());
            formBuilder.addLabeled(ProjectLocalize.platformTargetLabel(), platformLabel);
        }
        formBuilder.addLabeled(CommonLocalize.nameLabelText(), nameComponent);
        formBuilder.addLabeled(ProjectLocalize.sdkConfigureTypeHomePath(), homeComponent);

        DockLayout additionalDataPanel = DockLayout.create(Space.NONE);
        additionalDataPanel.setVisible(false);
        myAdditionalDataPanel = additionalDataPanel;

        VerticalLayout topPanel = VerticalLayout.create();
        topPanel.add(formBuilder.build());
        topPanel.add(additionalDataPanel);

        DockLayout mainPanel = DockLayout.create();
        mainPanel.top(topPanel);

        if (!myPathEditors.isEmpty()) {
            TabbedLayout tabbedLayout = TabbedLayout.create();
            for (SdkPathEditor pathEditor : myPathEditors.values()) {
                LocalizeValue title = pathEditor.getDisplayName();

                Tab tab = tabbedLayout.createTab();
                tab.setRenderer((it, presentation) -> presentation.append(title));

                tabbedLayout.addTab(tab, pathEditor.createUIComponent());
            }
            mainPanel.center(tabbedLayout);
        }

        return mainPanel;
    }

    @RequiredUIAccess
    public void updateName() {
        TextBox nameComponent = myNameComponent;
        if (nameComponent != null) {
            nameComponent.setValue(mySdk.getName(), false);
        }
    }

    protected boolean showTabForType(String orderRootTypeId) {
        return ((SdkType) mySdk.getSdkType()).isRootTypeApplicable(orderRootTypeId);
    }

    public @Nullable SdkPathEditor getPathEditor(String orderRootTypeId) {
        return myPathEditors.get(orderRootTypeId);
    }

    @RequiredUIAccess
    @Override
    public boolean isModified() {
        if (myMainPanel == null) {
            return false;
        }
        boolean isModified = !Comparing.equal(mySdk.getName(), myInitialName);
        isModified = isModified || !Comparing.equal(
            FileUtil.toSystemIndependentName(getHomeValue()),
            FileUtil.toSystemIndependentName(StringUtil.notNullize(myInitialPath))
        );
        for (PathEditor pathEditor : myPathEditors.values()) {
            isModified = isModified || pathEditor.isModified();
        }
        AdditionalDataConfigurable configurable = getAdditionalDataConfigurable();
        if (configurable != null) {
            isModified = isModified || configurable.isModified();
        }
        return isModified;
    }

    @RequiredUIAccess
    @Override
    public void apply() throws ConfigurationException {
        if (!Comparing.equal(myInitialName, mySdk.getName())) {
            if (mySdk.getName().isEmpty()) {
                throw new ConfigurationException(ProjectLocalize.sdkListNameRequiredError());
            }
        }
        myInitialName = mySdk.getName();
        myInitialPath = mySdk.getHomePath();
        SdkModificator sdkModificator = mySdk.getSdkModificator();
        SdkType sdkType = (SdkType) mySdk.getSdkType();
        if (sdkType.supportsUserAdd()) {
            sdkModificator.setHomePath(getHomeValue().replace(File.separatorChar, '/'));
        }
        for (SdkPathEditor pathEditor : myPathEditors.values()) {
            pathEditor.apply(sdkModificator);
        }
        Application.get().runWriteAction(sdkModificator::commitChanges);
        AdditionalDataConfigurable configurable = getAdditionalDataConfigurable();
        if (configurable != null) {
            configurable.apply();
        }
    }

    @RequiredUIAccess
    @Override
    public void reset() {
        resetPathEditors();
        PlatformTarget target = findPlatformTarget();
        if (target != null && target.getPlatform() == null) {
            target.connect().whenCompleteAsync((platform, error) -> {
                if (error == null && myMainPanel != null) {
                    resetPathEditors();
                }
            }, UIAccess.current());
        }
        String homePath = mySdk.getHomePath();
        setHomePathValue(homePath == null ? null : homePath.replace('/', File.separatorChar));
        myVersionString = null;
        updateAdditionalDataComponent();
        AdditionalDataConfigurable configurable = getAdditionalDataConfigurable();
        if (configurable != null) {
            configurable.reset();
        }
    }

    @RequiredUIAccess
    private void resetPathEditors() {
        SdkModificator sdkModificator = mySdk.getSdkModificator();
        for (SdkPathEditor pathEditor : myPathEditors.values()) {
            pathEditor.reset(sdkModificator);
        }
        sdkModificator.commitChanges();
    }

    @RequiredUIAccess
    @Override
    public void disposeUIResources() {
        for (AdditionalDataConfigurable configurable : myAdditionalDataConfigurables.values()) {
            configurable.disposeUIResources();
        }
        myMainPanel = null;
        myNameComponent = null;
        myHomeComponent = null;
        myAdditionalDataPanel = null;
        myUIDisposable = null;
        myAdditionalDataConfigurables.clear();
        myAdditionalDataComponents.clear();
    }

    private @Nullable PlatformTarget findPlatformTarget() {
        return Application.get().getInstance(PlatformTargetManager.class).findTarget(mySdk.getPlatformTargetId());
    }

    /**
     * The platform of the SDK while it is reachable - it is chosen once, when the SDK is added.
     */
    private @Nullable Platform getEditedPlatform() {
        PlatformTarget target = findPlatformTarget();
        return target == null ? null : target.getPlatform();
    }

    private String getHomeValue() {
        TextBoxWithExtensions homeComponent = myHomeComponent;
        if (homeComponent == null) {
            return StringUtil.notNullize(myInitialPath);
        }
        return homeComponent.getValueOrError().trim();
    }

    @RequiredUIAccess
    private void clearAllPaths() {
        for (PathEditor editor : myPathEditors.values()) {
            editor.clearList();
        }
    }

    @RequiredUIAccess
    private void setHomePathValue(@Nullable String absolutePath) {
        TextBoxWithExtensions homeComponent = myHomeComponent;
        if (homeComponent == null) {
            return;
        }

        homeComponent.setValue(absolutePath);
        ColorValue fg;
        Platform platform = getEditedPlatform();
        if (absolutePath != null && !absolutePath.isEmpty() && platform != null) {
            Path homeDir = platform.fs().getPath(absolutePath);
            boolean homeMustBeDirectory;

            SdkType sdkType = (SdkType) mySdk.getSdkType();
            if (sdkType instanceof PlatformAwareSdkType platformAwareSdkType) {
                homeMustBeDirectory = platformAwareSdkType.getHomeChooserDescriptor(platform).isChooseFolders();
            }
            else {
                homeMustBeDirectory = sdkType.getHomeChooserDescriptor().isChooseFolders();
            }

            fg = Files.exists(homeDir) && Files.isDirectory(homeDir) == homeMustBeDirectory ? null : StandardColors.RED;
        }
        else {
            fg = null;
        }
        homeComponent.setForegroundColor(fg);
    }

    @RequiredUIAccess
    private void doSelectHomePath() {
        SdkType sdkType = (SdkType) mySdk.getSdkType();
        if (sdkType instanceof PlatformAwareSdkType platformAwareSdkType) {
            PlatformTarget target = findPlatformTarget();
            CompletableFuture<Platform> platform = target == null
                ? CompletableFuture.failedFuture(new IllegalStateException(
                    ProjectLocalize.errorPlatformTargetMissing(mySdk.getPlatformTargetId()).get()
                ))
                : target.connect();
            platform.whenCompleteAsync((connected, error) -> {
                if (error != null) {
                    Throwable cause = error instanceof CompletionException && error.getCause() != null ? error.getCause() : error;
                    MessageBoxes.okError(cause).showAsync(myMainPanel);
                    return;
                }
                setHomePathValue(getHomeValue());
                SdkUtil.selectSdkHome(connected, platformAwareSdkType, path -> doSetHomePath(path.toString(), sdkType));
            }, UIAccess.current());
        }
        else {
            SdkUtil.selectSdkHome(sdkType, path -> doSetHomePath(path, sdkType));
        }
    }

    @RequiredUIAccess
    private void doSetHomePath(@Nullable String homePath, SdkType sdkType) {
        if (homePath == null) {
            return;
        }
        setHomePathValue(homePath.replace('/', File.separatorChar));

        String newSdkName = suggestSdkName(homePath);
        ((SdkImpl) mySdk).setName(newSdkName);
        updateName();

        try {
            Sdk dummySdk = (Sdk) mySdk.clone();
            SdkModificator sdkModificator = dummySdk.getSdkModificator();
            sdkModificator.setHomePath(homePath);
            sdkModificator.removeAllRoots();
            sdkModificator.commitChanges();

            sdkType.setupSdkPaths(dummySdk);

            clearAllPaths();
            myVersionString = dummySdk.getVersionString();
            if (myVersionString == null) {
                MessageBoxes.okError(ProjectLocalize.sdkJavaCorruptError(homePath))
                    .title(ProjectLocalize.sdkJavaCorruptTitle())
                    .showAsync(myMainPanel);
            }
            sdkModificator = dummySdk.getSdkModificator();
            for (Map.Entry<String, SdkPathEditor> entry : myPathEditors.entrySet()) {
                entry.getValue().addPaths(sdkModificator.getRoots(entry.getKey()));
            }
            mySdkModel.getMulticaster().sdkHomeSelected(dummySdk, homePath);
        }
        catch (CloneNotSupportedException e) {
            LOG.error(e);
        }
    }

    private String suggestSdkName(String homePath) {
        String currentName = mySdk.getName();
        String suggestedName = ((SdkType) mySdk.getSdkType()).suggestSdkName(currentName, homePath);
        if (Comparing.equal(currentName, suggestedName)) {
            return currentName;
        }
        String newSdkName = suggestedName;
        Set<String> allNames = new HashSet<>();
        for (Sdk sdk : mySdkModel.getSdks()) {
            allNames.add(sdk.getName());
        }
        int i = 0;
        while (allNames.contains(newSdkName)) {
            newSdkName = suggestedName + " (" + ++i + ")";
        }
        return newSdkName;
    }

    @RequiredUIAccess
    private void updateAdditionalDataComponent() {
        DockLayout additionalDataPanel = myAdditionalDataPanel;
        Disposable uiDisposable = myUIDisposable;
        if (additionalDataPanel == null || uiDisposable == null) {
            return;
        }

        AdditionalDataConfigurable configurable = getAdditionalDataConfigurable();
        Component component = null;
        if (configurable != null) {
            component = myAdditionalDataComponents.get(configurable);
            if (component == null) {
                component = ConfigurableUIMigrationUtil.createUIComponent(configurable, uiDisposable);
                if (component != null) {
                    myAdditionalDataComponents.put(configurable, component);
                }
            }
        }

        if (component != null) {
            additionalDataPanel.center(component);
        }
        additionalDataPanel.setVisible(component != null);
    }

    private @Nullable AdditionalDataConfigurable getAdditionalDataConfigurable() {
        SdkType sdkType = (SdkType) mySdk.getSdkType();
        AdditionalDataConfigurable configurable = myAdditionalDataConfigurables.get(sdkType);
        if (configurable == null) {
            configurable = sdkType.createAdditionalDataConfigurable(mySdkModel, myEditedSdkModificator);
            if (configurable != null) {
                myAdditionalDataConfigurables.put(sdkType, configurable);
            }
        }
        return configurable;
    }

    private class EditedSdkModificator implements SdkModificator {
        @Override
        public String getName() {
            return mySdk.getName();
        }

        @Override
        public void setName(String name) {
            ((SdkImpl) mySdk).setName(name);
        }

        @Override
        public String getHomePath() {
            return getHomeValue();
        }

        @Override
        @RequiredUIAccess
        public void setHomePath(String path) {
            doSetHomePath(path, (SdkType) mySdk.getSdkType());
        }

        @Override
        public Path getHomeNioPath() {
            Platform platform = getEditedPlatform();
            return platform == null ? Path.of(getHomeValue()) : platform.fs().getPath(getHomeValue());
        }

        @Override
        public @Nullable String getPlatformTargetId() {
            return mySdk.getPlatformTargetId();
        }

        @Override
        public void setPlatformTargetId(@Nullable String targetId) {
            throw new UnsupportedOperationException();
        }

        @Override
        @RequiredUIAccess
        public void setHomeNioPath(Path path) {
            setHomePath(path.toAbsolutePath().toString());
        }

        @Override
        public @Nullable String getVersionString() {
            return myVersionString != null ? myVersionString : mySdk.getVersionString();
        }

        @Override
        public void setVersionString(String versionString) {
            throw new UnsupportedOperationException();
        }

        @Override
        public SdkAdditionalData getSdkAdditionalData() {
            return mySdk.getSdkAdditionalData();
        }

        @Override
        public void setSdkAdditionalData(SdkAdditionalData data) {
            throw new UnsupportedOperationException();
        }

        @Override
        public VirtualFile[] getRoots(String rootType) {
            PathEditor editor = myPathEditors.get(rootType);
            if (editor == null) {
                throw new IllegalStateException("no editor for root type " + rootType);
            }
            return editor.getRoots();
        }

        @Override
        @RequiredUIAccess
        public void addRoot(VirtualFile root, String rootType) {
            SdkPathEditor editor = myPathEditors.get(rootType);
            if (editor != null) {
                editor.addPaths(root);
            }
        }

        @Override
        @RequiredUIAccess
        public void removeRoot(VirtualFile root, String rootType) {
            SdkPathEditor editor = myPathEditors.get(rootType);
            if (editor != null) {
                editor.removePaths(root);
            }
        }

        @Override
        @RequiredUIAccess
        public void removeRoots(String rootType) {
            SdkPathEditor editor = myPathEditors.get(rootType);
            if (editor != null) {
                editor.clearList();
            }
        }

        @Override
        @RequiredUIAccess
        public void removeAllRoots() {
            clearAllPaths();
        }

        @Override
        public void commitChanges() {
        }

        @Override
        public boolean isWritable() {
            return true;
        }
    }
}
