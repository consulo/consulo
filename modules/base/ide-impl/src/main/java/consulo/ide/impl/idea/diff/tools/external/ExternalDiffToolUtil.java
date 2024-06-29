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
package consulo.ide.impl.idea.diff.tools.external;

import consulo.application.AccessRule;
import consulo.application.Application;
import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.ProgressManager;
import consulo.application.progress.Task;
import consulo.application.util.function.ThrowableComputable;
import consulo.diff.content.DiffContent;
import consulo.diff.content.DocumentContent;
import consulo.diff.content.EmptyContent;
import consulo.diff.content.FileContent;
import consulo.diff.merge.MergeResult;
import consulo.diff.util.Side;
import consulo.diff.util.ThreeSide;
import consulo.document.Document;
import consulo.document.FileDocumentManager;
import consulo.ide.impl.idea.diff.contents.DirectoryContent;
import consulo.ide.impl.idea.diff.merge.ThreesideMergeRequest;
import consulo.ide.impl.idea.diff.util.DiffUserDataKeysEx;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.io.FileUtil;
import consulo.util.collection.ArrayUtil;
import consulo.ide.impl.idea.util.PathUtil;
import consulo.platform.LineSeparator;
import consulo.platform.Platform;
import consulo.process.ExecutionException;
import consulo.process.cmd.GeneralCommandLine;
import consulo.process.cmd.ParametersListUtil;
import consulo.project.Project;
import consulo.ui.ex.awt.Messages;
import consulo.util.io.CharsetToolkit;
import consulo.util.lang.StringUtil;
import consulo.util.lang.TimeoutUtil;
import consulo.util.lang.ref.Ref;
import consulo.virtualFileSystem.RawFileLoader;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFileWithoutContent;
import consulo.virtualFileSystem.fileType.FileType;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class ExternalDiffToolUtil {
  public static boolean canCreateFile(@Nonnull DiffContent content) {
    if (content instanceof EmptyContent) return true;
    if (content instanceof DocumentContent) return true;
    if (content instanceof FileContent fileContent) {
      return !(fileContent.getFile() instanceof VirtualFileWithoutContent);
    }
    if (content instanceof DirectoryContent directoryContent) {
      return directoryContent.getFile().isInLocalFileSystem();
    }
    return false;
  }

  @Nonnull
  private static InputFile createFile(@Nonnull DiffContent content, @Nonnull FileNameInfo fileName)
          throws IOException {

    if (content instanceof EmptyContent) {
      return new TempInputFile(createFile(new byte[0], fileName));
    }
    else if (content instanceof FileContent) {
      VirtualFile file = ((FileContent)content).getFile();

      Document document = FileDocumentManager.getInstance().getCachedDocument(file);
      if (document != null) {
        FileDocumentManager.getInstance().saveDocument(document);
      }

      if (file.isInLocalFileSystem()) {
        return new LocalInputFile(file);
      }

      return new TempInputFile(createTempFile(file, fileName));
    }
    else if (content instanceof DocumentContent) {
      return new TempInputFile(createTempFile((DocumentContent)content, fileName));
    }
    else if (content instanceof DirectoryContent) {
      VirtualFile file = ((DirectoryContent)content).getFile();
      if (file.isInLocalFileSystem()) {
        return new LocalInputFile(file);
      }

      throw new IllegalArgumentException(content.toString());
    }

    throw new IllegalArgumentException(content.toString());
  }

  @Nonnull
  private static File createTempFile(@Nonnull final DocumentContent content, @Nonnull FileNameInfo fileName) throws IOException {
    FileDocumentManager.getInstance().saveDocument(content.getDocument());

    LineSeparator separator = content.getLineSeparator();
    if (separator == null) separator = Platform.current().os().lineSeparator();

    Charset charset = content.getCharset();
    if (charset == null) charset = Charset.defaultCharset();

    Boolean hasBom = content.hasBom();
    if (hasBom == null) hasBom = CharsetToolkit.getMandatoryBom(charset) != null;

    ThrowableComputable<String,RuntimeException> action = () -> content.getDocument().getText();
    String contentData = AccessRule.read(action);
    if (separator != LineSeparator.LF) {
      contentData = StringUtil.convertLineSeparators(contentData, separator.getSeparatorString());
    }

    byte[] bytes = contentData.getBytes(charset);

    byte[] bom = hasBom ? CharsetToolkit.getPossibleBom(charset) : null;
    if (bom != null) {
      bytes = ArrayUtil.mergeArrays(bom, bytes);
    }

    return createFile(bytes, fileName);
  }

  @Nonnull
  private static File createTempFile(@Nonnull VirtualFile file, @Nonnull FileNameInfo fileName) throws IOException {
    byte[] bytes = file.contentsToByteArray();
    return createFile(bytes, fileName);
  }

  @Nonnull
  private static OutputFile createOutputFile(@Nonnull DiffContent content, @Nonnull FileNameInfo fileName) throws IOException {
    if (content instanceof FileContent) {
      VirtualFile file = ((FileContent)content).getFile();

      Document document = FileDocumentManager.getInstance().getCachedDocument(file);
      if (document != null) {
        FileDocumentManager.getInstance().saveDocument(document);
      }

      if (file.isInLocalFileSystem()) {
        return new LocalOutputFile(file);
      }

      File tempFile = createTempFile(file, fileName);
      return new NonLocalOutputFile(file, tempFile);
    }
    else if (content instanceof DocumentContent) {
      File tempFile = createTempFile(((DocumentContent)content), fileName);
      return new DocumentOutputFile(((DocumentContent)content).getDocument(), ((DocumentContent)content).getCharset(), tempFile);
    }
    throw new IllegalArgumentException(content.toString());
  }

  @Nonnull
  private static File createFile(@Nonnull byte[] bytes, @Nonnull FileNameInfo fileName) throws IOException {
    File tempFile = FileUtil.createTempFile(fileName.prefix + "_", "_" + fileName.name, true);
    FileUtil.writeToFile(tempFile, bytes);
    return tempFile;
  }

  public static void execute(@Nonnull ExternalDiffSettings settings,
                             @Nonnull List<? extends DiffContent> contents,
                             @Nonnull List<String> titles,
                             @Nullable String windowTitle)
          throws IOException, ExecutionException {
    assert contents.size() == 2 || contents.size() == 3;
    assert titles.size() == contents.size();

    List<InputFile> files = new ArrayList<>();
    for (int i = 0; i < contents.size(); i++) {
      DiffContent content = contents.get(i);
      FileNameInfo fileName = FileNameInfo.create(contents, titles, windowTitle, i);
      files.add(createFile(content, fileName));
    }

    Map<String, String> patterns = new HashMap<>();
    if (files.size() == 2) {
      patterns.put("%1", files.get(0).getPath());
      patterns.put("%2", files.get(1).getPath());
      patterns.put("%3", "");
    }
    else {
      patterns.put("%1", files.get(0).getPath());
      patterns.put("%2", files.get(2).getPath());
      patterns.put("%3", files.get(1).getPath());
    }


    execute(settings.getDiffExePath(), settings.getDiffParameters(), patterns);
  }

  @RequiredUIAccess
  public static void executeMerge(
    @Nullable Project project,
    @Nonnull ExternalDiffSettings settings,
    @Nonnull ThreesideMergeRequest request
  ) throws IOException, ExecutionException {
    boolean success = false;
    OutputFile outputFile = null;
    List<InputFile> inputFiles = new ArrayList<>();
    try {
      DiffContent outputContent = request.getOutputContent();
      List<? extends DiffContent> contents = request.getContents();
      List<String> titles = request.getContentTitles();
      String windowTitle = request.getTitle();

      assert contents.size() == 3;
      assert titles.size() == contents.size();

      for (int i = 0; i < contents.size(); i++) {
        DiffContent content = contents.get(i);
        FileNameInfo fileName = FileNameInfo.create(contents, titles, windowTitle, i);
        inputFiles.add(createFile(content, fileName));
      }

      outputFile = createOutputFile(outputContent, FileNameInfo.createMergeResult(outputContent, windowTitle));

      Map<String, String> patterns = new HashMap<>();
      patterns.put("%1", inputFiles.get(0).getPath());
      patterns.put("%2", inputFiles.get(2).getPath());
      patterns.put("%3", inputFiles.get(1).getPath());
      patterns.put("%4", outputFile.getPath());

      final Process process = execute(settings.getMergeExePath(), settings.getMergeParameters(), patterns);

      if (settings.isMergeTrustExitCode()) {
        final Ref<Boolean> resultRef = new Ref<>();

        ProgressManager.getInstance().run(new Task.Modal(project, "Waiting for External Tool", true) {
          @Override
          public void run(@Nonnull ProgressIndicator indicator) {
            final Semaphore semaphore = new Semaphore(0);

            final Thread waiter = new Thread("external process waiter") {
              @Override
              public void run() {
                try {
                  resultRef.set(process.waitFor() == 0);
                }
                catch (InterruptedException ignore) {
                }
                finally {
                  semaphore.release();
                }
              }
            };
            waiter.start();

            try {
              while (true) {
                indicator.checkCanceled();
                if (semaphore.tryAcquire(200, TimeUnit.MILLISECONDS)) break;
              }
            }
            catch (InterruptedException ignore) {
            }
            finally {
              waiter.interrupt();
            }
          }
        });

        success = resultRef.get() == Boolean.TRUE;
      }
      else {
        ProgressManager.getInstance().run(new Task.Modal(project, "Launching External Tool", false) {
          @Override
          public void run(@Nonnull ProgressIndicator indicator) {
            indicator.setIndeterminate(true);
            TimeoutUtil.sleep(1000);
          }
        });

        success = Messages.showYesNoDialog(
          project,
          "Press \"Mark as Resolved\" when you finish resolving conflicts in the external tool",
          "Merge In External Tool", "Mark as Resolved", "Revert", null
        ) == Messages.YES;
      }

      if (success) outputFile.apply();
    }
    finally {
      request.applyResult(success ? MergeResult.RESOLVED : MergeResult.CANCEL);

      if (outputFile != null) outputFile.cleanup();
      for (InputFile file : inputFiles) {
        file.cleanup();
      }
    }
  }

  @Nonnull
  private static Process execute(@Nonnull String exePath, @Nonnull String parametersTemplate, @Nonnull Map<String, String> patterns)
          throws ExecutionException {
    List<String> parameters = ParametersListUtil.parse(parametersTemplate, true);

    List<String> from = new ArrayList<>();
    List<String> to = new ArrayList<>();
    for (Map.Entry<String, String> entry : patterns.entrySet()) {
      from.add(entry.getKey());
      to.add(entry.getValue());
    }

    List<String> args = new ArrayList<>();
    for (String parameter : parameters) {
      String arg = StringUtil.replace(parameter, from, to);
      if (!StringUtil.isEmptyOrSpaces(arg)) args.add(arg);
    }

    GeneralCommandLine commandLine = new GeneralCommandLine();
    commandLine.setExePath(exePath);
    commandLine.addParameters(args);
    return commandLine.createProcess();
  }

  //
  // Helpers
  //


  private interface InputFile {
    @Nonnull
    String getPath();

    void cleanup();
  }

  private interface OutputFile extends InputFile {
    void apply() throws IOException;
  }

  private static class LocalOutputFile extends LocalInputFile implements OutputFile {
    public LocalOutputFile(@Nonnull VirtualFile file) {
      super(file);
    }

    @Override
    public void apply() {
      myFile.refresh(false, false);
    }
  }

  private static class NonLocalOutputFile extends TempInputFile implements OutputFile {
    @Nonnull
    private final VirtualFile myFile;

    public NonLocalOutputFile(@Nonnull VirtualFile file, @Nonnull File localFile) {
      super(localFile);
      myFile = file;
    }

    @Override
    public void apply() throws IOException {
      myFile.setBinaryContent(RawFileLoader.getInstance().loadFileBytes(myLocalFile));
    }
  }

  private static class DocumentOutputFile extends TempInputFile implements OutputFile {
    @Nonnull
    private final Document myDocument;
    @Nonnull
    private final Charset myCharset;

    public DocumentOutputFile(@Nonnull Document document, @Nullable Charset charset, @Nonnull File localFile) {
      super(localFile);
      myDocument = document;
      // TODO: potentially dangerous operation - we're using default charset
      myCharset = charset != null ? charset : Charset.defaultCharset();
    }

    @Override
    @RequiredUIAccess
    public void apply() throws IOException {
      final String content =
        StringUtil.convertLineSeparators(consulo.ide.impl.idea.openapi.util.io.FileUtil.loadFile(myLocalFile, myCharset));
      Application.get().runWriteAction(() -> myDocument.setText(content));
    }
  }

  private static class LocalInputFile implements InputFile {
    @Nonnull
    protected final VirtualFile myFile;

    public LocalInputFile(@Nonnull VirtualFile file) {
      myFile = file;
    }

    @Nonnull
    @Override
    public String getPath() {
      return myFile.getPath();
    }

    @Override
    public void cleanup() {
    }
  }

  private static class TempInputFile implements InputFile {
    @Nonnull
    protected final File myLocalFile;

    public TempInputFile(@Nonnull File localFile) {
      myLocalFile = localFile;
    }

    @Nonnull
    @Override
    public String getPath() {
      return myLocalFile.getPath();
    }

    @Override
    public void cleanup() {
      FileUtil.delete(myLocalFile);
    }
  }

  private static class FileNameInfo {
    @Nonnull
    public final String prefix;
    @Nonnull
    public final String name;

    public FileNameInfo(@Nonnull String prefix, @Nonnull String name) {
      this.prefix = prefix;
      this.name = name;
    }

    @Nonnull
    public static FileNameInfo create(@Nonnull List<? extends DiffContent> contents,
                                      @Nonnull List<String> titles,
                                      @Nullable String windowTitle,
                                      int index) {
      if (contents.size() == 2) {
        Side side = Side.fromIndex(index);
        DiffContent content = side.select(contents);
        String title = side.select(titles);
        String prefix = side.select("before", "after");

        String name = getFileName(content, title, windowTitle);
        return new FileNameInfo(prefix, name);
      }
      else if (contents.size() == 3) {
        ThreeSide side = ThreeSide.fromIndex(index);
        DiffContent content = side.select(contents);
        String title = side.select(titles);
        String prefix = side.select("left", "base", "right");

        String name = getFileName(content, title, windowTitle);
        return new FileNameInfo(prefix, name);
      }
      else {
        throw new IllegalArgumentException(String.valueOf(contents.size()));
      }
    }

    @Nonnull
    public static FileNameInfo createMergeResult(@Nonnull DiffContent content, @Nullable String windowTitle) {
      String name = getFileName(content, null, windowTitle);
      return new FileNameInfo("merge_result", name);
    }

    @Nonnull
    private static String getFileName(@Nonnull DiffContent content,
                                      @Nullable String title,
                                      @Nullable String windowTitle) {
      if (content instanceof EmptyContent) {
        return "no_content.tmp";
      }

      String fileName = content.getUserData(DiffUserDataKeysEx.FILE_NAME);

      if (fileName == null && content instanceof DocumentContent) {
        VirtualFile highlightFile = ((DocumentContent)content).getHighlightFile();
        fileName = highlightFile != null ? highlightFile.getName() : null;
      }

      if (fileName == null && content instanceof FileContent) {
        fileName = ((FileContent)content).getFile().getName();
      }

      if (!StringUtil.isEmptyOrSpaces(fileName)) {
        return fileName;
      }


      FileType fileType = content.getContentType();
      String ext = fileType != null ? fileType.getDefaultExtension() : null;
      if (StringUtil.isEmptyOrSpaces(ext)) ext = "tmp";

      String name = "";
      if (title != null && windowTitle != null) {
        name = title + "_" + windowTitle;
      }
      else if (title != null || windowTitle != null) {
        name = title != null ? title : windowTitle;
      }
      if (name.length() > 50) name = name.substring(0, 50);

      return PathUtil.suggestFileName(name + "." + ext, true, false);
    }
  }
}
