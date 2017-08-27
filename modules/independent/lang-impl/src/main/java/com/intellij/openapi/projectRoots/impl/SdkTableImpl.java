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
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.RoamingType;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.projectRoots.SdkTable;
import com.intellij.openapi.projectRoots.SdkType;
import com.intellij.openapi.projectRoots.SdkTypeId;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.vfs.*;
import com.intellij.util.messages.MessageBus;
import consulo.annotations.RequiredWriteAction;
import consulo.fileTypes.ArchiveFileType;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

@State(name = "SdkTable", storages = @Storage(value = "sdk.table.xml", roamingType = RoamingType.DISABLED))
public class SdkTableImpl extends SdkTable implements PersistentStateComponent<Element> {
  public static final Logger LOGGER = Logger.getInstance(SdkTableImpl.class);

  @NonNls
  public static final String ELEMENT_SDK = "sdk";

  private final List<Sdk> mySdks = new ArrayList<Sdk>();

  private final MessageBus myMessageBus;

  public SdkTableImpl() {
    myMessageBus = ApplicationManager.getApplication().getMessageBus();
    // support external changes to sdk libraries (Endorsed Standards Override)
    VirtualFileManager.getInstance().addVirtualFileListener(new VirtualFileListener() {
      @Override
      public void fileCreated(@NotNull VirtualFileEvent event) {
        updateSdks(event.getFile());
      }

      private void updateSdks(VirtualFile file) {
        if (file.isDirectory() || !(file.getFileType() instanceof ArchiveFileType)) {
          // consider only archive files that may contain libraries
          return;
        }
        for (Sdk sdk : mySdks) {
          final SdkType sdkType = (SdkType)sdk.getSdkType();
          final VirtualFile home = sdk.getHomeDirectory();
          if (home == null) {
            continue;
          }
          if (VfsUtilCore.isAncestor(home, file, true)) {
            sdkType.setupSdkPaths(sdk);
            // no need to iterate further assuming the file cannot be under the home of several SDKs
            break;
          }
        }
      }
    });
  }

  @Override
  @Nullable
  public Sdk findSdk(String name) {
    //noinspection ForLoopReplaceableByForEach
    for (int i = 0, len = mySdks.size(); i < len; ++i) { // avoid foreach,  it instantiates ArrayList$Itr, this traversal happens very often
      final Sdk jdk = mySdks.get(i);
      if (Comparing.strEqual(name, jdk.getName())) {
        return jdk;
      }
    }
    return null;
  }

  @NotNull
  @Override
  public Sdk[] getAllSdks() {
    return mySdks.toArray(new Sdk[mySdks.size()]);
  }

  @Override
  public List<Sdk> getSdksOfType(final SdkTypeId type) {
    List<Sdk> result = new ArrayList<Sdk>();
    final Sdk[] sdks = getAllSdks();
    for (Sdk sdk : sdks) {
      if (sdk.getSdkType() == type) {
        result.add(sdk);
      }
    }
    return result;
  }

  @Override
  @Nullable
  public Sdk findPredefinedSdkByType(@NotNull SdkTypeId sdkType) {
    for (Sdk sdk : mySdks) {
      if (sdk.isPredefined() && sdk.getSdkType() == sdkType) {
        return sdk;
      }
    }
    return null;
  }

  @Override
  @RequiredWriteAction
  public void addSdk(@NotNull Sdk sdk) {
    ApplicationManager.getApplication().assertWriteAccessAllowed();
    myMessageBus.syncPublisher(SDK_TABLE_TOPIC).beforeSdkAdded(sdk);
    mySdks.add(sdk);
    myMessageBus.syncPublisher(SDK_TABLE_TOPIC).sdkAdded(sdk);
  }

  @Override
  @RequiredWriteAction
  public void removeSdk(@NotNull Sdk sdk) {
    ApplicationManager.getApplication().assertWriteAccessAllowed();
    myMessageBus.syncPublisher(SDK_TABLE_TOPIC).beforeSdkRemoved(sdk);
    mySdks.remove(sdk);
    myMessageBus.syncPublisher(SDK_TABLE_TOPIC).sdkRemoved(sdk);
  }

  @Override
  @RequiredWriteAction
  public void updateSdk(@NotNull Sdk originalSdk, @NotNull Sdk modifiedSdk) {
    ApplicationManager.getApplication().assertWriteAccessAllowed();
    final String previousName = originalSdk.getName();
    final String newName = modifiedSdk.getName();

    boolean nameChanged = !previousName.equals(newName);
    if (nameChanged) {
      myMessageBus.syncPublisher(SDK_TABLE_TOPIC).beforeSdkNameChanged(originalSdk, previousName);
    }

    ((SdkImpl)modifiedSdk).copyTo((SdkImpl)originalSdk);

    if (nameChanged) {
      myMessageBus.syncPublisher(SDK_TABLE_TOPIC).sdkNameChanged(originalSdk, previousName);
    }
  }

  @Override
  public SdkTypeId getDefaultSdkType() {
    return UnknownSdkType.getInstance(null);
  }

  @Override
  public SdkTypeId getSdkTypeByName(String sdkTypeName) {
    return findSdkTypeByName(sdkTypeName);
  }

  public static SdkTypeId findSdkTypeByName(String sdkTypeName) {
    final SdkType[] allSdkTypes = SdkType.EP_NAME.getExtensions();
    for (final SdkType type : allSdkTypes) {
      if (type.getName().equals(sdkTypeName)) {
        return type;
      }
    }
    return UnknownSdkType.getInstance(sdkTypeName);
  }

  @NotNull
  @Override
  public Sdk createSdk(final String name, final SdkTypeId sdkType) {
    return new SdkImpl(name, sdkType);
  }

  @Override
  public void loadState(Element element) {
    mySdks.clear();

    List<Element> children = element.getChildren(ELEMENT_SDK);

    for (final Element child : children) {
      final SdkImpl sdk = new SdkImpl(null, null);
      sdk.loadState(child);
      mySdks.add(sdk);
    }
  }

  @Override
  public Element getState() {
    Element element = new Element("SdkTable");
    for (Sdk sdk : mySdks) {
      if (!sdk.isPredefined()) {
        element.addContent(((SdkImpl)sdk).getState().setName(ELEMENT_SDK));
      }
    }
    return element;
  }
}
