/*
 * Copyright 2000-2017 JetBrains s.r.o.
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
package consulo.index.io;

import consulo.index.io.data.DataInputOutputUtil;
import org.jetbrains.annotations.Contract;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * @author max
 */
public class StringRef {
  public static final StringRef[] EMPTY_ARRAY = new StringRef[0];

  private int id;
  private String name;
  private final AbstractStringEnumerator store;

  private StringRef(@Nonnull String name) {
    this.name = name;
    id = -1;
    store = null;
  }

  private StringRef(final int id, @Nonnull AbstractStringEnumerator store) {
    this.id = id;
    this.store = store;
  }

  public String getString() {
    String name = this.name;
    if (name == null) {
      try {
        this.name = name = store.valueOf(id);
      }
      catch (IOException e) {
        store.markCorrupted();
        throw new RuntimeException(e);
      }
    }
    return name;
  }

  public void writeTo(@Nonnull DataOutput out, @Nonnull AbstractStringEnumerator store) throws IOException {
    int nameId = getId(store);
    out.writeByte(nameId & 0xFF);
    DataInputOutputUtil.writeINT(out, nameId >> 8);
  }

  public int getId(@Nonnull AbstractStringEnumerator store) {
    if (id == -1) {
      try {
        id = store.enumerate(name);
      }
      catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
    return id;
  }

  @Override
  public String toString() {
    return getString();
  }

  public int length() {
    return getString().length();
  }

  @Override
  public int hashCode() {
    return toString().hashCode();
  }

  @Override
  public boolean equals(final Object that) {
    return that == this || that instanceof StringRef && toString().equals(that.toString());
  }

  @Contract("null -> null")
  public static String toString(@Nullable StringRef ref) {
    return ref != null ? ref.getString() : null;
  }

  @Contract("null -> null; !null -> !null")
  public static StringRef fromString(@Nullable String source) {
    return source == null ? null : new StringRef(source);
  }

  @Nonnull
  public static StringRef fromNullableString(@Nullable String source) {
    return new StringRef(source == null ? "" : source);
  }

  @Nullable
  public static StringRef fromStream(@Nonnull DataInput in, @Nonnull AbstractStringEnumerator store) throws IOException {
    final int nameId = DataInputOutputUtil.readINT(in);

    return nameId != 0 ? new StringRef(nameId, store) : null;
  }

  @Nullable
  public static String stringFromStream(@Nonnull DataInput in, @Nonnull AbstractStringEnumerator store) throws IOException {
    final int nameId = DataInputOutputUtil.readINT(in);
    return nameId != 0 ? store.valueOf(nameId) : null;
  }

  @Nonnull
  public static StringRef[] createArray(int count) {
    return count == 0 ? EMPTY_ARRAY : new StringRef[count];
  }
}
