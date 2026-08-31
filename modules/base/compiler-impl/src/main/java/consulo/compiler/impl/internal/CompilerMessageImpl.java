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
import consulo.localize.LocalizeValue;
import consulo.navigation.Navigatable;
import consulo.navigation.OpenFileDescriptorFactory;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFileManager;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

public final class CompilerMessageImpl implements CompilerMessage {
    private final Project myProject;

    private final CompilerMessageCategory myCategory;
    private @Nullable Navigatable myNavigatable;

    private final LocalizeValue myMessage;
    private final @Nullable String myUrl;
    private final int myRow;
    private final int myColumn;

    public CompilerMessageImpl(Project project, CompilerMessageCategory category, LocalizeValue message) {
        this(project, category, message, null, -1, -1, null);
    }

    public CompilerMessageImpl(
        Project project,
        CompilerMessageCategory category,
        LocalizeValue message,
        @Nullable String url,
        int row,
        int column,
        @Nullable Navigatable navigatable
    ) {
        myProject = project;
        myCategory = category;
        myNavigatable = navigatable;
        myMessage = message;
        myRow = row;
        myColumn = column;
        myUrl = url;
    }

    @Override
    public CompilerMessageCategory getCategory() {
        return myCategory;
    }

    @Override
    public LocalizeValue getMessage() {
        return myMessage;
    }

    @Override
    public @Nullable Navigatable getNavigatable() {
        if (myNavigatable != null) {
            return myNavigatable;
        }
        VirtualFile virtualFile = findVirtualFile();
        if (virtualFile != null && virtualFile.isValid()) {
            int line = getLine() - 1; // editor lines are zero-based
            if (line >= 0) {
                OpenFileDescriptorFactory factory = OpenFileDescriptorFactory.getInstance(myProject);
                return myNavigatable = factory.newBuilder(virtualFile).line(line).column(Math.max(0, getColumn() - 1)).build();
            }
        }
        return null;
    }

    private @Nullable VirtualFile findVirtualFile() {
        if (myUrl == null) {
            return null;
        }
        VirtualFileManager virtualFileManager = VirtualFileManager.getInstance();
        VirtualFile file = virtualFileManager.findFileByUrl(myUrl);
        if (file == null) {
            // generated sources may be placed in completely random directories which aren't refreshed automatically
            return virtualFileManager.refreshAndFindFileByUrl(myUrl);
        }
        return file;
    }

    @Override
    public @Nullable String getUrl() {
        return myUrl;
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
            && Objects.equals(myUrl, that.myUrl)
            && myMessage.equals(that.myMessage);
    }

    @Override
    public int hashCode() {
        int result = myCategory.hashCode();
        result = 29 * result + myMessage.hashCode();
        result = 29 * result + (myUrl != null ? myUrl.hashCode() : 0);
        result = 29 * result + myRow;
        return 29 * result + myColumn;
    }

    @Override
    public String toString() {
        return myMessage.get();
    }
}
