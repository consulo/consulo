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
package consulo.versionControlSystem.impl.internal.change;

import consulo.project.Project;
import consulo.util.collection.MultiMap;
import consulo.util.lang.Pair;
import consulo.versionControlSystem.AbstractVcs;
import consulo.versionControlSystem.ProjectLevelVcsManager;
import consulo.versionControlSystem.TreeDiffProvider;
import consulo.versionControlSystem.VcsConfiguration;
import consulo.versionControlSystem.base.FilePathImpl;
import consulo.versionControlSystem.change.Change;
import consulo.versionControlSystem.change.ChangesUtil;
import consulo.versionControlSystem.root.VcsRoot;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nullable;

import java.io.File;
import java.util.*;

public class RemoteRevisionsStateCache implements ChangesOnServerTracker {
  private final static long DISCRETE = 3600000;
  // true -> changed
  private final Map<String, Pair<Boolean, VcsRoot>> myChanged;

  private final MultiMap<VcsRoot, String> myQueries;
  private final Map<VcsRoot, Long> myTs;
  private final Object myLock;
  private final ProjectLevelVcsManager myVcsManager;
  private final VcsConfiguration myVcsConfiguration;

  RemoteRevisionsStateCache(Project project) {
    myVcsManager = ProjectLevelVcsManager.getInstance(project);
    myChanged = new HashMap<String, Pair<Boolean, VcsRoot>>();
    myQueries = new MultiMap<VcsRoot, String>();
    myTs = new HashMap<VcsRoot, Long>();
    myLock = new Object();
    myVcsConfiguration = VcsConfiguration.getInstance(project);
  }

  @Override
  public void invalidate(Collection<String> paths) {
    synchronized (myLock) {
      for (String path : paths) {
        myChanged.remove(path);
      }
    }
  }

  @Nullable
  private VirtualFile getRootForPath(String s) {
    return myVcsManager.getVcsRootFor(new FilePathImpl(new File(s), false));
  }
  
  @Override
  public boolean isUpToDate(Change change) {
    List<File> files = ChangesUtil.getIoFilesFromChanges(Collections.singletonList(change));
    synchronized (myLock) {
      for (File file : files) {
        String path = file.getAbsolutePath();
        Pair<Boolean, VcsRoot> data = myChanged.get(path);
        if (data != null && Boolean.TRUE.equals(data.getFirst())) return false;
      }
    }
    return true;
  }

  @Override
  public void plus(Pair<String, AbstractVcs> pair) {
    VirtualFile root = getRootForPath(pair.getFirst());
    if (root == null) return;
    synchronized (myLock) {
      myQueries.putValue(new VcsRoot(pair.getSecond(), root), pair.getFirst());
    }
  }

  @Override
  public void minus(Pair<String, AbstractVcs> pair) {
    VirtualFile root = getRootForPath(pair.getFirst());
    if (root == null) return;
    synchronized (myLock) {
      VcsRoot key = new VcsRoot(pair.getSecond(), root);
      if (myQueries.containsKey(key)) {
        myQueries.removeValue(key, pair.getFirst());
      }
      myChanged.remove(pair.getFirst());
    }
  }

  @Override
  public void directoryMappingChanged() {
    // todo will work?
    synchronized (myLock) {
      myChanged.clear();
      myTs.clear();
    }
  }

  @Override
  public boolean updateStep() {
    MultiMap<VcsRoot, String> dirty = new MultiMap<VcsRoot, String>();
    long oldPoint = System.currentTimeMillis() - (myVcsConfiguration.CHANGED_ON_SERVER_INTERVAL > 0 ?
                                                        myVcsConfiguration.CHANGED_ON_SERVER_INTERVAL * 60000 : DISCRETE);

    synchronized (myLock) {
      for (VcsRoot root : myQueries.keySet()) {
        Collection<String> collection = myQueries.get(root);
        for (String s : collection) {
          dirty.putValue(root, s);
        }
      }
      myQueries.clear();

      Set<VcsRoot> roots = new HashSet<VcsRoot>();
      for (Map.Entry<VcsRoot, Long> entry : myTs.entrySet()) {
        if (! dirty.get(entry.getKey()).isEmpty()) continue;

        Long ts = entry.getValue();
        if ((ts == null) || (oldPoint > ts)) {
          roots.add(entry.getKey());
        }
      }
      for (Map.Entry<String, Pair<Boolean, VcsRoot>> entry : myChanged.entrySet()) {
        VcsRoot vcsRoot = entry.getValue().getSecond();
        if ((! dirty.get(vcsRoot).isEmpty()) || roots.contains(vcsRoot)) {
          dirty.putValue(vcsRoot, entry.getKey());
        }
      }
    }

    if (dirty.isEmpty()) return false;

    Map<String, Pair<Boolean, VcsRoot>> results = new HashMap<String, Pair<Boolean, VcsRoot>>();
    for (VcsRoot vcsRoot : dirty.keySet()) {
      // todo - actually it means nothing since the only known VCS to use this scheme is Git and now it always allow
      // todo - background operations. when it changes, develop more flexible behavior here
      if (! vcsRoot.getVcs().isVcsBackgroundOperationsAllowed(vcsRoot.getPath())) continue;
      TreeDiffProvider provider = vcsRoot.getVcs().getTreeDiffProvider();
      if (provider == null) continue;

      Collection<String> paths = dirty.get(vcsRoot);
      Collection<String> remotelyChanged = provider.getRemotelyChanged(vcsRoot.getPath(), paths);
      for (String path : paths) {
        results.put(path, new Pair<Boolean, VcsRoot>(remotelyChanged.contains(path), vcsRoot));
      }
    }

    long curTime = System.currentTimeMillis();
    synchronized (myLock) {
      myChanged.putAll(results);
      for (VcsRoot vcsRoot : dirty.keySet()) {
        myTs.put(vcsRoot, curTime);
      }
    }

    return true;
  }
}
