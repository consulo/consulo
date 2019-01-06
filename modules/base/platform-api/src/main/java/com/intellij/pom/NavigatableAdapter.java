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
package com.intellij.pom;

import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.AsyncResult;
import com.intellij.openapi.vfs.VirtualFile;

import javax.annotation.Nonnull;

/**
 * Very often both methods <code>canNavigate</code> and <code>canNavigateToSource</code>
 * return <code>true</code>. This adapter class lets focus on navigation
 * routine only.
 *
 * @author Konstantin Bulenkov
 */
public abstract class NavigatableAdapter implements Navigatable {
  @Override
  public boolean canNavigate() {
    return true;
  }

  @Override
  public boolean canNavigateToSource() {
    return true;
  }

  @Deprecated
  public static void navigate(Project project, VirtualFile file, boolean requestFocus) {
    navigate(project, file, 0, requestFocus);
  }

  @Deprecated
  public static void navigate(Project project, VirtualFile file, int offset, boolean requestFocus) {
    new OpenFileDescriptor(project, file, offset).navigate(requestFocus);
  }

  @Nonnull
  public static AsyncResult<Void> navigateAsync(Project project, VirtualFile file, boolean requestFocus) {
    return navigateAsync(project, file, 0, requestFocus);
  }

  @Nonnull
  public static AsyncResult<Void> navigateAsync(Project project, VirtualFile file, int offset, boolean requestFocus) {
    return new OpenFileDescriptor(project, file, offset).navigateAsync(requestFocus);
  }
}
