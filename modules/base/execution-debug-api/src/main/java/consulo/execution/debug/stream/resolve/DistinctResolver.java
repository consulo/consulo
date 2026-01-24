// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.execution.debug.stream.resolve;

import consulo.execution.debug.stream.trace.TraceElement;
import consulo.execution.debug.stream.trace.TraceInfo;
import jakarta.annotation.Nonnull;

import java.util.List;
import java.util.Map;

/**
 * @author Vitaliy.Bibaev
 */
public class DistinctResolver implements ValuesOrderResolver {
    @Override
    public @Nonnull Result resolve(@Nonnull TraceInfo info) {
        Map<TraceElement, List<TraceElement>> direct = info.getDirectTrace();
        Map<TraceElement, List<TraceElement>> reverse = info.getReverseTrace();
        if (direct == null || reverse == null) {
            // TODO: throw correct exception
            throw new RuntimeException();
        }

        return Result.of(direct, reverse);
    }
}
