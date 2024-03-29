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
package consulo.index.io;

import consulo.index.io.data.DataExternalizer;

import jakarta.annotation.Nullable;
import java.io.DataOutput;
import java.io.IOException;
import java.io.DataInput;

/**
 * @author peter
 */
public class NullableDataExternalizer<T> implements DataExternalizer<T> {
  private final DataExternalizer<T> myNotNullExternalizer;

  public NullableDataExternalizer(DataExternalizer<T> externalizer) {
    myNotNullExternalizer = externalizer;
  }

  public void save(DataOutput out, T value) throws IOException {
    if (value == null) {
      out.writeBoolean(false);
    } else {
      out.writeBoolean(true);
      myNotNullExternalizer.save(out, value);
    }
  }

  @Nullable
  public T read(DataInput in) throws IOException {
    final boolean isDefined = in.readBoolean();
    if (isDefined) {
      return myNotNullExternalizer.read(in);
    }
    return null;
  }
}
