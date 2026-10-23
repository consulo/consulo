// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.application.content.impl.internal.bundle;

import consulo.annotation.DeprecationInfo;
import consulo.application.Application;
import consulo.application.ApplicationManager;
import consulo.content.RootProvider;
import consulo.content.RootProviderBase;
import consulo.content.bundle.*;
import consulo.content.platform.PlatformTarget;
import consulo.content.platform.PlatformTargetManager;
import consulo.logging.Logger;
import consulo.platform.Platform;
import consulo.project.localize.ProjectLocalize;
import consulo.util.dataholder.UserDataHolderBase;
import consulo.util.lang.StringUtil;
import consulo.util.xml.serializer.InvalidDataException;
import consulo.virtualFileSystem.StandardFileSystems;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.url.VirtualFileUrlManager;
import org.jdom.Element;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.Objects;

public class SdkImpl extends UserDataHolderBase implements Sdk, SdkModificator {
    private static final Logger LOG = Logger.getInstance(SdkImpl.class);

    private String myName;
    private String myVersionString;
    private boolean myVersionDefined;
    private String myHomePath = "";
    private final SdkRoots myRoots;
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
    private static final String ELEMENT_PLATFORM_TARGET = "platformTarget";
    private final MyRootProvider myRootProvider = new MyRootProvider();

    private boolean myPredefined;
    private @Nullable String myPlatformTargetId;

    @Deprecated
    @DeprecationInfo("Prefer with Path parameter")
    public SdkImpl(SdkTable sdkTable, String name, SdkTypeId sdkType) {
        mySdkTable = sdkTable;
        mySdkType = sdkType;
        myName = name;
        myRoots = new SdkRoots(Application.get().getInstance(VirtualFileUrlManager.class));
    }

    public SdkImpl(SdkTable sdkTable, SdkTypeId sdkType, Path homePath, String name) {
        mySdkTable = sdkTable;
        mySdkType = sdkType;
        myName = name;
        myHomePath = homePath.toAbsolutePath().toString();
        myRoots = new SdkRoots(Application.get().getInstance(VirtualFileUrlManager.class));
    }

    public SdkImpl(SdkTable sdkTable, String name, SdkTypeId sdkType, String homePath, String version) {
        this(sdkTable, name, sdkType);
        myHomePath = homePath;
        myVersionString = version;
    }

    @Override

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

    public String getName() {
        return myName;
    }

    @Override
    public void setName(String name) {
        myName = name;
    }

    @Override
    public final void setVersionString(@Nullable String versionString) {
        myVersionString = StringUtil.nullize(versionString);
        myVersionDefined = true;
    }

    @Override
    public String getVersionString() {
        if (mySdkType instanceof PlatformAwareSdkType bundleType) {
            return getBundleVersion(bundleType);
        }
        else {
            return getLegacyVersion();
        }
    }

    private @Nullable String getBundleVersion(PlatformAwareSdkType platformAwareSdkType) {
        if (myVersionString == null && !myVersionDefined) {
            Platform platform = findPlatform();
            if (platform != null) {
                setVersionString(platformAwareSdkType.getVersionString(platform, getHomeNioPath()));
            }
        }
        return myVersionString;
    }

    private @Nullable String getLegacyVersion() {
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
    public Path getHomeNioPath() {
        Platform platform = myPlatformTargetId == null ? Platform.current() : findPlatform();
        String home = myHomePath == null ? "" : myHomePath;
        return platform == null ? Path.of(home) : platform.fs().getPath(home);
    }


    @Override
    public Platform getPlatform() {
        Platform platform = findPlatform();
        if (platform == null) {
            throw new IllegalStateException(ProjectLocalize.errorPlatformTargetConnecting(myPlatformTargetId).get());
        }
        return platform;
    }

    /**
     * The platform while it is reachable; an unreachable remote one starts connecting and answers null.
     */
    private @Nullable Platform findPlatform() {
        if (myPlatformTargetId == null) {
            return Platform.current();
        }
        PlatformTarget target = Application.get().getInstance(PlatformTargetManager.class).findTarget(myPlatformTargetId);
        if (target == null) {
            return null;
        }
        Platform platform = target.getPlatform();
        if (platform == null) {
            target.connect();
        }
        return platform;
    }

    @Override
    public @Nullable String getPlatformTargetId() {
        return myPlatformTargetId;
    }

    @Override
    public void setPlatformTargetId(@Nullable String targetId) {
        if (!Objects.equals(myPlatformTargetId, targetId)) {
            myPlatformTargetId = targetId;
            resetVersionString();
        }
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

    public void readExternal(Element element) {
        readExternal(element, null);
    }

    public void readExternal(Element element, @Nullable SdkTable sdkTable) throws InvalidDataException {
        Element elementName = assertNotMissing(element, ELEMENT_NAME);
        myName = elementName.getAttributeValue(ATTRIBUTE_VALUE);
        Element typeChild = element.getChild(ELEMENT_TYPE);
        String sdkTypeName = typeChild != null ? typeChild.getAttributeValue(ATTRIBUTE_VALUE) : null;
        if (sdkTypeName != null) {
            if (sdkTable == null) {
                sdkTable = mySdkTable;
            }
            mySdkType = sdkTable.getSdkTypeByName(sdkTypeName);
        }
        Element version = element.getChild(ELEMENT_VERSION);

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
        Element platformTarget = element.getChild(ELEMENT_PLATFORM_TARGET);
        myPlatformTargetId = platformTarget == null ? null : platformTarget.getAttributeValue(ATTRIBUTE_VALUE);
        Element elementRoots = assertNotMissing(element, ELEMENT_ROOTS);
        myRoots.readExternal(elementRoots);

        Element additional = element.getChild(ELEMENT_ADDITIONAL);
        if (additional != null) {
            LOG.assertTrue(mySdkType != null);
            myAdditionalData = mySdkType.loadAdditionalData(this, additional);
        }
        else {
            myAdditionalData = null;
        }
    }


    private static Element assertNotMissing(Element parent, String childName) {
        Element child = parent.getChild(childName);
        if (child == null) {
            throw new InvalidDataException("mandatory element '" + childName + "' is missing: " + parent);
        }
        return child;
    }

    public void writeExternal(Element element) {
        Element name = new Element(ELEMENT_NAME);
        name.setAttribute(ATTRIBUTE_VALUE, myName);
        element.addContent(name);

        if (mySdkType != null) {
            Element sdkType = new Element(ELEMENT_TYPE);
            sdkType.setAttribute(ATTRIBUTE_VALUE, mySdkType.getId());
            element.addContent(sdkType);
        }

        if (myVersionString != null) {
            Element version = new Element(ELEMENT_VERSION);
            version.setAttribute(ATTRIBUTE_VALUE, myVersionString);
            element.addContent(version);
        }

        Element home = new Element(ELEMENT_HOMEPATH);
        home.setAttribute(ATTRIBUTE_VALUE, myHomePath);
        element.addContent(home);

        if (myPlatformTargetId != null) {
            Element platformTarget = new Element(ELEMENT_PLATFORM_TARGET);
            platformTarget.setAttribute(ATTRIBUTE_VALUE, myPlatformTargetId);
            element.addContent(platformTarget);
        }

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
        boolean changes = myHomePath == null ? path != null : !myHomePath.equals(path);
        myHomePath = path;
        if (changes) {
            resetVersionString(); // clear cached value if home path changed
        }
    }

    @Override
    public void setHomeNioPath(Path path) {
        setHomePath(path.toAbsolutePath().toString());
    }

    @Override

    public SdkImpl clone() {
        SdkImpl newSdk = new SdkImpl(mySdkTable, "", mySdkType);
        copyTo(newSdk);
        return newSdk;
    }

    @Override

    public RootProvider getRootProvider() {
        return myRootProvider;
    }

    public void setPredefined(boolean predefined) {
        myPredefined = predefined;
    }

    void copyTo(SdkImpl dest) {
        String name = getName();
        dest.setName(name);
        dest.setHomePath(getHomePath());
        dest.setPredefined(myPredefined);
        dest.myPlatformTargetId = myPlatformTargetId;
        dest.myVersionDefined = myVersionDefined;
        dest.myVersionString = myVersionString;
        dest.setSdkAdditionalData(getSdkAdditionalData());
        dest.myRoots.copyRootsFrom(myRoots);
        dest.myRootProvider.rootsChanged();
    }

    private class MyRootProvider extends RootProviderBase {
        @Override
        public String[] getUrls(String rootType) {
            return myRoots.getUrls(rootType);
        }

        @Override
        public VirtualFile[] getFiles(String rootType) {
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
    public VirtualFile[] getRoots(String rootType) {
        return myRoots.getFiles(rootType);
    }

    @Override
    public String[] getUrls(String rootType) {
        return myRoots.getUrls(rootType);
    }

    @Override
    public void addRoot(VirtualFile root, String rootType) {
        myRoots.addRoot(root, rootType);
    }

    @Override
    public void addRoot(String url, String rootType) {
        myRoots.addRoot(url, rootType);
    }

    @Override
    public void removeRoot(VirtualFile root, String rootType) {
        myRoots.removeRoot(root, rootType);
    }

    @Override
    public void removeRoot(String url, String rootType) {
        myRoots.removeRoot(url, rootType);
    }

    @Override
    public void removeRoots(String rootType) {
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
