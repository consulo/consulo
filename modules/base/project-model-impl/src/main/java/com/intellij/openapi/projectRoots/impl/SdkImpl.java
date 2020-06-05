// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.projectRoots.impl;

import consulo.disposer.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.projectRoots.*;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.roots.RootProvider;
import com.intellij.openapi.roots.impl.ProjectRootManagerImpl;
import com.intellij.openapi.roots.impl.RootProviderBaseImpl;
import consulo.disposer.Disposer;
import com.intellij.openapi.util.InvalidDataException;
import consulo.util.dataholder.UserDataHolderBase;
import com.intellij.openapi.vfs.StandardFileSystems;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.pointers.VirtualFilePointer;
import com.intellij.openapi.vfs.pointers.VirtualFilePointerListener;
import com.intellij.openapi.vfs.pointers.VirtualFilePointerManager;
import consulo.logging.Logger;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;

public class SdkImpl extends UserDataHolderBase implements Sdk, SdkModificator, Disposable {
  private static final Logger LOG = Logger.getInstance(SdkImpl.class);

  private String myName;
  private String myVersionString;
  private boolean myVersionDefined;
  private String myHomePath = "";
  private final RootsAsVirtualFilePointers myRoots;
  private SdkImpl myOrigin;
  private SdkAdditionalData myAdditionalData;
  private final SdkTable mySdkTable;
  private SdkTypeId mySdkType;
  @NonNls
  public static final String ELEMENT_NAME = "name";
  @NonNls
  public static final String ATTRIBUTE_VALUE = "value";
  @NonNls
  public static final String ELEMENT_TYPE = "type";
  @NonNls
  private static final String ELEMENT_VERSION = "version";
  @NonNls
  private static final String ELEMENT_ROOTS = "roots";
  @NonNls
  private static final String ELEMENT_HOMEPATH = "homePath";
  @NonNls
  private static final String ELEMENT_ADDITIONAL = "additional";
  private final MyRootProvider myRootProvider = new MyRootProvider();

  private boolean myPredefined;

  public SdkImpl(SdkTable sdkTable, String name, SdkTypeId sdkType) {
    mySdkTable = sdkTable;
    mySdkType = sdkType;
    myName = name;

    myRoots = new RootsAsVirtualFilePointers(true, tellAllProjectsTheirRootsAreGoingToChange, this);
    // register on VirtualFilePointerManager because we want our virtual pointers to be disposed before VFPM to avoid "pointer leaked" diagnostics fired
    Disposer.register((Disposable)VirtualFilePointerManager.getInstance(), this);
  }

  public SdkImpl(SdkTable sdkTable, String name, SdkTypeId sdkType, String homePath, String version) {
    this(sdkTable, name, sdkType);
    myHomePath = homePath;
    myVersionString = version;
  }

  private static final VirtualFilePointerListener tellAllProjectsTheirRootsAreGoingToChange = new VirtualFilePointerListener() {
    @Override
    public void beforeValidityChanged(@Nonnull VirtualFilePointer[] pointers) {
      //todo check if this sdk is really used in the project
      for (Project project : ProjectManager.getInstance().getOpenProjects()) {
        VirtualFilePointerListener listener = ((ProjectRootManagerImpl)ProjectRootManager.getInstance(project)).getRootsValidityChangedListener();
        listener.beforeValidityChanged(pointers);
      }
    }

    @Override
    public void validityChanged(@Nonnull VirtualFilePointer[] pointers) {
      //todo check if this sdk is really used in the project
      for (Project project : ProjectManager.getInstance().getOpenProjects()) {
        VirtualFilePointerListener listener = ((ProjectRootManagerImpl)ProjectRootManager.getInstance(project)).getRootsValidityChangedListener();
        listener.validityChanged(pointers);
      }
    }
  };

  @Nonnull
  public static VirtualFilePointerListener getGlobalVirtualFilePointerListener() {
    return tellAllProjectsTheirRootsAreGoingToChange;
  }

  @Override
  public void dispose() {
  }

  @Override
  @Nonnull
  public SdkTypeId getSdkType() {
    SdkTypeId sdkType = mySdkType;
    if (sdkType == null) {
      mySdkType = sdkType = mySdkTable.getDefaultSdkType();
    }
    return sdkType;
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

  public void readExternal(@Nonnull Element element) {
    readExternal(element, null);
  }

  public void readExternal(@Nonnull Element element, @Nullable SdkTable sdkTable) throws InvalidDataException {
    Element elementName = assertNotMissing(element, ELEMENT_NAME);
    myName = elementName.getAttributeValue(ATTRIBUTE_VALUE);
    final Element typeChild = element.getChild(ELEMENT_TYPE);
    final String sdkTypeName = typeChild != null ? typeChild.getAttributeValue(ATTRIBUTE_VALUE) : null;
    if (sdkTypeName != null) {
      if (sdkTable == null) {
        sdkTable = mySdkTable;
      }
      mySdkType = sdkTable.getSdkTypeByName(sdkTypeName);
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
    Element homePath = assertNotMissing(element, ELEMENT_HOMEPATH);
    myHomePath = homePath.getAttributeValue(ATTRIBUTE_VALUE);
    Element elementRoots = assertNotMissing(element, ELEMENT_ROOTS);
    myRoots.readExternal(elementRoots);

    final Element additional = element.getChild(ELEMENT_ADDITIONAL);
    if (additional != null) {
      LOG.assertTrue(mySdkType != null);
      myAdditionalData = mySdkType.loadAdditionalData(this, additional);
    }
    else {
      myAdditionalData = null;
    }
  }

  @Nonnull
  private static Element assertNotMissing(@Nonnull Element parent, @Nonnull String childName) {
    Element child = parent.getChild(childName);
    if (child == null) throw new InvalidDataException("mandatory element '" + childName + "' is missing: " + parent);
    return child;
  }

  public void writeExternal(@Nonnull Element element) {
    final Element name = new Element(ELEMENT_NAME);
    name.setAttribute(ATTRIBUTE_VALUE, myName);
    element.addContent(name);

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

    Element roots = new Element(ELEMENT_ROOTS);
    myRoots.writeExternal(roots);
    element.addContent(roots);

    Element additional = new Element(ELEMENT_ADDITIONAL);
    if (myAdditionalData != null) {
      LOG.assertTrue(mySdkType != null);
      mySdkType.saveAdditionalData(myAdditionalData, additional);
    }
    element.addContent(additional);
  }

  @Override
  public void setHomePath(String path) {
    final boolean changes = myHomePath == null ? path != null : !myHomePath.equals(path);
    myHomePath = path;
    if (changes) {
      resetVersionString(); // clear cached value if home path changed
    }
  }

  @SuppressWarnings("MethodDoesntCallSuperMethod")
  @Override
  @Nonnull
  public SdkImpl clone() {
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

  void copyTo(@Nonnull SdkImpl dest) {
    final String name = getName();
    dest.setName(name);
    dest.setHomePath(getHomePath());
    dest.setPredefined(myPredefined);
    dest.myVersionDefined = myVersionDefined;
    dest.myVersionString = myVersionString;
    dest.setSdkAdditionalData(getSdkAdditionalData());
    dest.myRoots.copyRootsFrom(myRoots);
    dest.myRootProvider.rootsChanged();
  }

  private class MyRootProvider extends RootProviderBaseImpl implements ProjectRootListener {
    @Override
    @Nonnull
    public String[] getUrls(@Nonnull OrderRootType rootType) {
      return myRoots.getUrls(rootType);
    }

    @Override
    @Nonnull
    public VirtualFile[] getFiles(@Nonnull final OrderRootType rootType) {
      return myRoots.getFiles(rootType);
    }

    @Override
    public void rootsChanged() {
      if (myDispatcher.hasListeners()) {
        ApplicationManager.getApplication().runWriteAction(this::fireRootSetChanged);
      }
    }
  }

  // SdkModificator implementation
  @Override
  @Nonnull
  public SdkModificator getSdkModificator() {
    SdkImpl sdk = clone();
    sdk.myOrigin = this;
    return sdk;
  }

  @Override
  public void commitChanges() {
    LOG.assertTrue(isWritable());

    copyTo(myOrigin);
    myOrigin = null;
    Disposer.dispose(this);
  }

  @Override
  public SdkAdditionalData getSdkAdditionalData() {
    return myAdditionalData;
  }

  @Override
  public void setSdkAdditionalData(SdkAdditionalData data) {
    myAdditionalData = data;
  }

  @Nonnull
  @Override
  public VirtualFile[] getRoots(@Nonnull OrderRootType rootType) {
    return myRoots.getFiles(rootType);
  }

  @Nonnull
  @Override
  public String[] getUrls(@Nonnull OrderRootType rootType) {
    return myRoots.getUrls(rootType);
  }

  @Override
  public void addRoot(@Nonnull VirtualFile root, @Nonnull OrderRootType rootType) {
    myRoots.addRoot(root, rootType);
  }

  @Override
  public void addRoot(@Nonnull String url, @Nonnull OrderRootType rootType) {
    myRoots.addRoot(url, rootType);
  }

  @Override
  public void removeRoot(@Nonnull VirtualFile root, @Nonnull OrderRootType rootType) {
    myRoots.removeRoot(root, rootType);
  }

  @Override
  public void removeRoot(@Nonnull String url, @Nonnull OrderRootType rootType) {
    myRoots.removeRoot(url, rootType);
  }

  @Override
  public void removeRoots(@Nonnull OrderRootType rootType) {
    myRoots.removeAllRoots(rootType);
  }

  @Override
  public void removeAllRoots() {
    myRoots.removeAllRoots();
  }

  @Override
  public boolean isWritable() {
    return myOrigin != null;
  }

  @Override
  public String toString() {
    return myName + (myVersionDefined ? ": " + myVersionString : "") + " (" + myHomePath + ")";
  }
}
