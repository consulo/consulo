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

import org.jspecify.annotations.Nullable;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 2014-08-21
 */
public class SdkUtil {
  public static String createUniqueSdkName(Platform platform, BundleType type, Path home, Sdk[] sdks) {
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
  @RequiredUIAccess
  public static @Nullable Sdk createAndAddSDK(String path, SdkType sdkType, UIAccess uiAccess) {
    Application app = Application.get();
    VirtualFile sdkHome = app.runWriteAction((Supplier<VirtualFile>)() -> LocalFileSystem.getInstance().refreshAndFindFileByPath(path));
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

  public static @Nullable Sdk setupSdk(Sdk[] allSdks,
                             VirtualFile homeDir,
                             SdkType sdkType,
                             boolean silent,
                             @Nullable SdkAdditionalData additionalData,
                             @Nullable String customSdkSuggestedName,
                             UIAccess uiAccess) {
    if (sdkType instanceof BundleType bundleType) {
      return setupBundle(Platform.current(), allSdks, homeDir, bundleType, silent, additionalData, customSdkSuggestedName, uiAccess);
    }

    return setupLegacySdk(allSdks, homeDir, sdkType, silent, additionalData, customSdkSuggestedName, uiAccess);
  }

  public static @Nullable Sdk setupBundle(Platform platform,
                                Sdk[] allSdks,
                                VirtualFile homeDir,
                                BundleType bundleType,
                                boolean silent,
                                @Nullable SdkAdditionalData additionalData,
                                @Nullable String customSdkSuggestedName,
                                UIAccess uiAccess) {
    Sdk sdk;
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
          Alerts.okError(
              "Error configuring SDK: " + e.getMessage() + ".\n" +
                  "Please make sure that " + FileUtil.toSystemDependentName(homeDir.getPath()) + " is a valid home path for this SDK type."
            )
            .title("Error Configuring SDK")
            .showAsync();
        });
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
          Alerts.okError(
              "Error configuring SDK: " + e.getMessage() + ".\n" +
                "Please make sure that " + FileUtil.toSystemDependentName(homeDir.getPath()) + " is a valid home path for this SDK type."
            )
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
  public static void selectSdkHome(SdkType sdkType, @RequiredUIAccess Consumer<String> consumer) {
    if (sdkType instanceof BundleType bundleType) {
      selectSdkHome(Platform.current(), bundleType, path -> consumer.accept(path.toString()));
    } else {
      selectLegacySdkHome(sdkType, consumer);
    }
  }

  @RequiredUIAccess
  public static void selectSdkHome(Platform platform, BundleType bundleType, @RequiredUIAccess Consumer<Path> consumer) {
    FileChooserDescriptor descriptor = bundleType.getHomeChooserDescriptor(platform);

    FileChooser.chooseFiles(descriptor, null, getSuggestedSdkPath(bundleType)).doWhenDone(virtualFiles -> {
      Path path = virtualFiles[0].toNioPath();
      if (bundleType.isValidSdkHome(platform, path)) {
        consumer.accept(path);
        return;
      }

      Path adjustedPath = bundleType.adjustSelectedSdkHome(platform, path);
      if (bundleType.isValidSdkHome(platform, adjustedPath)) {
        consumer.accept(adjustedPath);
      }
    });
  }

  @RequiredUIAccess
  private static void selectLegacySdkHome(SdkType sdkType, @RequiredUIAccess Consumer<String> consumer) {
    FileChooserDescriptor descriptor = sdkType.getHomeChooserDescriptor();

    FileChooser.chooseFiles(descriptor, null, getSuggestedSdkPath(sdkType)).doWhenDone(virtualFiles -> {
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
