package consulo.util.io.internal;

import consulo.util.io.Url;
import consulo.util.io.Urls;
import consulo.util.lang.StringUtil;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public final class LocalFileUrl implements Url {
  private final String path;

  /**
   * Use {@link Urls#newLocalFileUrl(String)} instead
   */
  public LocalFileUrl(@Nonnull String path) {
    this.path = path;
  }

  @Nonnull
  @Override
  public String getPath() {
    return path;
  }

  @Override
  public boolean isInLocalFileSystem() {
    return true;
  }

  @Override
  public String toDecodedForm() {
    return path;
  }

  @Nonnull
  @Override
  public String toExternalForm() {
    return path;
  }

  @Nullable
  @Override
  public String getScheme() {
    return null;
  }

  @Nullable
  @Override
  public String getAuthority() {
    return null;
  }

  @Nullable
  @Override
  public String getParameters() {
    return null;
  }

  @Nonnull
  @Override
  public Url trimParameters() {
    return this;
  }

  @Override
  public String toString() {
    return toExternalForm();
  }

  @Override
  public boolean equals(Object o) {
    return this == o || (o instanceof LocalFileUrl localFileUrl && path.equals(localFileUrl.path));
  }

  @Override
  public boolean equalsIgnoreCase(@Nullable Url o) {
    return this == o || (o instanceof LocalFileUrl localFileUrl && path.equalsIgnoreCase(localFileUrl.path));
  }

  @Override
  public boolean equalsIgnoreParameters(@Nullable Url url) {
    return equals(url);
  }

  @Override
  public int hashCode() {
    return path.hashCode();
  }

  @Override
  public int hashCodeCaseInsensitive() {
    return StringUtil.stringHashCodeInsensitive(path);
  }
}