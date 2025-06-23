/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package consulo.versionControlSystem.history;

import consulo.util.collection.SLRUMap;
import consulo.versionControlSystem.FilePath;
import consulo.versionControlSystem.VcsKey;
import consulo.versionControlSystem.annotate.VcsAnnotation;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.io.Serializable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * @author irengrig
 * @since 2011-02-28
 */
public class VcsHistoryCache {
  private final Object myLock;
  private final SLRUMap<HistoryCacheBaseKey, CachedHistory> myHistoryCache;
  private final SLRUMap<HistoryCacheWithRevisionKey, VcsAnnotation> myAnnotationCache;
  //private final SLRUMap<HistoryCacheWithRevisionKey, String> myContentCache;

  public VcsHistoryCache() {
    myLock = new Object();
    myHistoryCache = new SLRUMap<HistoryCacheBaseKey, CachedHistory>(10, 10);
    myAnnotationCache = new SLRUMap<HistoryCacheWithRevisionKey, VcsAnnotation>(10, 5);
    //myContentCache = new SLRUMap<HistoryCacheWithRevisionKey, String>(20, 20);
  }

  public <C extends Serializable, T extends VcsAbstractHistorySession> void put(final FilePath filePath,
                                                                                @Nullable final FilePath correctedPath,
                                                                                final VcsKey vcsKey,
                                                                                final T session,
                                                                                @Nonnull final VcsCacheableHistorySessionFactory<C, T> factory,
                                                                                boolean isFull) {
    synchronized (myLock) {
      myHistoryCache.put(new HistoryCacheBaseKey(filePath, vcsKey),
                         new CachedHistory(correctedPath != null ? correctedPath : filePath, session.getRevisionList(),
                                           session.getCurrentRevisionNumber(), factory.getAddinionallyCachedData(session), isFull));
    }
  }

  public void editCached(final FilePath filePath, final VcsKey vcsKey, final Consumer<List<VcsFileRevision>> consumer) {
    synchronized (myLock) {
      final CachedHistory cachedHistory = myHistoryCache.get(new HistoryCacheBaseKey(filePath, vcsKey));
      if (cachedHistory != null) {
        consumer.accept(cachedHistory.getRevisions());
      }
    }
  }

  @Nullable
  public <C extends Serializable, T extends VcsAbstractHistorySession> T getFull(final FilePath filePath, final VcsKey vcsKey,
                                                                                 @Nonnull final VcsCacheableHistorySessionFactory<C, T> factory) {
    synchronized (myLock) {
      final CachedHistory cachedHistory = myHistoryCache.get(new HistoryCacheBaseKey(filePath, vcsKey));
      if (cachedHistory == null || ! cachedHistory.isIsFull()) {
        return null;
      }
      return factory.createFromCachedData((C) cachedHistory.getCustomData(), cachedHistory.getRevisions(), cachedHistory.getPath(),
                                          cachedHistory.getCurrentRevision());
    }
  }

  @Nullable
  public <C extends Serializable, T extends VcsAbstractHistorySession> T getMaybePartial(final FilePath filePath, final VcsKey vcsKey,
                                                                                         @Nonnull final VcsCacheableHistorySessionFactory<C, T> factory) {
    synchronized (myLock) {
      final CachedHistory cachedHistory = myHistoryCache.get(new HistoryCacheBaseKey(filePath, vcsKey));
      if (cachedHistory == null) {
        return null;
      }
      return factory.createFromCachedData((C) cachedHistory.getCustomData(), cachedHistory.getRevisions(), cachedHistory.getPath(),
                                          cachedHistory.getCurrentRevision());
    }
  }

  public void clear() {
    synchronized (myLock) {
      final Iterator<Map.Entry<HistoryCacheBaseKey,CachedHistory>> iterator = myHistoryCache.entrySet().iterator();
      while (iterator.hasNext()) {
        final Map.Entry<HistoryCacheBaseKey, CachedHistory> next = iterator.next();
        if (! next.getKey().getFilePath().isNonLocal()) {
          iterator.remove();
        }
      }
    }
  }

  public void put(@Nonnull final FilePath filePath, @Nonnull final VcsKey vcsKey, @Nonnull final VcsRevisionNumber number,
                  @Nonnull final VcsAnnotation vcsAnnotation) {
    synchronized (myLock) {
      myAnnotationCache.put(new HistoryCacheWithRevisionKey(filePath, vcsKey, number), vcsAnnotation);
    }
  }

  public VcsAnnotation get(@Nonnull final FilePath filePath, @Nonnull final VcsKey vcsKey, @Nonnull final VcsRevisionNumber number) {
    synchronized (myLock) {
      return myAnnotationCache.get(new HistoryCacheWithRevisionKey(filePath, vcsKey, number));
    }
  }

  public static class CachedHistory {
    private final FilePath myPath;
    private final List<VcsFileRevision> myRevisions;
    private final VcsRevisionNumber myCurrentRevision;
    private final Object myCustomData;
    private final boolean myIsFull;

    public CachedHistory(FilePath path,
                         List<VcsFileRevision> revisions,
                         VcsRevisionNumber currentRevision,
                         Object customData,
                         boolean isFull) {
      myPath = path;
      myRevisions = revisions;
      myCurrentRevision = currentRevision;
      myCustomData = customData;
      myIsFull = isFull;
    }

    public FilePath getPath() {
      return myPath;
    }

    public List<VcsFileRevision> getRevisions() {
      return myRevisions;
    }

    public VcsRevisionNumber getCurrentRevision() {
      return myCurrentRevision;
    }

    public Object getCustomData() {
      return myCustomData;
    }

    public boolean isIsFull() {
      return myIsFull;
    }
  }
}
