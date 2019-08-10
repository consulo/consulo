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

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import consulo.logging.Logger;
import com.intellij.openapi.projectRoots.*;
import com.intellij.openapi.projectRoots.ex.SdkRoot;
import com.intellij.openapi.projectRoots.ex.SdkRootContainer;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.roots.RootProvider;
import com.intellij.openapi.roots.impl.RootProviderBaseImpl;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.UserDataHolderBase;
import com.intellij.openapi.vfs.StandardFileSystems;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.ArrayUtil;
import com.intellij.util.containers.ContainerUtil;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

public class SdkImpl extends UserDataHolderBase implements PersistentStateComponent<Element>, Sdk, SdkModificator {
  public static final Logger LOGGER = Logger.getInstance(SdkImpl.class);

  @NonNls
  public static final String ELEMENT_NAME = "name";
  @NonNls
  public static final String ATTRIBUTE_VALUE = "value";
  @NonNls
  public static final String ELEMENT_TYPE = "type";
  @NonNls
  public static final String ELEMENT_VERSION = "version";
  @NonNls
  private static final String ELEMENT_ROOTS = "roots";
  @NonNls
  public static final String ELEMENT_HOMEPATH = "homePath";
  @NonNls
  private static final String ELEMENT_ADDITIONAL = "additional";


  private final SdkRootContainerImpl myRootContainer;
  private final SdkTable mySdkTable;
  private String myName;
  private String myVersionString;
  private boolean myVersionDefined = false;
  private String myHomePath = "";
  private final MyRootProvider myRootProvider = new MyRootProvider();
  private SdkImpl myOrigin = null;
  private SdkAdditionalData myAdditionalData = null;
  private SdkTypeId mySdkType;
  private boolean myPredefined;

  public SdkImpl(SdkTable sdkTable, String name, SdkTypeId sdkType) {
    mySdkTable = sdkTable;
    mySdkType = sdkType;
    myRootContainer = new SdkRootContainerImpl(true);
    myName = name;
    myRootContainer.addProjectRootContainerListener(myRootProvider);
  }

  public SdkImpl(SdkTable sdkTable, String name, SdkTypeId sdkType, String homePath, String version) {
    this(sdkTable, name, sdkType);
    myHomePath = homePath;
    myVersionString = version;
  }

  @Override
  @Nonnull
  public SdkTypeId getSdkType() {
    if (mySdkType == null) {
      mySdkType = mySdkTable.getDefaultSdkType();
    }
    return mySdkType;
  }

  @Override
  public boolean isPredefined() {
    return myPredefined;
  }

  @Override
  @Nonnull
  public String getName() {
    return myName;
  }

  @Override
  public void setName(@Nonnull String name) {
    myName = name;
  }

  @Override
  public final void setVersionString(@Nullable String versionString) {
    myVersionString = versionString == null || versionString.isEmpty() ? null : versionString;
    myVersionDefined = true;
  }

  @Override
  public String getVersionString() {
    if (myVersionString == null && !myVersionDefined) {
      String homePath = getHomePath();
      if (homePath != null && !homePath.isEmpty()) {
        setVersionString(getSdkType().getVersionString(this));
      }
    }
    return myVersionString;
  }

  public final void resetVersionString() {
    myVersionDefined = false;
    myVersionString = null;
  }

  @Override
  public String getHomePath() {
    return myHomePath;
  }

  @Override
  public VirtualFile getHomeDirectory() {
    if (myHomePath == null) {
      return null;
    }
    return StandardFileSystems.local().findFileByPath(myHomePath);
  }

  @Override
  public void loadState(Element element) {
    myName = element.getChild(ELEMENT_NAME).getAttributeValue(ATTRIBUTE_VALUE);
    final Element typeChild = element.getChild(ELEMENT_TYPE);
    final String sdkTypeName = typeChild != null ? typeChild.getAttributeValue(ATTRIBUTE_VALUE) : null;
    if (sdkTypeName != null) {
      mySdkType = mySdkTable.getSdkTypeByName(sdkTypeName);
    }
    final Element version = element.getChild(ELEMENT_VERSION);

    // set version if it was cached (defined)
    // otherwise it will be null && undefined
    if (version != null) {
      setVersionString(version.getAttributeValue(ATTRIBUTE_VALUE));
    }
    else {
      myVersionDefined = false;
    }
    myHomePath = element.getChild(ELEMENT_HOMEPATH).getAttributeValue(ATTRIBUTE_VALUE);
    myRootContainer.loadState(element.getChild(ELEMENT_ROOTS));

    final Element additional = element.getChild(ELEMENT_ADDITIONAL);
    if (additional != null) {
      LOGGER.assertTrue(mySdkType != null);
      myAdditionalData = mySdkType.loadAdditionalData(this, additional);
    }
    else {
      myAdditionalData = null;
    }
  }

  @Nonnull
  @Override
  public Element getState() {
    Element element = new Element("state");

    final Element nameElement = new Element(ELEMENT_NAME);
    nameElement.setAttribute(ATTRIBUTE_VALUE, myName);
    element.addContent(nameElement);

    if (mySdkType != null) {
      final Element sdkType = new Element(ELEMENT_TYPE);
      sdkType.setAttribute(ATTRIBUTE_VALUE, mySdkType.getName());
      element.addContent(sdkType);
    }

    if (myVersionString != null) {
      final Element version = new Element(ELEMENT_VERSION);
      version.setAttribute(ATTRIBUTE_VALUE, myVersionString);
      element.addContent(version);
    }

    final Element home = new Element(ELEMENT_HOMEPATH);
    home.setAttribute(ATTRIBUTE_VALUE, myHomePath);
    element.addContent(home);

    element.addContent(myRootContainer.getState().setName(ELEMENT_ROOTS));

    Element additional = new Element(ELEMENT_ADDITIONAL);
    if (myAdditionalData != null) {
      LOGGER.assertTrue(mySdkType != null);
      mySdkType.saveAdditionalData(myAdditionalData, additional);
    }
    element.addContent(additional);
    return element;
  }

  @Override
  public void setHomePath(String path) {
    final boolean changes = myHomePath == null ? path != null : !myHomePath.equals(path);
    myHomePath = path;
    if (changes) {
      myVersionString = null; // clear cached value if home path changed
      myVersionDefined = false;
    }
  }

  @Override
  @Nonnull
  public Object clone() {
    SdkImpl newSdk = new SdkImpl(mySdkTable, "", mySdkType);
    copyTo(newSdk);
    return newSdk;
  }

  @Override
  @Nonnull
  public RootProvider getRootProvider() {
    return myRootProvider;
  }

  public void setPredefined(boolean predefined) {
    myPredefined = predefined;
  }

  public void copyTo(SdkImpl dest) {
    final String name = getName();
    dest.setName(name);
    dest.setHomePath(getHomePath());
    dest.setPredefined(isPredefined());
    if (myVersionDefined) {
      dest.setVersionString(getVersionString());
    }
    else {
      dest.resetVersionString();
    }
    dest.setSdkAdditionalData(getSdkAdditionalData());
    dest.myRootContainer.startChange();
    dest.myRootContainer.removeAllRoots();
    for (OrderRootType rootType : OrderRootType.getAllTypes()) {
      copyRoots(myRootContainer, dest.myRootContainer, rootType);
    }
    dest.myRootContainer.finishChange();
  }

  private static void copyRoots(SdkRootContainer srcContainer, SdkRootContainer destContainer, OrderRootType type) {
    final SdkRoot[] newRoots = srcContainer.getRoots(type);
    for (SdkRoot newRoot : newRoots) {
      destContainer.addRoot(newRoot, type);
    }
  }

  private class MyRootProvider extends RootProviderBaseImpl implements ProjectRootListener {
    @Override
    @Nonnull
    public String[] getUrls(@Nonnull OrderRootType rootType) {
      final SdkRoot[] rootFiles = myRootContainer.getRoots(rootType);
      final ArrayList<String> result = new ArrayList<String>();
      for (SdkRoot rootFile : rootFiles) {
        ContainerUtil.addAll(result, rootFile.getUrls());
      }
      return ArrayUtil.toStringArray(result);
    }

    @Override
    @Nonnull
    public VirtualFile[] getFiles(@Nonnull final OrderRootType rootType) {
      return myRootContainer.getRootFiles(rootType);
    }

    private final List<RootSetChangedListener> myListeners = ContainerUtil.createLockFreeCopyOnWriteList();

    @Override
    public void addRootSetChangedListener(@Nonnull RootSetChangedListener listener) {
      assert !myListeners.contains(listener);
      myListeners.add(listener);
      super.addRootSetChangedListener(listener);
    }

    @Override
    public void addRootSetChangedListener(@Nonnull final RootSetChangedListener listener, @Nonnull Disposable parentDisposable) {
      super.addRootSetChangedListener(listener, parentDisposable);
      Disposer.register(parentDisposable, new Disposable() {
        @Override
        public void dispose() {
          removeRootSetChangedListener(listener);
        }
      });
    }

    @Override
    public void removeRootSetChangedListener(@Nonnull RootSetChangedListener listener) {
      super.removeRootSetChangedListener(listener);
      myListeners.remove(listener);
    }

    @Override
    public void rootsChanged() {
      if (myListeners.isEmpty()) {
        return;
      }
      ApplicationManager.getApplication().runWriteAction(new Runnable() {
        @Override
        public void run() {
          fireRootSetChanged();
        }
      });
    }
  }

  // SdkModificator implementation
  @Override
  @Nonnull
  public SdkModificator getSdkModificator() {
    SdkImpl sdk = (SdkImpl)clone();
    sdk.myOrigin = this;
    sdk.myRootContainer.startChange();
    sdk.update();
    return sdk;
  }

  @Override
  public void commitChanges() {
    LOGGER.assertTrue(isWritable());
    myRootContainer.finishChange();
    copyTo(myOrigin);
    myOrigin = null;
  }

  @Override
  public SdkAdditionalData getSdkAdditionalData() {
    return myAdditionalData;
  }

  @Override
  public void setSdkAdditionalData(SdkAdditionalData data) {
    myAdditionalData = data;
  }

  @Override
  public VirtualFile[] getRoots(OrderRootType rootType) {
    final SdkRoot[] roots = myRootContainer.getRoots(rootType); // use getRoots() cause the data is most up-to-date there
    final List<VirtualFile> files = new ArrayList<VirtualFile>(roots.length);
    for (SdkRoot root : roots) {
      ContainerUtil.addAll(files, root.getVirtualFiles());
    }
    return VfsUtilCore.toVirtualFileArray(files);
  }

  @Override
  public void addRoot(@Nonnull VirtualFile root, @Nonnull OrderRootType rootType) {
    myRootContainer.addRoot(root, rootType);
  }

  @Override
  public void removeRoot(@Nonnull VirtualFile root, @Nonnull OrderRootType rootType) {
    myRootContainer.removeRoot(root, rootType);
  }

  @Override
  public void removeRoots(OrderRootType rootType) {
    myRootContainer.removeAllRoots(rootType);
  }

  @Override
  public void removeAllRoots() {
    myRootContainer.removeAllRoots();
  }

  @Override
  public boolean isWritable() {
    return myOrigin != null;
  }

  public void update() {
    myRootContainer.update();
  }

  @Override
  public String toString() {
    return getName() + ": " + getVersionString() + " (" + getHomePath() + ")";
  }
}
