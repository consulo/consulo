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
import consulo.compiler.generic.VirtualFileCompileItem;
import consulo.index.io.data.DataExternalizer;
import consulo.util.collection.SmartList;
import consulo.util.lang.Pair;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;

import java.util.List;

/**
 * @author nik
 */
public class ArtifactCompilerCompileItem extends VirtualFileCompileItem<ArtifactPackagingItemOutputState> {
    public static final DataExternalizer<ArtifactPackagingItemOutputState> OUTPUT_EXTERNALIZER = new ArtifactPackagingItemExternalizer();
    private final List<DestinationInfo> myDestinations = new SmartList<>();

    public ArtifactCompilerCompileItem(VirtualFile file) {
        super(file);
    }

    public void addDestination(DestinationInfo info) {
        myDestinations.add(info);
    }

    public List<DestinationInfo> getDestinations() {
        return myDestinations;
    }

    @Nonnull
    @Override
    public ArtifactPackagingItemOutputState computeOutputState() {
        List<Pair<String, Long>> pairs = new SmartList<>();
        for (DestinationInfo destination : myDestinations) {
            destination.update();
            VirtualFile outputFile = destination.getOutputFile();
            long timestamp = outputFile != null ? outputFile.getTimeStamp() : -1;
            pairs.add(Pair.create(destination.getOutputPath(), timestamp));
        }
        return new ArtifactPackagingItemOutputState(pairs);
    }

    @Override
    public boolean isOutputUpToDate(@Nonnull ArtifactPackagingItemOutputState state) {
        List<Pair<String, Long>> cachedDestinations = state.myDestinations;
        if (cachedDestinations.size() != myDestinations.size()) {
            return false;
        }

        for (DestinationInfo info : myDestinations) {
            VirtualFile outputFile = info.getOutputFile();
            long timestamp = outputFile != null ? outputFile.getTimeStamp() : -1;
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
}
