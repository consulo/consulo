// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.openapi.vfs.impl;

import consulo.ide.impl.idea.openapi.util.io.FileUtil;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;

/**
 * Node which stores explicit 'name' instead of nameId.
 * The latter can be unavailable (e.g. when creating the pointer from the url of non-yet-existing file)
 * or incorrect (e.g. when creating the pointer from the url "/x/y/Z.TXT" for the file "z.txt" on case-insensitive file system)
 * As soon as the corresponding file got created, this UrlPartNode is replaced with FilePointerPartNode, which contains nameId and is faster and more succinct
 */
class UrlPartNode extends FilePointerPartNode {
  @Nonnull
  private final String name;

  UrlPartNode(@Nonnull String name, @Nonnull FilePointerPartNode parent) {
    super(parent);
    this.name = name;
    assert !StringUtil.isEmptyOrSpaces(name) : '\'' + name + '\'';
  }

  @Override
  boolean urlEndsWithName(@Nonnull String urlAfter, VirtualFile fileAfter) {
    return StringUtil.endsWith(urlAfter, getName());
  }

  @Nonnull
  @Override
  CharSequence getName() {
    return name;
  }

  @Override
  boolean nameEqualTo(int nameId) {
    return FileUtil.PATH_CHAR_SEQUENCE_HASHING_STRATEGY.equals(getName(), fromNameId(nameId));
  }

  @Override
  public String toString() {
    return "UrlPartNode: '" + getName() + "' -> " + children.length;
  }
}
