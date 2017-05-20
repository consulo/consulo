/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package com.intellij.openapi.components.impl.stores;

import com.intellij.application.options.PathMacrosImpl;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.components.PathMacroManager;
import com.intellij.openapi.components.StoragePathMacros;
import com.intellij.openapi.components.TrackingPathMacroSubstitutor;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.messages.MessageBus;
import consulo.application.ex.ApplicationEx2;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

public class ApplicationStoreImpl extends ComponentStoreImpl implements IApplicationStore {
  private static final Logger LOG = Logger.getInstance(ApplicationStoreImpl.class);

  private static final String ROOT_ELEMENT_NAME = "application";

  private final ApplicationEx2 myApplication;
  private final StateStorageManager myStateStorageManager;

  private String myConfigPath;

  // created from PicoContainer
  @SuppressWarnings({"UnusedDeclaration"})
  public ApplicationStoreImpl(final ApplicationEx2 application, PathMacroManager pathMacroManager) {
    myApplication = application;
    myStateStorageManager =
            new StateStorageManagerImpl(pathMacroManager.createTrackingSubstitutor(), ROOT_ELEMENT_NAME, application, application.getPicoContainer()) {
              private boolean myConfigDirectoryRefreshed;

              @NotNull
              @Override
              protected String getConfigurationMacro(boolean directorySpec) {
                return directorySpec ? StoragePathMacros.ROOT_CONFIG : StoragePathMacros.APP_CONFIG;
              }

              @Override
              protected StorageData createStorageData(@NotNull String fileSpec, @NotNull String filePath) {
                return new StorageData(ROOT_ELEMENT_NAME);
              }

              @Override
              protected TrackingPathMacroSubstitutor getMacroSubstitutor(@NotNull final String fileSpec) {
                if (fileSpec.equals(StoragePathMacros.APP_CONFIG + '/' + PathMacrosImpl.EXT_FILE_NAME + DirectoryStorageData.DEFAULT_EXT)) return null;
                return super.getMacroSubstitutor(fileSpec);
              }

              @Override
              protected boolean isUseXmlProlog() {
                return false;
              }

              @Override
              protected void beforeFileBasedStorageCreate() {
                if (!myConfigDirectoryRefreshed && (application.isUnitTestMode() || application.isDispatchThread())) {
                  try {
                    VirtualFile configDir = LocalFileSystem.getInstance().refreshAndFindFileByPath(getConfigPath());
                    if (configDir != null) {
                      VfsUtil.markDirtyAndRefresh(false, true, true, configDir);
                    }
                  }
                  finally {
                    myConfigDirectoryRefreshed = true;
                  }
                }
              }
            };
  }

  @Override
  public void load() throws IOException {
    long t = System.currentTimeMillis();
    myApplication.init();
    t = System.currentTimeMillis() - t;
    LOG.info(myApplication.getComponentConfigurations().length + " application components initialized in " + t + " ms");
  }

  @Override
  public void setOptionsPath(@NotNull String path) {
    myStateStorageManager.addMacro(StoragePathMacros.APP_CONFIG, path);
    myStateStorageManager.addMacro(StoragePathMacros.DEFAULT_FILE, path + "/other" + DirectoryStorageData.DEFAULT_EXT);
  }

  @Override
  public void setConfigPath(@NotNull final String configPath) {
    myStateStorageManager.addMacro(StoragePathMacros.ROOT_CONFIG, configPath);
    myConfigPath = configPath;
  }

  @Override
  @NotNull
  public String getConfigPath() {
    String configPath = myConfigPath;
    if (configPath == null) {
      // unrealistic case, but we keep backward compatibility
      configPath = PathManager.getConfigPath();
    }
    return configPath;
  }

  @Override
  @NotNull
  protected MessageBus getMessageBus() {
    return myApplication.getMessageBus();
  }

  @NotNull
  @Override
  public StateStorageManager getStateStorageManager() {
    return myStateStorageManager;
  }

  @Nullable
  @Override
  protected PathMacroManager getPathMacroManagerForDefaults() {
    return null;
  }
}
