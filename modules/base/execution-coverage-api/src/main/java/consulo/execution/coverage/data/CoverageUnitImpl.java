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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CoverageUnitImpl implements CoverageUnit {
    private final String myName;
    private List<CoverageLine> myLines = Collections.emptyList();

    public CoverageUnitImpl(String name) {
        myName = name;
    }

    @Override
    public String getName() {
        return myName;
    }

    @Nullable
    @Override
    public CoverageLine getLine(int lineNumber) {
        return lineNumber >= 0 && lineNumber < myLines.size() ? myLines.get(lineNumber) : null;
    }

    @Override
    public List<CoverageLine> getLines() {
        return myLines;
    }

    @Override
    public void setLines(List<CoverageLine> lines) {
        myLines = new ArrayList<>(lines);
    }

    @Override
    public void registerMethodSignature(CoverageLine line) {
    }
}
