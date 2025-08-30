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
package consulo.versionControlSystem.log.impl.internal.data.index;

import consulo.disposer.Disposable;
import consulo.index.io.DataIndexer;
import consulo.index.io.StorageException;
import consulo.index.io.VoidDataExternalizer;
import consulo.logging.Logger;
import consulo.util.collection.primitive.ints.IntSet;
import consulo.versionControlSystem.log.VcsFullCommitDetails;
import consulo.versionControlSystem.log.VcsUser;
import consulo.versionControlSystem.log.impl.internal.FatalErrorHandler;
import consulo.versionControlSystem.log.impl.internal.data.VcsUserRegistryImpl;
import jakarta.annotation.Nonnull;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import static consulo.versionControlSystem.log.impl.internal.data.index.VcsLogPersistentIndex.getVersion;

public class VcsLogUserIndex extends VcsLogFullDetailsIndex<Void> {
  private static final Logger LOG = Logger.getInstance(VcsLogUserIndex.class);
  public static final String USERS = "users";
  @Nonnull
  private final VcsUserRegistryImpl myUserRegistry;

  public VcsLogUserIndex(@Nonnull String logId,
                         @Nonnull VcsUserRegistryImpl userRegistry,
                         @Nonnull FatalErrorHandler consumer,
                         @Nonnull Disposable disposableParent) throws IOException {
    super(logId, USERS, getVersion(), new UserIndexer(userRegistry), VoidDataExternalizer.INSTANCE,
          consumer, disposableParent);
    myUserRegistry = userRegistry;
    ((UserIndexer)myIndexer).setFatalErrorConsumer(e -> consumer.consume(this, e));
  }

  public IntSet getCommitsForUsers(@Nonnull Set<VcsUser> users) throws IOException, StorageException {
    Set<Integer> ids = new HashSet<>();
    for (VcsUser user : users) {
      ids.add(myUserRegistry.getUserId(user));
    }
    return getCommitsWithAnyKey(ids);
  }

  private static class UserIndexer implements DataIndexer<Integer, Void, VcsFullCommitDetails> {
    @Nonnull
    private final VcsUserRegistryImpl myRegistry;
    @Nonnull
    private Consumer<Exception> myFatalErrorConsumer = LOG::error;

    public UserIndexer(@Nonnull VcsUserRegistryImpl registry) {
      myRegistry = registry;
    }

    @Nonnull
    @Override
    public Map<Integer, Void> map(@Nonnull VcsFullCommitDetails inputData) {
      Map<Integer, Void> result = new HashMap<>();

      try {
        result.put(myRegistry.getUserId(inputData.getAuthor()), null);
      }
      catch (IOException e) {
        myFatalErrorConsumer.accept(e);
      }

      return result;
    }

    public void setFatalErrorConsumer(@Nonnull Consumer<Exception> fatalErrorConsumer) {
      myFatalErrorConsumer = fatalErrorConsumer;
    }
  }
}
