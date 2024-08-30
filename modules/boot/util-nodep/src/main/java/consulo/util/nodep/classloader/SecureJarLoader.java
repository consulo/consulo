// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.util.nodep.classloader;

import consulo.util.nodep.ArrayUtilRt;
import consulo.util.nodep.io.FileUtilRt;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.CodeSigner;
import java.security.CodeSource;
import java.security.Permissions;
import java.security.ProtectionDomain;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

class SecureJarLoader extends JarLoader {
  private ProtectionDomain myProtectionDomain;
  private final Object myProtectionDomainMonitor = new Object();

  SecureJarLoader(URL url, int index, ClassPath configuration, Set<String> fullJarIndex) throws IOException {
    super(url, index, configuration, fullJarIndex);
  }

  @Override
  protected MemoryResource createMemoryResource(URL baseUrl, ZipFile zipFile, ZipEntry entry, Map<Resource.Attribute, String> attributes) throws IOException {
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

    JarEntry jarEntry = (JarEntry)entry;

    CodeSigner[] codeSigners = jarEntry.getCodeSigners();
    return new MySecureMemoryResource(url, content, attributes, codeSigners);
  }

  @Override
  protected Resource instantiateResource(URL url, ZipEntry entry) throws IOException {
    return new MySecureResource(url, (JarEntry)entry);
  }
  private class MySecureMemoryResource extends MemoryResource {
    private final CodeSigner[] myCodeSigners;

    MySecureMemoryResource(URL url, byte[] content, Map<Attribute, String> attributes, CodeSigner[] codeSigners) {
      super(url, content, attributes);
      myCodeSigners = codeSigners;
    }


    @Override
    public ProtectionDomain getProtectionDomain() {
      synchronized (myProtectionDomainMonitor) {
        if (myProtectionDomain == null) {
          CodeSource codeSource = new CodeSource(myUrl, myCodeSigners);
          myProtectionDomain = new ProtectionDomain(codeSource, new Permissions());
        }

        return myProtectionDomain;
      }
    }
  }

  private class MySecureResource extends JarLoader.MyResource {
    MySecureResource(URL url, JarEntry entry) throws IOException {
      super(url, entry);
    }

    @Override
    public byte[] getBytes() throws IOException {
      JarFile file = (JarFile)getJarFile();
      InputStream stream = null;
      byte[] result;
      try {
        stream = file.getInputStream(myEntry);
        result = FileUtilRt.loadBytes(stream, (int)myEntry.getSize());
        synchronized (myProtectionDomainMonitor) {
          if (myProtectionDomain == null) {
            JarEntry jarEntry = file.getJarEntry(myEntry.getName());
            CodeSource codeSource = new CodeSource(myUrl, jarEntry.getCodeSigners());
            myProtectionDomain = new ProtectionDomain(codeSource, new Permissions());
          }
        }
      }
      finally {
        if (stream != null) stream.close();
        releaseZipFile(file);
      }
      return result;
    }


    @Override
    public ProtectionDomain getProtectionDomain() {
      synchronized (myProtectionDomainMonitor) {
        return myProtectionDomain;
      }
    }
  }
}
