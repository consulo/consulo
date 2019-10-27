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
package com.intellij.psi.impl.smartPointers;

import com.intellij.openapi.util.Segment;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiAnchor;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

class ClsElementInfo extends SmartPointerElementInfo {
  @Nonnull
  private final PsiAnchor.StubIndexReference myStubIndexReference;

  ClsElementInfo(@Nonnull PsiAnchor.StubIndexReference stubReference) {
    myStubIndexReference = stubReference;
  }

  @Override
  PsiElement restoreElement(@Nonnull SmartPointerManagerImpl manager) {
    return myStubIndexReference.retrieve();
  }

  @Override
  int elementHashCode() {
    return myStubIndexReference.hashCode();
  }

  @Override
  boolean pointsToTheSameElementAs(@Nonnull SmartPointerElementInfo other, @Nonnull SmartPointerManagerImpl manager) {
    return other instanceof ClsElementInfo && myStubIndexReference.equals(((ClsElementInfo)other).myStubIndexReference);
  }

  @Override
  @Nonnull
  VirtualFile getVirtualFile() {
    return myStubIndexReference.getVirtualFile();
  }

  @Override
  Segment getRange(@Nonnull SmartPointerManagerImpl manager) {
    return null;
  }

  @Nullable
  @Override
  Segment getPsiRange(@Nonnull SmartPointerManagerImpl manager) {
    return null;
  }

  @Override
  PsiFile restoreFile(@Nonnull SmartPointerManagerImpl manager) {
    return myStubIndexReference.getFile();
  }

  @Override
  public String toString() {
    return myStubIndexReference.toString();
  }
}
