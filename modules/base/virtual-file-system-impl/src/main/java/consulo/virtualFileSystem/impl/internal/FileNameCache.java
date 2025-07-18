/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package consulo.virtualFileSystem.impl.internal;

import consulo.platform.Platform;
import consulo.util.lang.ByteArrayCharSequence;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.impl.internal.util.IntObjectLRUMap;
import consulo.virtualFileSystem.impl.internal.util.IntSLRUCache;
import jakarta.annotation.Nonnull;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author peter
 */
public class FileNameCache {

  @SuppressWarnings("unchecked")
  private static final IntSLRUCache<CharSequence>[] ourNameCache = new IntSLRUCache[16];

  static {
    final int protectedSize = 40000 / ourNameCache.length;
    final int probationalSize = 20000 / ourNameCache.length;
    for (int i = 0; i < ourNameCache.length; ++i) {
      ourNameCache[i] = new IntSLRUCache<>(protectedSize, probationalSize);
    }
  }

  private static final String FS_SEPARATORS = "/" + (File.separatorChar == '/' ? "" : File.separatorChar);

  public static int storeName(@Nonnull String name) {
    assertShortFileName(name);
    final int idx = FSRecords.getNameId(name);
    cacheData(name, idx, calcStripeIdFromNameId(idx));
    return idx;
  }

  private static void assertShortFileName(@Nonnull String name) {
    if (name.length() <= 1) return;
    int start = 0;
    if (Platform.current().os().isWindows() && name.startsWith("//")) {  // Windows UNC: //Network/Ubuntu
      final int idx = name.indexOf('/', 2);
      start = idx == -1 ? 2 : idx + 1;
    }
    if (StringUtil.containsAnyChar(name, FS_SEPARATORS, start, name.length())) {
      throw new IllegalArgumentException("Must not intern long path: '" + name + "'");
    }
  }

  @Nonnull
  private static IntObjectLRUMap.MapEntry<CharSequence> cacheData(String name, int id, int stripe) {
    if (name == null) {
      FSRecords.handleError(new RuntimeException("VFS name enumerator corrupted"));
    }

    CharSequence rawName = ByteArrayCharSequence.convertToBytesIfPossible(name);
    IntSLRUCache<CharSequence> cache = ourNameCache[stripe];
    //noinspection SynchronizationOnLocalVariableOrMethodParameter
    synchronized (cache) {
      return cache.cacheEntry(id, rawName);
    }
  }

  private static int calcStripeIdFromNameId(int id) {
    int h = id;
    h -= h << 6;
    h ^= h >> 17;
    h -= h << 9;
    h ^= h << 4;
    h -= h << 3;
    h ^= h << 10;
    h ^= h >> 15;
    return h % ourNameCache.length;
  }

  private static final boolean ourTrackStats = false;
  private static final int ourLOneSize = 1024;
  @SuppressWarnings("unchecked")
  private static final IntObjectLRUMap.MapEntry<CharSequence>[] ourArrayCache = new IntObjectLRUMap.MapEntry[ourLOneSize];

  private static final AtomicInteger ourQueries = new AtomicInteger();
  private static final AtomicInteger ourMisses = new AtomicInteger();


  @FunctionalInterface
  public interface NameComputer {
    String compute(int id) throws IOException;
  }

  @Nonnull
  public static CharSequence getVFileName(int nameId, @Nonnull NameComputer computeName) throws IOException {
    assert nameId > 0 : nameId;

    if (ourTrackStats) {
      int frequency = 10000000;
      int queryCount = ourQueries.incrementAndGet();
      if (queryCount >= frequency && ourQueries.compareAndSet(queryCount, 0)) {
        double misses = ourMisses.getAndSet(0);
        //noinspection UseOfSystemOutOrSystemErr
        System.out.println("Misses: " + misses / frequency);
        ourQueries.set(0);
      }
    }

    int l1 = nameId % ourLOneSize;
    IntObjectLRUMap.MapEntry<CharSequence> entry = ourArrayCache[l1];
    if (entry != null && entry.key == nameId) {
      return entry.value;
    }

    if (ourTrackStats) {
      ourMisses.incrementAndGet();
    }

    final int stripe = calcStripeIdFromNameId(nameId);
    IntSLRUCache<CharSequence> cache = ourNameCache[stripe];
    //noinspection SynchronizationOnLocalVariableOrMethodParameter
    synchronized (cache) {
      entry = cache.getCachedEntry(nameId);
    }
    if (entry == null) {
      entry = cacheData(computeName.compute(nameId), nameId, stripe);
    }
    ourArrayCache[l1] = entry;
    return entry.value;
  }

  @Nonnull
  public static CharSequence getVFileName(int nameId) {
    try {
      return getVFileName(nameId, FSRecords::getNameByNameId);
    }
    catch (IOException e) {
      throw new RuntimeException(e); // actually will be caught in getNameByNameId
    }
  }
}
