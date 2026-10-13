/*
 * Copyright 2013-2016 consulo.io
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
package consulo.content.bundle;

import consulo.annotation.DeprecationInfo;
import consulo.application.Application;
import consulo.component.util.pointer.NamedPointer;
import consulo.content.platform.PlatformTarget;
import consulo.content.platform.PlatformTargetManager;
import consulo.fileChooser.FileChooser;
import consulo.fileChooser.FileChooserDescriptor;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.platform.Platform;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.MessageBoxes;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.image.Image;
import consulo.ui.image.ImageEffects;
import consulo.util.io.FileUtil;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletionException;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 2014-08-21
 */
public class SdkUtil {
    private static final Logger LOG = Logger.getInstance(SdkUtil.class);

    public static String createUniqueSdkName(Platform platform, PlatformAwareSdkType type, Path home, Sdk[] sdks) {
        return createUniqueSdkName(type.suggestSdkName(platform, null, home), sdks);
    }

    public static String createUniqueSdkName(SdkType type, String home, Sdk[] sdks) {
        return createUniqueSdkName(type.suggestSdkName(null, home), sdks);
    }

    public static String createUniqueSdkName(String suggestedName, Sdk[] sdks) {
        Set<String> names = new HashSet<>();
        for (Sdk sdk : sdks) {
            names.add(sdk.getName());
        }
        String newSdkName = suggestedName;
        int i = 0;
        while (names.contains(newSdkName)) {
            newSdkName = suggestedName + " (" + (++i) + ")";
        }
        return newSdkName;
    }

    @DeprecationInfo(value = "Use SdkPointerManager.getInstance()")
    public static NamedPointer<Sdk> createPointer(Sdk sdk) {
        return SdkPointerManager.getInstance().create(sdk);
    }

    @DeprecationInfo(value = "Use SdkPointerManager.getInstance()")
    public static NamedPointer<Sdk> createPointer(String name) {
        return SdkPointerManager.getInstance().create(name);
    }

    public static Image getIcon(@Nullable Sdk sdk) {
        if (sdk == null) {
            return PlatformIconGroup.actionsHelp();
        }
        SdkType sdkType = (SdkType) sdk.getSdkType();
        Image icon = sdkType.getIcon();
        if (sdk.isPredefined()) {
            return ImageEffects.layered(icon, PlatformIconGroup.nodesLocked());
        }
        else {
            return icon;
        }
    }

    /**
     * Tries to create an SDK identified by path; if successful, add the SDK to the global SDK table.
     *
     * @param path       identifies the SDK
     * @param sdkType
     * @param uiAccess
     * @return newly created SDK, or null.
     */
    @RequiredUIAccess
    public static @Nullable Sdk createAndAddSDK(String path, SdkType sdkType, UIAccess uiAccess) {
        Application app = Application.get();
        VirtualFile sdkHome =
            app.runWriteAction((Supplier<VirtualFile>) () -> LocalFileSystem.getInstance().refreshAndFindFileByPath(path));
        if (sdkHome != null) {
            SdkTable sdkTable = SdkTable.getInstance();
            Sdk newSdk = setupSdk(sdkTable.getAllSdks(), sdkHome, sdkType, true, null, null, uiAccess);
            if (newSdk != null) {
                app.runWriteAction(() -> sdkTable.addSdk(newSdk));
            }
            return newSdk;
        }
        return null;
    }

    public static @Nullable Sdk setupSdk(
        Sdk[] allSdks,
        VirtualFile homeDir,
        SdkType sdkType,
        boolean silent,
        @Nullable SdkAdditionalData additionalData,
        @Nullable String customSdkSuggestedName,
        UIAccess uiAccess
    ) {
        if (sdkType instanceof PlatformAwareSdkType bundleType) {
            return setupBundle(Platform.current(), allSdks, homeDir, bundleType, silent, additionalData, customSdkSuggestedName, uiAccess);
        }

        return setupLegacySdk(allSdks, homeDir, sdkType, silent, additionalData, customSdkSuggestedName, uiAccess);
    }

    public static @Nullable Sdk setupBundle(
        Platform platform,
        Sdk[] allSdks,
        VirtualFile homeDir,
        PlatformAwareSdkType platformAwareSdkType,
        boolean silent,
        @Nullable SdkAdditionalData additionalData,
        @Nullable String customSdkSuggestedName,
        UIAccess uiAccess
    ) {
        Sdk sdk;
        try {
            Path homeNioPath = homeDir.toNioPath();

            String sdkName;
            if (customSdkSuggestedName == null) {
                sdkName = createUniqueSdkName(platform, platformAwareSdkType, homeNioPath, allSdks);
            }
            else {
                sdkName = createUniqueSdkName(customSdkSuggestedName, allSdks);
            }

            sdk = SdkTable.getInstance().createSdk(homeNioPath, sdkName, platformAwareSdkType);

            SdkModificator modificator = sdk.getSdkModificator();

            if (additionalData != null) {
                modificator.setSdkAdditionalData(additionalData);
            }

            modificator.commitChanges();

            platformAwareSdkType.setupSdkPaths(sdk);
        }
        catch (Exception e) {
            LOG.warn(e);
            if (!silent) {
                uiAccess.give(
                    () -> MessageBoxes.okError(
                            LocalizeValue.localizeTODO(
                                "Error configuring SDK: " + e.getMessage() + ".\n" +
                                    "Please make sure that " + FileUtil.toSystemDependentName(homeDir.getPath()) +
                                    " is a valid home path for this SDK type."
                            )
                        )
                        .title(LocalizeValue.localizeTODO("Error Configuring SDK"))
                        .showAsync()
                );
            }
            return null;
        }
        return sdk;
    }

    public static @Nullable Sdk setupLegacySdk(
        Sdk[] allSdks,
        VirtualFile homeDir,
        SdkType sdkType,
        boolean silent,
        @Nullable SdkAdditionalData additionalData,
        @Nullable String customSdkSuggestedName,
        UIAccess uiAccess
    ) {
        Sdk sdk;
        try {
            String sdkPath = sdkType.sdkPath(homeDir);

            String sdkName = customSdkSuggestedName == null
                ? createUniqueSdkName(sdkType, sdkPath, allSdks)
                : createUniqueSdkName(customSdkSuggestedName, allSdks);

            sdk = SdkTable.getInstance().createSdk(sdkName, sdkType);

            SdkModificator modificator = sdk.getSdkModificator();

            if (additionalData != null) {
                // additional initialization.
                // E.g. some ruby sdks must be initialized before
                // setupSdkPaths() method invocation
                modificator.setSdkAdditionalData(additionalData);
            }

            modificator.setHomePath(sdkPath);
            modificator.commitChanges();

            sdkType.setupSdkPaths(sdk);
        }
        catch (Exception e) {
            LOG.warn(e);
            if (!silent) {
                uiAccess.give(
                    () -> MessageBoxes.okError(
                            LocalizeValue.localizeTODO(
                                "Error configuring SDK: " + e.getMessage() + ".\n" +
                                    "Please make sure that " + FileUtil.toSystemDependentName(homeDir.getPath()) +
                                    " is a valid home path for this SDK type."
                            )
                        )
                        .title(LocalizeValue.localizeTODO("Error Configuring SDK"))
                        .showAsync()
                );
            }
            return null;
        }
        return sdk;
    }

    @Deprecated
    @RequiredUIAccess
    public static void selectSdkHome(SdkType sdkType, @RequiredUIAccess Consumer<String> consumer) {
        if (sdkType instanceof PlatformAwareSdkType platformAwareSdkType) {
            selectSdkHome(Platform.current(), platformAwareSdkType, path -> consumer.accept(path.toString()));
        }
        else {
            selectLegacySdkHome(sdkType, consumer);
        }
    }

    /**
     * Chooses the home of a new SDK on any machine - the local one or a remote platform, each as a tab of the chooser.
     * The consumer gets the target the home lives on, null for the local machine.
     */
    @RequiredUIAccess
    public static void selectSdkHomeOnAnyPlatform(
        PlatformAwareSdkType platformAwareSdkType,
        @RequiredUIAccess BiConsumer<@Nullable PlatformTarget, Path> consumer
    ) {
        PlatformTargetManager targetManager = Application.get().getInstance(PlatformTargetManager.class);
        Platform local = Platform.current();
        FileChooserDescriptor descriptor = platformAwareSdkType.getHomeChooserDescriptor(local).withAnyPlatform();

        List<Path> suggested = new ArrayList<>();
        platformAwareSdkType.collectHomePaths(local, suggested::add);
        Path toSelect = suggested.isEmpty() ? null : suggested.getFirst();

        FileChooser.choosePaths(descriptor, null, toSelect).whenComplete((paths, error) -> {
            if (error != null || paths.isEmpty()) {
                return;
            }

            Path path = paths.getFirst();
            PlatformTarget target = targetManager.findTargetOf(path);
            Platform platform = target == null ? null : target.getPlatform();
            if (platform == null) {
                return;
            }
            PlatformTarget owner = target == targetManager.getLocalTarget() ? null : target;

            if (platformAwareSdkType.isValidSdkHome(platform, path)) {
                consumer.accept(owner, path);
                return;
            }

            Path adjustedPath = platformAwareSdkType.adjustSelectedSdkHome(platform, path);
            if (platformAwareSdkType.isValidSdkHome(platform, adjustedPath)) {
                consumer.accept(owner, adjustedPath);
            }
        }).exceptionally(error -> {
            Throwable cause = error instanceof CompletionException && error.getCause() != null ? error.getCause() : error;
            if (!(cause instanceof CancellationException)) {
                LOG.error(cause);
            }
            return null;
        });
    }

    @RequiredUIAccess
    public static void selectSdkHome(
        Platform platform,
        PlatformAwareSdkType platformAwareSdkType,
        @RequiredUIAccess Consumer<Path> consumer
    ) {
        FileChooserDescriptor descriptor = platformAwareSdkType.getHomeChooserDescriptor(platform);

        List<Path> suggested = new ArrayList<>();
        platformAwareSdkType.collectHomePaths(platform, suggested::add);
        Path toSelect = suggested.isEmpty() ? null : suggested.getFirst();

        FileChooser.choosePaths(descriptor, null, toSelect).whenComplete((paths, error) -> {
            if (error != null || paths.isEmpty()) {
                return;
            }

            Path path = paths.getFirst();
            if (platformAwareSdkType.isValidSdkHome(platform, path)) {
                consumer.accept(path);
                return;
            }

            Path adjustedPath = platformAwareSdkType.adjustSelectedSdkHome(platform, path);
            if (platformAwareSdkType.isValidSdkHome(platform, adjustedPath)) {
                consumer.accept(adjustedPath);
            }
        });
    }

    @RequiredUIAccess
    private static void selectLegacySdkHome(SdkType sdkType, @RequiredUIAccess Consumer<String> consumer) {
        FileChooserDescriptor descriptor = sdkType.getHomeChooserDescriptor();

        FileChooser.chooseFiles(descriptor, null, getSuggestedSdkPath(sdkType)).whenComplete((virtualFiles, error) -> {
            if (error != null) {
                return;
            }

            String path = virtualFiles[0].getPath();
            if (sdkType.isValidSdkHome(path)) {
                consumer.accept(path);
                return;
            }

            String adjustedPath = sdkType.adjustSelectedSdkHome(path);
            if (sdkType.isValidSdkHome(adjustedPath)) {
                consumer.accept(adjustedPath);
            }
        });
    }

    public static @Nullable VirtualFile getSuggestedSdkPath(SdkType sdkType) {
        List<Path> paths = new ArrayList<>();
        if (sdkType instanceof PlatformAwareSdkType platformAwareSdkType) {
            platformAwareSdkType.collectHomePaths(Platform.current(), paths::add);
        }
        else {
            for (String path : sdkType.suggestHomePaths()) {
                paths.add(Path.of(path));
            }
        }

        if (paths.isEmpty()) {
            return null;
        }

        for (Path path : paths) {
            VirtualFile maybeSdkHomePath = LocalFileSystem.getInstance().findFileByNioFile(path);
            if (maybeSdkHomePath != null) {
                return maybeSdkHomePath;
            }
        }
        return null;
    }
}
