/*
 * Copyright 2013-2024 consulo.io
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
package consulo.component.store.impl.internal.storage.nio;

import consulo.platform.LineSeparator;
import jakarta.annotation.Nonnull;

import java.io.FilterReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Objects;

/**
 * @author VISTALL
 * @since 2024-08-10
 */
public class DetectingSeparatorReader extends FilterReader {
    private LineSeparator myLineSeparator;

    public DetectingSeparatorReader(Reader in) {
        super(in);
    }

    @Nonnull
    public LineSeparator getLineSeparator(@Nonnull LineSeparator defaultSeparator) {
        return Objects.requireNonNullElse(myLineSeparator, defaultSeparator);
    }

    @Override
    public int read() throws IOException {
        int c = super.read();
        if (myLineSeparator == null) {
            detect((char) c);
        }
        return c;
    }

    @Override
    public int read(char[] cbuf, int off, int len) throws IOException {
        int read = super.read(cbuf, off, len);
        if (myLineSeparator == null) {
            for (int i = off; i < len; i++) {
                char c = cbuf[i];
                if (detect(c)) {
                    break;
                }
            }
        }
        return read;
    }


    private boolean detect(char c) {
        if (c == '\r') {
            myLineSeparator = LineSeparator.CRLF;
            return true;
        }
        else if (c == '\n') {
            // if we are here, there was no \r before
            myLineSeparator = LineSeparator.LF;
            return true;
        }
        return false;
    }
}
