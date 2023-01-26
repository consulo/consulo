// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.util.nodep.classloader;

import consulo.util.nodep.ArrayUtilRt;
import consulo.util.nodep.JavaVersion;
import consulo.util.nodep.LoggerRt;
import consulo.util.nodep.Pair;
import consulo.util.nodep.io.FileUtilRt;
import consulo.util.nodep.io.UnsyncByteArrayInputStream;
import consulo.util.nodep.map.SimpleMultiMap;
import consulo.util.nodep.reference.SoftReference;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static consulo.util.nodep.Pair.pair;

class JarLoader extends Loader {
  private static final List<Pair<Resource.Attribute, Attributes.Name>> PACKAGE_FIELDS =
    Arrays.asList(pair(Resource.Attribute.SPEC_TITLE, Attributes.Name.SPECIFICATION_TITLE),
                  pair(Resource.Attribute.SPEC_VERSION, Attributes.Name.SPECIFICATION_VERSION),
                  pair(Resource.Attribute.SPEC_VENDOR, Attributes.Name.SPECIFICATION_VENDOR),
                  pair(Resource.Attribute.IMPL_TITLE, Attributes.Name.IMPLEMENTATION_TITLE),
                  pair(Resource.Attribute.IMPL_VERSION, Attributes.Name.IMPLEMENTATION_VERSION),
                  pair(Resource.Attribute.IMPL_VENDOR, Attributes.Name.IMPLEMENTATION_VENDOR));

  private final String myFilePath;
  private final ClassPath myConfiguration;
  private final URL myUrl;
  private volatile SoftReference<JarFile> myZipFileSoftReference; // Used only when myConfiguration.myCanLockJars==true
  private volatile Map<Resource.Attribute, String> myAttributes;
  private volatile String myClassPathManifestAttribute;
  private static final String NULL_STRING = "<null>";

  private JarIndex myJarIndex;
  private PreloadedJar myPreloadedJar;

  JarLoader(URL url, int index, ClassPath configuration) throws IOException {
    super(new URL("jar", "", -1, url + "!/"), index);

    myFilePath = urlToFilePath(url);
    myConfiguration = configuration;
    myUrl = url;

    if (configuration.myEnableJarIndex) {
      JarFile jarFile = getJarFile();
      try {
        myJarIndex = JarIndex.getJarIndex(jarFile);
      }
      finally {
        releaseZipFile(jarFile);
      }
    }

    if (!configuration.myLazyClassloadingCaches) {
      ZipFile zipFile = getJarFile(); // IOException from opening is propagated to caller if zip file isn't valid,
      try {
        if (configuration.myPreloadJarContents) {
          myPreloadedJar = PreloadedJar.load(zipFile, getBaseURL(), this);
        }
      }
      finally {
        releaseZipFile(zipFile);
      }
    }
  }

  @Override
  boolean containsPath(String name) {
    // data already loaded - dont use jar index
    if (myLoadingFilter != null) {
      return true;
    }

    if (myJarIndex != null) {
      List<String> paths = myJarIndex.get(name);
      if (paths != null && !paths.isEmpty()) {
        return true;
      }
      
      // if we have index and name not from index - skip loader for loading, and processing
      return false;
    }

    return true;
  }

  protected MemoryResource createMemoryResource(URL baseUrl,
                                                ZipFile zipFile,
                                                ZipEntry entry,
                                                Map<Resource.Attribute, String> attributes) throws IOException {
    String name = entry.getName();
    URL url = new URL(baseUrl, name);

    byte[] content = ArrayUtilRt.EMPTY_BYTE_ARRAY;
    InputStream stream = zipFile.getInputStream(entry);
    if (stream != null) {
      try {
        content = FileUtilRt.loadBytes(stream, (int)entry.getSize());
      }
      finally {
        stream.close();
      }
    }

    return new MemoryResource(url, content, attributes);
  }

  Map<Resource.Attribute, String> getAttributes() {
    loadManifestAttributes();
    return myAttributes;
  }


  String getClassPathManifestAttribute() {
    loadManifestAttributes();
    String manifestAttribute = myClassPathManifestAttribute;
    return manifestAttribute != NULL_STRING ? manifestAttribute : null;
  }

  private static String urlToFilePath(URL url) {
    try {
      return new File(url.toURI()).getPath();
    }
    catch (Throwable ignore) { // URISyntaxException or IllegalArgumentException
      return url.getPath();
    }
  }


  private static Map<Resource.Attribute, String> getAttributes(Attributes attributes) {
    if (attributes == null) return null;
    Map<Resource.Attribute, String> map = null;

    for (Pair<Resource.Attribute, Attributes.Name> p : PACKAGE_FIELDS) {
      String value = attributes.getValue(p.second);
      if (value != null) {
        if (map == null) map = new EnumMap<Resource.Attribute, String>(Resource.Attribute.class);
        map.put(p.first, value);
      }
    }

    return map;
  }

  private void loadManifestAttributes() {
    if (myClassPathManifestAttribute != null) return;
    synchronized (this) {
      try {
        if (myClassPathManifestAttribute != null) return;
        ZipFile zipFile = getJarFile();
        try {
          Attributes manifestAttributes = myConfiguration.getManifestData(myUrl);
          if (manifestAttributes == null) {
            ZipEntry entry = zipFile.getEntry(JarFile.MANIFEST_NAME);
            InputStream zipEntryStream = entry != null ? zipFile.getInputStream(entry) : null;
            manifestAttributes = loadManifestAttributes(zipFile, zipEntryStream);
            if (manifestAttributes == null) manifestAttributes = new Attributes(0);
            myConfiguration.cacheManifestData(myUrl, manifestAttributes);
          }

          myAttributes = getAttributes(manifestAttributes);
          Object attribute = manifestAttributes.get(Attributes.Name.CLASS_PATH);
          myClassPathManifestAttribute = attribute instanceof String ? (String)attribute : NULL_STRING;
        }
        finally {
          releaseZipFile(zipFile);
        }
      }
      catch (IOException io) {
        throw new RuntimeException(io);
      }
    }
  }


  protected Attributes loadManifestAttributes(ZipFile zipFile, InputStream stream) {
    if (stream == null) return null;
    try {
      try {
        return new Manifest(stream).getMainAttributes();
      }
      finally {
        stream.close();
      }
    }
    catch (Exception ignored) {
    }
    return null;
  }


  @Override
  public ClasspathCache.LoaderData buildData() throws IOException {
    if (myPreloadedJar != null) {
      return buildDataImpl(myPreloadedJar.getResources().keySet().iterator());
    }

    ZipFile zipFile = getJarFile();
    try {
      return buildDataImpl(new ZipEntryNameIterator(zipFile));
    }
    finally {
      releaseZipFile(zipFile);
    }
  }

  private ClasspathCache.LoaderData buildDataImpl(Iterator<String> zipEntryNameIterator) {
    ClasspathCache.LoaderDataBuilder loaderDataBuilder = new ClasspathCache.LoaderDataBuilder();

    while (zipEntryNameIterator.hasNext()) {
      String name = zipEntryNameIterator.next();

      if (name.endsWith(UrlClassLoader.CLASS_EXTENSION)) {
        loaderDataBuilder.addClassPackageFromName(name);
      }
      else {
        loaderDataBuilder.addResourcePackageFromName(name);
      }

      loaderDataBuilder.addPossiblyDuplicateNameEntry(name);
    }

    return loaderDataBuilder.build();
  }

  private final AtomicInteger myNumberOfRequests = new AtomicInteger();
  private volatile IntHashSet myPackageHashesInside;

  private IntHashSet buildPackageHashes() {
    try {
      ZipFile zipFile = getJarFile();
      try {
        Enumeration<? extends ZipEntry> entries = zipFile.entries();
        IntHashSet result = new IntHashSet(zipFile.size());

        while (entries.hasMoreElements()) {
          ZipEntry entry = entries.nextElement();
          result.add(ClasspathCache.getPackageNameHash(entry.getName()));
        }
        result.add(0); // empty package is in every jar
        return result;
      }
      finally {
        releaseZipFile(zipFile);
      }
    }
    catch (Exception e) {
      error("url: " + myFilePath, e);
      return new IntHashSet(0);
    }
  }

  @Override
  Resource getResource(String name) {
    if (myConfiguration.myLazyClassloadingCaches) {
      int numberOfHits = myNumberOfRequests.incrementAndGet();
      IntHashSet packagesInside = myPackageHashesInside;

      if (numberOfHits > ClasspathCache.NUMBER_OF_ACCESSES_FOR_LAZY_CACHING && packagesInside == null) {
        myPackageHashesInside = packagesInside = buildPackageHashes();
      }

      if (packagesInside != null && !packagesInside.contains(ClasspathCache.getPackageNameHash(name))) {
        return null;
      }
    }

    PreloadedJar loader = myPreloadedJar;
    if (loader != null) {
      Resource resource = loader.getResource(name);

      if (resource != null) {
        return resource;
      }

      // if memory preloader exists - do not try search inside file
      return null;
    }

    return getJarResource(name);
  }

  private Resource getJarResource(String name) {
    try {
      JarFile zipFile = getJarFile();

      try {
        JarEntry entry = zipFile.getJarEntry(name);
        if (entry != null) {
          return instantiateResource(getBaseURL(), entry);
        }
      }
      finally {
        releaseZipFile(zipFile);
      }
    }
    catch (Exception e) {
      error("url: " + myFilePath, e);
    }

    return null;
  }

  protected Resource instantiateResource(URL url, ZipEntry entry) throws IOException {
    return new MyResource(url, entry);
  }

  protected class MyResource extends Resource {
    protected final URL myUrl;
    protected final ZipEntry myEntry;

    MyResource(URL url, ZipEntry entry) throws IOException {
      myUrl = new URL(url, entry.getName());
      myEntry = entry;
    }

    @Override
    public URL getURL() {
      return myUrl;
    }

    @Override
    public InputStream getInputStream() throws IOException {
      return new UnsyncByteArrayInputStream(getBytes());
    }

    @Override
    public byte[] getBytes() throws IOException {
      ZipFile file = getJarFile();
      InputStream stream = null;
      try {
        stream = file.getInputStream(myEntry);
        return FileUtilRt.loadBytes(stream, (int)myEntry.getSize());
      }
      finally {
        if (stream != null) stream.close();
        releaseZipFile(file);
      }
    }

    @Override
    public String getValue(Attribute key) {
      loadManifestAttributes();
      return myAttributes != null ? myAttributes.get(key) : null;
    }
  }

  protected void error(String message, Throwable t) {
    if (myConfiguration.myLogErrorOnMissingJar) {
      LoggerRt.getInstance(JarLoader.class).error(message, t);
    }
    else {
      LoggerRt.getInstance(JarLoader.class).warn(message, t);
    }
  }

  private static final Object ourLock = new Object();

  protected JarFile getJarFile() throws IOException {
    // This code is executed at least 100K times (O(number of classes needed to load)) and it takes considerable time to open ZipFile's
    // such number of times so we store reference to ZipFile if we allowed to lock the file (assume it isn't changed)
    if (myConfiguration.myCanLockJars) {
      JarFile zipFile = SoftReference.dereference(myZipFileSoftReference);
      if (zipFile != null) return zipFile;

      synchronized (ourLock) {
        zipFile = SoftReference.dereference(myZipFileSoftReference);
        if (zipFile != null) return zipFile;

        // ZipFile's native implementation (ZipFile.c, zip_util.c) has path -> file descriptor cache
        zipFile = createZipFile(myFilePath);
        myZipFileSoftReference = new SoftReference<>(zipFile);
        return zipFile;
      }
    }
    else {
      return createZipFile(myFilePath);
    }
  }

  protected JarFile createZipFile(String path) throws IOException {
    return new JarFile(path);
  }

  protected void releaseZipFile(ZipFile zipFile) throws IOException {
    // Closing of zip file when myConfiguration.myCanLockJars=true happens in ZipFile.finalize
    if (!myConfiguration.myCanLockJars) {
      zipFile.close();
    }
  }

  @Override
  public String toString() {
    return "JarLoader [" + myFilePath + "]";
  }
}
