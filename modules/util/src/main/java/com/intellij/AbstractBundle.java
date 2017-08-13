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
package com.intellij;

import com.intellij.util.containers.ConcurrentWeakFactoryMap;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.FactoryMap;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

/**
 * Base class for particular scoped bundles (e.g. <code>'vcs'</code> bundles, <code>'aop'</code> bundles etc).
 * <p/>
 * Usage pattern:
 * <pre>
 * <ol>
 *   <li>Create class that extends this class and provides path to the target bundle to the current class constructor;</li>
 *   <li>
 *     Optionally create static facade method at the subclass - create single shared instance and delegate
 *     to its {@link #getMessage(String, Object...)};
 *   </li>
 * </ol>
 * </pre>
 *
 * @author Denis Zhdanov
 * @since 8/1/11 2:37 PM
 */
public abstract class AbstractBundle {
  private Reference<ResourceBundle> myBundle;
  @NonNls private final String myPathToBundle;

  protected AbstractBundle() {
    myPathToBundle = "messages." + getClass().getName();
  }

  protected AbstractBundle(@NonNls @NotNull String pathToBundle) {
    myPathToBundle = pathToBundle;
  }

  public String getMessage(@NotNull String key, Object... params) {
    return CommonBundle.message(getBundle(), key, params);
  }

  private ResourceBundle getBundle() {
    ResourceBundle bundle = null;
    if (myBundle != null) bundle = myBundle.get();
    if (bundle == null) {
      bundle = getResourceBundle(myPathToBundle, getClass().getClassLoader());
      myBundle = new SoftReference<ResourceBundle>(bundle);
    }
    return bundle;
  }

  @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
  private static final FactoryMap<ClassLoader, Map<String, SoftReference<ResourceBundle>>> ourCache =
          new ConcurrentWeakFactoryMap<ClassLoader, Map<String, SoftReference<ResourceBundle>>>() {
            @Override
            protected Map<String, SoftReference<ResourceBundle>> create(ClassLoader key) {
              return ContainerUtil.newConcurrentMap();
            }
          };

  public static ResourceBundle getResourceBundle(@NotNull String pathToBundle, @NotNull ClassLoader loader) {
    Map<String, SoftReference<ResourceBundle>> map = ourCache.get(loader);
    SoftReference<ResourceBundle> reference = map.get(pathToBundle);
    ResourceBundle result = reference == null ? null : reference.get();
    if (result == null) {
      map.put(pathToBundle, new SoftReference<ResourceBundle>(result = ResourceBundle.getBundle(pathToBundle, Locale.getDefault(), loader)));
    }
    return result;
  }
}
