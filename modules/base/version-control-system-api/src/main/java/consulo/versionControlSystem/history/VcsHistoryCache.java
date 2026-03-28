/*
 * Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
 */
package consulo.versionControlSystem.history;

import consulo.util.collection.SLRUMap;
import consulo.versionControlSystem.FilePath;
import consulo.versionControlSystem.VcsKey;
import consulo.versionControlSystem.annotate.VcsAnnotation;

import org.jspecify.annotations.Nullable;
import java.io.Serializable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * In-memory cache for VCS history sessions, annotations, and last-revision mappings.
 * Ported from JetBrains IntelliJ Community {@code VcsHistoryCache.kt}.
 *
 * @author irengrig
 * @since 2011-02-28
 */
public class VcsHistoryCache {
  private final Object myLock;
  private final SLRUMap<HistoryCacheBaseKey, CachedHistory> myHistoryCache;
  /** Stores arbitrary annotation data (e.g. {@code GitAnnotationProvider.CachedData} or {@link VcsAnnotation}). */
  private final SLRUMap<HistoryCacheWithRevisionKey, Object> myAnnotationCache;
  /** Maps (filePath, vcsKey, currentRevision) → lastModifiedRevision, enabling fast annotation lookups. */
  private final SLRUMap<HistoryCacheWithRevisionKey, VcsRevisionNumber> myLastRevisionCache;

  public VcsHistoryCache() {
    myLock = new Object();
    myHistoryCache = new SLRUMap<>(10, 10);
    myAnnotationCache = new SLRUMap<>(10, 5);
    myLastRevisionCache = new SLRUMap<>(50, 50);
  }

  // -------------------------------------------------------------------------
  // History session cache (same as old put/getFull/getMaybePartial)
  // -------------------------------------------------------------------------

  /** @deprecated Use {@link #putSession} */
  @Deprecated
  public <C extends Serializable, T extends VcsAbstractHistorySession> void put(FilePath filePath,
                                                                                @Nullable FilePath correctedPath,
                                                                                VcsKey vcsKey,
                                                                                T session,
                                                                                VcsCacheableHistorySessionFactory<C, T> factory,
                                                                                boolean isFull) {
    putSession(filePath, correctedPath, vcsKey, session, factory, isFull);
  }

  /** Stores a history session in the cache (ported from JB {@code VcsHistoryCache.putSession}). */
  public <C extends Serializable, T extends VcsAbstractHistorySession> void putSession(FilePath filePath,
                                                                                       @Nullable FilePath correctedPath,
                                                                                       VcsKey vcsKey,
                                                                                       T session,
                                                                                       VcsCacheableHistorySessionFactory<C, T> factory,
                                                                                       boolean isFull) {
    synchronized (myLock) {
      myHistoryCache.put(new HistoryCacheBaseKey(filePath, vcsKey),
                         new CachedHistory(correctedPath != null ? correctedPath : filePath, session.getRevisionList(),
                                           session.getCurrentRevisionNumber(), factory.getAddinionallyCachedData(session), isFull));
    }
  }

  public void editCached(FilePath filePath, VcsKey vcsKey, Consumer<List<VcsFileRevision>> consumer) {
    synchronized (myLock) {
      CachedHistory cachedHistory = myHistoryCache.get(new HistoryCacheBaseKey(filePath, vcsKey));
      if (cachedHistory != null) {
        consumer.accept(cachedHistory.getRevisions());
      }
    }
  }

  /** @deprecated Use {@link #getSession} */
  @Deprecated
  public @Nullable <C extends Serializable, T extends VcsAbstractHistorySession> T getFull(FilePath filePath, VcsKey vcsKey,
                                                                                           VcsCacheableHistorySessionFactory<C, T> factory) {
    return getSession(filePath, vcsKey, factory, false);
  }

  /** @deprecated Use {@link #getSession} with {@code allowPartial=true} */
  @Deprecated
  public @Nullable <C extends Serializable, T extends VcsAbstractHistorySession> T getMaybePartial(FilePath filePath, VcsKey vcsKey,
                                                                                                   VcsCacheableHistorySessionFactory<C, T> factory) {
    return getSession(filePath, vcsKey, factory, true);
  }

  /**
   * Retrieves a cached history session (ported from JB {@code VcsHistoryCache.getSession}).
   *
   * @param allowPartial {@code true} to return a partial session if no full session is cached
   */
  public @Nullable <C extends Serializable, T extends VcsAbstractHistorySession> T getSession(FilePath filePath, VcsKey vcsKey,
                                                                                              VcsCacheableHistorySessionFactory<C, T> factory,
                                                                                              boolean allowPartial) {
    synchronized (myLock) {
      CachedHistory cachedHistory = myHistoryCache.get(new HistoryCacheBaseKey(filePath, vcsKey));
      if (cachedHistory == null || (!cachedHistory.isIsFull() && !allowPartial)) {
        return null;
      }
      //noinspection unchecked
      return factory.createFromCachedData((C) cachedHistory.getCustomData(), cachedHistory.getRevisions(), cachedHistory.getPath(),
                                          cachedHistory.getCurrentRevision());
    }
  }

  // -------------------------------------------------------------------------
  // Annotation cache
  // -------------------------------------------------------------------------

  /**
   * Stores annotation data for a file at a specific revision.
   * The data can be any type (e.g. {@code GitAnnotationProvider.CachedData} or {@link VcsAnnotation}).
   * Ported from JB {@code VcsHistoryCache.putAnnotation}.
   */
  public void putAnnotation(FilePath filePath, VcsKey vcsKey, VcsRevisionNumber number, Object annotationData) {
    synchronized (myLock) {
      myAnnotationCache.put(new HistoryCacheWithRevisionKey(filePath, vcsKey, number), annotationData);
    }
  }

  /**
   * Returns cached annotation data for a file at a specific revision, or {@code null} if not cached.
   * Ported from JB {@code VcsHistoryCache.getAnnotation}.
   */
  public @Nullable Object getAnnotation(FilePath filePath, VcsKey vcsKey, VcsRevisionNumber number) {
    synchronized (myLock) {
      return myAnnotationCache.get(new HistoryCacheWithRevisionKey(filePath, vcsKey, number));
    }
  }

  /** @deprecated Use {@link #putAnnotation} */
  @Deprecated
  public void put(FilePath filePath, VcsKey vcsKey, VcsRevisionNumber number, VcsAnnotation vcsAnnotation) {
    putAnnotation(filePath, vcsKey, number, vcsAnnotation);
  }

  /** @deprecated Use {@link #getAnnotation} */
  @Deprecated
  public @Nullable Object get(FilePath filePath, VcsKey vcsKey, VcsRevisionNumber number) {
    return getAnnotation(filePath, vcsKey, number);
  }

  // -------------------------------------------------------------------------
  // Last-revision cache (ported from JB VcsHistoryCache)
  // -------------------------------------------------------------------------

  /**
   * Stores the last-modified revision for a file, indexed by (filePath, vcsKey, currentRevision).
   * Ported from JB {@code VcsHistoryCache.putLastRevision}.
   */
  public void putLastRevision(FilePath filePath, VcsKey vcsKey,
                              VcsRevisionNumber currentRevision, VcsRevisionNumber lastRevision) {
    synchronized (myLock) {
      myLastRevisionCache.put(new HistoryCacheWithRevisionKey(filePath, vcsKey, currentRevision), lastRevision);
    }
  }

  /**
   * Returns the cached last-modified revision for a file, or {@code null} if not cached.
   * Ported from JB {@code VcsHistoryCache.getLastRevision}.
   */
  public @Nullable VcsRevisionNumber getLastRevision(FilePath filePath, VcsKey vcsKey, VcsRevisionNumber currentRevision) {
    synchronized (myLock) {
      return myLastRevisionCache.get(new HistoryCacheWithRevisionKey(filePath, vcsKey, currentRevision));
    }
  }

  // -------------------------------------------------------------------------
  // Cache clearing (ported from JB VcsHistoryCache)
  // -------------------------------------------------------------------------

  /**
   * Clears the history session cache for local files.
   * Ported from JB {@code VcsHistoryCache.clearHistory}.
   */
  public void clearHistory() {
    clear();
  }

  /**
   * Clears all cached annotation data.
   * Ported from JB {@code VcsHistoryCache.clearAnnotations}.
   */
  public void clearAnnotations() {
    synchronized (myLock) {
      myAnnotationCache.clear();
    }
  }

  /**
   * Clears all cached last-revision mappings.
   * Ported from JB {@code VcsHistoryCache.clearLastRevisions}.
   */
  public void clearLastRevisions() {
    synchronized (myLock) {
      myLastRevisionCache.clear();
    }
  }

  /**
   * Clears all caches (history, annotations, last revisions).
   * Ported from JB {@code VcsHistoryCache.clearAll}.
   */
  public void clearAll() {
    clearHistory();
    clearAnnotations();
    clearLastRevisions();
  }

  /** Clears non-local file history entries. */
  public void clear() {
    synchronized (myLock) {
      Iterator<Map.Entry<HistoryCacheBaseKey, CachedHistory>> iterator = myHistoryCache.entrySet().iterator();
      while (iterator.hasNext()) {
        Map.Entry<HistoryCacheBaseKey, CachedHistory> next = iterator.next();
        if (!next.getKey().getFilePath().isNonLocal()) {
          iterator.remove();
        }
      }
    }
  }

  // -------------------------------------------------------------------------
  // Inner types
  // -------------------------------------------------------------------------

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
