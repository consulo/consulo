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
