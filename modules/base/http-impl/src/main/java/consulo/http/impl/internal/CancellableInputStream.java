/*
 * Copyright 2013-2026 consulo.io
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
package consulo.http.impl.internal;

import consulo.application.progress.ProgressIndicator;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * A response body which stops being read once the progress of the exchange is canceled.
 *
 * @author VISTALL
 * @since 2026-11-09
 */
final class CancellableInputStream extends FilterInputStream {
    private final ProgressIndicator myIndicator;

    CancellableInputStream(InputStream in, ProgressIndicator indicator) {
        super(in);
        myIndicator = indicator;
    }

    @Override
    public int read() throws IOException {
        myIndicator.checkCanceled();
        return super.read();
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        myIndicator.checkCanceled();
        return super.read(b, off, len);
    }

    @Override
    public long skip(long n) throws IOException {
        myIndicator.checkCanceled();
        return super.skip(n);
    }
}
