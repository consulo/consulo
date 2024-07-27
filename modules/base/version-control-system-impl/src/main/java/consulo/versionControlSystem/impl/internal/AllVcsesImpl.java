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
package consulo.versionControlSystem.impl.internal;

import consulo.annotation.component.ServiceImpl;
import consulo.component.extension.ExtensionPointCacheKey;
import consulo.disposer.Disposable;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.versionControlSystem.*;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

@Singleton
@ServiceImpl
public class AllVcsesImpl implements AllVcses, Disposable {
  private static final ExtensionPointCacheKey<VcsFactory, Map<String, VcsFactory>> BY_ID = ExtensionPointCacheKey.groupBy("BY_ID", VcsFactory::getId);
  private static final Logger LOG = Logger.getInstance(AllVcsesImpl.class);

  private final Map<String, AbstractVcs> myVcses;

  private final Object myLock;
  private final Project myProject;

  @Inject
  AllVcsesImpl(final Project project) {
    myProject = project;
    myVcses = new HashMap<>();
    myLock = new Object();
  }

  private void addVcs(final AbstractVcs vcs) {
    registerVcs(vcs);
    myVcses.put(vcs.getName(), vcs);
  }

  private void registerVcs(final AbstractVcs vcs) {
    try {
      vcs.loadSettings();
      vcs.doStart();
    }
    catch (VcsException e) {
      LOG.debug(e);
    }
    vcs.getProvidedStatuses();
  }

  @Override
  public AbstractVcs getByName(final String name) {
    synchronized (myLock) {
      final AbstractVcs vcs = myVcses.get(name);
      if (vcs != null) {
        return vcs;
      }
    }

    Map<String, VcsFactory> byIdMap = myProject.getExtensionPoint(VcsFactory.class).getOrBuildCache(BY_ID);
    final VcsFactory ep = byIdMap.get(name);
    if (ep == null) {
      return null;
    }

    // guarantees to always return the same vcs value
    final AbstractVcs newVcs = ep.createVcs();

    newVcs.setupEnvironments();

    synchronized (myLock) {
      if (!myVcses.containsKey(name)) {
        addVcs(newVcs);
      }
      return newVcs;
    }
  }

  @Nullable
  @Override
  public VcsDescriptor getDescriptor(String name) {
    Map<String, VcsFactory> byIdMap = myProject.getExtensionPoint(VcsFactory.class).getOrBuildCache(BY_ID);
    VcsFactory ep = byIdMap.get(name);
    return ep == null ? null : ep.createDescriptor();
  }

  @Override
  public void dispose() {
    synchronized (myLock) {
      for (AbstractVcs vcs : myVcses.values()) {
        unregisterVcs(vcs);
      }
    }
  }

  private void unregisterVcs(AbstractVcs vcs) {
    try {
      vcs.doShutdown();
    }
    catch (VcsException e) {
      LOG.info(e);
    }
  }

  @Override
  public boolean isEmpty() {
    return myProject.getExtensionPoint(VcsFactory.class).hasAnyExtensions();
  }

  @Override
  public VcsDescriptor[] getAll() {
    final List<VcsDescriptor> result = new ArrayList<>();
    myProject.getExtensionPoint(VcsFactory.class).forEachExtensionSafe(vcsFactory -> {
      result.add(vcsFactory.createDescriptor());
    });
    Collections.sort(result);
    return result.toArray(new VcsDescriptor[result.size()]);
  }

  @Nonnull
  @Override
  public Collection<AbstractVcs> getSupportedVcses() {
    ArrayList<String> names;
    synchronized (myLock) {
      names = new ArrayList<>(myVcses.keySet());
    }
    return names.stream().map(this::getByName).filter(Objects::nonNull).collect(Collectors.toList());
  }
}
