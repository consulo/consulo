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
package consulo.diff.impl.internal;

import consulo.annotation.component.ServiceImpl;
import consulo.diff.DiffContentFactory;
import consulo.diff.DiffFilePath;
import consulo.diff.InvalidDiffRequestException;
import consulo.diff.content.DiffContent;
import consulo.diff.content.DocumentContent;
import consulo.diff.content.FileContent;
import consulo.diff.impl.internal.request.BinaryMergeRequestImpl;
import consulo.diff.impl.internal.request.TextMergeRequestImpl;
import consulo.diff.internal.DiffImplUtil;
import consulo.diff.internal.DiffContentFactoryEx;
import consulo.diff.internal.DiffFilePathFactory;
import consulo.diff.internal.DiffRequestFactoryEx;
import consulo.diff.localize.DiffLocalize;
import consulo.diff.merge.MergeRequest;
import consulo.diff.merge.MergeResult;
import consulo.diff.merge.TextMergeRequest;
import consulo.diff.request.ContentDiffRequest;
import consulo.diff.request.SimpleDiffRequest;
import consulo.document.Document;
import consulo.document.FileDocumentManager;
import consulo.project.Project;
import consulo.util.collection.ContainerUtil;
import consulo.util.lang.Comparing;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.fileType.FileType;
import org.jspecify.annotations.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@Singleton
@ServiceImpl
public class DiffRequestFactoryImpl implements DiffRequestFactoryEx {
  private final DiffContentFactoryEx myContentFactory;
  private final DiffFilePathFactory myDiffFilePathFactory;

  @Inject
  public DiffRequestFactoryImpl(DiffContentFactory contentFactory, DiffFilePathFactory diffFilePathFactory) {
    myContentFactory = (DiffContentFactoryEx) contentFactory;
    myDiffFilePathFactory = diffFilePathFactory;
  }

  //
  // Diff
  //

  
  @Override
  public ContentDiffRequest createFromFiles(@Nullable Project project, VirtualFile file1, VirtualFile file2) {
    DiffContent content1 = myContentFactory.create(project, file1);
    DiffContent content2 = myContentFactory.create(project, file2);

    String title1 = getContentTitle(file1);
    String title2 = getContentTitle(file2);

    String title = getTitle(file1, file2);

    return new SimpleDiffRequest(title, content1, content2, title1, title2);
  }

  
  @Override
  public ContentDiffRequest createFromFiles(@Nullable Project project,
                                            VirtualFile leftFile,
                                            VirtualFile baseFile,
                                            VirtualFile rightFile) {
    DiffContent content1 = myContentFactory.create(project, leftFile);
    DiffContent content2 = myContentFactory.create(project, baseFile);
    DiffContent content3 = myContentFactory.create(project, rightFile);

    String title1 = getContentTitle(leftFile);
    String title2 = getContentTitle(baseFile);
    String title3 = getContentTitle(rightFile);

    return new SimpleDiffRequest(null, content1, content2, content3, title1, title2, title3);
  }

  
  @Override
  public ContentDiffRequest createClipboardVsValue(String value) {
    DiffContent content1 = myContentFactory.createClipboardContent();
    DiffContent content2 = myContentFactory.create(value);

    String title1 = DiffLocalize.diffContentClipboardContentTitle().get();
    String title2 = DiffLocalize.diffContentSelectedValue().get();

    String title = DiffLocalize.diffClipboardVsValueDialogTitle().get();

    return new SimpleDiffRequest(title, content1, content2, title1, title2);
  }

  //
  // Titles
  //

  
  @Override
  public String getContentTitle(VirtualFile file) {
    return getContentTitle(myDiffFilePathFactory.createFilePath(file));
  }

  
  @Override
  public String getTitle(VirtualFile file1, VirtualFile file2) {
    return getTitle(myDiffFilePathFactory.createFilePath(file1), myDiffFilePathFactory.createFilePath(file2), " vs ");
  }

  
  @Override
  public String getTitle(VirtualFile file) {
    return getTitle(file, file);
  }

  @Override
  
  public String getContentTitle(DiffFilePath path) {
    if (path.isDirectory()) return path.getPresentableUrl();
    DiffFilePath parent = path.getParentPath();
    return getContentTitle(path.getName(), path.getPresentableUrl(), parent != null ? parent.getPresentableUrl() : null);
  }

  @Override
  
  public String getTitle(DiffFilePath path1, DiffFilePath path2, String separator) {
    if ((path1.isDirectory() || path2.isDirectory()) && path1.getPath().equals(path2.getPath())) {
      return path1.getPresentableUrl();
    }

    String name1 = path1.getName();
    String name2 = path2.getName();

    if (path1.isDirectory() ^ path2.isDirectory()) {
      if (path1.isDirectory()) name1 += File.separatorChar;
      if (path2.isDirectory()) name2 += File.separatorChar;
    }

    DiffFilePath parent1 = path1.getParentPath();
    DiffFilePath parent2 = path2.getParentPath();
    return getRequestTitle(name1, path1.getPresentableUrl(), parent1 != null ? parent1.getPresentableUrl() : null,
                           name2, path2.getPresentableUrl(), parent2 != null ? parent2.getPresentableUrl() : null,
                           separator);
  }

  
  private static String getContentTitle(String name, String path, @Nullable String parentPath) {
    if (parentPath != null) {
      return name + " (" + parentPath + ")";
    }
    else {
      return path;
    }
  }

  
  private static String getRequestTitle(String name1, String path1, @Nullable String parentPath1,
                                        String name2, String path2, @Nullable String parentPath2,
                                        String sep) {
    if (path1.equals(path2)) return getContentTitle(name1, path1, parentPath1);

    if (Comparing.equal(parentPath1, parentPath2)) {
      if (parentPath1 != null) {
        return name1 + sep + name2 + " (" + parentPath1 + ")";
      }
      else {
        return path1 + sep + path2;
      }
    }
    else {
      if (name1.equals(name2)) {
        if (parentPath1 != null && parentPath2 != null) {
          return name1 + " (" + parentPath1 + sep + parentPath2 + ")";
        }
        else {
          return path1 + sep + path2;
        }
      }
      else {
        if (parentPath1 != null && parentPath2 != null) {
          return name1 + sep + name2 + " (" + parentPath1 + sep + parentPath2 + ")";
        }
        else {
          return path1 + sep + path2;
        }
      }
    }
  }

  //
  // Merge
  //

  
  @Override
  public MergeRequest createMergeRequest(@Nullable Project project,
                                         @Nullable FileType fileType,
                                         Document outputDocument,
                                         List<String> textContents,
                                         @Nullable String title,
                                         List<String> titles,
                                         @Nullable Consumer<MergeResult> applyCallback) throws InvalidDiffRequestException {
    if (textContents.size() != 3) throw new IllegalArgumentException();
    if (titles.size() != 3) throw new IllegalArgumentException();

    if (!DiffImplUtil.canMakeWritable(outputDocument)) throw new InvalidDiffRequestException("Output is read only");

    DocumentContent outputContent = myContentFactory.create(project, outputDocument, fileType);
    CharSequence originalContent = outputDocument.getImmutableCharSequence();

    List<DocumentContent> contents = new ArrayList<>(3);
    for (String text : textContents) {
      contents.add(myContentFactory.create(project, text, fileType));
    }

    return new TextMergeRequestImpl(project, outputContent, originalContent, contents, title, titles, applyCallback);
  }

  
  @Override
  public MergeRequest createMergeRequest(@Nullable Project project,
                                         VirtualFile output,
                                         List<byte[]> byteContents,
                                         @Nullable String title,
                                         List<String> contentTitles,
                                         @Nullable Consumer<MergeResult> applyCallback) throws InvalidDiffRequestException {
    if (byteContents.size() != 3) throw new IllegalArgumentException();
    if (contentTitles.size() != 3) throw new IllegalArgumentException();

    try {
      return createTextMergeRequest(project, output, byteContents, title, contentTitles, applyCallback);
    }
    catch (InvalidDiffRequestException e) {
      return createBinaryMergeRequest(project, output, byteContents, title, contentTitles, applyCallback);
    }
  }

  
  @Override
  public TextMergeRequest createTextMergeRequest(@Nullable Project project,
                                                 VirtualFile output,
                                                 List<byte[]> byteContents,
                                                 @Nullable String title,
                                                 List<String> contentTitles,
                                                 @Nullable Consumer<MergeResult> applyCallback) throws InvalidDiffRequestException {
    if (byteContents.size() != 3) throw new IllegalArgumentException();
    if (contentTitles.size() != 3) throw new IllegalArgumentException();

    Document outputDocument = FileDocumentManager.getInstance().getDocument(output);
    if (outputDocument == null) throw new InvalidDiffRequestException("Can't get output document: " + output.getPresentableUrl());
    if (!DiffImplUtil.canMakeWritable(outputDocument)) throw new InvalidDiffRequestException("Output is read only: " + output.getPresentableUrl());

    DocumentContent outputContent = myContentFactory.create(project, outputDocument);
    CharSequence originalContent = outputDocument.getImmutableCharSequence();

    List<DocumentContent> contents = new ArrayList<>(3);
    for (byte[] bytes : byteContents) {
      contents.add(myContentFactory.createDocumentFromBytes(project, bytes, output));
    }

    return new TextMergeRequestImpl(project, outputContent, originalContent, contents, title, contentTitles, applyCallback);
  }

  
  @Override
  public MergeRequest createBinaryMergeRequest(@Nullable Project project,
                                               VirtualFile output,
                                               List<byte[]> byteContents,
                                               @Nullable String title,
                                               List<String> contentTitles,
                                               @Nullable Consumer<MergeResult> applyCallback) throws InvalidDiffRequestException {
    if (byteContents.size() != 3) throw new IllegalArgumentException();
    if (contentTitles.size() != 3) throw new IllegalArgumentException();

    try {
      FileContent outputContent = myContentFactory.createFile(project, output);
      if (outputContent == null) throw new InvalidDiffRequestException("Can't process file: " + output);
      byte[] originalContent = output.contentsToByteArray();

      List<DiffContent> contents = new ArrayList<>(3);
      for (byte[] bytes : byteContents) {
        contents.add(myContentFactory.createFromBytes(project, bytes, output));
      }

      return new BinaryMergeRequestImpl(project, outputContent, originalContent, contents, byteContents, title, contentTitles, applyCallback);
    }
    catch (IOException e) {
      throw new InvalidDiffRequestException("Can't read from file", e);
    }
  }

  
  @Override
  public MergeRequest createMergeRequestFromFiles(@Nullable Project project,
                                                  VirtualFile output,
                                                  List<VirtualFile> fileContents,
                                                  @Nullable Consumer<MergeResult> applyCallback) throws InvalidDiffRequestException {
    String title = "Merge " + output.getPresentableUrl();
    List<String> titles = ContainerUtil.list("Your Version", "Base Version", "Their Version");
    return createMergeRequestFromFiles(project, output, fileContents, title, titles, applyCallback);
  }

  
  @Override
  public MergeRequest createMergeRequestFromFiles(@Nullable Project project,
                                                  VirtualFile output,
                                                  List<VirtualFile> fileContents,
                                                  @Nullable String title,
                                                  List<String> contentTitles,
                                                  @Nullable Consumer<MergeResult> applyCallback) throws InvalidDiffRequestException {
    if (fileContents.size() != 3) throw new IllegalArgumentException();
    if (contentTitles.size() != 3) throw new IllegalArgumentException();

    try {
      return createTextMergeRequestFromFiles(project, output, fileContents, title, contentTitles, applyCallback);
    }
    catch (InvalidDiffRequestException e) {
      return createBinaryMergeRequestFromFiles(project, output, fileContents, title, contentTitles, applyCallback);
    }
  }

  
  @Override
  public TextMergeRequest createTextMergeRequestFromFiles(@Nullable Project project,
                                                          VirtualFile output,
                                                          List<VirtualFile> fileContents,
                                                          @Nullable String title,
                                                          List<String> contentTitles,
                                                          @Nullable Consumer<MergeResult> applyCallback) throws InvalidDiffRequestException {
    List<byte[]> byteContents = new ArrayList<>(3);
    for (VirtualFile file : fileContents) {
      try {
        byteContents.add(file.contentsToByteArray());
      }
      catch (IOException e) {
        throw new InvalidDiffRequestException("Can't read from file: " + file.getPresentableUrl(), e);
      }
    }

    return createTextMergeRequest(project, output, byteContents, title, contentTitles, applyCallback);
  }

  
  public MergeRequest createBinaryMergeRequestFromFiles(@Nullable Project project,
                                                        VirtualFile output,
                                                        List<VirtualFile> fileContents,
                                                        @Nullable String title,
                                                        List<String> contentTitles,
                                                        @Nullable Consumer<MergeResult> applyCallback) throws InvalidDiffRequestException {
    if (fileContents.size() != 3) throw new IllegalArgumentException();
    if (contentTitles.size() != 3) throw new IllegalArgumentException();

    try {
      FileContent outputContent = myContentFactory.createFile(project, output);
      if (outputContent == null) throw new InvalidDiffRequestException("Can't process file: " + output.getPresentableUrl());
      byte[] originalContent = output.contentsToByteArray();

      List<DiffContent> contents = new ArrayList<>(3);
      List<byte[]> byteContents = new ArrayList<>(3);
      for (VirtualFile file : fileContents) {
        FileContent content = myContentFactory.createFile(project, file);
        if (content == null) throw new InvalidDiffRequestException("Can't process file: " + file.getPresentableUrl());
        contents.add(content);
        byteContents.add(file.contentsToByteArray()); // TODO: we can read contents from file when needed
      }

      return new BinaryMergeRequestImpl(project, outputContent, originalContent, contents, byteContents, title, contentTitles, applyCallback);
    }
    catch (IOException e) {
      throw new InvalidDiffRequestException("Can't read from file", e);
    }
  }
}
