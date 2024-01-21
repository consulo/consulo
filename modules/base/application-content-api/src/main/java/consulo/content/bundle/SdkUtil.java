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
import consulo.application.ApplicationManager;
import consulo.component.util.pointer.NamedPointer;
import consulo.fileChooser.FileChooser;
import consulo.fileChooser.FileChooserDescriptor;
import consulo.platform.Platform;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.Alerts;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.image.Image;
import consulo.ui.image.ImageEffects;
import consulo.util.io.FileUtil;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/**
 * @author VISTALL
 * @since 21.08.14
 */
public class SdkUtil {
  @Nonnull
  public static String createUniqueSdkName(@Nonnull Platform platform, @Nonnull BundleType type, Path home, final Sdk[] sdks) {
    return createUniqueSdkName(type.suggestSdkName(platform, null, home), sdks);
  }

  @Nonnull
  public static String createUniqueSdkName(@Nonnull SdkType type, String home, final Sdk[] sdks) {
    return createUniqueSdkName(type.suggestSdkName(null, home), sdks);
  }

  @Nonnull
  public static String createUniqueSdkName(final String suggestedName, final Sdk[] sdks) {
    final Set<String> names = new HashSet<>();
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

  @Nonnull
  @DeprecationInfo(value = "Use SdkPointerManager.getInstance()")
  public static NamedPointer<Sdk> createPointer(@Nonnull Sdk sdk) {
    return SdkPointerManager.getInstance().create(sdk);
  }

  @Nonnull
  @DeprecationInfo(value = "Use SdkPointerManager.getInstance()")
  public static NamedPointer<Sdk> createPointer(@Nonnull String name) {
    return SdkPointerManager.getInstance().create(name);
  }

  @Nonnull
  public static Image getIcon(@Nullable Sdk sdk) {
    if (sdk == null) {
      return PlatformIconGroup.actionsHelp();
    }
    SdkType sdkType = (SdkType)sdk.getSdkType();
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
   * @param predefined
   * @return newly created SDK, or null.
   */
  @Nullable
  public static Sdk createAndAddSDK(final String path, SdkType sdkType, @Nonnull UIAccess uiAccess) {
    VirtualFile sdkHome = ApplicationManager.getApplication()
                                            .runWriteAction(() -> LocalFileSystem.getInstance().refreshAndFindFileByPath(path));
    if (sdkHome != null) {
      final Sdk newSdk = setupSdk(SdkTable.getInstance().getAllSdks(), sdkHome, sdkType, true, null, null, uiAccess);
      if (newSdk != null) {
        ApplicationManager.getApplication().runWriteAction(() -> SdkTable.getInstance().addSdk(newSdk));
      }
      return newSdk;
    }
    return null;
  }

  @Nullable
  public static Sdk setupSdk(final Sdk[] allSdks,
                             final VirtualFile homeDir,
                             final SdkType sdkType,
                             final boolean silent,
                             @Nullable final SdkAdditionalData additionalData,
                             @Nullable final String customSdkSuggestedName,
                             @Nonnull UIAccess uiAccess) {
    if (sdkType instanceof BundleType bundleType) {
      return setupBundle(Platform.current(), allSdks, homeDir, bundleType, silent, additionalData, customSdkSuggestedName, uiAccess);
    }

    return setupLegacySdk(allSdks, homeDir, sdkType, silent, additionalData, customSdkSuggestedName, uiAccess);
  }

  @Nullable
  public static Sdk setupBundle(Platform platform,
                                Sdk[] allSdks,
                                VirtualFile homeDir,
                                BundleType bundleType,
                                boolean silent,
                                @Nullable final SdkAdditionalData additionalData,
                                @Nullable final String customSdkSuggestedName,
                                @Nonnull UIAccess uiAccess) {
    final Sdk sdk;
    try {
      Path homeNioPath = homeDir.toNioPath();

      String sdkName;
      if (customSdkSuggestedName == null) {
        sdkName = createUniqueSdkName(platform, bundleType, homeNioPath, allSdks);
      }
      else {
        sdkName = createUniqueSdkName(customSdkSuggestedName, allSdks);
      }

      sdk = SdkTable.getInstance().createSdk(homeNioPath, sdkName, bundleType);

      SdkModificator modificator = sdk.getSdkModificator();

      if (additionalData != null) {
        modificator.setSdkAdditionalData(additionalData);
      }

      modificator.commitChanges();

      bundleType.setupSdkPaths(sdk);
    }
    catch (Exception e) {
      if (!silent) {
        uiAccess.give(() -> {
          Alerts.okError("Error configuring SDK: " + e.getMessage() + ".\nPlease make sure that " + FileUtil.toSystemDependentName(homeDir.getPath()) + " is a valid home path for this SDK type.")
                .title("Error Configuring SDK")
                .showAsync();
        });
      }
      return null;
    }
    return sdk;
  }

  @Nullable
  public static Sdk setupLegacySdk(final Sdk[] allSdks,
                                   final VirtualFile homeDir,
                                   final SdkType sdkType,
                                   final boolean silent,
                                   @Nullable final SdkAdditionalData additionalData,
                                   @Nullable final String customSdkSuggestedName,
                                   @Nonnull UIAccess uiAccess) {
    final Sdk sdk;
    try {
      String sdkPath = sdkType.sdkPath(homeDir);

      String sdkName = customSdkSuggestedName == null ? createUniqueSdkName(sdkType, sdkPath, allSdks) : createUniqueSdkName(
        customSdkSuggestedName,
        allSdks);

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
      if (!silent) {
        uiAccess.give(() -> {
          Alerts.okError("Error configuring SDK: " + e.getMessage() + ".\nPlease make sure that " + FileUtil.toSystemDependentName(homeDir.getPath()) + " is a valid home path for this SDK type.")
                .title("Error Configuring SDK")
                .showAsync();
        });
      }
      return null;
    }
    return sdk;
  }

  @RequiredUIAccess
  @Deprecated
  public static void selectSdkHome(final SdkType sdkType, @Nonnull @RequiredUIAccess final Consumer<String> consumer) {
    if (sdkType instanceof BundleType bundleType) {
      selectSdkHome(Platform.current(), bundleType, path -> consumer.accept(path.toString()));
    }
    else {
      selectLegacySdkHome(sdkType, consumer);
    }
  }

  @RequiredUIAccess
  public static void selectSdkHome(Platform platform, BundleType bundleType, @Nonnull @RequiredUIAccess final Consumer<Path> consumer) {
    final FileChooserDescriptor descriptor = bundleType.getHomeChooserDescriptor(platform);

    FileChooser.chooseFiles(descriptor, null, getSuggestedSdkPath(bundleType)).doWhenDone(virtualFiles -> {
      final Path path = virtualFiles[0].toNioPath();
      if (bundleType.isValidSdkHome(platform, path)) {
        consumer.accept(path);
        return;
      }

      final Path adjustedPath = bundleType.adjustSelectedSdkHome(platform, path);
      if (bundleType.isValidSdkHome(platform, adjustedPath)) {
        consumer.accept(adjustedPath);
      }
    });
  }

  @RequiredUIAccess
  private static void selectLegacySdkHome(final SdkType sdkType, @Nonnull @RequiredUIAccess final Consumer<String> consumer) {
    final FileChooserDescriptor descriptor = sdkType.getHomeChooserDescriptor();

    FileChooser.chooseFiles(descriptor, null, getSuggestedSdkPath(sdkType)).doWhenDone(virtualFiles -> {
      final String path = virtualFiles[0].getPath();
      if (sdkType.isValidSdkHome(path)) {
        consumer.accept(path);
        return;
      }

      final String adjustedPath = sdkType.adjustSelectedSdkHome(path);
      if (sdkType.isValidSdkHome(adjustedPath)) {
        consumer.accept(adjustedPath);
      }
    });
  }

  @Nullable
  public static VirtualFile getSuggestedSdkPath(SdkType sdkType) {
    List<Path> paths = new ArrayList<>();
    if (sdkType instanceof BundleType bundleType) {
      bundleType.collectHomePaths(Platform.current(), paths::add);
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
