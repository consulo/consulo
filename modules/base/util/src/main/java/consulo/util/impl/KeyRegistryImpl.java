/*
 * Copyright 2013-2017 consulo.io
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
package consulo.util.impl;

import com.intellij.openapi.util.Key;
import com.intellij.util.Function;
import com.intellij.util.containers.ConcurrentIntObjectMap;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.IntObjectMap;
import consulo.util.KeyRegistry;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author VISTALL
 * @since 15-Oct-17
 */
public class KeyRegistryImpl implements KeyRegistry {
  private final ConcurrentIntObjectMap<Key> myAllKeys = ContainerUtil.createConcurrentIntObjectWeakValueMap();
  private final AtomicInteger myKeyCounter = new AtomicInteger();

  @Override
  public int register(Key<?> key) {
    int index = myKeyCounter.getAndIncrement();
    myAllKeys.put(index, key);
    return index;
  }

  @Override
  public Key<?> findKeyByName(String name, Function<Key<?>, String> nameFunc) {
    for (IntObjectMap.Entry<Key> key : myAllKeys.entrySet()) {
      if (name.equals(nameFunc.fun(key.getValue()))) {
        return key.getValue();
      }
    }
    return null;
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> Key<T> getKeyByIndex(int index) {
    return myAllKeys.get(index);
  }
}
