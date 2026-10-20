// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.versionControlSystem.change.ignore;

import consulo.application.Application;
import consulo.document.Document;
import consulo.document.FileDocumentManager;
import consulo.document.util.TextRange;
import consulo.language.ignore.IgnoreFileConstants;
import consulo.language.ignore.IgnoreLanguage;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiManager;
import consulo.project.Project;
import consulo.undoRedo.CommandProcessor;
import consulo.util.lang.Pair;
import consulo.versionControlSystem.AbstractVcs;
import consulo.versionControlSystem.VcsKey;
import consulo.versionControlSystem.change.IgnoredFileContentProvider;
import consulo.versionControlSystem.change.IgnoredFileDescriptor;
import consulo.versionControlSystem.util.VcsImplUtil;
import consulo.versionControlSystem.util.VcsUtil;
import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public final class IgnoreFileUtil {
    public static void addNewElementsToIgnoreBlock(
        Project project,
        VirtualFile ignoreFile,
        String ignoredGroupDescription,
        IgnoredFileDescriptor... newEntries
    ) {
        addNewElementsToIgnoreBlock(project, ignoreFile, ignoredGroupDescription, null, newEntries);
    }

    public static void addNewElementsToIgnoreBlock(
        Project project,
        VirtualFile ignoreFile,
        String ignoredGroupDescription,
        @Nullable VcsKey vcs,
        IgnoredFileDescriptor... newEntries
    ) {
        changeIgnoreFile(
            project,
            ignoreFile,
            vcs,
            provider -> addNewElementsToIgnoreBlock(ignoredGroupDescription, ignoreFile, newEntries, provider)
        );
    }

    public static void addNewElementsToIgnoreFile(
        Project project,
        VirtualFile ignoreFile,
        List<Pair<String, List<IgnoredFileDescriptor>>> groupedEntries,
        @Nullable VcsKey vcs
    ) {
        changeIgnoreFile(project, ignoreFile, vcs, provider -> addNewBlocksToIgnoreFile(ignoreFile, groupedEntries, provider));
    }

    public static void addNewElements(
        Project project,
        VirtualFile ignoreFile,
        List<? extends IgnoredFileDescriptor> newEntries,
        @Nullable VcsKey vcs,
        @Nullable VirtualFile ignoreEntryRoot
    ) {
        changeIgnoreFile(project, ignoreFile, vcs, provider -> {
            Document document = FileDocumentManager.getInstance().getDocument(ignoreFile);
            if (document == null) {
                return;
            }
            if (document.getTextLength() != 0 && document.getCharsSequence().charAt(document.getTextLength() - 1) != '\n') {
                document.insertString(document.getTextLength(), IgnoreFileConstants.NEWLINE);
            }
            int textEndOffset = document.getTextLength();
            String text = newEntries.stream()
                .map(it -> toText(it, provider, ignoreFile, ignoreEntryRoot))
                .collect(Collectors.joining(IgnoreFileConstants.NEWLINE, "", IgnoreFileConstants.NEWLINE));
            document.insertString(textEndOffset, text);
        });
    }

    private static void changeIgnoreFile(
        Project project,
        VirtualFile ignoreFile,
        @Nullable VcsKey vcs,
        Consumer<IgnoredFileContentProvider> action
    ) {
        VcsKey determinedVcs = vcs;
        if (determinedVcs == null) {
            AbstractVcs fileVcs = VcsUtil.getVcsFor(project, ignoreFile);
            determinedVcs = fileVcs != null ? fileVcs.getKeyInstanceMethod() : null;
        }
        if (determinedVcs == null) {
            return;
        }
        IgnoredFileContentProvider ignoredFileContentProvider = VcsImplUtil.findIgnoredFileContentProvider(project, determinedVcs);
        if (ignoredFileContentProvider == null) {
            return;
        }

        Application.get().invokeAndWait(() -> CommandProcessor.getInstance().newCommand()
            .project(project)
            .inUndoTransparentAction()
            .inWriteAction()
            .run(() -> {
                if (project.isDisposed()) {
                    return;
                }
                if (!ignoreFile.isValid()) {
                    return;
                }
                PsiFile psiFile = PsiManager.getInstance(project).findFile(ignoreFile);
                if (psiFile == null || !(psiFile.getLanguage() instanceof IgnoreLanguage)) {
                    return;
                }
                action.accept(ignoredFileContentProvider);
                save(ignoreFile);
            }));
    }

    private static void addNewElementsToIgnoreBlock(
        String ignoredGroupDescription,
        VirtualFile ignoreFile,
        IgnoredFileDescriptor[] newEntries,
        IgnoredFileContentProvider provider
    ) {
        Document document = FileDocumentManager.getInstance().getDocument(ignoreFile);
        if (document == null) {
            return;
        }
        TextRange contentRange = getOrCreateIgnoreBlockContentTextRange(document, ignoredGroupDescription);
        Set<String> existingEntries = new HashSet<>(
            document.getCharsSequence().subSequence(contentRange.getStartOffset(), contentRange.getEndOffset()).toString().lines().toList()
        );
        String newEntriesText = Arrays.stream(newEntries)
            .map(it -> toText(it, provider, ignoreFile, null))
            .filter(it -> !existingEntries.contains(it))
            .collect(Collectors.joining(IgnoreFileConstants.NEWLINE, "", IgnoreFileConstants.NEWLINE));
        document.insertString(contentRange.getEndOffset(), newEntriesText);
    }

    private static void addNewBlocksToIgnoreFile(
        VirtualFile ignoreFile,
        List<Pair<String, List<IgnoredFileDescriptor>>> groupedEntries,
        IgnoredFileContentProvider provider
    ) {
        Document document = FileDocumentManager.getInstance().getDocument(ignoreFile);
        if (document == null) {
            return;
        }
        CharSequence text = document.getCharsSequence();
        StringBuilder newIgnoredContent = new StringBuilder();
        if (!text.isEmpty() && text.charAt(text.length() - 1) != IgnoreFileConstants.NEWLINE.charAt(0)) {
            newIgnoredContent.append(IgnoreFileConstants.NEWLINE);
        }
        for (Pair<String, List<IgnoredFileDescriptor>> group : groupedEntries) {
            String ignoredGroupDescription = group.getFirst();
            List<IgnoredFileDescriptor> newEntries = group.getSecond();
            if (newEntries.isEmpty()) {
                continue;
            }
            String newEntriesText = newEntries.stream()
                .map(entry -> toText(entry, provider, ignoreFile, null))
                .collect(Collectors.joining(IgnoreFileConstants.NEWLINE, "", IgnoreFileConstants.NEWLINE));
            newIgnoredContent.append(ignoredGroupDescription);
            newIgnoredContent.append(IgnoreFileConstants.NEWLINE);
            newIgnoredContent.append(newEntriesText);
        }
        document.insertString(document.getTextLength(), newIgnoredContent.toString());
    }

    private static TextRange getOrCreateIgnoreBlockContentTextRange(Document ignoreFile, String ignoredGroupDescription) {
        CharSequence text = ignoreFile.getCharsSequence();
        List<String> lines = text.toString().lines().toList();
        int groupDescrLine = lines.indexOf(ignoredGroupDescription);
        if (groupDescrLine == -1) {
            String ignoreGroupToAppend = createIgnoreGroup(text, ignoredGroupDescription);
            ignoreFile.insertString(ignoreFile.getTextLength(), ignoreGroupToAppend);
            int lastIndex = ignoreFile.getTextLength();
            return new TextRange(lastIndex, lastIndex);
        }
        else {
            int groupDescrStartOffset = ignoreFile.getLineStartOffset(groupDescrLine);
            CharSequence tail = text.subSequence(groupDescrStartOffset, ignoreFile.getTextLength());
            List<String> tailLines = tail.toString().lines().skip(1).toList();
            int emptyLine = -1;
            for (int i = 0; i < tailLines.size(); i++) {
                String line = tailLines.get(i);
                if (line.isBlank() || line.startsWith(IgnoreFileConstants.HASH)) {
                    emptyLine = i;
                    break;
                }
            }
            int groupEndOffset;
            if (emptyLine == -1) {
                ignoreFile.insertString(ignoreFile.getTextLength(), IgnoreFileConstants.NEWLINE);
                groupEndOffset = ignoreFile.getTextLength();
            }
            else {
                groupEndOffset = ignoreFile.getLineStartOffset(emptyLine + 1 + groupDescrLine);
            }
            int contentStartOffset = ignoreFile.getLineCount() <= groupDescrLine + 1
                ? groupEndOffset
                : ignoreFile.getLineStartOffset(groupDescrLine + 1);
            return new TextRange(contentStartOffset, groupEndOffset);
        }
    }

    private static String createIgnoreGroup(CharSequence text, String ignoredGroupDescription) {
        boolean newlineRequired = !text.isEmpty() && text.charAt(text.length() - 1) != IgnoreFileConstants.NEWLINE.charAt(0);
        StringBuilder builder = new StringBuilder();
        if (newlineRequired) {
            builder.append(IgnoreFileConstants.NEWLINE);
        }
        builder.append(ignoredGroupDescription);
        builder.append(IgnoreFileConstants.NEWLINE);
        return builder.toString();
    }

    private static String toText(
        IgnoredFileDescriptor descriptor,
        IgnoredFileContentProvider ignoredFileContentProvider,
        VirtualFile ignoreFile,
        @Nullable VirtualFile ignoreEntryRoot
    ) {
        String ignorePath = descriptor.getPath();
        String ignoreMask = descriptor.getMask();
        if (ignoreMask != null) {
            return ignoreMask;
        }
        if (ignorePath != null) {
            VirtualFile ignoreFileContainingDir = ignoreEntryRoot != null ? ignoreEntryRoot : ignoreFile.getParent();
            if (ignoreFileContainingDir == null) {
                throw new IllegalStateException("Cannot determine ignore file path for " + ignoreFile);
            }
            return ignoredFileContentProvider.buildIgnoreEntryContent(ignoreFileContainingDir, descriptor);
        }
        else {
            throw new IllegalStateException("IgnoredFileBean: path and mask cannot be null at the same time");
        }
    }

    // Requires write action
    private static void save(VirtualFile file) {
        if (file.isDirectory() || !file.isValid()) {
            return;
        }
        FileDocumentManager documentManager = FileDocumentManager.getInstance();
        if (documentManager.isFileModified(file)) {
            Document document = documentManager.getDocument(file);
            if (document != null) {
                Application.get().invokeLater(() -> documentManager.saveDocumentAsIs(document));
            }
        }
    }

    private IgnoreFileUtil() {
    }
}
