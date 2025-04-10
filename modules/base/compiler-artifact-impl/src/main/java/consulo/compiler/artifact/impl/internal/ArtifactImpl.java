/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package consulo.compiler.artifact.impl.internal;

import consulo.compiler.artifact.ArtifactProperties;
import consulo.compiler.artifact.ArtifactPropertiesProvider;
import consulo.compiler.artifact.ArtifactType;
import consulo.compiler.artifact.ModifiableArtifact;
import consulo.compiler.artifact.element.ArchivePackagingElement;
import consulo.compiler.artifact.element.CompositePackagingElement;
import consulo.compiler.artifact.event.ArtifactListener;
import consulo.proxy.EventDispatcher;
import consulo.util.dataholder.UserDataHolderBase;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * @author nik
 */
public class ArtifactImpl extends UserDataHolderBase implements ModifiableArtifact {
    private CompositePackagingElement<?> myRootElement;
    private String myName;
    private boolean myBuildOnMake;
    private String myOutputPath;
    private final EventDispatcher<ArtifactListener> myDispatcher;
    private ArtifactType myArtifactType;
    private Map<ArtifactPropertiesProvider, ArtifactProperties<?>> myProperties;

    public ArtifactImpl(
        @Nonnull String name,
        @Nonnull ArtifactType artifactType,
        boolean buildOnMake,
        @Nonnull CompositePackagingElement<?> rootElement,
        String outputPath
    ) {
        this(name, artifactType, buildOnMake, rootElement, outputPath, null);
    }

    public ArtifactImpl(
        @Nonnull String name,
        @Nonnull ArtifactType artifactType,
        boolean buildOnMake,
        @Nonnull CompositePackagingElement<?> rootElement,
        String outputPath,
        EventDispatcher<ArtifactListener> dispatcher
    ) {
        myName = name;
        myArtifactType = artifactType;
        myBuildOnMake = buildOnMake;
        myRootElement = rootElement;
        myOutputPath = outputPath;
        myDispatcher = dispatcher;
        myProperties = new HashMap<>();
        resetProperties();
    }

    private void resetProperties() {
        myProperties.clear();
        for (ArtifactPropertiesProvider provider : ArtifactPropertiesProvider.EP_NAME.getExtensionList()) {
            if (provider.isAvailableFor(myArtifactType)) {
                myProperties.put(provider, provider.createProperties(myArtifactType));
            }
        }
    }

    @Override
    @Nonnull
    public ArtifactType getArtifactType() {
        return myArtifactType;
    }

    @Override
    @Nonnull
    public String getName() {
        return myName;
    }

    @Override
    public boolean isBuildOnMake() {
        return myBuildOnMake;
    }

    @Override
    @Nonnull
    public CompositePackagingElement<?> getRootElement() {
        return myRootElement;
    }

    @Override
    public String getOutputPath() {
        return myOutputPath;
    }

    @Override
    public Collection<? extends ArtifactPropertiesProvider> getPropertiesProviders() {
        return Collections.unmodifiableCollection(myProperties.keySet());
    }

    public ArtifactImpl createCopy(EventDispatcher<ArtifactListener> dispatcher) {
        ArtifactImpl artifact = new ArtifactImpl(myName, myArtifactType, myBuildOnMake, myRootElement, myOutputPath, dispatcher);
        for (Map.Entry<ArtifactPropertiesProvider, ArtifactProperties<?>> entry : myProperties.entrySet()) {
            ArtifactProperties newProperties = artifact.myProperties.get(entry.getKey());
            //noinspection unchecked
            newProperties.loadState(entry.getValue().getState());
        }
        return artifact;
    }

    @Override
    public void setName(@Nonnull String name) {
        String oldName = myName;
        myName = name;
        if (myDispatcher != null) {
            myDispatcher.getMulticaster().artifactChanged(this, oldName);
        }
    }

    @Override
    public String toString() {
        return "artifact:" + myName;
    }

    @Override
    public void setRootElement(CompositePackagingElement<?> root) {
        myRootElement = root;
    }

    @Override
    public void setProperties(ArtifactPropertiesProvider provider, ArtifactProperties<?> properties) {
        if (properties != null) {
            myProperties.put(provider, properties);
        }
        else {
            myProperties.remove(provider);
        }
    }

    @Override
    public void setArtifactType(@Nonnull ArtifactType selected) {
        myArtifactType = selected;
        resetProperties();
    }

    @Override
    public void setBuildOnMake(boolean buildOnMake) {
        myBuildOnMake = buildOnMake;
    }

    @Override
    public void setOutputPath(String outputPath) {
        myOutputPath = outputPath;
    }

    @Override
    public ArtifactProperties<?> getProperties(@Nonnull ArtifactPropertiesProvider provider) {
        return myProperties.get(provider);
    }

    @Override
    public VirtualFile getOutputFile() {
        String filePath = getOutputFilePath();
        return !StringUtil.isEmpty(filePath) ? LocalFileSystem.getInstance().findFileByPath(filePath) : null;
    }

    @Override
    public String getOutputFilePath() {
        if (StringUtil.isEmpty(myOutputPath)) {
            return null;
        }

        String filePath;
        if (myRootElement instanceof ArchivePackagingElement archivePackagingElement) {
            filePath = myOutputPath + "/" + archivePackagingElement.getArchiveFileName();
        }
        else {
            filePath = myOutputPath;
        }
        return filePath;
    }

    @Nullable
    public String getOutputDirectoryPathToCleanOnRebuild() {
        if (myRootElement instanceof ArchivePackagingElement || StringUtil.isEmpty(myOutputPath)) {
            return null;
        }
        return myOutputPath;
    }

    public void copyFrom(ArtifactImpl modified) {
        myName = modified.getName();
        myOutputPath = modified.getOutputPath();
        myBuildOnMake = modified.isBuildOnMake();
        myRootElement = modified.getRootElement();
        myProperties = modified.myProperties;
        myArtifactType = modified.getArtifactType();
    }
}
