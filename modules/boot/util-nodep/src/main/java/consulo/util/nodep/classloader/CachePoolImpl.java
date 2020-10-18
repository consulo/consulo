// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.util.nodep.classloader;

import javax.annotation.Nonnull;

import java.net.URL;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.Attributes;

/**
 * @author peter
 */
class CachePoolImpl implements UrlClassLoader.CachePool {
  private final Map<URL, ClasspathCache.LoaderData> myLoaderIndexCache = new ConcurrentHashMap<URL, ClasspathCache.LoaderData>();

  void cacheData(@Nonnull URL url, @Nonnull ClasspathCache.LoaderData data) {
    myLoaderIndexCache.put(url, data);
  }

  ClasspathCache.LoaderData getCachedData(@Nonnull URL url) {
    return myLoaderIndexCache.get(url);
  }

  private final Map<URL, Attributes> myManifestData = new ConcurrentHashMap<URL, Attributes>();

  Attributes getManifestData(URL url) {
    return myManifestData.get(url);
  }

  void cacheManifestData(URL url, Attributes manifestAttributes) {
    myManifestData.put(url, manifestAttributes);
  }
}
