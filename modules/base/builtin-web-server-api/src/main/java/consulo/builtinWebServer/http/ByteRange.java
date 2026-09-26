// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.builtinWebServer.http;

import org.jspecify.annotations.Nullable;

record ByteRange(long start, long end) {
    @Nullable ByteRange intersect(long otherStart, long otherEnd) {
        if (start <= otherStart && otherEnd <= end) {
            return new ByteRange(otherStart, otherEnd);
        }
        if (otherStart <= start && end <= otherEnd) {
            return this;
        }
        if (end <= otherStart || otherEnd <= start) {
            return null;
        }
        return new ByteRange(Math.max(start, otherStart), Math.min(end, otherEnd));
    }

    long length() {
        return end - start;
    }
}
