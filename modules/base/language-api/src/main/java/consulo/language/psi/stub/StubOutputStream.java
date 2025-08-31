/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package consulo.language.psi.stub;

import consulo.index.io.AbstractStringEnumerator;
import consulo.index.io.data.DataInputOutputUtil;
import consulo.index.io.data.DataOutputStream;
import consulo.index.io.data.IOUtil;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.io.IOException;
import java.io.OutputStream;

/**
 * @author yole
 */
public class StubOutputStream extends DataOutputStream {
  private final AbstractStringEnumerator myNameStorage;
  private final byte[] myStringIOBuffer = IOUtil.allocReadWriteUTFBuffer();

  public StubOutputStream(@Nonnull OutputStream out, @Nonnull AbstractStringEnumerator nameStorage) {
    super(out);
    myNameStorage = nameStorage;
  }

  public void writeUTFFast(@Nonnull String arg) throws IOException {
    IOUtil.writeUTFFast(myStringIOBuffer, this, arg);
  }

  public void writeName(@Nullable String arg) throws IOException {
    int nameId = arg != null ? myNameStorage.enumerate(arg) : 0;
    DataInputOutputUtil.writeINT(this, nameId);
  }

  public void writeVarInt(int value) throws IOException {
    DataInputOutputUtil.writeINT(this, value);
  }
}
