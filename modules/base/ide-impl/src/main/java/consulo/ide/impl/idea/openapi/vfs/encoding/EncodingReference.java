/*
 * Copyright 2013-2020 consulo.io
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
package consulo.ide.impl.idea.openapi.vfs.encoding;

import consulo.util.io.CharsetToolkit;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.nio.charset.Charset;
import java.util.Objects;

/**
 * from kotlin
 */
public class EncodingReference {
  public static final EncodingReference DEFAULT = new EncodingReference((Charset)null);

  @Nullable
  private final Charset charset;

  public EncodingReference(@Nullable String charsetName) {
    charset = CharsetToolkit.forName(charsetName);
  }

  public EncodingReference(@Nullable Charset charset) {
    this.charset = charset;
  }

  @Nonnull
  public Charset dereference() {
    return charset == null ? CharsetToolkit.getDefaultSystemCharset() : charset;
  }

  @Nullable
  public Charset getCharset() {
    return charset;
  }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        EncodingReference that = (EncodingReference) o;
        return Objects.equals(charset, that.charset);
    }

    @Override
    public int hashCode() {
        return Objects.hash(charset);
    }
}
