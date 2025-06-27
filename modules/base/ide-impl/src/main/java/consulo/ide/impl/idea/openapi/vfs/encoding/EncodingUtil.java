// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.openapi.vfs.encoding;

import consulo.application.Application;
import consulo.application.util.function.ThrowableComputable;
import consulo.codeEditor.Editor;
import consulo.component.messagebus.MessageBusConnection;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.document.Document;
import consulo.document.FileDocumentManager;
import consulo.document.event.FileDocumentManagerListener;
import consulo.ide.impl.idea.openapi.fileEditor.impl.FileDocumentManagerImpl;
import consulo.ide.impl.idea.openapi.util.text.StringUtilRt;
import consulo.ide.localize.IdeLocalize;
import consulo.language.editor.refactoring.util.CommonRefactoringUtil;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.project.ProjectLocator;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.Messages;
import consulo.util.collection.ArrayUtil;
import consulo.util.io.CharsetToolkit;
import consulo.util.lang.Pair;
import consulo.util.lang.StringUtil;
import consulo.util.lang.ref.SimpleReference;
import consulo.virtualFileSystem.ReadonlyStatusHandler;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.encoding.EncodingManager;
import consulo.virtualFileSystem.encoding.EncodingProjectManager;
import consulo.virtualFileSystem.event.VFileContentChangeEvent;
import consulo.virtualFileSystem.fileType.FileType;
import consulo.virtualFileSystem.fileType.FileTypeWithPredefinedCharset;
import consulo.virtualFileSystem.internal.LoadTextUtil;
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
    static Magic8 isSafeToReloadIn(
        @Nonnull VirtualFile virtualFile,
        @Nonnull CharSequence text,
        @Nonnull byte[] bytes,
        @Nonnull Charset charset
    ) {
        // file has BOM but the charset hasn't
        byte[] bom = virtualFile.getBOM();
        if (bom != null && !CharsetToolkit.canHaveBom(charset, bom)) {
            return Magic8.NO_WAY;
        }

        // the charset has mandatory BOM (e.g. UTF-xx) but the file hasn't or has wrong
        byte[] mandatoryBom = CharsetToolkit.getMandatoryBom(charset);
        if (mandatoryBom != null && !ArrayUtil.startsWith(bytes, mandatoryBom)) {
            return Magic8.NO_WAY;
        }

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

        return !Arrays.equals(bytesToSave, bytes) ? Magic8.NO_WAY : StringUtil.equals(
            loaded,
            text
        ) ? Magic8.ABSOLUTELY : Magic8.WELL_IF_YOU_INSIST;
    }

    @Nonnull
    static Magic8 isSafeToConvertTo(
        @Nonnull VirtualFile virtualFile,
        @Nonnull CharSequence text,
        @Nonnull byte[] bytesOnDisk,
        @Nonnull Charset charset
    ) {
        try {
            String lineSeparator = FileDocumentManager.getInstance().getLineSeparator(virtualFile, null);
            CharSequence textToSave = lineSeparator.equals("\n") ? text : StringUtilRt.convertLineSeparators(text, lineSeparator);

            Pair<Charset, byte[]> chosen =
                LoadTextUtil.chooseMostlyHarmlessCharset(virtualFile.getCharset(), charset, textToSave.toString());

            byte[] saved = chosen.second;

            CharSequence textLoadedBack = LoadTextUtil.getTextByBinaryPresentation(saved, charset);

            return !StringUtil.equals(text, textLoadedBack) ? Magic8.NO_WAY
                : Arrays.equals(saved, bytesOnDisk) ? Magic8.ABSOLUTELY : Magic8.WELL_IF_YOU_INSIST;
        }
        catch (UnsupportedOperationException e) { // unsupported encoding
            return Magic8.NO_WAY;
        }
    }

    @RequiredUIAccess
    static void saveIn(
        @Nonnull Document document,
        Editor editor,
        @Nonnull VirtualFile virtualFile,
        @Nonnull Charset charset
    ) {
        FileDocumentManager documentManager = FileDocumentManager.getInstance();
        documentManager.saveDocument(document);
        Project project = ProjectLocator.getInstance().guessProjectForFile(virtualFile);
        boolean writable = project == null ? virtualFile.isWritable() : ReadonlyStatusHandler.ensureFilesWritable(project, virtualFile);
        if (!writable) {
            CommonRefactoringUtil.showErrorHint(
                project,
                editor,
                IdeLocalize.dialogMessageCannotSaveTheFile0(virtualFile.getPresentableUrl()).get(),
                IdeLocalize.dialogTitleUnableToSave().get(),
                null
            );
            return;
        }

        EncodingProjectManagerImpl.suppressReloadDuring(() -> {
            EncodingManager.getInstance().setEncoding(virtualFile, charset);
            try {
                Application.get().runWriteAction((ThrowableComputable<Object, IOException>) () -> {
                    virtualFile.setCharset(charset);
                    LoadTextUtil.write(project, virtualFile, virtualFile, document.getText(), document.getModificationStamp());
                    return null;
                });
            }
            catch (IOException io) {
                Messages.showErrorDialog(project, io.getMessage(), IdeLocalize.dialogTitleErrorWritingFile().get());
            }
        });
    }

    @RequiredUIAccess
    static void reloadIn(@Nonnull final VirtualFile virtualFile, @Nonnull Charset charset, Project project) {
        FileDocumentManager documentManager = FileDocumentManager.getInstance();

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
        MessageBusConnection connection = Application.get().getMessageBus().connect(disposable);
        connection.subscribe(FileDocumentManagerListener.class, new FileDocumentManagerListener() {
            @Override
            public void beforeFileContentReload(@Nonnull VirtualFile file, @Nonnull Document document) {
                if (!file.equals(virtualFile)) {
                    return;
                }
                Disposer.dispose(disposable); // disconnect

                setEncoding.accept(file);

                LoadTextUtil.clearCharsetAutoDetectionReason(file);
            }
        });

        // if file was modified, the user will be asked here
        try {
            EncodingProjectManagerImpl.suppressReloadDuring(
                () ->
                    ((FileDocumentManagerImpl) documentManager).contentsChanged(new VFileContentChangeEvent(null, virtualFile, 0, 0, false))
            );
        }
        finally {
            Disposer.dispose(disposable);
        }
    }

    // returns file type description if the charset is hard-coded or null if file type does not restrict encoding
    private static String checkHardcodedCharsetFileType(@Nonnull VirtualFile virtualFile) {
        FileType fileType = virtualFile.getFileType();
        if (fileType instanceof FileTypeWithPredefinedCharset fileTypeWithPredefinedCharset) {
            return fileTypeWithPredefinedCharset.getPredefinedCharset(virtualFile).getSecond();
        }
        return null;
    }

    public static boolean canReload(@Nonnull VirtualFile virtualFile) {
        return checkCanReload(virtualFile, null) == null;
    }

    @Nullable
    static FailReason checkCanReload(@Nonnull VirtualFile virtualFile, @Nullable SimpleReference<? super Charset> current) {
        if (virtualFile.isDirectory()) {
            return FailReason.IS_DIRECTORY;
        }
        FileDocumentManager documentManager = FileDocumentManager.getInstance();
        Document document = documentManager.getDocument(virtualFile);
        if (document == null) {
            return FailReason.IS_BINARY;
        }
        Charset charsetFromContent = EncodingManagerImpl.computeCharsetFromContent(virtualFile);
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
        if (current != null) {
            current.set(existing);
        }
        return result;
    }

    @Nullable
    private static FailReason fileTypeDescriptionError(@Nonnull VirtualFile virtualFile) {
        if (virtualFile.getFileType().isBinary()) {
            return FailReason.IS_BINARY;
        }

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

        Charset charsetFromContent = EncodingManagerImpl.computeCharsetFromContent(virtualFile);
        return charsetFromContent != null ? FailReason.BY_FILE : fileTypeDescriptionError(virtualFile);
    }

    @Nullable
    static FailReason checkCanConvertAndReload(@Nonnull VirtualFile selectedFile) {
        FailReason result = checkCanConvert(selectedFile);
        if (result == null) {
            return null;
        }
        return checkCanReload(selectedFile, null);
    }

    @Nullable
    public static Pair<Charset, String> getCharsetAndTheReasonTooltip(@Nonnull VirtualFile file) {
        FailReason r1 = checkCanConvert(file);
        if (r1 == null) {
            return null;
        }
        SimpleReference<Charset> current = SimpleReference.create();
        FailReason r2 = checkCanReload(file, current);
        if (r2 == null) {
            return null;
        }
        String errorDescription = r1 == r2 ? reasonToString(r1, file).get() : reasonToString(r1, file) + ", " + reasonToString(r2, file);
        return Pair.create(current.get(), errorDescription);
    }

    @Nonnull
    static LocalizeValue reasonToString(@Nonnull FailReason reason, VirtualFile file) {
        return switch (reason) {
            case IS_DIRECTORY -> IdeLocalize.noCharsetSetReasonDisabledForDirectory();
            case IS_BINARY -> IdeLocalize.noCharsetSetReasonDisabledForBinaryFile();
            case BY_FILE -> IdeLocalize.noCharsetSetReasonCharsetHardCodedInFile();
            case BY_BOM -> IdeLocalize.noCharsetSetReasonCharsetAutoDetectedByBom();
            case BY_BYTES -> IdeLocalize.noCharsetSetReasonCharsetAutoDetectedFromContent();
            case BY_FILETYPE -> IdeLocalize.noCharsetSetReasonDisabledForFileType(file.getFileType().getDescription());
        };
    }
}
