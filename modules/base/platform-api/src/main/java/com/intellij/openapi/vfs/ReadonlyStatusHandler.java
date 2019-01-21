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
package com.intellij.openapi.vfs;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.AsyncResult;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import consulo.annotations.DeprecationInfo;
import consulo.annotations.RequiredReadAction;
import consulo.ui.UIAccess;

import javax.annotation.Nonnull;
import java.util.Collection;

public abstract class ReadonlyStatusHandler {
  public abstract static class OperationStatus {
    @Nonnull
    public abstract VirtualFile[] getReadonlyFiles();

    public abstract boolean hasReadonlyFiles();

    @Nonnull
    public abstract String getReadonlyFilesMessage();
  }

  public static ReadonlyStatusHandler getInstance(Project project) {
    return ServiceManager.getService(project, ReadonlyStatusHandler.class);
  }

  @Nonnull
  @RequiredReadAction
  public static AsyncResult<Void> ensureFilesWritableAsync(@Nonnull Project project, @Nonnull UIAccess uiAccess, @Nonnull VirtualFile... files) {
    AsyncResult<OperationStatus> result = getInstance(project).ensureFilesWritableAsync(uiAccess, files);
    AsyncResult<Void> boolResult = AsyncResult.undefined();
    result.doWhenDone((s) -> {
      if (!s.hasReadonlyFiles()) {
        boolResult.setDone();
      }
      else {
        boolResult.setRejected();
      }
    });
    return boolResult;
  }

  @Nonnull
  @RequiredReadAction
  public static AsyncResult<Void> ensureDocumentWritableAsync(@Nonnull Project project, @Nonnull UIAccess uiAccess, @Nonnull Document document) {
    final PsiFile psiFile = PsiDocumentManager.getInstance(project).getPsiFile(document);
    AsyncResult<Void> okWritable;
    if (psiFile == null) {
      okWritable = document.isWritable() ? AsyncResult.resolved() : AsyncResult.rejected();
    }
    else {
      final VirtualFile virtualFile = psiFile.getVirtualFile();
      if (virtualFile != null) {
        okWritable = ensureFilesWritableAsync(project, uiAccess, virtualFile);
      }
      else {
        okWritable = psiFile.isWritable() ? AsyncResult.resolved() : AsyncResult.rejected();
      }
    }
    return okWritable;
  }

  @Nonnull
  @RequiredReadAction
  public abstract AsyncResult<OperationStatus> ensureFilesWritableAsync(@Nonnull UIAccess uiAccess, @Nonnull VirtualFile... files);

  @RequiredReadAction
  @Nonnull
  public AsyncResult<OperationStatus> ensureFilesWritableAsync(@Nonnull UIAccess uiAccess, @Nonnull Collection<VirtualFile> files) {
    return ensureFilesWritableAsync(uiAccess, VfsUtilCore.toVirtualFileArray(files));
  }

  // region Deprecated staff

  @RequiredReadAction
  @Deprecated
  @DeprecationInfo("Use #ensureFilesWritableAsync()")
  public static boolean ensureFilesWritable(@Nonnull Project project, @Nonnull VirtualFile... files) {
    return !getInstance(project).ensureFilesWritable(files).hasReadonlyFiles();
  }

  @RequiredReadAction
  @Deprecated
  @DeprecationInfo("Use #ensureFilesWritableAsync()")
  public static boolean ensureDocumentWritable(@Nonnull Project project, @Nonnull Document document) {
    final PsiFile psiFile = PsiDocumentManager.getInstance(project).getPsiFile(document);
    boolean okWritable;
    if (psiFile == null) {
      okWritable = document.isWritable();
    }
    else {
      final VirtualFile virtualFile = psiFile.getVirtualFile();
      if (virtualFile != null) {
        okWritable = ensureFilesWritable(project, virtualFile);
      }
      else {
        okWritable = psiFile.isWritable();
      }
    }
    return okWritable;
  }

  @Deprecated
  @DeprecationInfo("Use #ensureFilesWritableAsync()")
  public abstract OperationStatus ensureFilesWritable(@Nonnull VirtualFile... files);

  @Deprecated
  @DeprecationInfo("Use #ensureFilesWritableAsync()")
  public OperationStatus ensureFilesWritable(@Nonnull Collection<VirtualFile> files) {
    return ensureFilesWritable(VfsUtilCore.toVirtualFileArray(files));
  }

  // endregion
}
