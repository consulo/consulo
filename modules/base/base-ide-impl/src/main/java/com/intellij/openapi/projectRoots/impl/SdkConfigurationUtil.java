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

package com.intellij.openapi.projectRoots.impl;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.project.ProjectBundle;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.projectRoots.SdkAdditionalData;
import com.intellij.openapi.projectRoots.SdkTable;
import com.intellij.openapi.projectRoots.SdkType;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.Consumer;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.fileChooser.FileChooser;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.*;

/**
 * @author yole
 */
public class SdkConfigurationUtil {
  public static final String PREDEFINED_PREFIX = " (predefined)";

  private SdkConfigurationUtil() {
  }

  private static FileChooserDescriptor createCompositeDescriptor(final SdkType... sdkTypes) {
    FileChooserDescriptor descriptor0 = sdkTypes[0].getHomeChooserDescriptor();
    FileChooserDescriptor descriptor =
      new FileChooserDescriptor(descriptor0.isChooseFiles(), descriptor0.isChooseFolders(), descriptor0.isChooseJars(),
                                descriptor0.isChooseJarsAsFiles(), descriptor0.isChooseJarContents(), descriptor0.isChooseMultiple()) {

        @Override
        public void validateSelectedFiles(final VirtualFile[] files) throws Exception {
          if (files.length > 0) {
            for (SdkType type : sdkTypes) {
              if (type.isValidSdkHome(files[0].getPath())) {
                return;
              }
            }
          }
          String message = files.length > 0 && files[0].isDirectory()
                           ? ProjectBundle.message("sdk.configure.home.invalid.error", sdkTypes[0].getPresentableName())
                           : ProjectBundle.message("sdk.configure.home.file.invalid.error", sdkTypes[0].getPresentableName());
          throw new Exception(message);
        }
      };
    descriptor.setTitle(descriptor0.getTitle());
    return descriptor;
  }

  public static void addSdk(@Nonnull final Sdk sdk) {
    ApplicationManager.getApplication().runWriteAction(new Runnable() {
      @Override
      public void run() {
        SdkTable.getInstance().addSdk(sdk);
      }
    });
  }

  public static void removeSdk(final Sdk sdk) {
    ApplicationManager.getApplication().runWriteAction(new Runnable() {
      @Override
      public void run() {
        SdkTable.getInstance().removeSdk(sdk);
      }
    });
  }

  @Nullable
  public static Sdk setupSdk(final Sdk[] allSdks,
                             final VirtualFile homeDir,
                             final SdkType sdkType,
                             final boolean silent,
                             boolean predefined,
                             @Nullable final SdkAdditionalData additionalData,
                             @Nullable final String customSdkSuggestedName) {
    final SdkImpl sdk;
    try {
      String sdkPath = sdkType.sdkPath(homeDir);

      String sdkName = null;
      if (predefined) {
        sdkName = sdkType.getName() + PREDEFINED_PREFIX;
      }
      else {
        sdkName = customSdkSuggestedName == null
                  ? createUniqueSdkName(sdkType, sdkPath, allSdks)
                  : createUniqueSdkName(customSdkSuggestedName, allSdks);
      }

      sdk = new SdkImpl(SdkTable.getInstance(), sdkName, sdkType);
      sdk.setPredefined(predefined);

      if (additionalData != null) {
        // additional initialization.
        // E.g. some ruby sdks must be initialized before
        // setupSdkPaths() method invocation
        sdk.setSdkAdditionalData(additionalData);
      }

      sdk.setHomePath(sdkPath);
      sdkType.setupSdkPaths((Sdk)sdk);
    }
    catch (Exception e) {
      if (!silent) {
        Messages.showErrorDialog("Error configuring SDK: " +
                                 e.getMessage() +
                                 ".\nPlease make sure that " +
                                 FileUtil.toSystemDependentName(homeDir.getPath()) +
                                 " is a valid home path for this SDK type.", "Error Configuring SDK");
      }
      return null;
    }
    return sdk;
  }

  /**
   * Tries to create an SDK identified by path; if successful, add the SDK to the global SDK table.
   *
   * @param path    identifies the SDK
   * @param sdkType
   * @param predefined
   * @return newly created SDK, or null.
   */
  @Nullable
  public static Sdk createAndAddSDK(final String path, SdkType sdkType, boolean predefined) {
    VirtualFile sdkHome = ApplicationManager.getApplication().runWriteAction(new Computable<VirtualFile>() {
      @Override
      public VirtualFile compute() {
        return LocalFileSystem.getInstance().refreshAndFindFileByPath(path);
      }
    });
    if (sdkHome != null) {
      final Sdk newSdk = setupSdk(SdkTable.getInstance().getAllSdks(), sdkHome, sdkType, true, predefined, null, null);
      if (newSdk != null) {
        addSdk(newSdk);
      }
      return newSdk;
    }
    return null;
  }

  public static String createUniqueSdkName(SdkType type, String home, final Sdk[] sdks) {
    return createUniqueSdkName(type.suggestSdkName(null, home), sdks);
  }

  public static String createUniqueSdkName(final String suggestedName, final Sdk[] sdks) {
    final Set<String> names = new HashSet<String>();
    for (Sdk jdk : sdks) {
      names.add(jdk.getName());
    }
    String newSdkName = suggestedName;
    int i = 0;
    while (names.contains(newSdkName)) {
      newSdkName = suggestedName + " (" + (++i) + ")";
    }
    return newSdkName;
  }

  @RequiredUIAccess
  public static void selectSdkHome(final SdkType sdkType, @Nonnull @RequiredUIAccess final Consumer<String> consumer) {
    final FileChooserDescriptor descriptor = sdkType.getHomeChooserDescriptor();
    if (ApplicationManager.getApplication().isUnitTestMode()) {
      Sdk sdk = SdkTable.getInstance().findMostRecentSdkOfType(sdkType);
      if (sdk == null) throw new RuntimeException("No SDK of type " + sdkType + " found");
      consumer.consume(sdk.getHomePath());
      return;
    }

    FileChooser.chooseFiles(descriptor, null, getSuggestedSdkPath(sdkType)).doWhenDone(virtualFiles -> {
      final String path = virtualFiles[0].getPath();
      if (sdkType.isValidSdkHome(path)) {
        consumer.consume(path);
        return;
      }

      final String adjustedPath = sdkType.adjustSelectedSdkHome(path);
      if (sdkType.isValidSdkHome(adjustedPath)) {
        consumer.consume(adjustedPath);
      }
    });
  }

  @Nullable
  public static VirtualFile getSuggestedSdkPath(SdkType sdkType) {
    Collection<String> paths = sdkType.suggestHomePaths();
    if(paths.isEmpty()) {
      return null;
    }

    for (String path : paths) {
      VirtualFile maybeSdkHomePath = LocalFileSystem.getInstance().findFileByPath(path);
      if(maybeSdkHomePath != null) {
        return maybeSdkHomePath;
      }
    }
    return null;
  }
}
