/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package consulo.virtualFileSystem;

import consulo.util.collection.Sets;
import consulo.util.io.FileUtil;
import org.jetbrains.annotations.NonNls;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Set;

/**
 * @author max
 */
public class FileAttribute {
  private static final Set<String> ourRegisteredIds = Sets.newConcurrentHashSet();
  private static final int UNDEFINED_VERSION = -1;
  private final String myId;
  private final int myVersion;
  private final boolean myFixedSize;

  public FileAttribute(@NonNls @Nonnull String id) {
    this(id, UNDEFINED_VERSION, false);
  }

  /**
   * @deprecated
   * @see #FileAttribute(String, int, boolean)
   */
  public FileAttribute(@NonNls @Nonnull String id, int version) {
    this(id, version, false);
  }

  public FileAttribute(@NonNls @Nonnull String id, int version, boolean fixedSize) {
    this(version, fixedSize, id);
    boolean added = ourRegisteredIds.add(id);
    assert added : "Attribute id='" + id+ "' is not unique";
  }

  private FileAttribute(int version, boolean fixedSize,@Nonnull String id) {
    myId = id;
    myVersion = version;
    myFixedSize = fixedSize;
  }

  @Nullable
  public DataInputStream readAttribute(@Nonnull VirtualFile file) {
    return ManagingFS.getInstance().readAttribute(file, this);
  }

  @Nonnull
  public DataOutputStream writeAttribute(@Nonnull VirtualFile file) {
    return ManagingFS.getInstance().writeAttribute(file, this);
  }

  @Nullable
  public byte[] readAttributeBytes(VirtualFile file) throws IOException {
    DataInputStream stream = readAttribute(file);
    if (stream == null) return null;

    try {
      int len = stream.readInt();
      return FileUtil.loadBytes(stream, len);
    }
    finally {
      stream.close();
    }
  }

  public void writeAttributeBytes(VirtualFile file, @Nonnull byte[] bytes) throws IOException {
    writeAttributeBytes(file, bytes, 0, bytes.length);
  }

  public void writeAttributeBytes(VirtualFile file, byte[] bytes, int offset, int len) throws IOException {
    try (DataOutputStream stream = writeAttribute(file)) {
      stream.writeInt(len);
      stream.write(bytes, offset, len);
    }
  }

  @Nonnull
  public String getId() {
    return myId;
  }

  public boolean isFixedSize() {
    return myFixedSize;
  }

  @Nonnull
  public FileAttribute newVersion(int newVersion) {
    return new FileAttribute(newVersion, myFixedSize, myId);
  }

  public int getVersion() {
    return myVersion;
  }

  public boolean isVersioned() {
    return myVersion != UNDEFINED_VERSION;
  }
}
