/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

package consulo.virtualFileSystem.internal.matcher;

import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.fileType.matcher.ExtensionFileNameMatcher;
import jakarta.annotation.Nonnull;

/**
 * @author max
 */
public class ExtensionFileNameMatcherImpl implements ExtensionFileNameMatcher {
  private final String myExtension;
  private final String myDotExtension;

  public ExtensionFileNameMatcherImpl(@Nonnull String extension) {
    myExtension = StringUtil.toLowerCase(extension);
    myDotExtension = "." + myExtension;
  }

  @Override
  public boolean acceptsCharSequence(@Nonnull CharSequence fileName) {
    return StringUtil.endsWithIgnoreCase(fileName, myDotExtension);
  }

  @Override
  @Nonnull
  public String getPresentableString() {
    return "*." + myExtension;
  }

  @Nonnull
  @Override
  public String getExtension() {
    return myExtension;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    ExtensionFileNameMatcherImpl that = (ExtensionFileNameMatcherImpl)o;

    if (!myExtension.equals(that.myExtension)) return false;

    return true;
  }

  @Override
  public int hashCode() {
    return myExtension.hashCode();
  }

  @Override
  public String toString() {
    return getPresentableString();
  }
}
