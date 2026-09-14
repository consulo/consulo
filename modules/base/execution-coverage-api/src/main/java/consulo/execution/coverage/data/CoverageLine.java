package consulo.execution.coverage.data;

import org.jspecify.annotations.Nullable;

public interface CoverageLine {
    int getLineNumber();

    LineStatus getStatus();

    void setStatus(LineStatus status);

    int getHits();

    @Nullable
    String getMethodSignature();

    boolean isCoveredBySingleTest();
}
