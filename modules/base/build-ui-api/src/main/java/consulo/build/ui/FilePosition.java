// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.build.ui;

import java.io.File;

/**
 * 0-based position in file.
 *
 * @param file        file
 * @param startLine   0-based start line number
 * @param startColumn 0-based start column number
 * @param endLine     0-based end line number
 * @param endColumn   0-based end column number
 *
 * @author Vladislav.Soroka
 */
public record FilePosition(File file, int startLine, int startColumn, int endLine, int endColumn) {
    /**
     * @param file file
     */
    public FilePosition(File file) {
        this(file, 0, 0);
    }

    /**
     * @param file   file
     * @param line   0-based line number
     * @param column 0-based column number
     */
    public FilePosition(File file, int line, int column) {
        this(file, line, column, line, column);
    }

    public File getFile() {
        return file();
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
