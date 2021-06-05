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
package com.intellij.vcs.log.data;

import consulo.disposer.Disposable;
import consulo.logging.Logger;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Condition;
import consulo.disposer.Disposer;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.CommonProcessors;
import com.intellij.util.io.IOUtil;
import com.intellij.util.io.KeyDescriptor;
import com.intellij.util.io.PersistentEnumeratorBase;
import com.intellij.vcs.log.*;
import com.intellij.vcs.log.impl.FatalErrorHandler;
import com.intellij.vcs.log.impl.HashImpl;
import com.intellij.vcs.log.impl.VcsRefImpl;
import com.intellij.vcs.log.util.PersistentUtil;
import consulo.util.collection.primitive.objects.ObjectIntMap;
import consulo.util.collection.primitive.objects.ObjectMaps;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Supports the int <-> Hash and int <-> VcsRef persistent mappings.
 */
public class VcsLogStorageImpl implements Disposable, VcsLogStorage {
  @Nonnull
  private static final Logger LOG = Logger.getInstance(VcsLogStorage.class);
  @Nonnull
  private static final String HASHES_STORAGE = "hashes";
  @Nonnull
  private static final String REFS_STORAGE = "refs";
  @Nonnull
  public static final VcsLogStorage EMPTY = new EmptyLogStorage();

  public static final int VERSION = 5;
  private static final int REFS_VERSION = 1;
  @Nonnull
  private static final String ROOT_STORAGE_KIND = "roots";

  public static final int NO_INDEX = -1;

  @Nonnull
  private final PersistentEnumeratorBase<CommitId> myCommitIdEnumerator;
  @Nonnull
  private final PersistentEnumeratorBase<VcsRef> myRefsEnumerator;
  @Nonnull
  private final FatalErrorHandler myExceptionReporter;
  private volatile boolean myDisposed = false;

  public VcsLogStorageImpl(@Nonnull Project project,
                           @Nonnull Map<VirtualFile, VcsLogProvider> logProviders,
                           @Nonnull FatalErrorHandler exceptionReporter,
                           @Nonnull Disposable parent) throws IOException {
    myExceptionReporter = exceptionReporter;

    List<VirtualFile> roots =
      logProviders.keySet().stream().sorted((o1, o2) -> o1.getPath().compareTo(o2.getPath())).collect(Collectors.toList());

    String logId = PersistentUtil.calcLogId(project, logProviders);
    MyCommitIdKeyDescriptor commitIdKeyDescriptor = new MyCommitIdKeyDescriptor(roots);
    myCommitIdEnumerator = PersistentUtil.createPersistentEnumerator(commitIdKeyDescriptor, HASHES_STORAGE, logId, VERSION);
    myRefsEnumerator =
            PersistentUtil.createPersistentEnumerator(new VcsRefKeyDescriptor(logProviders, commitIdKeyDescriptor), REFS_STORAGE, logId,
                                                      VERSION + REFS_VERSION);

    // cleanup old root storages, to remove after 2016.3 release
    PersistentUtil
            .cleanupOldStorageFile(ROOT_STORAGE_KIND, project.getName() + "." + project.getBaseDir().getPath().hashCode());

    Disposer.register(parent, this);
  }

  @Nullable
  private CommitId doGetCommitId(int index) throws IOException {
    return myCommitIdEnumerator.valueOf(index);
  }

  private int getOrPut(@Nonnull Hash hash, @Nonnull VirtualFile root) throws IOException {
    return myCommitIdEnumerator.enumerate(new CommitId(hash, root));
  }

  @Override
  public int getCommitIndex(@Nonnull Hash hash, @Nonnull VirtualFile root) {
    checkDisposed();
    try {
      return getOrPut(hash, root);
    }
    catch (IOException e) {
      myExceptionReporter.consume(this, e);
    }
    return NO_INDEX;
  }

  @Override
  @Nullable
  public CommitId getCommitId(int commitIndex) {
    checkDisposed();
    try {
      CommitId commitId = doGetCommitId(commitIndex);
      if (commitId == null) {
        myExceptionReporter.consume(this, new RuntimeException("Unknown commit index: " + commitIndex));
      }
      return commitId;
    }
    catch (IOException e) {
      myExceptionReporter.consume(this, e);
    }
    return null;
  }

  @Override
  @Nullable
  public CommitId findCommitId(@Nonnull final Condition<CommitId> condition) {
    checkDisposed();
    try {
      final Ref<CommitId> hashRef = Ref.create();
      myCommitIdEnumerator.iterateData(new CommonProcessors.FindProcessor<CommitId>() {
        @Override
        protected boolean accept(CommitId commitId) {
          boolean matches = condition.value(commitId);
          if (matches) {
            hashRef.set(commitId);
          }
          return matches;
        }
      });
      return hashRef.get();
    }
    catch (IOException e) {
      myExceptionReporter.consume(this, e);
      return null;
    }
  }

  @Override
  public int getRefIndex(@Nonnull VcsRef ref) {
    checkDisposed();
    try {
      return myRefsEnumerator.enumerate(ref);
    }
    catch (IOException e) {
      myExceptionReporter.consume(this, e);
    }
    return NO_INDEX;
  }

  @Nullable
  @Override
  public VcsRef getVcsRef(int refIndex) {
    checkDisposed();
    try {
      return myRefsEnumerator.valueOf(refIndex);
    }
    catch (IOException e) {
      myExceptionReporter.consume(this, e);
      return null;
    }
  }

  public void flush() {
    checkDisposed();
    myCommitIdEnumerator.force();
    myRefsEnumerator.force();
  }

  @Override
  public void dispose() {
    try {
      myDisposed = true;
      myCommitIdEnumerator.close();
      myRefsEnumerator.close();
    }
    catch (IOException e) {
      LOG.warn(e);
    }
  }

  private void checkDisposed() {
    if (myDisposed) throw new ProcessCanceledException();
  }

  private static class MyCommitIdKeyDescriptor implements KeyDescriptor<CommitId> {
    @Nonnull
    private final List<VirtualFile> myRoots;
    @Nonnull
    private final ObjectIntMap<VirtualFile> myRootsReversed;

    public MyCommitIdKeyDescriptor(@Nonnull List<VirtualFile> roots) {
      myRoots = roots;

      myRootsReversed = ObjectMaps.newObjectIntHashMap();
      for (int i = 0; i < roots.size(); i++) {
        myRootsReversed.putInt(roots.get(i), i);
      }
    }

    @Override
    public void save(@Nonnull DataOutput out, CommitId value) throws IOException {
      ((HashImpl)value.getHash()).write(out);
      out.writeInt(myRootsReversed.getInt(value.getRoot()));
    }

    @Override
    public CommitId read(@Nonnull DataInput in) throws IOException {
      Hash hash = HashImpl.read(in);
      VirtualFile root = myRoots.get(in.readInt());
      if (root == null) return null;
      return new CommitId(hash, root);
    }
  }

  private static class EmptyLogStorage implements VcsLogStorage {
    @Override
    public int getCommitIndex(@Nonnull Hash hash, @Nonnull VirtualFile root) {
      return 0;
    }

    @Nonnull
    @Override
    public CommitId getCommitId(int commitIndex) {
      throw new UnsupportedOperationException("Illegal access to empty hash map by index " + commitIndex);
    }

    @Nullable
    @Override
    public CommitId findCommitId(@Nonnull Condition<CommitId> string) {
      return null;
    }

    @Override
    public int getRefIndex(@Nonnull VcsRef ref) {
      return 0;
    }

    @Nullable
    @Override
    public VcsRef getVcsRef(int refIndex) {
      throw new UnsupportedOperationException("Illegal access to empty ref map by index " + refIndex);
    }

    @Override
    public void flush() {
    }
  }

  private static class VcsRefKeyDescriptor implements KeyDescriptor<VcsRef> {
    @Nonnull
    private final Map<VirtualFile, VcsLogProvider> myLogProviders;
    @Nonnull
    private final KeyDescriptor<CommitId> myCommitIdKeyDescriptor;

    public VcsRefKeyDescriptor(@Nonnull Map<VirtualFile, VcsLogProvider> logProviders,
                               @Nonnull KeyDescriptor<CommitId> commitIdKeyDescriptor) {
      myLogProviders = logProviders;
      myCommitIdKeyDescriptor = commitIdKeyDescriptor;
    }

    @Override
    public void save(@Nonnull DataOutput out, @Nonnull VcsRef value) throws IOException {
      myCommitIdKeyDescriptor.save(out, new CommitId(value.getCommitHash(), value.getRoot()));
      IOUtil.writeUTF(out, value.getName());
      myLogProviders.get(value.getRoot()).getReferenceManager().serialize(out, value.getType());
    }

    @Override
    public VcsRef read(@Nonnull DataInput in) throws IOException {
      CommitId commitId = myCommitIdKeyDescriptor.read(in);
      if (commitId == null) throw new IOException("Can not read commit id for reference");
      String name = IOUtil.readUTF(in);
      VcsRefType type = myLogProviders.get(commitId.getRoot()).getReferenceManager().deserialize(in);
      return new VcsRefImpl(commitId.getHash(), name, type, commitId.getRoot());
    }
  }
}
