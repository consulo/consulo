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

package consulo.application.util;

import consulo.util.dataholder.Key;
import consulo.util.dataholder.UserDataHolder;
import consulo.util.dataholder.UserDataHolderEx;

/**
 * @deprecated For recalculate-able caches, use {@link CachedValuesManager#getCachedValue},
 * {@link CachedValuesManager#getProjectPsiDependentCache}
 * or other similar methods, as a less verbose and often more correct alternative.
 * For lazy values calculated just once, consider {@code *LazyValue} classes
 * or explicit {@link UserDataHolder#getUserData} and {@link UserDataHolder#putUserData} calls for less verbosity.
 */
@Deprecated
public abstract class UserDataCache<T, Owner extends UserDataHolder, Param> extends FieldCache<T, Owner, Key<T>, Param> {
  private static final RecursionGuard ourGuard = RecursionManager.createGuard("userDataCache");
  private final Key<T> myKey;

  protected UserDataCache() {
    myKey = null;
  }

  public UserDataCache(String keyName) {
    myKey = Key.create(keyName);
  }

  public T get(final Owner owner, final Param parameter) {
    return get(myKey, owner, parameter);
  }

  public void put(final Owner owner, final T value) {
    putValue(value, owner, myKey);
  }

  @Override
  protected final T getValue(final Owner owner, final Key<T> key) {
    return owner.getUserData(key);
  }

  @Override
  protected final void putValue(final T t, final Owner owner, final Key<T> key) {
    owner.putUserData(key, t);
  }

  @Override
  public T get(Key<T> a, Owner owner, Param p) {
    T value = owner.getUserData(a);
    if (value == null) {
      RecursionGuard.StackStamp stamp = ourGuard.markStack();
      value = compute(owner, p);
      if (stamp.mayCacheNow()) {
        value = ((UserDataHolderEx)owner).putUserDataIfAbsent(a, value);
      }
    }
    return value;
  }

  @Override
  public void clear(Key<T> key, Owner owner) {
    owner.putUserData(key, null);
  }
}
