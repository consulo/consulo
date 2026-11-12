// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.build.ui;

import java.io.File;
import java.nio.file.Path;

/**
 * 0-based position in file.
 *
 * @param path        file - a path of the machine the build ran on, which may be a remote one
 * @param startLine   0-based start line number
 * @param startColumn 0-based start column number
 * @param endLine     0-based end line number
 * @param endColumn   0-based end column number
 *
 * @author Vladislav.Soroka
 */
public record FilePosition(Path path, int startLine, int startColumn, int endLine, int endColumn) {
    public FilePosition(Path path) {
        this(path, 0, 0);
    }

    /**
     * @param path   file
     * @param line   0-based line number
     * @param column 0-based column number
     */
    public FilePosition(Path path, int line, int column) {
        this(path, line, column, line, column);
    }

    public FilePosition(File file, int startLine, int startColumn, int endLine, int endColumn) {
        this(file.toPath(), startLine, startColumn, endLine, endColumn);
    }

    public FilePosition(File file) {
        this(file.toPath());
    }

    public FilePosition(File file, int line, int column) {
        this(file.toPath(), line, column);
    }

    public Path getPath() {
        return path();
    }

    /**
     * @deprecated a position of a remote build has no local file, use {@link #getPath()}
     */
    @Deprecated
    public File getFile() {
        return path().toFile();
    }

    public int getStartLine() {
        return startLine();
    }

    public int getStartColumn() {
        return startColumn();
    }

    public int getEndLine() {
        return endLine();
    }

    public int getEndColumn() {
        return endColumn();
    }
}
