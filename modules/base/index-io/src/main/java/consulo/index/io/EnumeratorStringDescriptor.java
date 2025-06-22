/*
 * Copyright 2000-2014 JetBrains s.r.o.
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

import consulo.index.io.data.IOUtil;

import jakarta.annotation.Nonnull;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * @author Eugene Zhuravlev
 * @since 2007-12-18
 */
public class EnumeratorStringDescriptor implements KeyDescriptor<String> {
  public static final EnumeratorStringDescriptor INSTANCE = new EnumeratorStringDescriptor();

  @Override
  public void save(@Nonnull final DataOutput storage, @Nonnull final String value) throws IOException {
    IOUtil.writeUTF(storage, value);
  }

  @Override
  public String read(@Nonnull final DataInput storage) throws IOException {
    return IOUtil.readUTF(storage);
  }
}
