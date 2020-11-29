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
package com.intellij.diff;

import com.intellij.diff.actions.DocumentFragmentContent;
import com.intellij.diff.contents.*;
import com.intellij.diff.tools.util.DiffNotifications;
import com.intellij.diff.util.DiffUserDataKeysEx;
import com.intellij.diff.util.DiffUtil;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileTypes.BinaryFileTypeDecompilers;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.PlainTextFileType;
import com.intellij.openapi.fileTypes.UnknownFileType;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.ThrowableComputable;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vfs.CharsetToolkit;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.testFramework.BinaryLightVirtualFile;
import com.intellij.testFramework.LightVirtualFile;
import com.intellij.util.LineSeparator;
import com.intellij.util.PathUtil;
import consulo.application.AccessRule;
import consulo.fileTypes.ArchiveFileType;
import consulo.logging.Logger;
import consulo.ui.style.StandardColors;
import jakarta.inject.Singleton;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.awt.datatransfer.DataFlavor;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;

@Singleton
public class DiffContentFactoryImpl extends DiffContentFactoryEx {
  public static final Logger LOG = Logger.getInstance(DiffContentFactoryImpl.class);

  @Nonnull
  @Override
  public EmptyContent createEmpty() {
    return new EmptyContent();
  }


  @Nonnull
  @Override
  public DocumentContent create(@Nonnull String text) {
    return create(null, text);
  }

  @Nonnull
  @Override
  public DocumentContent create(@Nonnull String text, @Nullable FileType type) {
    return create(null, text, type);
  }

  @Nonnull
  @Override
  public DocumentContent create(@Nonnull String text, @Nullable FileType type, boolean respectLineSeparators) {
    return create(null, text, type, respectLineSeparators);
  }

  @Nonnull
  @Override
  public DocumentContent create(@Nonnull String text, @Nullable VirtualFile highlightFile) {
    return create(null, text, highlightFile);
  }

  @Nonnull
  @Override
  public DocumentContent create(@Nonnull String text, @javax.annotation.Nullable DocumentContent referent) {
    return create(null, text, referent);
  }


  @Nonnull
  @Override
  public DocumentContent create(@javax.annotation.Nullable Project project, @Nonnull String text) {
    return create(project, text, (FileType)null);
  }

  @Nonnull
  @Override
  public DocumentContent create(@javax.annotation.Nullable Project project, @Nonnull String text, @Nullable FileType type) {
    return create(project, text, type, true);
  }

  @Nonnull
  @Override
  public DocumentContent create(@Nullable Project project, @Nonnull String text, @javax.annotation.Nullable FileType type, boolean respectLineSeparators) {
    return createImpl(project, text, type, null, null, respectLineSeparators, true);
  }

  @Nonnull
  @Override
  public DocumentContent create(@Nullable Project project, @Nonnull String text, @Nonnull FilePath filePath) {
    return createImpl(project, text, filePath.getFileType(), filePath.getName(), filePath.getVirtualFile(), true, true);
  }

  @Nonnull
  @Override
  public DocumentContent create(@javax.annotation.Nullable Project project, @Nonnull String text, @Nullable VirtualFile highlightFile) {
    FileType fileType = highlightFile != null ? highlightFile.getFileType() : null;
    String fileName = highlightFile != null ? highlightFile.getName() : null;
    return createImpl(project, text, fileType, fileName, highlightFile, true, true);
  }

  @Nonnull
  @Override
  public DocumentContent create(@javax.annotation.Nullable Project project, @Nonnull String text, @Nullable DocumentContent referent) {
    if (referent == null) return create(text);
    return createImpl(project, text, referent.getContentType(), null, referent.getHighlightFile(), false, true);
  }


  @Nonnull
  @Override
  public DocumentContent create(@Nonnull Document document, @javax.annotation.Nullable DocumentContent referent) {
    return create(null, document, referent);
  }


  @Nonnull
  @Override
  public DocumentContent create(@Nullable Project project, @Nonnull Document document) {
    return create(project, document, (FileType)null);
  }

  @Nonnull
  @Override
  public DocumentContent create(@javax.annotation.Nullable Project project, @Nonnull Document document, @Nullable FileType fileType) {
    VirtualFile file = FileDocumentManager.getInstance().getFile(document);
    if (file == null) return new DocumentContentImpl(project, document, fileType, null, null, null, null);
    return create(project, document, file);
  }

  @Nonnull
  @Override
  public DocumentContent create(@Nullable Project project, @Nonnull Document document, @Nullable VirtualFile file) {
    if (file != null) return new FileDocumentContentImpl(project, document, file);
    return new DocumentContentImpl(document);
  }

  @Nonnull
  @Override
  public DocumentContent create(@Nullable Project project, @Nonnull Document document, @javax.annotation.Nullable DocumentContent referent) {
    if (referent == null) return new DocumentContentImpl(document);
    return new DocumentContentImpl(project, document, referent.getContentType(), referent.getHighlightFile(), null, null, null);
  }


  @Nonnull
  @Override
  public DiffContent create(@Nullable Project project, @Nonnull VirtualFile file) {
    if (file.isDirectory()) return new DirectoryContentImpl(project, file);
    DocumentContent content = createDocument(project, file);
    if (content != null) return content;
    return new FileContentImpl(project, file);
  }

  @javax.annotation.Nullable
  @Override
  public DocumentContent createDocument(@javax.annotation.Nullable Project project, @Nonnull final VirtualFile file) {
    // TODO: add notification, that file is decompiled ?
    if (file.isDirectory()) return null;
    ThrowableComputable<Document, RuntimeException> action = () -> {
      return FileDocumentManager.getInstance().getDocument(file);
    };
    Document document = AccessRule.read(action);
    if (document == null) return null;
    return new FileDocumentContentImpl(project, document, file);
  }

  @Nullable
  @Override
  public FileContent createFile(@javax.annotation.Nullable Project project, @Nonnull VirtualFile file) {
    if (file.isDirectory()) return null;
    return (FileContent)create(project, file);
  }


  @Nonnull
  @Override
  public DocumentContent createFragment(@Nullable Project project, @Nonnull Document document, @Nonnull TextRange range) {
    DocumentContent content = create(project, document);
    return new DocumentFragmentContent(project, content, range);
  }

  @Nonnull
  @Override
  public DocumentContent createFragment(@javax.annotation.Nullable Project project, @Nonnull DocumentContent content, @Nonnull TextRange range) {
    return new DocumentFragmentContent(project, content, range);
  }


  @Nonnull
  @Override
  public DiffContent createClipboardContent() {
    return createClipboardContent(null, null);
  }

  @Nonnull
  @Override
  public DocumentContent createClipboardContent(@javax.annotation.Nullable DocumentContent referent) {
    return createClipboardContent(null, referent);
  }

  @Nonnull
  @Override
  public DiffContent createClipboardContent(@javax.annotation.Nullable Project project) {
    return createClipboardContent(project, null);
  }

  @Nonnull
  @Override
  public DocumentContent createClipboardContent(@javax.annotation.Nullable Project project, @Nullable DocumentContent referent) {
    String text = CopyPasteManager.getInstance().getContents(DataFlavor.stringFlavor);

    FileType type = referent != null ? referent.getContentType() : null;
    VirtualFile highlightFile = referent != null ? referent.getHighlightFile() : null;

    return createImpl(project, StringUtil.notNullize(text), type, "Clipboard.txt", highlightFile, true, false);
  }


  @Nonnull
  @Override
  public DiffContent createFromBytes(@javax.annotation.Nullable Project project,
                                     @Nonnull byte[] content,
                                     @Nonnull FilePath filePath) throws IOException {
    if (filePath.getFileType().isBinary()) {
      return createBinaryImpl(project, content, filePath.getFileType(), filePath.getName(), filePath.getVirtualFile());
    }

    return createDocumentFromBytes(project, content, filePath);
  }

  @Nonnull
  @Override
  public DiffContent createFromBytes(@javax.annotation.Nullable Project project,
                                     @Nonnull byte[] content,
                                     @Nonnull VirtualFile highlightFile) throws IOException {
    // TODO: check if FileType.UNKNOWN is actually a text ?
    if (highlightFile.getFileType().isBinary()) {
      return createBinaryImpl(project, content, highlightFile.getFileType(), highlightFile.getName(), highlightFile);
    }

    return createDocumentFromBytes(project, content, highlightFile);
  }

  @Nonnull
  @Override
  public DocumentContent createDocumentFromBytes(@javax.annotation.Nullable Project project, @Nonnull byte[] content, @Nonnull FilePath filePath) {
    return createFromBytesImpl(project, content, filePath.getFileType(), filePath.getName(), filePath.getVirtualFile(),
                               filePath.getCharset());
  }

  @Nonnull
  @Override
  public DocumentContent createDocumentFromBytes(@javax.annotation.Nullable Project project, @Nonnull byte[] content, @Nonnull VirtualFile highlightFile) {
    return createFromBytesImpl(project, content, highlightFile.getFileType(), highlightFile.getName(), highlightFile,
                               highlightFile.getCharset());
  }

  @Nonnull
  @Override
  public DiffContent createBinary(@javax.annotation.Nullable Project project,
                                  @Nonnull byte[] content,
                                  @Nonnull FileType type,
                                  @Nonnull String fileName) throws IOException {
    return createBinaryImpl(project, content, type, fileName, null);
  }

  @Nonnull
  private DiffContent createBinaryImpl(@javax.annotation.Nullable Project project,
                                       @Nonnull byte[] content,
                                       @Nonnull FileType type,
                                       @Nonnull String fileName,
                                       @javax.annotation.Nullable VirtualFile highlightFile) throws IOException {
    // workaround - our JarFileSystem and decompilers can't process non-local files
    boolean useTemporalFile = type instanceof ArchiveFileType || BinaryFileTypeDecompilers.INSTANCE.forFileType(type) != null;

    VirtualFile file;
    if (useTemporalFile) {
      file = createTemporalFile(project, "tmp", fileName, content);
    }
    else {
      file = new BinaryLightVirtualFile(fileName, type, content);
      file.setWritable(false);
    }

    return new FileContentImpl(project, file, highlightFile);
  }

  @Nonnull
  private static DocumentContent createImpl(@javax.annotation.Nullable Project project,
                                            @Nonnull String text,
                                            @javax.annotation.Nullable FileType fileType,
                                            @javax.annotation.Nullable String fileName,
                                            @javax.annotation.Nullable VirtualFile highlightFile,
                                            boolean respectLineSeparators,
                                            boolean readOnly) {
    return createImpl(project, text, fileType, fileName, highlightFile, null, null, respectLineSeparators, readOnly);
  }

  @Nonnull
  private static DocumentContent createImpl(@javax.annotation.Nullable Project project,
                                            @Nonnull String text,
                                            @Nullable FileType fileType,
                                            @Nullable String fileName,
                                            @Nullable VirtualFile highlightFile,
                                            @javax.annotation.Nullable Charset charset,
                                            @javax.annotation.Nullable Boolean bom,
                                            boolean respectLineSeparators,
                                            boolean readOnly) {
    if (UnknownFileType.INSTANCE == fileType) fileType = PlainTextFileType.INSTANCE;

    // TODO: detect invalid (different across the file) separators ?
    LineSeparator separator = respectLineSeparators ? StringUtil.detectSeparators(text) : null;
    String correctedContent = StringUtil.convertLineSeparators(text);

    Document document = createDocument(project, correctedContent, fileType, fileName, readOnly);
    DocumentContent content = new DocumentContentImpl(project, document, fileType, highlightFile, separator, charset, bom);

    if (fileName != null) content.putUserData(DiffUserDataKeysEx.FILE_NAME, fileName);

    return content;
  }

  @Nonnull
  private static DocumentContent createFromBytesImpl(@Nullable Project project,
                                                     @Nonnull byte[] content,
                                                     @Nonnull FileType fileType,
                                                     @Nonnull String fileName,
                                                     @javax.annotation.Nullable VirtualFile highlightFile,
                                                     @Nonnull Charset charset) {
    Charset bomCharset = CharsetToolkit.guessFromBOM(content);
    boolean isBOM = bomCharset != null;
    if (isBOM) charset = bomCharset;

    boolean malformedContent = false;
    String text = CharsetToolkit.tryDecodeString(content, charset);

    LineSeparator separator = StringUtil.detectSeparators(text);
    String correctedContent = StringUtil.convertLineSeparators(text);

    DocumentContent documentContent = createImpl(project, correctedContent, fileType, fileName, highlightFile, charset, isBOM, true, true);

    if (malformedContent) {
      String notificationText = "Content was decoded with errors (using " + "'" + charset.name() + "' charset)";
      DiffUtil.addNotification(DiffNotifications.createNotification(notificationText, StandardColors.LIGHT_RED), documentContent);
    }

    return documentContent;
  }

  @Nonnull
  private static VirtualFile createTemporalFile(@javax.annotation.Nullable Project project,
                                                @Nonnull String prefix,
                                                @Nonnull String suffix,
                                                @Nonnull byte[] content) throws IOException {
    File tempFile = FileUtil.createTempFile(PathUtil.suggestFileName(prefix + "_", true, false),
                                            PathUtil.suggestFileName("_" + suffix, true, false), true);
    if (content.length != 0) {
      FileUtil.writeToFile(tempFile, content);
    }
    if (!tempFile.setWritable(false, false)) LOG.warn("Can't set writable attribute of temporal file");

    VirtualFile file = VfsUtil.findFileByIoFile(tempFile, true);
    if (file == null) {
      throw new IOException("Can't create temp file for revision content");
    }
    VfsUtil.markDirtyAndRefresh(true, true, true, file);
    return file;
  }

  @Nonnull
  private static Document createDocument(@Nullable Project project,
                                         @Nonnull String content,
                                         @javax.annotation.Nullable FileType fileType,
                                         @Nullable String fileName,
                                         boolean readOnly) {
    if (project != null && !project.isDefault() &&
        fileType != null && !fileType.isBinary() &&
        Registry.is("diff.enable.psi.highlighting")) {
      if (fileName == null) {
        fileName = "diff." + StringUtil.defaultIfEmpty(fileType.getDefaultExtension(), "txt");
      }

      Document document = createPsiDocument(project, content, fileType, fileName, readOnly);
      if (document != null) return document;
    }

    Document document = EditorFactory.getInstance().createDocument(content);
    document.setReadOnly(readOnly);
    return document;
  }

  @Nullable
  private static Document createPsiDocument(@Nonnull Project project,
                                            @Nonnull String content,
                                            @Nonnull FileType fileType,
                                            @Nonnull String fileName,
                                            boolean readOnly) {
    ThrowableComputable<Document,RuntimeException> action = () -> {
      LightVirtualFile file = new LightVirtualFile(fileName, fileType, content);
      file.setWritable(!readOnly);

      file.putUserData(DiffPsiFileSupport.KEY, true);

      Document document = FileDocumentManager.getInstance().getDocument(file);
      if (document == null) return null;

      PsiDocumentManager.getInstance(project).getPsiFile(document);

      return document;
    };
    return AccessRule.read(action);
  }
}
