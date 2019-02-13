/*
 * Copyright 2013-2019 consulo.io
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
package consulo.externalStorage.storage;

import com.intellij.openapi.util.Pair;
import com.intellij.util.io.UnsyncByteArrayInputStream;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author VISTALL
 * @since 2019-02-11
 */
public class DataCompressorTest extends Assert {
  @Test
  public void testCompressAndUncompress() throws Exception {
    String test = "helloWorld";
    byte[] originalBytes = test.getBytes();

    byte[] compress = DataCompressor.compress(originalBytes, 1);

    Pair<byte[], Integer> uncompress = DataCompressor.uncompress(new UnsyncByteArrayInputStream(compress));

    assertArrayEquals(originalBytes, uncompress.getFirst());
    assertEquals((Integer)1, uncompress.getSecond());
  }
}
