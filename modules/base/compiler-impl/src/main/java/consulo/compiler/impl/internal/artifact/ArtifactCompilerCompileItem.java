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

import consulo.compiler.artifact.element.DestinationInfo;
import consulo.compiler.generic.CompileItem;
import consulo.compiler.generic.VirtualFilePersistentState;
import consulo.compiler.util.CompilerUtil;
import consulo.index.io.data.DataExternalizer;
import consulo.util.collection.SmartList;
import consulo.util.io.FileUtil;
import consulo.util.io.URLUtil;
import consulo.util.lang.Pair;

import java.nio.file.Path;
import java.util.List;

/**
 * @author nik
 */
public class ArtifactCompilerCompileItem extends CompileItem<String, VirtualFilePersistentState, ArtifactPackagingItemOutputState> {
    public static final DataExternalizer<ArtifactPackagingItemOutputState> OUTPUT_EXTERNALIZER = new ArtifactPackagingItemExternalizer();
    private final String mySourcePath;
    private final List<DestinationInfo> myDestinations = new SmartList<>();

    public ArtifactCompilerCompileItem(String sourcePath) {
        mySourcePath = sourcePath;
    }

    public String getSourcePath() {
        return mySourcePath;
    }

    public void addDestination(DestinationInfo info) {
        myDestinations.add(info);
    }

    public List<DestinationInfo> getDestinations() {
        return myDestinations;
    }

    @Override
    public VirtualFilePersistentState computeSourceState() {
        return new VirtualFilePersistentState(sourceTimestamp());
    }

    @Override
    public boolean isSourceUpToDate(VirtualFilePersistentState state) {
        return sourceTimestamp() == state.getSourceTimestamp();
    }

    private long sourceTimestamp() {
        int archiveSeparatorIndex = mySourcePath.indexOf(URLUtil.ARCHIVE_SEPARATOR);
        String filePath = archiveSeparatorIndex == -1 ? mySourcePath : mySourcePath.substring(0, archiveSeparatorIndex);
        return CompilerUtil.lastModified(Path.of(FileUtil.toSystemDependentName(filePath)));
    }

    @Override
    public String getKey() {
        return mySourcePath;
    }

    @Override
    public ArtifactPackagingItemOutputState computeOutputState() {
        List<Pair<String, Long>> pairs = new SmartList<>();
        for (DestinationInfo destination : myDestinations) {
            long timestamp = outputTimestamp(destination);
            pairs.add(Pair.create(destination.getOutputPath(), timestamp));
        }
        return new ArtifactPackagingItemOutputState(pairs);
    }

    @Override
    public boolean isOutputUpToDate(ArtifactPackagingItemOutputState state) {
        List<Pair<String, Long>> cachedDestinations = state.myDestinations;
        if (cachedDestinations.size() != myDestinations.size()) {
            return false;
        }

        for (DestinationInfo info : myDestinations) {
            long timestamp = outputTimestamp(info);
            String path = info.getOutputPath();
            boolean found = false;
            //todo[nik] use map if list contains many items
            for (Pair<String, Long> cachedDestination : cachedDestinations) {
                if (cachedDestination.first.equals(path)) {
                    if (cachedDestination.second != timestamp) {
                        return false;
                    }
                    found = true;
                    break;
                }
            }
            if (!found) {
                return false;
            }
        }

        return true;
    }

    private static long outputTimestamp(DestinationInfo destination) {
        return CompilerUtil.lastModified(Path.of(FileUtil.toSystemDependentName(destination.getOutputFilePath())));
    }
}
