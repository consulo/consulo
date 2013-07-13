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

import com.intellij.ide.highlighter.ArchiveFileType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.components.*;
import com.intellij.openapi.project.ProjectBundle;
import com.intellij.openapi.projectRoots.*;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.WriteExternalException;
import com.intellij.openapi.vfs.*;
import com.intellij.util.messages.MessageBus;
import org.consulo.lombok.annotations.Logger;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@State(
  name = "SdkTable",
  roamingType = RoamingType.DISABLED,
  storages = {@Storage(
    file = StoragePathMacros.APP_CONFIG + "/sdk.table.xml")})
@Logger
public class SdkTableImpl extends SdkTable implements PersistentStateComponent<Element>, ExportableComponent {
  @NonNls
  public static final String ELEMENT_SDK = "sdk";

  private final List<Sdk> mySdks = new ArrayList<Sdk>();

  private final MessageBus myMessageBus;

  public SdkTableImpl() {
    myMessageBus = ApplicationManager.getApplication().getMessageBus();
    // support external changes to sdk libraries (Endorsed Standards Override)
    VirtualFileManager.getInstance().addVirtualFileListener(new VirtualFileAdapter() {
      @Override
      public void fileCreated(VirtualFileEvent event) {
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
  @NotNull
  public File[] getExportFiles() {
    return new File[]{PathManager.getOptionsFile("sdk.table")};
  }

  @Override
  @NotNull
  public String getPresentableName() {
    return ProjectBundle.message("sdk.table.settings");
  }

  @Override
  @Nullable
  public Sdk findSdk(String name) {
    for (Sdk sdk : mySdks) {
      if (Comparing.strEqual(name, sdk.getName())) {
        return sdk;
      }
    }
    return null;
  }

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
  public void addSdk(Sdk sdk) {
    ApplicationManager.getApplication().assertWriteAccessAllowed();
    mySdks.add(sdk);
    myMessageBus.syncPublisher(SDK_TABLE_TOPIC).sdkAdded(sdk);
  }

  @Override
  public void removeSdk(Sdk sdk) {
    ApplicationManager.getApplication().assertWriteAccessAllowed();
    myMessageBus.syncPublisher(SDK_TABLE_TOPIC).sdkRemoved(sdk);
    mySdks.remove(sdk);
  }

  @Override
  public void updateSdk(Sdk originalJdk, Sdk modifiedJdk) {
    final String previousName = originalJdk.getName();
    final String newName = modifiedJdk.getName();

    ((SdkImpl)modifiedJdk).copyTo((SdkImpl)originalJdk);

    if (!previousName.equals(newName)) {
      // fire changes because after renaming JDK its name may match the associated sdk name of modules/project
      myMessageBus.syncPublisher(SDK_TABLE_TOPIC).sdkNameChanged(originalJdk, previousName);
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

  @Override
  public Sdk createSdk(final String name, final SdkTypeId sdkType) {
    return new SdkImpl(name, sdkType);
  }

  @Override
  public void loadState(Element element) {
    mySdks.clear();

    final List<Element> children = element.getChildren(ELEMENT_SDK);
    for (final Element child : children) {
      final SdkImpl sdk = new SdkImpl(null, null);
      try {
        sdk.readExternal(child);
      }
      catch (InvalidDataException ex) {
        SdkTableImpl.LOGGER.error(ex);
      }
      mySdks.add(sdk);
    }

    for (BundledSdkProvider bundledSdkProvider : BundledSdkProvider.EP_NAME.getExtensions()) {
      final Sdk bundledSdk = bundledSdkProvider.createBundledSdk();
      if(bundledSdk == null) {
        continue;
      }
      if(bundledSdk instanceof SdkImpl) {
        ((SdkImpl)bundledSdk).setBundled();
      }
      mySdks.add(bundledSdk);
    }
  }

  @Override
  public Element getState() {
    Element element = new Element("ProjectSdkTable");
    for (Sdk sdk : mySdks) {
      if (sdk.isBundled()) {
        continue;
      }
      final Element e = new Element(ELEMENT_SDK);
      try {
        ((SdkImpl)sdk).writeExternal(e);
        element.addContent(e);
      }
      catch (WriteExternalException e1) {
      }
    }
    return element;
  }
}
