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
package consulo.process.io;

import jakarta.annotation.Nonnull;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

/**
 * @author traff
 */
public class BaseInputStreamReader extends InputStreamReader {
  private final InputStream myInputStream;

  public BaseInputStreamReader(@Nonnull InputStream in) {
    super(in);
    myInputStream = in;
  }

  public BaseInputStreamReader(@Nonnull InputStream in, @Nonnull Charset cs) {
    super(in, cs);
    myInputStream = in;
  }

  @Override
  public void close() throws IOException {
    myInputStream.close(); // close underlying input stream without locking in StreamDecoder.
  }
}