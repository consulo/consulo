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
package com.intellij.compiler.impl;

import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.vfs.newvfs.FileAttribute;
import com.intellij.util.indexing.FileBasedIndex;
import com.intellij.util.io.DataInputOutputUtil;
import consulo.logging.Logger;
import gnu.trove.TIntHashSet;
import gnu.trove.TIntLongHashMap;
import gnu.trove.TIntObjectHashMap;
import gnu.trove.TIntProcedure;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.*;

public class TranslationSourceFileInfo {
  private static final Logger LOG = Logger.getInstance(TranslationSourceFileInfo.class);

  private static final FileAttribute ourSourceFileAttribute = new FileAttribute("_make_source_file_info_", 4, false);

  @Nullable
  public static TranslationSourceFileInfo loadSourceInfo(final VirtualFile file) {
    try {
      final DataInputStream is = ourSourceFileAttribute.readAttribute(file);
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
      final Throwable cause = e.getCause();
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


  private TIntLongHashMap myTimestamps; // ProjectId -> last compiled stamp
  private TIntObjectHashMap<Serializable> myProjectToOutputPathMap; // ProjectId -> either a single output path or a set of output paths

  TranslationSourceFileInfo() {
  }

  TranslationSourceFileInfo(@Nonnull DataInput in) throws IOException {
    final int projCount = DataInputOutputUtil.readINT(in);
    for (int idx = 0; idx < projCount; idx++) {
      final int projectId = DataInputOutputUtil.readINT(in);
      final long stamp = DataInputOutputUtil.readTIME(in);
      updateTimestamp(projectId, stamp);

      final int pathsCount = DataInputOutputUtil.readINT(in);
      for (int i = 0; i < pathsCount; i++) {
        final int path = in.readInt();
        addOutputPath(projectId, path);
      }
    }
  }

  public void save(@Nonnull final DataOutput out) throws IOException {
    final int[] projects = getProjectIds().toArray();
    DataInputOutputUtil.writeINT(out, projects.length);
    for (int projectId : projects) {
      DataInputOutputUtil.writeINT(out, projectId);
      DataInputOutputUtil.writeTIME(out, getTimestamp(projectId));
      final Object value = myProjectToOutputPathMap != null ? myProjectToOutputPathMap.get(projectId) : null;
      if (value instanceof Integer) {
        DataInputOutputUtil.writeINT(out, 1);
        out.writeInt(((Integer)value).intValue());
      }
      else if (value instanceof TIntHashSet) {
        final TIntHashSet set = (TIntHashSet)value;
        DataInputOutputUtil.writeINT(out, set.size());
        final IOException[] ex = new IOException[]{null};
        set.forEach(new TIntProcedure() {
          @Override
          public boolean execute(final int value) {
            try {
              out.writeInt(value);
              return true;
            }
            catch (IOException e) {
              ex[0] = e;
              return false;
            }
          }
        });
        if (ex[0] != null) {
          throw ex[0];
        }
      }
      else {
        DataInputOutputUtil.writeINT(out, 0);
      }
    }
  }

  public void updateTimestamp(final int projectId, final long stamp) {
    if (stamp > 0L) {
      if (myTimestamps == null) {
        myTimestamps = new TIntLongHashMap(1, 0.98f);
      }
      myTimestamps.put(projectId, stamp);
    }
    else {
      if (myTimestamps != null) {
        myTimestamps.remove(projectId);
      }
    }
  }

  public TIntHashSet getProjectIds() {
    final TIntHashSet result = new TIntHashSet();
    if (myTimestamps != null) {
      result.addAll(myTimestamps.keys());
    }
    if (myProjectToOutputPathMap != null) {
      result.addAll(myProjectToOutputPathMap.keys());
    }
    return result;
  }

  public void addOutputPath(final int projectId, @Nonnull VirtualFile outputPath) {
    addOutputPath(projectId, FileBasedIndex.getFileId(outputPath));
  }

  private void addOutputPath(final int projectId, final int outputPath) {
    if (myProjectToOutputPathMap == null) {
      myProjectToOutputPathMap = new TIntObjectHashMap<>(1, 0.98f);
      myProjectToOutputPathMap.put(projectId, outputPath);
    }
    else {
      final Object val = myProjectToOutputPathMap.get(projectId);
      if (val == null) {
        myProjectToOutputPathMap.put(projectId, outputPath);
      }
      else {
        TIntHashSet set;
        if (val instanceof Integer) {
          set = new TIntHashSet();
          set.add(((Integer)val).intValue());
          myProjectToOutputPathMap.put(projectId, set);
        }
        else {
          assert val instanceof TIntHashSet;
          set = (TIntHashSet)val;
        }
        set.add(outputPath);
      }
    }
  }

  public boolean clearPaths(final int projectId) {
    if (myProjectToOutputPathMap != null) {
      final Serializable removed = myProjectToOutputPathMap.remove(projectId);
      return removed != null;
    }
    return false;
  }

  long getTimestamp(final int projectId) {
    return myTimestamps == null ? -1L : myTimestamps.get(projectId);
  }

  public void processOutputPaths(final int projectId, final TranslatingCompilerFilesMonitorImpl.Proc proc) {
    if (myProjectToOutputPathMap != null) {
      final Object val = myProjectToOutputPathMap.get(projectId);
      if (val instanceof Integer) {
        proc.execute(projectId, VirtualFileManager.getInstance().findFileById((Integer)val));
      }
      else if (val instanceof TIntHashSet) {
        ((TIntHashSet)val).forEach(value -> {
          proc.execute(projectId, VirtualFileManager.getInstance().findFileById(value));
          return true;
        });
      }
    }
  }

  public boolean isAssociated(int projectId, String outputPath) {
    if (myProjectToOutputPathMap != null) {
      final Object val = myProjectToOutputPathMap.get(projectId);
      if (val instanceof Integer) {
        VirtualFile fileById = VirtualFileManager.getInstance().findFileById((Integer)val);
        return FileUtil.pathsEqual(outputPath, fileById != null ? fileById.getPath() : "");
      }
      if (val instanceof TIntHashSet) {
        VirtualFile vf = LocalFileSystem.getInstance().findFileByPath(outputPath);
        int _outputPath = vf == null ? -1 : FileBasedIndex.getFileId(vf);
        return ((TIntHashSet)val).contains(_outputPath);
      }
    }
    return false;
  }
}
