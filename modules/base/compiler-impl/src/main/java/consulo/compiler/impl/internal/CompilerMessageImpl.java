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

import consulo.compiler.CompilerBundle;
import consulo.compiler.CompilerMessage;
import consulo.compiler.CompilerMessageCategory;
import consulo.navigation.Navigatable;
import consulo.navigation.OpenFileDescriptorFactory;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nullable;

public final class CompilerMessageImpl implements CompilerMessage {

  private final Project myProject;
  private final CompilerMessageCategory myCategory;
  @Nullable
  private Navigatable myNavigatable;
  private final String myMessage;
  private VirtualFile myFile;
  private final int myRow;
  private final int myColumn;

  public CompilerMessageImpl(Project project, CompilerMessageCategory category, String message) {
    this(project, category, message, null, -1, -1, null);
  }

  public CompilerMessageImpl(Project project,
                             CompilerMessageCategory category,
                             String message,
                             @Nullable final VirtualFile file,
                             int row,
                             int column,
                             @Nullable final Navigatable navigatable) {
    myProject = project;
    myCategory = category;
    myNavigatable = navigatable;
    myMessage = message == null ? "" : message;
    myRow = row;
    myColumn = column;
    myFile = file;
  }

  @Override
  public CompilerMessageCategory getCategory() {
    return myCategory;
  }

  @Override
  public String getMessage() {
    return myMessage;
  }

  @Override
  public Navigatable getNavigatable() {
    if (myNavigatable != null) {
      return myNavigatable;
    }
    final VirtualFile virtualFile = getVirtualFile();
    if (virtualFile != null && virtualFile.isValid()) {
      final int line = getLine() - 1; // editor lines are zero-based
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
      return CompilerBundle.message("compiler.results.export.text.prefix", getLine());
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
    if (this == o) return true;
    if (!(o instanceof CompilerMessage)) return false;

    final CompilerMessageImpl compilerMessage = (CompilerMessageImpl)o;

    if (myColumn != compilerMessage.myColumn) return false;
    if (myRow != compilerMessage.myRow) return false;
    if (!myCategory.equals(compilerMessage.myCategory)) return false;
    if (myFile != null ? !myFile.equals(compilerMessage.myFile) : compilerMessage.myFile != null) return false;
    if (!myMessage.equals(compilerMessage.myMessage)) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result;
    result = myCategory.hashCode();
    result = 29 * result + myMessage.hashCode();
    result = 29 * result + (myFile != null ? myFile.hashCode() : 0);
    result = 29 * result + myRow;
    result = 29 * result + myColumn;
    return result;
  }

  @Override
  public String toString() {
    return myMessage;
  }
}
