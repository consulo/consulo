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
package consulo.ide.impl.idea.util.indexing;

import consulo.application.progress.ProgressManager;
import consulo.index.io.StorageException;
import consulo.util.lang.function.ThrowableRunnable;
import consulo.index.io.ID;
import consulo.util.lang.TimeoutUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author peter
 */
enum RebuildStatus {
  OK,
  REQUIRES_REBUILD,
  DOING_REBUILD;

  private static final Map<ID<?, ?>, AtomicReference<RebuildStatus>> ourRebuildStatus = new HashMap<>();

  static void registerIndex(ID<?, ?> indexId) {
    ourRebuildStatus.put(indexId, new AtomicReference<>(OK));
  }

  static boolean isOk(ID<?, ?> indexId) {
    return ourRebuildStatus.get(indexId).get() == OK;
  }

  static boolean requestRebuild(ID<?, ?> indexId) {
    return ourRebuildStatus.get(indexId).compareAndSet(OK, REQUIRES_REBUILD);
  }

  static void clearIndexIfNecessary(ID<?, ?> indexId, ThrowableRunnable<StorageException> clearAction) throws StorageException {
    AtomicReference<RebuildStatus> rebuildStatus = ourRebuildStatus.get(indexId);
    if (rebuildStatus == null) {
      throw new StorageException("Problem updating " + indexId);
    }

    if (rebuildStatus.compareAndSet(REQUIRES_REBUILD, DOING_REBUILD)) {
      doClear(clearAction, rebuildStatus);
    } else {
      waitUntilIndexReady(rebuildStatus);
    }
  }

  private static void doClear(ThrowableRunnable<StorageException> clearAction, AtomicReference<RebuildStatus> status) throws StorageException {
    try {
      clearAction.run();
    }
    catch (StorageException e) {
      status.compareAndSet(DOING_REBUILD, REQUIRES_REBUILD);
      throw e;
    }
    if (!status.compareAndSet(DOING_REBUILD, OK)) {
      FileBasedIndexImpl.LOG.error("Unexpected status " + status.get());
    }
  }

  private static void waitUntilIndexReady(AtomicReference<RebuildStatus> rebuildStatus) {
    while (rebuildStatus.get() != OK) {
      ProgressManager.checkCanceled();
      TimeoutUtil.sleep(50);
    }
  }

}
