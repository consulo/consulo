// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.util.nodep.classloader;

import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * @author Dmitry Avdeev
 */
public class PreloadedJar {
  private static final PreloadedJar EMPTY = new PreloadedJar(Collections.<String, Resource>emptyMap());

  private final Map<String, Resource> myResources;

  private PreloadedJar(Map<String, Resource> resources) {
    myResources = resources;
  }

  public Resource getResource(String entryName) {
    return myResources.get(entryName);
  }

  public Map<String, Resource> getResources() {
    return myResources;
  }

  static PreloadedJar load(ZipFile zipFile, URL baseUrl, JarLoader attributesProvider) throws IOException {
    Enumeration<? extends ZipEntry> entries = zipFile.entries();
    if (!entries.hasMoreElements()) return EMPTY;

    Map<String, Resource> resources = new HashMap<String, Resource>();
    while (entries.hasMoreElements()) {
      ZipEntry entry = entries.nextElement();
      MemoryResource resource = attributesProvider.createMemoryResource(baseUrl, zipFile, entry, attributesProvider.getAttributes());
      resources.put(entry.getName(), resource);
    }
    return new PreloadedJar(resources);
  }
}
