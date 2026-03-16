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
package consulo.diff;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.application.Application;
import consulo.diff.merge.MergeRequest;
import consulo.diff.merge.MergeResult;
import consulo.diff.merge.TextMergeRequest;
import consulo.diff.request.ContentDiffRequest;
import consulo.document.Document;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.fileType.FileType;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;

/**
 * Use ProgressManager.executeProcessUnderProgress() to pass modality state if needed
 */
@ServiceAPI(ComponentScope.APPLICATION)
public interface DiffRequestFactory {
  
  public static DiffRequestFactory getInstance() {
    return Application.get().getInstance(DiffRequestFactory.class);
  }

  //
  // Diff
  //

  
  ContentDiffRequest createFromFiles(@Nullable Project project, VirtualFile file1, VirtualFile file2);

  
  ContentDiffRequest createFromFiles(@Nullable Project project,
                                     VirtualFile leftFile,
                                     VirtualFile baseFile,
                                     VirtualFile rightFile);

  
  ContentDiffRequest createClipboardVsValue(String value);

  //
  // Titles
  //

  
  String getContentTitle(VirtualFile file);

  
  String getTitle(VirtualFile file1, VirtualFile file2);

  
  String getTitle(VirtualFile file);

  //
  // Merge
  //

  
  MergeRequest createMergeRequest(@Nullable Project project,
                                  @Nullable FileType fileType,
                                  Document output,
                                  List<String> textContents,
                                  @Nullable String title,
                                  List<String> titles,
                                  @Nullable Consumer<MergeResult> applyCallback) throws InvalidDiffRequestException;

  
  MergeRequest createMergeRequest(@Nullable Project project,
                                  VirtualFile output,
                                  List<byte[]> byteContents,
                                  @Nullable String title,
                                  List<String> contentTitles,
                                  @Nullable Consumer<MergeResult> applyCallback) throws InvalidDiffRequestException;

  
  TextMergeRequest createTextMergeRequest(@Nullable Project project,
                                          VirtualFile output,
                                          List<byte[]> byteContents,
                                          @Nullable String title,
                                          List<String> contentTitles,
                                          @Nullable Consumer<MergeResult> applyCallback) throws InvalidDiffRequestException;

  
  MergeRequest createBinaryMergeRequest(@Nullable Project project,
                                        VirtualFile output,
                                        List<byte[]> byteContents,
                                        @Nullable String title,
                                        List<String> contentTitles,
                                        @Nullable Consumer<MergeResult> applyCallback) throws InvalidDiffRequestException;

  
  MergeRequest createMergeRequestFromFiles(@Nullable Project project,
                                           VirtualFile output,
                                           List<VirtualFile> contents,
                                           @Nullable Consumer<MergeResult> applyCallback) throws InvalidDiffRequestException;

  
  MergeRequest createMergeRequestFromFiles(@Nullable Project project,
                                           VirtualFile output,
                                           List<VirtualFile> contents,
                                           @Nullable String title,
                                           List<String> contentTitles,
                                           @Nullable Consumer<MergeResult> applyCallback) throws InvalidDiffRequestException;

  
  TextMergeRequest createTextMergeRequestFromFiles(@Nullable Project project,
                                                   VirtualFile output,
                                                   List<VirtualFile> contents,
                                                   @Nullable String title,
                                                   List<String> contentTitles,
                                                   @Nullable Consumer<MergeResult> applyCallback) throws InvalidDiffRequestException;
}
