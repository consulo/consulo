// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.util.nodep.classloader;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * @author Dmitry Avdeev
 */
public class JarMemoryLoader {
  private static final JarMemoryLoader EMPTY = new JarMemoryLoader(Collections.<String, Resource>emptyMap());

  private final Map<String, Resource> myResources;

  private JarMemoryLoader(Map<String, Resource> resources) {
    myResources = resources;
  }

  @Nullable
  public Resource getResource(String entryName) {
    return myResources.remove(entryName);
  }

  @Nonnull
  static JarMemoryLoader load(ZipFile zipFile, URL baseUrl, @Nullable JarLoader attributesProvider) throws IOException {
    Enumeration<? extends ZipEntry> entries = zipFile.entries();
    if (!entries.hasMoreElements()) return EMPTY;

    Map<String, Resource> resources = new HashMap<String, Resource>();
    while (entries.hasMoreElements()) {
      ZipEntry entry = entries.nextElement();

      MemoryResource resource = MemoryResource.load(baseUrl, zipFile, entry, attributesProvider != null ? attributesProvider.getAttributes() : null);

      resources.put(entry.getName(), resource);
    }
    return new JarMemoryLoader(resources);
  }
}
