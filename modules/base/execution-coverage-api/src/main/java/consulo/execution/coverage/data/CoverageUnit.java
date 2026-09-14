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
package consulo.execution.coverage.data;

import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Set;

/**
 * A named unit of coverage. For JVM engines this is a class; for file based engines it is a source file path.
 */
public interface CoverageUnit {
    String getName();

    @Nullable
    CoverageLine getLine(int lineNumber);

    /**
     * Lines indexed by line number. Entries without executable code are {@code null}.
     */
    List<CoverageLine> getLines();

    void setLines(List<CoverageLine> lines);

    void registerMethodSignature(CoverageLine line);

    Set<String> getMethodSignatures();

    /**
     * Coverage of a single method, derived from the lines carrying its signature.
     */
    LineStatus getMethodStatus(String signature);

    @Nullable
    String getSourceFile();

    void setSourceFile(@Nullable String sourceFile);
}
