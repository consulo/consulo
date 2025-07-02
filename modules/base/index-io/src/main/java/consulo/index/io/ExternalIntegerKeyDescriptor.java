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

import consulo.index.io.data.DataInputOutputUtil;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * @author max
 */
public class ExternalIntegerKeyDescriptor implements KeyDescriptor<Integer> {
  public int getHashCode(final Integer value) {
    return value;
  }

  public boolean isEqual(final Integer val1, final Integer val2) {
    return val1.equals(val2);
  }

  @Override
  public void save(final DataOutput out, final Integer value) throws IOException {
    DataInputOutputUtil.writeINT(out, value);
  }

  @Override
  public Integer read(final DataInput in) throws IOException {
    return DataInputOutputUtil.readINT(in);
  }
}