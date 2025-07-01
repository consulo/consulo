/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package consulo.compiler.generic;

import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFileManager;
import consulo.index.io.data.DataExternalizer;
import consulo.index.io.data.IOUtil;
import jakarta.annotation.Nonnull;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author nik
 */
public class VirtualFileWithDependenciesState {
    public static final DataExternalizer<VirtualFileWithDependenciesState> EXTERNALIZER = new VirtualFileWithDependenciesExternalizer();
    private long mySourceTimestamp;
    private Map<String, Long> myDependencies = new HashMap<>();

    public VirtualFileWithDependenciesState(long sourceTimestamp) {
        mySourceTimestamp = sourceTimestamp;
    }

    public void addDependency(@Nonnull VirtualFile file) {
        myDependencies.put(file.getUrl(), file.getTimeStamp());
    }

    public boolean isUpToDate(@Nonnull VirtualFile sourceFile) {
        if (sourceFile.getTimeStamp() != mySourceTimestamp) {
            return false;
        }

        VirtualFileManager manager = VirtualFileManager.getInstance();
        for (Map.Entry<String, Long> entry : myDependencies.entrySet()) {
            VirtualFile file = manager.findFileByUrl(entry.getKey());
            if (file == null || file.getTimeStamp() != entry.getValue()) {
                return false;
            }
        }
        return true;
    }


    private static class VirtualFileWithDependenciesExternalizer implements DataExternalizer<VirtualFileWithDependenciesState> {
        @Override
        public void save(@Nonnull DataOutput out, VirtualFileWithDependenciesState value) throws IOException {
            out.writeLong(value.mySourceTimestamp);
            Map<String, Long> dependencies = value.myDependencies;
            out.writeInt(dependencies.size());
            for (Map.Entry<String, Long> entry : dependencies.entrySet()) {
                IOUtil.writeUTF(out, entry.getKey());
                out.writeLong(entry.getValue());
            }
        }

        @Override
        public VirtualFileWithDependenciesState read(@Nonnull DataInput in) throws IOException {
            VirtualFileWithDependenciesState state = new VirtualFileWithDependenciesState(in.readLong());
            int size = in.readInt();
            while (size-- > 0) {
                String url = IOUtil.readUTF(in);
                long timestamp = in.readLong();
                state.myDependencies.put(url, timestamp);
            }
            return state;
        }
    }
}
