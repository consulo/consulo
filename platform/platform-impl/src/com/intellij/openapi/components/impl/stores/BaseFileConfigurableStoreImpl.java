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
package com.intellij.openapi.components.impl.stores;

import com.intellij.openapi.components.*;
import com.intellij.openapi.project.impl.ProjectManagerImpl;
import com.intellij.util.SmartList;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.List;
import java.util.Set;

abstract class BaseFileConfigurableStoreImpl extends ComponentStoreImpl {
  @NonNls protected static final String VERSION_OPTION = "version";
  @NonNls public static final String ATTRIBUTE_NAME = "name";

  private static final List<String> ourConversionProblemsStorage = new SmartList<String>();

  private final ComponentManager myComponentManager;
  private final DefaultsStateStorage myDefaultsStateStorage;
  private StateStorageManager myStateStorageManager;

  protected BaseFileConfigurableStoreImpl(@NotNull ComponentManager componentManager) {
    myComponentManager = componentManager;
    myDefaultsStateStorage = new DefaultsStateStorage(PathMacroManager.getInstance(myComponentManager));
  }

  @NotNull
  public ComponentManager getComponentManager() {
    return myComponentManager;
  }

  protected static class BaseStorageData extends FileBasedStorage.FileStorageData {
    protected int myVersion;

    public BaseStorageData(final String rootElementName) {
      super(rootElementName);
    }

    protected BaseStorageData(BaseStorageData storageData) {
      super(storageData);

      myVersion = ProjectManagerImpl.CURRENT_FORMAT_VERSION;
    }

    @Override
    public void load(@NotNull Element rootElement, @Nullable PathMacroSubstitutor pathMacroSubstitutor, boolean intern) {
      super.load(rootElement, pathMacroSubstitutor, intern);

      final String v = rootElement.getAttributeValue(VERSION_OPTION);
      if (v != null) {
        myVersion = Integer.parseInt(v);
      }
      else {
        myVersion = ProjectManagerImpl.CURRENT_FORMAT_VERSION;
      }
    }

    @Override
    @NotNull
    protected Element save() {
      Element root = super.save();
      if (root == null) {
        root = new Element(myRootElementName);
      }

      root.setAttribute(VERSION_OPTION, Integer.toString(myVersion));
      return root;
    }

    @Override
    public StorageData clone() {
      return new BaseStorageData(this);
    }

    @Override
    protected int computeHash() {
      int result = super.computeHash();
      result = result * 31 + myVersion;
      return result;
    }

    @Nullable
    @Override
    public Set<String> getChangedComponentNames(@NotNull StorageData storageData, @Nullable PathMacroSubstitutor substitutor) {
      BaseStorageData data = (BaseStorageData)storageData;
      if (myVersion != data.myVersion) {
        return null;
      }
      return super.getChangedComponentNames(storageData, substitutor);
    }
  }

  protected abstract XmlElementStorage getMainStorage();

  @Nullable
  static List<String> getConversionProblemsStorage() {
    return ourConversionProblemsStorage;
  }

  @Override
  public void load() throws IOException, StateStorageException {
    getMainStorageData(); //load it
  }

  public BaseStorageData getMainStorageData() throws StateStorageException {
    return (BaseStorageData)getMainStorage().getStorageData();
  }

  @Nullable
  @Override
  protected StateStorage getDefaultsStorage() {
    return myDefaultsStateStorage;
  }

  @NotNull
  @Override
  public StateStorageManager getStateStorageManager() {
    if (myStateStorageManager == null) {
      myStateStorageManager = createStateStorageManager();
    }
    return myStateStorageManager;
  }

  @NotNull
  protected abstract StateStorageManager createStateStorageManager();
}
