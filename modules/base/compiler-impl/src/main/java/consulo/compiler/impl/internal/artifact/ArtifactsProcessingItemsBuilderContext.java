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
import consulo.compiler.impl.internal.CompilerPathsEx;
import consulo.util.collection.Maps;
import consulo.util.io.FileUtil;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author nik
 */
public class ArtifactsProcessingItemsBuilderContext implements ArtifactIncrementalCompilerContext {
    protected final Map<String, ArtifactCompilerCompileItem> myItemsBySource;
    private final Map<String, String> mySourceByOutput;
    private final Map<String, ArchivePackageInfo> myJarByPath;
    private final CompileContext myCompileContext;
    private final boolean myPrintToLog;
    private @Nullable Set<String> myExcludedPaths;

    public ArtifactsProcessingItemsBuilderContext(CompileContext compileContext) {
        myCompileContext = compileContext;
        myItemsBySource = Maps.newHashMap(FileUtil.PATH_HASHING_STRATEGY);
        mySourceByOutput = Maps.newHashMap(FileUtil.PATH_HASHING_STRATEGY);
        myJarByPath = new HashMap<>();
        myPrintToLog = ArtifactsCompilerInstance.FULL_LOG.isDebugEnabled();
    }

    public boolean addDestination(String sourcePath, DestinationInfo destinationInfo) {
        if (destinationInfo instanceof ExplodedDestinationInfo && FileUtil.pathsEqual(sourcePath, destinationInfo.getOutputPath())) {
            return false;
        }

        if (checkOutputPath(destinationInfo.getOutputPath(), sourcePath)) {
            if (myPrintToLog) {
                ArtifactsCompilerInstance.FULL_LOG.debug("  " + sourcePath + " -> " + destinationInfo);
            }
            getOrCreateProcessingItem(sourcePath).addDestination(destinationInfo);
            return true;
        }
        return false;
    }

    public Collection<ArtifactCompilerCompileItem> getProcessingItems() {
        return myItemsBySource.values();
    }

    public boolean checkOutputPath(String outputPath, String sourcePath) {
        String old = mySourceByOutput.get(outputPath);
        if (old == null) {
            mySourceByOutput.put(outputPath, sourcePath);
            return true;
        }
        //todo[nik] show warning?
        return false;
    }

    public ArtifactCompilerCompileItem getItemBySource(String sourcePath) {
        return myItemsBySource.get(sourcePath);
    }

    public boolean registerJarFile(ArchivePackageInfo archivePackageInfo, String outputPath) {
        if (mySourceByOutput.containsKey(outputPath) || myJarByPath.containsKey(outputPath)) {
            return false;
        }
        myJarByPath.put(outputPath, archivePackageInfo);
        return true;
    }

    public @Nullable ArchivePackageInfo getJarInfo(String outputPath) {
        return myJarByPath.get(outputPath);
    }

    public @Nullable String getSourceByOutput(String outputPath) {
        return mySourceByOutput.get(outputPath);
    }

    public CompileContext getCompileContext() {
        return myCompileContext;
    }

    public Set<String> getExcludedPaths() {
        if (myExcludedPaths == null) {
            myExcludedPaths = CompilerPathsEx.getExcludedPaths(myCompileContext.getProject());
        }
        return myExcludedPaths;
    }

    public ArtifactCompilerCompileItem getOrCreateProcessingItem(String sourcePath) {
        ArtifactCompilerCompileItem item = myItemsBySource.get(sourcePath);
        if (item == null) {
            item = new ArtifactCompilerCompileItem(sourcePath);
            myItemsBySource.put(sourcePath, item);
        }
        return item;
    }
}
