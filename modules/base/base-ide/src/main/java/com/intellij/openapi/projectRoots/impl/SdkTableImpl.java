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

import com.intellij.openapi.application.Application;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.RoamingType;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.projectRoots.SdkTable;
import com.intellij.openapi.projectRoots.SdkType;
import com.intellij.openapi.projectRoots.SdkTypeId;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.vfs.newvfs.BulkFileListener;
import com.intellij.openapi.vfs.newvfs.events.VFileCreateEvent;
import com.intellij.openapi.vfs.newvfs.events.VFileEvent;
import com.intellij.util.containers.SmartHashSet;
import com.intellij.util.messages.MessageBusConnection;
import consulo.annotation.access.RequiredWriteAction;
import consulo.fileTypes.ArchiveFileType;
import jakarta.inject.Inject;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import jakarta.inject.Singleton;
import java.util.*;
import java.util.function.Consumer;

@Singleton
@State(name = "SdkTable", storages = @Storage(value = "sdk.table.xml", roamingType = RoamingType.DISABLED))
public class SdkTableImpl extends SdkTable implements PersistentStateComponent<Element> {
  @NonNls
  private static final String ELEMENT_SDK = "sdk";

  private final List<SdkImpl> mySdks = new ArrayList<>();

  private final Application myApplication;

  @Inject
  public SdkTableImpl(Application application) {
    myApplication = application;
    final MessageBusConnection connection = myApplication.getMessageBus().connect();

    // support external changes to sdk libraries (Endorsed Standards Override)
    connection.subscribe(VirtualFileManager.VFS_CHANGES, new BulkFileListener() {
      private final FileTypeManager myFileTypeManager = FileTypeManager.getInstance();

      @Override
      public void after(@Nonnull List<? extends VFileEvent> events) {
        if (!events.isEmpty()) {
          final Set<Sdk> affected = new SmartHashSet<>();
          for (VFileEvent event : events) {
            addAffectedSdk(event, affected);
          }
          if (!affected.isEmpty()) {
            for (Sdk sdk : affected) {
              ((SdkType)sdk.getSdkType()).setupSdkPaths(sdk);
            }
          }
        }
      }

      private void addAffectedSdk(VFileEvent event, Set<? super Sdk> affected) {
        CharSequence fileName = null;
        if (event instanceof VFileCreateEvent) {
          if (((VFileCreateEvent)event).isDirectory()) return;
          fileName = ((VFileCreateEvent)event).getChildName();
        }
        else {
          final VirtualFile file = event.getFile();

          if (file != null && file.isValid()) {
            if (file.isDirectory()) {
              return;
            }
            fileName = file.getNameSequence();
          }
        }
        if (fileName == null) {
          final String eventPath = event.getPath();
          fileName = VfsUtil.extractFileName(eventPath);
        }
        if (fileName != null) {
          // avoid calling getFileType() because it will try to detect file type from content for unknown/text file types
          // consider only archive files that may contain libraries
          FileType fileType = myFileTypeManager.getFileTypeByFileName(fileName);
          if (!(fileType instanceof ArchiveFileType)) {
            return;
          }
        }

        for (Sdk sdk : mySdks) {
          if (!affected.contains(sdk)) {
            final String homePath = sdk.getHomePath();
            final String eventPath = event.getPath();
            if (!StringUtil.isEmpty(homePath) && FileUtil.isAncestor(homePath, eventPath, true)) {
              affected.add(sdk);
            }
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

  @Nonnull
  @Override
  public Sdk[] getAllSdks() {
    return mySdks.toArray(new Sdk[mySdks.size()]);
  }

  @Override
  public void forEachBundle(@Nonnull Consumer<Sdk> sdkConsumer) {
    for (SdkImpl sdk : mySdks) {
      sdkConsumer.accept(sdk);
    }
  }

  @Override
  public List<Sdk> getSdksOfType(final SdkTypeId type) {
    List<Sdk> result = new ArrayList<>();
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
  public Sdk findPredefinedSdkByType(@Nonnull SdkTypeId sdkType) {
    for (Sdk sdk : mySdks) {
      if (sdk.isPredefined() && sdk.getSdkType() == sdkType) {
        return sdk;
      }
    }
    return null;
  }

  /**
   * Add sdks without write access, and without listener notify
   */
  public void addSdksUnsafe(@Nonnull Collection<? extends Sdk> sdks) {
    for (Sdk sdk : sdks) {
      mySdks.add((SdkImpl)sdk);
    }
  }

  @Override
  @RequiredWriteAction
  public void addSdk(@Nonnull Sdk sdk) {
    myApplication.assertWriteAccessAllowed();
    myApplication.getMessageBus().syncPublisher(SDK_TABLE_TOPIC).beforeSdkAdded(sdk);
    mySdks.add((SdkImpl)sdk);
    myApplication.getMessageBus().syncPublisher(SDK_TABLE_TOPIC).sdkAdded(sdk);
  }

  @Override
  @RequiredWriteAction
  public void removeSdk(@Nonnull Sdk sdk) {
    myApplication.assertWriteAccessAllowed();
    myApplication.getMessageBus().syncPublisher(SDK_TABLE_TOPIC).beforeSdkRemoved(sdk);
    mySdks.remove(sdk);
    myApplication.getMessageBus().syncPublisher(SDK_TABLE_TOPIC).sdkRemoved(sdk);
  }

  @Override
  @RequiredWriteAction
  public void updateSdk(@Nonnull Sdk originalSdk, @Nonnull Sdk modifiedSdk) {
    myApplication.assertWriteAccessAllowed();
    final String previousName = originalSdk.getName();
    final String newName = modifiedSdk.getName();

    boolean nameChanged = !previousName.equals(newName);
    if (nameChanged) {
      myApplication.getMessageBus().syncPublisher(SDK_TABLE_TOPIC).beforeSdkNameChanged(originalSdk, previousName);
    }

    ((SdkImpl)modifiedSdk).copyTo((SdkImpl)originalSdk);

    if (nameChanged) {
      myApplication.getMessageBus().syncPublisher(SDK_TABLE_TOPIC).sdkNameChanged(originalSdk, previousName);
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
    for (final SdkType type : SdkType.EP_NAME.getExtensionList()) {
      if (type.getName().equals(sdkTypeName)) {
        return type;
      }
    }
    return UnknownSdkType.getInstance(sdkTypeName);
  }

  @Nonnull
  @Override
  public Sdk createSdk(final String name, final SdkTypeId sdkType) {
    return new SdkImpl(this, name, sdkType);
  }

  @Override
  public void loadState(Element element) {
    Iterator<SdkImpl> iterator = mySdks.iterator();
    while (iterator.hasNext()) {
      SdkImpl sdk = iterator.next();

      if (!sdk.isPredefined()) {
        iterator.remove();
      }
    }

    List<Element> children = element.getChildren(ELEMENT_SDK);

    for (final Element child : children) {
      final SdkImpl sdk = new SdkImpl(this, null, null);
      sdk.readExternal(child, this);
      mySdks.add(sdk);
    }
  }

  @Override
  public Element getState() {
    Element element = new Element("SdkTable");
    for (Sdk sdk : mySdks) {
      if (!sdk.isPredefined()) {
        Element e = new Element(ELEMENT_SDK);
        ((SdkImpl)sdk).writeExternal(e);
        element.addContent(e);
      }
    }
    return element;
  }
}
