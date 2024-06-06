// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.openapi.vfs.encoding;

import consulo.application.ApplicationManager;
import consulo.document.Document;
import consulo.codeEditor.Editor;
import consulo.document.FileDocumentManager;
import consulo.document.event.FileDocumentManagerListener;
import consulo.ide.impl.idea.openapi.fileEditor.impl.FileDocumentManagerImpl;
import consulo.virtualFileSystem.encoding.EncodingManager;
import consulo.language.impl.internal.psi.LoadTextUtil;
import consulo.virtualFileSystem.encoding.EncodingProjectManager;
import consulo.virtualFileSystem.fileType.FileType;
import consulo.project.Project;
import consulo.project.ProjectLocator;
import consulo.ui.ex.awt.Messages;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.util.lang.Pair;
import consulo.util.lang.ref.Ref;
import consulo.application.util.function.ThrowableComputable;
import consulo.util.lang.StringUtil;
import consulo.ide.impl.idea.openapi.util.text.StringUtilRt;
import consulo.util.io.CharsetToolkit;
import consulo.virtualFileSystem.ReadonlyStatusHandler;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.event.VFileContentChangeEvent;
import consulo.language.editor.refactoring.util.CommonRefactoringUtil;
import consulo.ide.impl.idea.util.ArrayUtil;
import consulo.component.messagebus.MessageBusConnection;
import consulo.virtualFileSystem.fileType.FileTypeWithPredefinedCharset;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.function.Consumer;

public class EncodingUtil {

  enum FailReason {
    IS_DIRECTORY,
    IS_BINARY,
    BY_FILE,
    BY_BOM,
    BY_BYTES,
    BY_FILETYPE
  }

  // the result of wild guess
  public enum Magic8 {
    ABSOLUTELY,
    WELL_IF_YOU_INSIST,
    NO_WAY
  }

  // check if file can be loaded in the encoding correctly:
  // returns ABSOLUTELY if bytes on disk, converted to text with the charset, converted back to bytes matched
  // returns NO_WAY if the new encoding is incompatible (bytes on disk will differ)
  // returns WELL_IF_YOU_INSIST if the bytes on disk remain the same but the text will change
  @Nonnull
  static Magic8 isSafeToReloadIn(@Nonnull VirtualFile virtualFile, @Nonnull CharSequence text, @Nonnull byte[] bytes, @Nonnull Charset charset) {
    // file has BOM but the charset hasn't
    byte[] bom = virtualFile.getBOM();
    if (bom != null && !CharsetToolkit.canHaveBom(charset, bom)) return Magic8.NO_WAY;

    // the charset has mandatory BOM (e.g. UTF-xx) but the file hasn't or has wrong
    byte[] mandatoryBom = CharsetToolkit.getMandatoryBom(charset);
    if (mandatoryBom != null && !ArrayUtil.startsWith(bytes, mandatoryBom)) return Magic8.NO_WAY;

    String loaded = LoadTextUtil.getTextByBinaryPresentation(bytes, charset).toString();

    String separator = FileDocumentManager.getInstance().getLineSeparator(virtualFile, null);
    String toSave = StringUtil.convertLineSeparators(loaded, separator);

    LoadTextUtil.AutoDetectionReason failReason = LoadTextUtil.getCharsetAutoDetectionReason(virtualFile);
    if (failReason != null && StandardCharsets.UTF_8.equals(virtualFile.getCharset()) && !StandardCharsets.UTF_8.equals(charset)) {
      return Magic8.NO_WAY; // can't reload utf8-autodetected file in another charset
    }

    byte[] bytesToSave;
    try {
      bytesToSave = toSave.getBytes(charset);
    }
    // turned out some crazy charsets have incorrectly implemented .newEncoder() returning null
    catch (UnsupportedOperationException | NullPointerException e) {
      return Magic8.NO_WAY;
    }
    if (bom != null && !ArrayUtil.startsWith(bytesToSave, bom)) {
      bytesToSave = ArrayUtil.mergeArrays(bom, bytesToSave); // for 2-byte encodings String.getBytes(Charset) adds BOM automatically
    }

    return !Arrays.equals(bytesToSave, bytes) ? Magic8.NO_WAY : StringUtil.equals(loaded, text) ? Magic8.ABSOLUTELY : Magic8.WELL_IF_YOU_INSIST;
  }

  @Nonnull
  static Magic8 isSafeToConvertTo(@Nonnull VirtualFile virtualFile, @Nonnull CharSequence text, @Nonnull byte[] bytesOnDisk, @Nonnull Charset charset) {
    try {
      String lineSeparator = FileDocumentManager.getInstance().getLineSeparator(virtualFile, null);
      CharSequence textToSave = lineSeparator.equals("\n") ? text : StringUtilRt.convertLineSeparators(text, lineSeparator);

      consulo.util.lang.Pair<Charset, byte[]> chosen = LoadTextUtil.chooseMostlyHarmlessCharset(virtualFile.getCharset(), charset, textToSave.toString());

      byte[] saved = chosen.second;

      CharSequence textLoadedBack = LoadTextUtil.getTextByBinaryPresentation(saved, charset);

      return !StringUtil.equals(text, textLoadedBack) ? Magic8.NO_WAY : Arrays.equals(saved, bytesOnDisk) ? Magic8.ABSOLUTELY : Magic8.WELL_IF_YOU_INSIST;
    }
    catch (UnsupportedOperationException e) { // unsupported encoding
      return Magic8.NO_WAY;
    }
  }

  static void saveIn(@Nonnull final Document document, final Editor editor, @Nonnull final VirtualFile virtualFile, @Nonnull final Charset charset) {
    FileDocumentManager documentManager = FileDocumentManager.getInstance();
    documentManager.saveDocument(document);
    final Project project = ProjectLocator.getInstance().guessProjectForFile(virtualFile);
    boolean writable = project == null ? virtualFile.isWritable() : ReadonlyStatusHandler.ensureFilesWritable(project, virtualFile);
    if (!writable) {
      CommonRefactoringUtil.showErrorHint(project, editor, "Cannot save the file " + virtualFile.getPresentableUrl(), "Unable to Save", null);
      return;
    }

    EncodingProjectManagerImpl.suppressReloadDuring(() -> {
      EncodingManager.getInstance().setEncoding(virtualFile, charset);
      try {
        ApplicationManager.getApplication().runWriteAction((ThrowableComputable<Object, IOException>)() -> {
          virtualFile.setCharset(charset);
          LoadTextUtil.write(project, virtualFile, virtualFile, document.getText(), document.getModificationStamp());
          return null;
        });
      }
      catch (IOException io) {
        Messages.showErrorDialog(project, io.getMessage(), "Error Writing File");
      }
    });
  }

  static void reloadIn(@Nonnull final VirtualFile virtualFile, @Nonnull final Charset charset, final Project project) {
    final FileDocumentManager documentManager = FileDocumentManager.getInstance();

    Consumer<VirtualFile> setEncoding = file -> {
      if (project == null) {
        EncodingManager.getInstance().setEncoding(file, charset);
      }
      else {
        EncodingProjectManager.getInstance(project).setEncoding(file, charset);
      }
    };
    if (documentManager.getCachedDocument(virtualFile) == null) {
      // no need to reload document
      setEncoding.accept(virtualFile);
      return;
    }

    final Disposable disposable = Disposable.newDisposable();
    MessageBusConnection connection = ApplicationManager.getApplication().getMessageBus().connect(disposable);
    connection.subscribe(FileDocumentManagerListener.class, new FileDocumentManagerListener() {
      @Override
      public void beforeFileContentReload(@Nonnull VirtualFile file, @Nonnull Document document) {
        if (!file.equals(virtualFile)) return;
        Disposer.dispose(disposable); // disconnect

        setEncoding.accept(file);

        LoadTextUtil.clearCharsetAutoDetectionReason(file);
      }
    });

    // if file was modified, the user will be asked here
    try {
      EncodingProjectManagerImpl.suppressReloadDuring(() -> ((FileDocumentManagerImpl)documentManager).contentsChanged(new VFileContentChangeEvent(null, virtualFile, 0, 0, false)));
    }
    finally {
      Disposer.dispose(disposable);
    }
  }

  // returns file type description if the charset is hard-coded or null if file type does not restrict encoding
  private static String checkHardcodedCharsetFileType(@Nonnull VirtualFile virtualFile) {
    FileType fileType = virtualFile.getFileType();
    if (fileType instanceof FileTypeWithPredefinedCharset) {
      return ((FileTypeWithPredefinedCharset)fileType).getPredefinedCharset(virtualFile).getSecond();
    }
    return null;
  }

  public static boolean canReload(@Nonnull VirtualFile virtualFile) {
    return checkCanReload(virtualFile, null) == null;
  }

  @Nullable
  static FailReason checkCanReload(@Nonnull VirtualFile virtualFile, @Nullable Ref<? super Charset> current) {
    if (virtualFile.isDirectory()) {
      return FailReason.IS_DIRECTORY;
    }
    FileDocumentManager documentManager = FileDocumentManager.getInstance();
    Document document = documentManager.getDocument(virtualFile);
    if (document == null) return FailReason.IS_BINARY;
    Charset charsetFromContent = ((EncodingManagerImpl)EncodingManager.getInstance()).computeCharsetFromContent(virtualFile);
    Charset existing = virtualFile.getCharset();
    LoadTextUtil.AutoDetectionReason autoDetectedFrom = LoadTextUtil.getCharsetAutoDetectionReason(virtualFile);
    FailReason result;
    if (autoDetectedFrom != null) {
      // no point changing encoding if it was auto-detected
      result = autoDetectedFrom == LoadTextUtil.AutoDetectionReason.FROM_BOM ? FailReason.BY_BOM : FailReason.BY_BYTES;
    }
    else if (charsetFromContent != null) {
      result = FailReason.BY_FILE;
      existing = charsetFromContent;
    }
    else {
      result = fileTypeDescriptionError(virtualFile);
    }
    if (current != null) current.set(existing);
    return result;
  }

  @Nullable
  private static FailReason fileTypeDescriptionError(@Nonnull VirtualFile virtualFile) {
    if (virtualFile.getFileType().isBinary()) return FailReason.IS_BINARY;

    String fileTypeDescription = checkHardcodedCharsetFileType(virtualFile);
    return fileTypeDescription == null ? null : FailReason.BY_FILETYPE;
  }

  /**
   * @param virtualFile
   * @return null means enabled, notnull means disabled and contains error message
   */
  @Nullable
  static FailReason checkCanConvert(@Nonnull VirtualFile virtualFile) {
    if (virtualFile.isDirectory()) {
      return FailReason.IS_DIRECTORY;
    }

    Charset charsetFromContent = ((EncodingManagerImpl)EncodingManager.getInstance()).computeCharsetFromContent(virtualFile);
    return charsetFromContent != null ? FailReason.BY_FILE : fileTypeDescriptionError(virtualFile);
  }

  @Nullable
  static FailReason checkCanConvertAndReload(@Nonnull VirtualFile selectedFile) {
    FailReason result = checkCanConvert(selectedFile);
    if (result == null) return null;
    return checkCanReload(selectedFile, null);
  }

  @Nullable
  public static Pair<Charset, String> getCharsetAndTheReasonTooltip(@Nonnull VirtualFile file) {
    FailReason r1 = checkCanConvert(file);
    if (r1 == null) return null;
    Ref<Charset> current = Ref.create();
    FailReason r2 = checkCanReload(file, current);
    if (r2 == null) return null;
    String errorDescription = r1 == r2 ? reasonToString(r1, file) : reasonToString(r1, file) + ", " + reasonToString(r2, file);
    return Pair.create(current.get(), errorDescription);
  }

  static String reasonToString(@Nonnull FailReason reason, VirtualFile file) {
    switch (reason) {
      case IS_DIRECTORY:
        return "disabled for a directory";
      case IS_BINARY:
        return "disabled for a binary file";
      case BY_FILE:
        return "charset is hard-coded in the file";
      case BY_BOM:
        return "charset is auto-detected by BOM";
      case BY_BYTES:
        return "charset is auto-detected from content";
      case BY_FILETYPE:
        return "disabled for " + file.getFileType().getDescription();
    }
    throw new AssertionError(reason);
  }
}
