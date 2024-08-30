// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.util.nodep.classloader;

import java.io.IOException;
import java.net.URL;

/**
 * An object responsible for loading classes and resources from a particular classpath element: a jar or a directory.
 *
 * @see JarLoader
 * @see FileLoader
 */
abstract class Loader {
  private final URL myURL;
  private final int myIndex;
  protected ClasspathCache.NameFilter myLoadingFilter;

  Loader(URL url, int index) {
    myURL = url;
    myIndex = index;
  }

  URL getBaseURL() {
    return myURL;
  }

  void close() throws Exception {
  }

  abstract Resource getResource(String name);

  abstract ClasspathCache.LoaderData buildData() throws IOException;

  int getIndex() {
    return myIndex;
  }

  boolean containsPath(String resourcePath) {
    return true;
  }

  boolean containsName(String name, String shortName) {
    if (name == null || name.isEmpty()) return true;
    ClasspathCache.NameFilter filter = myLoadingFilter;
    return filter == null || filter.maybeContains(shortName);
  }

  void applyData(ClasspathCache.LoaderData loaderData) {
    myLoadingFilter = loaderData.getNameFilter();
  }
}
