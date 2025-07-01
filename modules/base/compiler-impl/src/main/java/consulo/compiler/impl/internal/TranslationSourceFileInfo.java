/*
 * Copyright 2013-2019 consulo.io
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
package consulo.compiler.impl.internal;

import consulo.index.io.data.DataInputOutputUtil;
import consulo.language.psi.stub.FileBasedIndex;
import consulo.logging.Logger;
import consulo.util.collection.primitive.ints.IntSet;
import consulo.util.collection.primitive.ints.IntSets;
import consulo.util.io.FileUtil;
import consulo.virtualFileSystem.FileAttribute;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFileManager;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.io.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class TranslationSourceFileInfo {
    private static final Logger LOG = Logger.getInstance(TranslationSourceFileInfo.class);

    private static final FileAttribute ourSourceFileAttribute = new FileAttribute("_make_source_file_info_", 4, false);

    @Nullable
    public static TranslationSourceFileInfo loadSourceInfo(VirtualFile file) {
        try {
            DataInputStream is = ourSourceFileAttribute.readAttribute(file);
            if (is != null) {
                try {
                    return new TranslationSourceFileInfo(is);
                }
                finally {
                    is.close();
                }
            }
        }
        catch (RuntimeException e) {
            Throwable cause = e.getCause();
            if (cause instanceof IOException) {
                LOG.info(e); // ignore IOExceptions
            }
            else {
                throw e;
            }
        }
        catch (IOException ignored) {
            LOG.info(ignored);
        }
        return null;
    }

    public static void removeSourceInfo(VirtualFile file) {
        saveSourceInfo(file, new TranslationSourceFileInfo());
    }

    public static void saveSourceInfo(VirtualFile file, TranslationSourceFileInfo descriptor) {
        try {
            try (DataOutputStream out = ourSourceFileAttribute.writeAttribute(file)) {
                descriptor.save(out);
            }
        }
        catch (IOException ignored) {
            LOG.info(ignored);
        }
    }


    private Map<Integer, Long> myTimestamps; // ProjectId -> last compiled stamp
    private Map<Integer, Object> myProjectToOutputPathMap; // ProjectId -> either a single output path or a set of output paths

    TranslationSourceFileInfo() {
    }

    TranslationSourceFileInfo(@Nonnull DataInput in) throws IOException {
        int projCount = DataInputOutputUtil.readINT(in);
        for (int idx = 0; idx < projCount; idx++) {
            int projectId = DataInputOutputUtil.readINT(in);
            long stamp = DataInputOutputUtil.readTIME(in);
            updateTimestamp(projectId, stamp);

            int pathsCount = DataInputOutputUtil.readINT(in);
            for (int i = 0; i < pathsCount; i++) {
                int path = in.readInt();
                addOutputPath(projectId, path);
            }
        }
    }

    public void save(@Nonnull DataOutput out) throws IOException {
        Integer[] projects = getProjectIds().toArray(Integer[]::new);
        DataInputOutputUtil.writeINT(out, projects.length);
        for (int projectId : projects) {
            DataInputOutputUtil.writeINT(out, projectId);
            DataInputOutputUtil.writeTIME(out, getTimestamp(projectId));
            Object value = myProjectToOutputPathMap != null ? myProjectToOutputPathMap.get(projectId) : null;
            if (value instanceof Integer intValue) {
                DataInputOutputUtil.writeINT(out, 1);
                out.writeInt(intValue);
            }
            else if (value instanceof IntSet set) {
                DataInputOutputUtil.writeINT(out, set.size());
                set.forEach(t -> {
                    try {
                        out.writeInt(t);
                    }
                    catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
            }
            else {
                DataInputOutputUtil.writeINT(out, 0);
            }
        }
    }

    public void updateTimestamp(int projectId, long stamp) {
        if (stamp > 0L) {
            if (myTimestamps == null) {
                myTimestamps = new HashMap<>(1, 0.98f);
            }
            myTimestamps.put(projectId, stamp);
        }
        else {
            if (myTimestamps != null) {
                myTimestamps.remove(projectId);
            }
        }
    }

    public Set<Integer> getProjectIds() {
        Set<Integer> result = new HashSet<>();
        if (myTimestamps != null) {
            result.addAll(myTimestamps.keySet());
        }
        if (myProjectToOutputPathMap != null) {
            result.addAll(myProjectToOutputPathMap.keySet());
        }
        return result;
    }

    public void addOutputPath(int projectId, @Nonnull VirtualFile outputPath) {
        addOutputPath(projectId, FileBasedIndex.getFileId(outputPath));
    }

    private void addOutputPath(int projectId, int outputPath) {
        if (myProjectToOutputPathMap == null) {
            myProjectToOutputPathMap = new HashMap<>(1, 0.98f);
            myProjectToOutputPathMap.put(projectId, outputPath);
        }
        else {
            Object val = myProjectToOutputPathMap.get(projectId);
            if (val == null) {
                myProjectToOutputPathMap.put(projectId, outputPath);
            }
            else {
                IntSet set;
                if (val instanceof Integer intVal) {
                    set = IntSets.newHashSet();
                    set.add(intVal);
                    myProjectToOutputPathMap.put(projectId, set);
                }
                else {
                    assert val instanceof IntSet;
                    set = (IntSet) val;
                }
                set.add(outputPath);
            }
        }
    }

    public boolean clearPaths(int projectId) {
        if (myProjectToOutputPathMap != null) {
            Object removed = myProjectToOutputPathMap.remove(projectId);
            return removed != null;
        }
        return false;
    }

    long getTimestamp(int projectId) {
        return myTimestamps == null ? -1L : myTimestamps.getOrDefault(projectId, 0L);
    }

    public void processOutputPaths(int projectId, TranslatingCompilerFilesMonitorImpl.Proc proc) {
        if (myProjectToOutputPathMap != null) {
            Object val = myProjectToOutputPathMap.get(projectId);
            VirtualFileManager virtualFileManager = VirtualFileManager.getInstance();
            if (val instanceof Integer intVal) {
                proc.execute(projectId, virtualFileManager.findFileById(intVal));
            }
            else if (val instanceof IntSet intSet) {
                intSet.forEach(value -> proc.execute(projectId, virtualFileManager.findFileById(value)));
            }
        }
    }

    public boolean isAssociated(int projectId, String outputPath) {
        if (myProjectToOutputPathMap != null) {
            Object val = myProjectToOutputPathMap.get(projectId);
            if (val instanceof Integer intVal) {
                VirtualFile fileById = VirtualFileManager.getInstance().findFileById(intVal);
                return FileUtil.pathsEqual(outputPath, fileById != null ? fileById.getPath() : "");
            }
            if (val instanceof IntSet intSet) {
                VirtualFile vf = LocalFileSystem.getInstance().findFileByPath(outputPath);
                int _outputPath = vf == null ? -1 : FileBasedIndex.getFileId(vf);
                return intSet.contains(_outputPath);
            }
        }
        return false;
    }
}
