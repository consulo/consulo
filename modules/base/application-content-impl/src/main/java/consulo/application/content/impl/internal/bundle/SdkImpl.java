// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.application.content.impl.internal.bundle;

import consulo.annotation.DeprecationInfo;
import consulo.application.Application;
import consulo.application.ApplicationManager;
import consulo.content.OrderRootType;
import consulo.content.RootProvider;
import consulo.content.RootProviderBase;
import consulo.content.bundle.*;
import consulo.content.internal.GlobalLibraryRootListenerProvider;
import consulo.application.content.impl.internal.RootsAsVirtualFilePointers;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.logging.Logger;
import consulo.platform.Platform;
import consulo.util.dataholder.UserDataHolderBase;
import consulo.util.lang.StringUtil;
import consulo.util.xml.serializer.InvalidDataException;
import consulo.virtualFileSystem.StandardFileSystems;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.pointer.VirtualFilePointerListener;
import consulo.virtualFileSystem.pointer.VirtualFilePointerManager;
import org.jdom.Element;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.nio.file.Path;

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
  public static final String ELEMENT_NAME = "name";
  public static final String ATTRIBUTE_VALUE = "value";
  public static final String ELEMENT_TYPE = "type";
  private static final String ELEMENT_VERSION = "version";
  private static final String ELEMENT_ROOTS = "roots";
  private static final String ELEMENT_HOMEPATH = "homePath";
  private static final String ELEMENT_ADDITIONAL = "additional";
  private final MyRootProvider myRootProvider = new MyRootProvider();

  private boolean myPredefined;

  @Deprecated
  @DeprecationInfo("Prefer with Path parameter")
  public SdkImpl(SdkTable sdkTable, String name, SdkTypeId sdkType) {
    mySdkTable = sdkTable;
    mySdkType = sdkType;
    myName = name;

    VirtualFilePointerListener listener = Application.get().getInstance(GlobalLibraryRootListenerProvider.class).getListener();

    myRoots = new RootsAsVirtualFilePointers(true, listener, this);

    // register on VirtualFilePointerManager because we want our virtual pointers to be disposed before VFPM to avoid "pointer leaked" diagnostics fired
    Disposer.register((Disposable)VirtualFilePointerManager.getInstance(), this);
  }

  public SdkImpl(SdkTable sdkTable, SdkTypeId sdkType, Path homePath, String name) {
    mySdkTable = sdkTable;
    mySdkType = sdkType;
    myName = name;
    myHomePath = homePath.toAbsolutePath().toString();
    VirtualFilePointerListener listener = Application.get().getInstance(GlobalLibraryRootListenerProvider.class).getListener();
    myRoots = new RootsAsVirtualFilePointers(true, listener, this);

    // register on VirtualFilePointerManager because we want our virtual pointers to be disposed before VFPM to avoid "pointer leaked" diagnostics fired
    Disposer.register((Disposable)VirtualFilePointerManager.getInstance(), this);
  }

  public SdkImpl(SdkTable sdkTable, String name, SdkTypeId sdkType, String homePath, String version) {
    this(sdkTable, name, sdkType);
    myHomePath = homePath;
    myVersionString = version;
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
    myVersionString = StringUtil.nullize(versionString);
    myVersionDefined = true;
  }

  @Override
  public String getVersionString() {
    if (mySdkType instanceof BundleType bundleType) {
      return getBundleVersion(bundleType);
    }
    else {
      return getLegacyVersion();
    }
  }

  @Nullable
  private String getBundleVersion(BundleType bundleType) {
    if (myVersionString == null && !myVersionDefined) {
      setVersionString(bundleType.getVersionString(getPlatform(), getHomeNioPath()));
    }
    return myVersionString;
  }

  @Nullable
  private String getLegacyVersion() {
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

  @Nonnull
  @Override
  public Path getHomeNioPath() {
    // TODO [VISTALL] better handle remote paths
    if (myHomePath != null) {
      return Path.of(myHomePath);
    }
    return Path.of("");
  }

  @Nonnull
  @Override
  public Platform getPlatform() {
    // TODO [VISTALL] better handle remote platform
    return Platform.current();
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
      sdkType.setAttribute(ATTRIBUTE_VALUE, mySdkType.getId());
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

  @Override
  public void setHomeNioPath(@Nonnull Path path) {
    // TODO [VISTALL] better handle
    setHomePath(path.toAbsolutePath().toString());
  }

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

  private class MyRootProvider extends RootProviderBase {
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
