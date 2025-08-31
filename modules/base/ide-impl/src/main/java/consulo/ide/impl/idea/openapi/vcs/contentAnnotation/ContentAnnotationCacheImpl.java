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
package consulo.ide.impl.idea.openapi.vcs.contentAnnotation;

import consulo.annotation.component.ServiceImpl;
import consulo.document.util.TextRange;
import consulo.versionControlSystem.VcsKey;
import consulo.versionControlSystem.action.VcsContextFactory;
import consulo.versionControlSystem.annotate.FileAnnotation;
import consulo.versionControlSystem.history.HistoryCacheWithRevisionKey;
import consulo.versionControlSystem.history.VcsRevisionNumber;
import consulo.virtualFileSystem.VirtualFile;
import consulo.util.lang.ThreeState;
import consulo.util.collection.SLRUMap;
import jakarta.inject.Singleton;

import jakarta.annotation.Nullable;

import java.util.Date;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * @author Irina.Chernushina
 * @since 2011-08-08
 */
@Singleton
@ServiceImpl
public class ContentAnnotationCacheImpl implements ContentAnnotationCache {
  private final SLRUMap<HistoryCacheWithRevisionKey, TreeMap<Integer, Long>> myCache;
  private final Object myLock;

  public ContentAnnotationCacheImpl() {
    myLock = new Object();
    myCache = new SLRUMap<HistoryCacheWithRevisionKey, TreeMap<Integer, Long>>(50, 50);
  }

  @Override
  @Nullable
  public ThreeState isRecent(VirtualFile vf,
                             VcsKey vcsKey,
                             VcsRevisionNumber number,
                             TextRange range,
                             long boundTime) {
    TreeMap<Integer, Long> treeMap;
    synchronized (myLock) {
      treeMap = myCache.get(new HistoryCacheWithRevisionKey(VcsContextFactory.SERVICE.getInstance().createFilePathOn(vf), vcsKey, number));
    }
    if (treeMap != null) {
      Map.Entry<Integer, Long> last = treeMap.floorEntry(range.getEndOffset());
      if (last == null || last.getKey() < range.getStartOffset()) return ThreeState.NO;
      Map.Entry<Integer, Long> first = treeMap.ceilingEntry(range.getStartOffset());
      assert first != null;
      SortedMap<Integer,Long> interval = treeMap.subMap(first.getKey(), last.getKey());
      for (Map.Entry<Integer, Long> entry : interval.entrySet()) {
        if (entry.getValue() >= boundTime) return ThreeState.YES;
      }
      return ThreeState.NO;
    }
    return ThreeState.UNSURE;
  }

  @Override
  public void register(VirtualFile vf, VcsKey vcsKey, VcsRevisionNumber number, FileAnnotation fa) {
    HistoryCacheWithRevisionKey key = new HistoryCacheWithRevisionKey(VcsContextFactory.SERVICE.getInstance().createFilePathOn(vf), vcsKey, number);
    synchronized (myLock) {
      if (myCache.get(key) != null) return;
    }
    long absoluteLimit = System.currentTimeMillis() - VcsContentAnnotationSettings.ourAbsoluteLimit;
    TreeMap<Integer, Long> map = new TreeMap<Integer, Long>();
    int lineCount = fa.getLineCount();
    for (int i = 0; i < lineCount; i++) {
      Date lineDate = fa.getLineDate(i);
      if (lineDate == null) return;
      if (lineDate.getTime() >= absoluteLimit) map.put(i, lineDate.getTime());
    }
    synchronized (myLock) {
      myCache.put(key, map);
    }
  }
}
