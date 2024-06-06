// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.openapi.vfs.newvfs;

import consulo.util.io.FileAttributes;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.event.ChildInfo;
import consulo.ide.impl.idea.openapi.vfs.newvfs.impl.FileNameCache;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

//@ApiStatus.Internal
public class ChildInfoImpl implements ChildInfo {
  public static final int UNKNOWN_ID_YET = -1;

  private final int id;
  private final int nameId;
  private final String symLinkTarget;
  /**
   * null means children are unknown
   */
  private final
  @Nullable
  ChildInfo[] children;

  private final byte fileAttributesType;  // inlined FileAttributes to reduce memory
  private final
  @FileAttributes.Flags
  byte flags; // -1 means getFileAttributes == null
  private final long length;
  private final long lastModified;

  public ChildInfoImpl(int id, @Nonnull String name, @Nullable FileAttributes attributes, @Nullable ChildInfo[] children, @Nullable String symLinkTarget) {
    this(id, FileNameCache.storeName(name), attributes, children, symLinkTarget);
  }

  public ChildInfoImpl(int id, int nameId, @Nullable FileAttributes attributes, @Nullable ChildInfo[] children, @Nullable String symLinkTarget) {
    this.nameId = nameId;
    this.id = id;
    this.children = children;
    this.symLinkTarget = symLinkTarget;
    if (id <= 0 && id != UNKNOWN_ID_YET || nameId <= 0) throw new IllegalArgumentException("invalid arguments id: " + id + "; nameId: " + nameId);
    if (attributes == null) {
      fileAttributesType = -1;
      flags = -1;
      length = 0;
      lastModified = 0;
    }
    else {
      fileAttributesType = attributes.type == null ? -1 : (byte)attributes.type.ordinal();
      flags = attributes.flags;
      length = attributes.length;
      lastModified = attributes.lastModified;
    }
  }

  @Override
  public int getId() {
    return id;
  }

  @Nonnull
  @Override
  public CharSequence getName() {
    return FileNameCache.getVFileName(nameId);
  }

  @Override
  public int getNameId() {
    return nameId;
  }

  @Override
  public String getSymLinkTarget() {
    return symLinkTarget;
  }

  @Nullable
  @Override
  public ChildInfo[] getChildren() {
    return children;
  }

  @Override
  public FileAttributes getFileAttributes() {
    return flags == -1 ? null : FileAttributes.createFrom(fileAttributesType, flags, length, lastModified);
  }

  @Override
  public String toString() {
    return nameId + " id: " + id + " (" + getFileAttributes() + ")" + (children == null ? "" : "\n  " + StringUtil.join(children, info -> info.toString().replaceAll("\n", "\n  "), "\n  "));
  }
}