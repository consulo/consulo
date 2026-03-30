// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.util.nodep.classloader;

import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.ProtectionDomain;

abstract class Resource {
  public enum Attribute {
    SPEC_TITLE,
    SPEC_VERSION,
    SPEC_VENDOR,
    IMPL_TITLE,
    IMPL_VERSION,
    IMPL_VENDOR
  }

  public abstract @Nullable URL getURL();

  public abstract InputStream getInputStream() throws IOException;

  public abstract byte[] getBytes() throws IOException;

  public @Nullable String getValue(Attribute key) {
    return null;
  }

  public @Nullable ProtectionDomain getProtectionDomain() {
    return null;
  }

  @Override
  public String toString() {
    return String.valueOf(getURL());
  }
}
