/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package consulo.ide.impl.idea.diff;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.diff.merge.MergeRequest;
import consulo.diff.merge.MergeResult;
import consulo.ide.impl.idea.diff.merge.TextMergeRequest;
import consulo.diff.request.ContentDiffRequest;
import consulo.ide.ServiceManager;
import consulo.document.Document;
import consulo.virtualFileSystem.fileType.FileType;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import java.util.function.Consumer;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.List;

/**
 * Use ProgressManager.executeProcessUnderProgress() to pass modality state if needed
 */
@ServiceAPI(ComponentScope.APPLICATION)
public abstract class DiffRequestFactory {
  @Nonnull
  public static DiffRequestFactory getInstance() {
    return ServiceManager.getService(DiffRequestFactory.class);
  }

  //
  // Diff
  //

  @Nonnull
  public abstract ContentDiffRequest createFromFiles(@Nullable Project project, @Nonnull VirtualFile file1, @Nonnull VirtualFile file2);

  @Nonnull
  public abstract ContentDiffRequest createFromFiles(@Nullable Project project,
                                                     @Nonnull VirtualFile leftFile,
                                                     @Nonnull VirtualFile baseFile,
                                                     @Nonnull VirtualFile rightFile);

  @Nonnull
  public abstract ContentDiffRequest createClipboardVsValue(@Nonnull String value);

  //
  // Titles
  //

  @Nonnull
  public abstract String getContentTitle(@Nonnull VirtualFile file);

  @Nonnull
  public abstract String getTitle(@Nonnull VirtualFile file1, @Nonnull VirtualFile file2);

  @Nonnull
  public abstract String getTitle(@Nonnull VirtualFile file);

  //
  // Merge
  //

  @Nonnull
  public abstract MergeRequest createMergeRequest(@Nullable Project project,
                                                  @Nullable FileType fileType,
                                                  @Nonnull Document output,
                                                  @Nonnull List<String> textContents,
                                                  @Nullable String title,
                                                  @Nonnull List<String> titles,
                                                  @Nullable Consumer<MergeResult> applyCallback) throws InvalidDiffRequestException;

  @Nonnull
  public abstract MergeRequest createMergeRequest(@Nullable Project project,
                                                  @Nonnull VirtualFile output,
                                                  @Nonnull List<byte[]> byteContents,
                                                  @Nullable String title,
                                                  @Nonnull List<String> contentTitles,
                                                  @Nullable Consumer<MergeResult> applyCallback) throws InvalidDiffRequestException;

  @Nonnull
  public abstract TextMergeRequest createTextMergeRequest(@Nullable Project project,
                                                          @Nonnull VirtualFile output,
                                                          @Nonnull List<byte[]> byteContents,
                                                          @Nullable String title,
                                                          @Nonnull List<String> contentTitles,
                                                          @Nullable Consumer<MergeResult> applyCallback) throws InvalidDiffRequestException;

  @Nonnull
  public abstract MergeRequest createBinaryMergeRequest(@Nullable Project project,
                                                        @Nonnull VirtualFile output,
                                                        @Nonnull List<byte[]> byteContents,
                                                        @Nullable String title,
                                                        @Nonnull List<String> contentTitles,
                                                        @Nullable Consumer<MergeResult> applyCallback) throws InvalidDiffRequestException;

  @Nonnull
  public abstract MergeRequest createMergeRequestFromFiles(@Nullable Project project,
                                                           @Nonnull VirtualFile output,
                                                           @Nonnull List<VirtualFile> contents,
                                                           @Nullable Consumer<MergeResult> applyCallback) throws InvalidDiffRequestException;

  @Nonnull
  public abstract MergeRequest createMergeRequestFromFiles(@Nullable Project project,
                                                           @Nonnull VirtualFile output,
                                                           @Nonnull List<VirtualFile> contents,
                                                           @Nullable String title,
                                                           @Nonnull List<String> contentTitles,
                                                           @Nullable Consumer<MergeResult> applyCallback) throws InvalidDiffRequestException;

  @Nonnull
  public abstract TextMergeRequest createTextMergeRequestFromFiles(@Nullable Project project,
                                                                   @Nonnull VirtualFile output,
                                                                   @Nonnull List<VirtualFile> contents,
                                                                   @Nullable String title,
                                                                   @Nonnull List<String> contentTitles,
                                                                   @Nullable Consumer<MergeResult> applyCallback) throws InvalidDiffRequestException;
}
