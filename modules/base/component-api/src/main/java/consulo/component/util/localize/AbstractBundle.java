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
package consulo.component.util.localize;

import consulo.annotation.DeprecationInfo;

import jakarta.annotation.Nonnull;
import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.concurrent.ConcurrentHashMap;

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
@Deprecated
@DeprecationInfo("Migrate to new localize")
public abstract class AbstractBundle {
  private Reference<ResourceBundle> myBundle;
  private final String myPathToBundle;

  protected AbstractBundle() {
    myPathToBundle = "messages." + getClass().getName();
  }

  protected AbstractBundle(@Nonnull String pathToBundle) {
    myPathToBundle = pathToBundle;
  }

  public String getMessage(@Nonnull String key, Object... params) {
    return BundleBase.message(getBundle(), key, params);
  }

  private ResourceBundle getBundle() {
    ResourceBundle bundle = null;
    if (myBundle != null) bundle = myBundle.get();
    if (bundle == null) {
      bundle = getResourceBundle(myPathToBundle, getClass().getClassLoader());
      myBundle = new SoftReference<>(bundle);
    }
    return bundle;
  }

  private static final Map<ClassLoader, Map<String, ResourceBundle>> ourCache = new ConcurrentHashMap<>();

  @Nonnull
  public static ResourceBundle getResourceBundle(@Nonnull String pathToBundle, @Nonnull ClassLoader loader) {
    Map<String, ResourceBundle> map = ourCache.computeIfAbsent(loader, classLoader -> new ConcurrentHashMap<>());
    return map.computeIfAbsent(pathToBundle, s -> ResourceBundle.getBundle(s, Locale.getDefault(), loader));
  }
}
