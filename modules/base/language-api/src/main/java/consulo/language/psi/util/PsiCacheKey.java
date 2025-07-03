/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package consulo.language.psi.util;

import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiModificationTracker;
import consulo.util.dataholder.Key;
import consulo.util.lang.Pair;
import consulo.util.lang.ref.SoftReference;
import org.jetbrains.annotations.NonNls;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.function.Function;

/**
 * @author max
 */
public class PsiCacheKey<T, H extends PsiElement> extends Key<SoftReference<Pair<Long, T>>> {
  private final Function<H, T> myFunction;
  /**
   * One of {@link PsiModificationTracker} constants that marks when to flush cache
   */
  @Nonnull
  private final Key<?> myModifyCause;

  private PsiCacheKey(@NonNls @Nonnull String name, @Nonnull Function<H, T> function, @Nonnull Key<?> modifyCause) {
    super(name);
    myFunction = function;
    myModifyCause = modifyCause;
  }

  public final T getValue(@Nonnull H h) {
    T result = getCachedValueOrNull(h);
    if (result != null) {
      return result;
    }

    result = myFunction.apply(h);
    final long count = getModificationCount(h);
    h.putUserData(this, new SoftReference<Pair<Long, T>>(new Pair<Long, T>(count, result)));
    return result;
  }

  @Nullable
  public final T getCachedValueOrNull(@Nonnull H h) {
    SoftReference<Pair<Long, T>> ref = h.getUserData(this);
    Pair<Long, T> data = SoftReference.dereference(ref);
    if (data == null || data.getFirst() != getModificationCount(h)) {
      return null;
    }

    return data.getSecond();
  }


  /**
   * Gets modification count from tracker based on {@link #myModifyCause}
   *
   * @param tracker track to get modification count from
   * @return modification count
   * @throws AssertionError if {@link #myModifyCause} is junk
   */
  private long getModificationCount(@Nonnull PsiElement element) {
    PsiFile file = element.getContainingFile();
    long fileStamp = file == null || file.isPhysical() ? 0 : file.getModificationStamp();
    PsiModificationTracker tracker = file == null ? element.getManager().getModificationTracker() : file.getManager().getModificationTracker();

    if (myModifyCause.equals(PsiModificationTracker.JAVA_STRUCTURE_MODIFICATION_COUNT)) {
      return fileStamp + tracker.getJavaStructureModificationCount();
    }
    if (myModifyCause.equals(PsiModificationTracker.OUT_OF_CODE_BLOCK_MODIFICATION_COUNT)) {
      return fileStamp + tracker.getOutOfCodeBlockModificationCount();
    }
    if (myModifyCause.equals(PsiModificationTracker.MODIFICATION_COUNT)) {
      return fileStamp + tracker.getModificationCount();
    }
    throw new AssertionError("No modification tracker found for key " + myModifyCause);
  }

  /**
   * Creates cache key value
   *
   * @param name        key name
   * @param function    function to reproduce new value when old value is stale
   * @param modifyCause one one {@link PsiModificationTracker}'s constants that marks when to flush cache
   * @param <T>         value type
   * @param <H>         key type
   * @return instance
   */
  public static <T, H extends PsiElement> PsiCacheKey<T, H> create(@NonNls @Nonnull String name,
                                                                   @Nonnull Function<H, T> function,
                                                                   @Nonnull Key<?> modifyCause) {
    return new PsiCacheKey<T, H>(name, function, modifyCause);
  }

  /**
   * Creates cache key value using {@link PsiModificationTracker#JAVA_STRUCTURE_MODIFICATION_COUNT} as
   * modification count to flush cache
   *
   * @param name     key name
   * @param function function to reproduce new value when old value is stale
   * @param <T>      value type
   * @param <H>      key type
   * @return instance
   */
  public static <T, H extends PsiElement> PsiCacheKey<T, H> create(@NonNls @Nonnull String name, @Nonnull Function<H, T> function) {
    return create(name, function, PsiModificationTracker.JAVA_STRUCTURE_MODIFICATION_COUNT);
  }
}
