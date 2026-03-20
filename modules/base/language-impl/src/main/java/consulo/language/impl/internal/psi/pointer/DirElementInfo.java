/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package consulo.language.impl.internal.psi.pointer;

import consulo.document.util.Segment;
import consulo.language.psi.PsiDirectory;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;

import org.jspecify.annotations.Nullable;
import java.util.Objects;

class DirElementInfo extends SmartPointerElementInfo {
  
  private final VirtualFile myVirtualFile;
  
  private final Project myProject;

  DirElementInfo(PsiDirectory directory) {
    myProject = directory.getProject();
    myVirtualFile = directory.getVirtualFile();
  }

  @Override
  PsiElement restoreElement(SmartPointerManagerImpl manager) {
    return SelfElementInfo.restoreDirectoryFromVirtual(myVirtualFile, myProject);
  }

  @Override
  PsiFile restoreFile(SmartPointerManagerImpl manager) {
    return null;
  }

  @Override
  int elementHashCode() {
    return myVirtualFile.hashCode();
  }

  @Override
  boolean pointsToTheSameElementAs(SmartPointerElementInfo other, SmartPointerManagerImpl manager) {
    return other instanceof DirElementInfo && Objects.equals(myVirtualFile, ((DirElementInfo)other).myVirtualFile);
  }

  
  @Override
  VirtualFile getVirtualFile() {
    return myVirtualFile;
  }

  @Override
  Segment getRange(SmartPointerManagerImpl manager) {
    return null;
  }

  @Nullable
  @Override
  Segment getPsiRange(SmartPointerManagerImpl manager) {
    return null;
  }

  @Override
  public String toString() {
    return "dir{" + myVirtualFile + "}";
  }
}
