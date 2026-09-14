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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class CoverageUnitImpl implements CoverageUnit {
    private final String myName;
    private final List<CoverageLine> myLines = new ArrayList<>();
    private final Set<String> myMethodSignatures = new LinkedHashSet<>();

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
        myLines.clear();
        myLines.addAll(lines);
    }

    @Override
    public void registerMethodSignature(CoverageLine line) {
        String signature = line.getMethodSignature();
        if (signature != null) {
            myMethodSignatures.add(signature);
        }
    }

    @Override
    public Set<String> getMethodSignatures() {
        return myMethodSignatures;
    }

    public void merge(CoverageUnit data) {
        List<CoverageLine> otherLines = data.getLines();
        while (myLines.size() < otherLines.size()) {
            myLines.add(null);
        }
        for (int i = 0; i < otherLines.size(); i++) {
            CoverageLine other = otherLines.get(i);
            if (other == null) {
                continue;
            }
            CoverageLine mine = myLines.get(i);
            if (mine instanceof CoverageLineImpl lineImpl) {
                lineImpl.merge(other);
            }
            else {
                CoverageLineImpl copy = new CoverageLineImpl(other.getLineNumber(), other.getMethodSignature());
                copy.merge(other);
                myLines.set(i, copy);
            }
        }
        myMethodSignatures.addAll(data.getMethodSignatures());
    }
}
