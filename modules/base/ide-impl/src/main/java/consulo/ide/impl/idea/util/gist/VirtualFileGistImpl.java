/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package consulo.ide.impl.idea.util.gist;

import consulo.application.ApplicationManager;
import consulo.application.progress.ProgressManager;
import consulo.virtualFileSystem.internal.PersistentFS;
import consulo.index.io.data.DataExternalizer;
import consulo.index.io.data.DataInputOutputUtil;
import consulo.language.psi.stub.gist.GistManager;
import consulo.language.psi.stub.gist.VirtualFileGist;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.util.collection.FactoryMap;
import consulo.util.lang.Pair;
import consulo.virtualFileSystem.FileAttribute;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFileWithId;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.function.BiFunction;

/**
 * @author peter
 */
class VirtualFileGistImpl<Data> implements VirtualFileGist<Data> {
  private static final Logger LOG = Logger.getInstance(VirtualFileGistImpl.class);
  private static final int ourInternalVersion = 2;

  @Nonnull
  private final String myId;
  private final int myVersion;
  @Nonnull
  private final BiFunction<Project, VirtualFile, Data> myCalculator;
  @Nonnull
  private final DataExternalizer<Data> myExternalizer;

  VirtualFileGistImpl(@Nonnull String id, int version, @Nonnull DataExternalizer<Data> externalizer, @Nonnull BiFunction<Project, VirtualFile, Data> calcData) {
    myId = id;
    myVersion = version;
    myExternalizer = externalizer;
    myCalculator = calcData;
  }

  @Override
  public Data getFileData(@Nullable Project project, @Nonnull VirtualFile file) {
    ApplicationManager.getApplication().assertReadAccessAllowed();
    ProgressManager.checkCanceled();

    if (!(file instanceof VirtualFileWithId)) return myCalculator.apply(project, file);

    int stamp = PersistentFS.getInstance().getModificationCount(file) + ((GistManagerImpl)GistManager.getInstance()).getReindexCount();

    try (DataInputStream stream = getFileAttribute(project).readAttribute(file)) {
      if (stream != null && DataInputOutputUtil.readINT(stream) == stamp) {
        return stream.readBoolean() ? myExternalizer.read(stream) : null;
      }
    }
    catch (IOException e) {
      LOG.error(e);
    }

    Data result = myCalculator.apply(project, file);
    cacheResult(stamp, result, project, file);
    return result;
  }

  private void cacheResult(int modCount, @Nullable Data result, Project project, VirtualFile file) {
    try (DataOutputStream out = getFileAttribute(project).writeAttribute(file)) {
      DataInputOutputUtil.writeINT(out, modCount);
      out.writeBoolean(result != null);
      if (result != null) {
        myExternalizer.save(out, result);
      }
    }
    catch (IOException e) {
      LOG.error(e);
    }
  }

  private static final Map<Pair<String, Integer>, FileAttribute> ourAttributes = FactoryMap.create(key -> new FileAttribute(key.first, key.second, false));

  private FileAttribute getFileAttribute(@Nullable Project project) {
    synchronized (ourAttributes) {
      return ourAttributes.get(Pair.create(myId + (project == null ? "###noProject###" : project.getLocationHash()), myVersion + ourInternalVersion));
    }
  }
}

