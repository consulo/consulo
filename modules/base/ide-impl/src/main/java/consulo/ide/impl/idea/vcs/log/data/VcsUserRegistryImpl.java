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
package consulo.ide.impl.idea.vcs.log.data;

import consulo.annotation.component.ServiceImpl;
import consulo.ide.impl.idea.util.containers.ContainerUtil;
import consulo.index.io.Page;
import consulo.index.io.PersistentBTreeEnumerator;
import consulo.index.io.PersistentEnumeratorBase;
import consulo.versionControlSystem.log.VcsUser;
import consulo.versionControlSystem.log.VcsUserRegistry;
import consulo.ide.impl.idea.vcs.log.impl.VcsUserImpl;
import consulo.container.boot.ContainerPathManager;
import consulo.disposer.Disposable;
import consulo.index.io.KeyDescriptor;
import consulo.index.io.data.IOUtil;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.util.interner.Interner;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

@Singleton
@ServiceImpl
public class VcsUserRegistryImpl implements Disposable, VcsUserRegistry {

  private static final File USER_CACHE_APP_DIR = new File(ContainerPathManager.get().getSystemPath(), "vcs-users");
  private static final Logger LOG = Logger.getInstance(VcsUserRegistryImpl.class);
  private static final int STORAGE_VERSION = 2;
  private static final PersistentEnumeratorBase.DataFilter ACCEPT_ALL_DATA_FILTER = id -> true;

  @Nullable private final PersistentEnumeratorBase<VcsUser> myPersistentEnumerator;
  @Nonnull
  private final Interner<VcsUser> myInterner;

  @Inject
  VcsUserRegistryImpl(@Nonnull Project project) {
    final File mapFile = new File(USER_CACHE_APP_DIR, project.getLocationHash() + "." + STORAGE_VERSION);
    myPersistentEnumerator = initEnumerator(mapFile);
    myInterner = Interner.createConcurrentHashInterner();
  }

  @Nullable
  private PersistentEnumeratorBase<VcsUser> initEnumerator(@Nonnull final File mapFile) {
    try {
      return IOUtil.openCleanOrResetBroken(() -> new PersistentBTreeEnumerator<>(mapFile, new MyDescriptor(), Page.PAGE_SIZE, null,
                                                                                 STORAGE_VERSION), mapFile);
    }
    catch (IOException e) {
      LOG.warn(e);
      return null;
    }
  }

  @Nonnull
  @Override
  public VcsUser createUser(@Nonnull String name, @Nonnull String email) {
    synchronized (myInterner) {
      return myInterner.intern(new VcsUserImpl(name, email));
    }
  }

  public void addUser(@Nonnull VcsUser user) {
    try {
      if (myPersistentEnumerator != null) {
        myPersistentEnumerator.enumerate(user);
      }
    }
    catch (IOException e) {
      LOG.warn(e);
    }
  }

  public void addUsers(@Nonnull Collection<VcsUser> users) {
    for (VcsUser user : users) {
      addUser(user);
    }
  }

  @Override
  @Nonnull
  public Set<VcsUser> getUsers() {
    try {
      Collection<VcsUser> users = myPersistentEnumerator != null ?
                                  myPersistentEnumerator.getAllDataObjects(ACCEPT_ALL_DATA_FILTER) :
                                  Collections.<VcsUser>emptySet();
      return ContainerUtil.newHashSet(users);
    }
    catch (IOException e) {
      LOG.warn(e);
      return Collections.emptySet();
    }
  }

  public void flush() {
    if (myPersistentEnumerator != null) {
      myPersistentEnumerator.force();
    }
  }

  @Override
  public void dispose() {
    try {
      if (myPersistentEnumerator != null) {
        myPersistentEnumerator.close();
      }
    }
    catch (IOException e) {
      LOG.warn(e);
    }
  }

  public int getUserId(@Nonnull VcsUser user) throws IOException {
    return myPersistentEnumerator.enumerate(user);
  }

  @Nullable
  public VcsUser getUserById(Integer userId) throws IOException {
    return myPersistentEnumerator.valueOf(userId);
  }

  private class MyDescriptor implements KeyDescriptor<VcsUser> {
    @Override
    public void save(@Nonnull DataOutput out, VcsUser value) throws IOException {
      IOUtil.writeUTF(out, value.getName());
      IOUtil.writeUTF(out, value.getEmail());
    }

    @Override
    public VcsUser read(@Nonnull DataInput in) throws IOException {
      String name = IOUtil.readUTF(in);
      String email = IOUtil.readUTF(in);
      return createUser(name, email);
    }
  }
}
