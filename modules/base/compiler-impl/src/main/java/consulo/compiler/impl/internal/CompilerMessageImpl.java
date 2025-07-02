/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package consulo.compiler.impl.internal;

import consulo.compiler.CompilerMessage;
import consulo.compiler.CompilerMessageCategory;
import consulo.compiler.localize.CompilerLocalize;
import consulo.navigation.Navigatable;
import consulo.navigation.OpenFileDescriptorFactory;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Objects;

public final class CompilerMessageImpl implements CompilerMessage {
    @Nonnull
    private final Project myProject;
    @Nonnull
    private final CompilerMessageCategory myCategory;
    @Nullable
    private Navigatable myNavigatable;
    @Nonnull
    private final String myMessage;
    @Nullable
    private final VirtualFile myFile;
    private final int myRow;
    private final int myColumn;

    public CompilerMessageImpl(@Nonnull Project project, @Nonnull CompilerMessageCategory category, @Nullable String message) {
        this(project, category, message, null, -1, -1, null);
    }

    public CompilerMessageImpl(
        @Nonnull Project project,
        @Nonnull CompilerMessageCategory category,
        @Nullable String message,
        @Nullable VirtualFile file,
        int row,
        int column,
        @Nullable Navigatable navigatable
    ) {
        myProject = project;
        myCategory = category;
        myNavigatable = navigatable;
        myMessage = message == null ? "" : message;
        myRow = row;
        myColumn = column;
        myFile = file;
    }

    @Nonnull
    @Override
    public CompilerMessageCategory getCategory() {
        return myCategory;
    }

    @Nonnull
    @Override
    public String getMessage() {
        return myMessage;
    }

    @Nullable
    @Override
    public Navigatable getNavigatable() {
        if (myNavigatable != null) {
            return myNavigatable;
        }
        VirtualFile virtualFile = getVirtualFile();
        if (virtualFile != null && virtualFile.isValid()) {
            int line = getLine() - 1; // editor lines are zero-based
            if (line >= 0) {
                OpenFileDescriptorFactory factory = OpenFileDescriptorFactory.getInstance(myProject);
                return myNavigatable = factory.newBuilder(virtualFile).line(line).column(Math.max(0, getColumn() - 1)).build();
            }
        }
        return null;
    }

    @Override
    public VirtualFile getVirtualFile() {
        return myFile;
    }

    @Override
    public String getExportTextPrefix() {
        if (getLine() >= 0) {
            return CompilerLocalize.compilerResultsExportTextPrefix(getLine()).get();
        }
        return "";
    }

    @Override
    public String getRenderTextPrefix() {
        if (getLine() >= 0) {
            return "(" + getLine() + ", " + getColumn() + ")";
        }
        return "";
    }

    @Override
    public int getLine() {
        return myRow;
    }

    @Override
    public int getColumn() {
        return myColumn;
    }

    @Override
    public boolean equals(Object o) {
        return this == o
            || o instanceof CompilerMessageImpl that
            && myColumn == that.myColumn
            && myRow == that.myRow
            && myCategory.equals(that.myCategory)
            && Objects.equals(myFile, that.myFile)
            && myMessage.equals(that.myMessage);
    }

    @Override
    public int hashCode() {
        int result = myCategory.hashCode();
        result = 29 * result + myMessage.hashCode();
        result = 29 * result + (myFile != null ? myFile.hashCode() : 0);
        result = 29 * result + myRow;
        return 29 * result + myColumn;
    }

    @Override
    public String toString() {
        return myMessage;
    }
}
