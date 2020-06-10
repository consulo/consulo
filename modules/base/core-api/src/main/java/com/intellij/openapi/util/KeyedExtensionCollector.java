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

/*
 * @author max
 */
package com.intellij.openapi.util;

import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.util.KeyedLazyInstance;
import com.intellij.util.SmartList;
import consulo.logging.Logger;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class KeyedExtensionCollector<T, KeyT> {
  private static final Logger LOG = Logger.getInstance(KeyedExtensionCollector.class);

  private final ConcurrentMap<String, List<T>> myCache = new ConcurrentHashMap<>();

  private final ExtensionPointName<? extends KeyedLazyInstance<T>> myEPName;

  public KeyedExtensionCollector(@Nonnull String epName) {
    myEPName = ExtensionPointName.create(epName);
  }

  public KeyedExtensionCollector(@Nonnull ExtensionPointName<? extends KeyedLazyInstance<T>> epName) {
    myEPName = epName;
  }

  @Nonnull
  protected String keyToString(@Nonnull KeyT key) {
    return key.toString();
  }

  /**
   * @see #findSingle(Object)
   */
  @Nonnull
  public List<T> forKey(@Nonnull KeyT key) {
    return myCache.computeIfAbsent(keyToString(key), stringKey -> buildExtensions(stringKey, key));
  }

  public T findSingle(@Nonnull KeyT key) {
    List<T> list = forKey(key);
    return list.isEmpty() ? null : list.get(0);
  }

  @Nonnull
  protected List<T> buildExtensions(@Nonnull String stringKey, @Nonnull KeyT key) {
    return buildExtensions(Collections.singleton(stringKey));
  }

  @Nonnull
  protected final List<T> buildExtensions(@Nonnull Set<String> keys) {
    List<T> result = null;

    for (KeyedLazyInstance<T> bean : myEPName.getExtensionList()) {
      if (keys.contains(bean.getKey())) {
        final T instance;
        try {
          instance = bean.getInstance();
        }
        catch (ProcessCanceledException e) {
          throw e;
        }
        catch (Throwable e) {
          LOG.error(e);
          continue;
        }
        if (result == null) {
          result = new SmartList<>();
        }
        result.add(instance);
      }
    }
    return result == null ? Collections.<T>emptyList() : result;
  }

  public boolean hasAnyExtensions() {
    return myEPName.hasAnyExtensions();
  }

  @Nonnull
  public List<T> getExtensions() {
    List<? extends KeyedLazyInstance<T>> extensionList = myEPName.getExtensionList();

    List<T> result = new ArrayList<>();
    for (KeyedLazyInstance<T> bean : extensionList) {
      try {
        result.add(bean.getInstance());
      }
      catch (ProcessCanceledException e) {
        throw e;
      }
      catch (Throwable e) {
        LOG.error(e);
      }
    }

    return result;
  }

  @Nonnull
  public final ExtensionPointName<? extends KeyedLazyInstance<T>> getExtensionPointName() {
    return myEPName;
  }
}
