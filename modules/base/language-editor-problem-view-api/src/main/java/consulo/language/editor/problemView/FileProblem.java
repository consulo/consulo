package consulo.language.editor.problemView;

import consulo.virtualFileSystem.VirtualFile;

public interface FileProblem extends Problem {
    /**
     * The file that the problem belongs to.
     */
    
    VirtualFile getFile();

    /**
     * Zero-based line number in the corresponding file,
     * or -1 if there is no problem line to navigate.
     */
    default int getLine() {
        return -1;
    }

    /**
     * Zero-based column number in the corresponding file.
     */
    default int getColumn() {
        return -1;
    }
}
