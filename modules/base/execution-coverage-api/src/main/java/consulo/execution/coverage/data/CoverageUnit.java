package consulo.execution.coverage.data;

import org.jspecify.annotations.Nullable;

import java.util.List;

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
}
