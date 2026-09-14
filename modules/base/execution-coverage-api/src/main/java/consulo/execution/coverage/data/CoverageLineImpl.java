package consulo.execution.coverage.data;

import org.jspecify.annotations.Nullable;

public class CoverageLineImpl implements CoverageLine {
    private final int myLineNumber;
    private final String myMethodSignature;
    private LineStatus myStatus = LineStatus.NOT_COVERED;
    private int myHits;

    public CoverageLineImpl(int lineNumber, @Nullable String methodSignature) {
        myLineNumber = lineNumber;
        myMethodSignature = methodSignature;
    }

    @Override
    public int getLineNumber() {
        return myLineNumber;
    }

    @Override
    public LineStatus getStatus() {
        return myStatus;
    }

    @Override
    public void setStatus(LineStatus status) {
        myStatus = status;
    }

    @Override
    public int getHits() {
        return myHits;
    }

    public void setHits(int hits) {
        myHits = hits;
    }

    @Nullable
    @Override
    public String getMethodSignature() {
        return myMethodSignature;
    }

    @Override
    public boolean isCoveredBySingleTest() {
        return false;
    }
}
