/*
 * Copyright 2000-2010 JetBrains s.r.o.
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
package consulo.compiler.impl.internal.artifact;

import consulo.compiler.CompileContext;
import consulo.compiler.artifact.element.ArchivePackageInfo;
import consulo.compiler.artifact.element.ArtifactIncrementalCompilerContext;
import consulo.compiler.artifact.element.DestinationInfo;
import consulo.compiler.artifact.element.ExplodedDestinationInfo;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * @author nik
 */
public class ArtifactsProcessingItemsBuilderContext implements ArtifactIncrementalCompilerContext {
    protected final Map<VirtualFile, ArtifactCompilerCompileItem> myItemsBySource;
    private final Map<String, VirtualFile> mySourceByOutput;
    private final Map<String, ArchivePackageInfo> myJarByPath;
    private final CompileContext myCompileContext;
    private final boolean myPrintToLog;

    public ArtifactsProcessingItemsBuilderContext(CompileContext compileContext) {
        myCompileContext = compileContext;
        myItemsBySource = new HashMap<>();
        mySourceByOutput = new HashMap<>();
        myJarByPath = new HashMap<>();
        myPrintToLog = ArtifactsCompilerInstance.FULL_LOG.isDebugEnabled();
    }

    public boolean addDestination(@Nonnull VirtualFile sourceFile, @Nonnull DestinationInfo destinationInfo) {
        if (destinationInfo instanceof ExplodedDestinationInfo && sourceFile.equals(destinationInfo.getOutputFile())) {
            return false;
        }

        if (checkOutputPath(destinationInfo.getOutputPath(), sourceFile)) {
            if (myPrintToLog) {
                ArtifactsCompilerInstance.FULL_LOG.debug("  " + sourceFile.getPath() + " -> " + destinationInfo);
            }
            getOrCreateProcessingItem(sourceFile).addDestination(destinationInfo);
            return true;
        }
        return false;
    }

    public Collection<ArtifactCompilerCompileItem> getProcessingItems() {
        return myItemsBySource.values();
    }

    public boolean checkOutputPath(final String outputPath, final VirtualFile sourceFile) {
        VirtualFile old = mySourceByOutput.get(outputPath);
        if (old == null) {
            mySourceByOutput.put(outputPath, sourceFile);
            return true;
        }
        //todo[nik] show warning?
        return false;
    }

    public ArtifactCompilerCompileItem getItemBySource(VirtualFile source) {
        return myItemsBySource.get(source);
    }

    public boolean registerJarFile(@Nonnull ArchivePackageInfo archivePackageInfo, @Nonnull String outputPath) {
        if (mySourceByOutput.containsKey(outputPath) || myJarByPath.containsKey(outputPath)) {
            return false;
        }
        myJarByPath.put(outputPath, archivePackageInfo);
        return true;
    }

    @jakarta.annotation.Nullable
    public ArchivePackageInfo getJarInfo(String outputPath) {
        return myJarByPath.get(outputPath);
    }

    @jakarta.annotation.Nullable
    public VirtualFile getSourceByOutput(String outputPath) {
        return mySourceByOutput.get(outputPath);
    }

    public CompileContext getCompileContext() {
        return myCompileContext;
    }

    public ArtifactCompilerCompileItem getOrCreateProcessingItem(VirtualFile sourceFile) {
        ArtifactCompilerCompileItem item = myItemsBySource.get(sourceFile);
        if (item == null) {
            item = new ArtifactCompilerCompileItem(sourceFile);
            myItemsBySource.put(sourceFile, item);
        }
        return item;
    }
}
