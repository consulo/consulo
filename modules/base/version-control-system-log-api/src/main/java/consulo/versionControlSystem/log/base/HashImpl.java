/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package consulo.versionControlSystem.log.base;

import consulo.index.io.data.DataInputOutputUtil;
import consulo.versionControlSystem.log.Hash;
import jakarta.annotation.Nonnull;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Arrays;

/**
 * <p>The {@link Hash} implementation which stores a hash in a byte array thus saving some memory.</p>
 */
public class HashImpl implements Hash {

  private static final int BASE = 16;
  private static final int SHORT_HASH_LENGTH = 7;

  @Nonnull
  private final byte[] myData;
  private final int myHashCode;

  @Nonnull
  public static Hash build(@Nonnull String inputStr) {
    byte[] data = buildData(inputStr);
    assert data.length > 0 : "Can not build hash for string " + inputStr;
    return new HashImpl(data);
  }

  @Nonnull
  public static Hash read(@Nonnull DataInput in) throws IOException {
    int length = DataInputOutputUtil.readINT(in);
    if (length == 0) throw new IOException("Can not read hash: data length is zero");
    byte[] buf = new byte[length];
    in.readFully(buf);
    return new HashImpl(buf);
  }

  public void write(@Nonnull DataOutput out) throws IOException {
    DataInputOutputUtil.writeINT(out, myData.length);
    out.write(myData);
  }

  @Nonnull
  private static byte[] buildData(@Nonnull String inputStr) {
    // if length == 5, need 3 byte + 1 signal byte
    int length = inputStr.length();
    byte even = (byte)(length % 2);
    byte[] data = new byte[length / 2 + 1 + even];
    data[0] = even;
    for (int i = 0; i < length / 2; i++) {
      int k = parseChar(inputStr, 2 * i) * BASE + parseChar(inputStr, 2 * i + 1);
      data[i + 1] = (byte)(k - 128);
    }
    if (even == 1) {
      int k = parseChar(inputStr, length - 1);
      data[length / 2 + 1] = (byte)(k - 128);
    }
    return data;
  }

  private static int parseChar(@Nonnull String inputString, int index) {
    int k = Character.digit(inputString.charAt(index), BASE);
    if (k < 0) {
      throw new IllegalArgumentException("bad hash string: " + inputString);
    }
    return k;
  }

  private HashImpl(@Nonnull byte[] hash) {
    myData = hash;
    myHashCode = Arrays.hashCode(hash);
  }

  @Nonnull
  @Override
  public String asString() {
    assert myData.length > 0 : "bad length Hash.data";
    byte even = myData[0];
    StringBuilder sb = new StringBuilder();
    for (int i = 1; i < myData.length; i++) {
      int k1 = (myData[i] + 128) / 16;
      int k2 = (myData[i] + 128) % 16;
      char c1 = Character.forDigit(k1, 16);
      char c2 = Character.forDigit(k2, 16);
      if (i == myData.length - 1 && even == 1) {
        sb.append(c2);
      }
      else {
        sb.append(c1).append(c2);
      }
    }
    return sb.toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    HashImpl that = (HashImpl)o;
    return myHashCode == that.myHashCode && Arrays.equals(myData, that.myData);
  }

  @Override
  public int hashCode() {
    return myHashCode;
  }

  @Override
  public String toString() {
    return asString();
  }

  @Nonnull
  @Override
  public String toShortString() {
    String s = asString();
    return s.substring(0, Math.min(s.length(), SHORT_HASH_LENGTH));
  }
}
