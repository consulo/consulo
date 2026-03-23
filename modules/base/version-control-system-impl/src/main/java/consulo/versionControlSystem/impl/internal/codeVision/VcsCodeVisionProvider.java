// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.versionControlSystem.impl.internal.codeVision;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.application.Application;
import consulo.codeEditor.Editor;
import consulo.document.Document;
import consulo.document.util.DocumentUtil;
import consulo.document.util.TextRange;
import consulo.language.editor.DaemonCodeAnalyzer;
import consulo.language.editor.codeVision.*;
import consulo.language.psi.*;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.util.dataholder.Key;
import consulo.util.lang.Pair;
import consulo.versionControlSystem.AbstractVcs;
import consulo.versionControlSystem.ProjectLevelVcsManager;
import consulo.versionControlSystem.VcsException;
import consulo.versionControlSystem.annotate.AnnotationProvider;
import consulo.versionControlSystem.annotate.FileAnnotation;
import consulo.versionControlSystem.annotate.LineAnnotationAspect;
import consulo.versionControlSystem.codeVision.VcsCodeVisionLanguageContext;
import consulo.versionControlSystem.internal.UpToDateLineNumberProviderImpl;
import consulo.versionControlSystem.localize.VcsLocalize;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.status.FileStatus;
import consulo.virtualFileSystem.status.FileStatusManager;
import org.jspecify.annotations.Nullable;

import java.util.*;

/**
 * Code vision provider that shows the primary author and number of other authors for each
 * language element accepted by the registered {@link VcsCodeVisionLanguageContext} extensions.
 * <p>
 * Annotations are loaded lazily: the first call will schedule a background load, returning
 * an empty list. Subsequent calls will use the cached annotation stored on the editor.
 */
@ExtensionImpl
public class VcsCodeVisionProvider implements DaemonBoundCodeVisionProvider {
    public static final String ID = "vcs.code.vision";

    /** Cached annotation stored per editor. */
    private static final Key<FileAnnotation> VCS_CODE_AUTHOR_ANNOTATION = Key.create("Vcs.CodeAuthor.Annotation");

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public LocalizeValue getName() {
        return VcsLocalize.codeVisionAuthorLabel();
    }

    @Override
    public List<CodeVisionRelativeOrdering> getRelativeOrderings() {
        return Collections.emptyList();
    }

    @Override
    public CodeVisionAnchorKind getDefaultAnchor() {
        return CodeVisionAnchorKind.Default;
    }

    @Override
    @RequiredReadAction
    public List<Pair<TextRange, CodeVisionEntry>> computeForEditor(Editor editor, PsiFile file) {
        Project project = editor.getProject();
        if (project == null || project.isDefault()) return Collections.emptyList();

        VcsCodeVisionLanguageContext languageContext = VcsCodeVisionLanguageContext.forLanguage(file.getLanguage());
        if (languageContext == null) return Collections.emptyList();

        VirtualFile virtualFile = file.getViewProvider().getVirtualFile();
        AbstractVcs vcs = ProjectLevelVcsManager.getInstance(project).getVcsFor(virtualFile);
        if (vcs == null) return Collections.emptyList();

        AnnotationProvider annotationProvider = vcs.getAnnotationProvider();
        if (annotationProvider == null || !annotationProvider.isCaching()) return Collections.emptyList();

        FileStatus status = FileStatusManager.getInstance(project).getStatus(virtualFile);
        if (status == FileStatus.UNKNOWN || status == FileStatus.IGNORED) return Collections.emptyList();

        // Try to get annotation from editor user data (previously loaded)
        FileAnnotation annotation = editor.getUserData(VCS_CODE_AUTHOR_ANNOTATION);

        if (annotation == null) {
            if (status == FileStatus.ADDED) {
                // New file has no VCS annotation — treat as "new code"
                // Fall through with null annotation to show "new code"
            } else {
                // Try loading from cache asynchronously
                scheduleAnnotationLoad(editor, project, virtualFile, annotationProvider);
                return Collections.emptyList();
            }
        }

        // AUTHOR aspect from the loaded annotation
        LineAnnotationAspect authorAspect = null;
        if (annotation != null) {
            registerAnnotationIfAbsent(annotation, editor);
            for (LineAnnotationAspect aspect : annotation.getAspects()) {
                if (LineAnnotationAspect.AUTHOR.equals(aspect.getId())) {
                    authorAspect = aspect;
                    break;
                }
            }
        }

        Document document = editor.getDocument();
        UpToDateLineNumberProviderImpl lineNumberProvider = new UpToDateLineNumberProviderImpl(document, project);

        List<Pair<TextRange, CodeVisionEntry>> result = new ArrayList<>();
        SyntaxTraverser<PsiElement> traverser = SyntaxTraverser.psiTraverser(file);

        for (PsiElement element : traverser) {
            if (!languageContext.isAccepted(element)) continue;

            TextRange elementRange = getTextRangeWithoutLeadingCommentsAndWhitespaces(element);
            TextRange trimmedRange = languageContext.computeEffectiveRange(element);

            // Clamp to document length
            int docLength = document.getTextLength();
            elementRange = clamp(elementRange, docLength);
            trimmedRange = clamp(trimmedRange, docLength);

            VcsCodeAuthorInfo codeAuthorInfo = getCodeAuthorInfo(trimmedRange, document, lineNumberProvider, authorAspect);
            String text = codeAuthorInfoText(codeAuthorInfo);

            SmartPsiElementPointer<PsiElement> pointer = SmartPointerManager.createPointer(element);
            ClickableTextCodeVisionEntry entry = new ClickableTextCodeVisionEntry(
                text,
                ID,
                (event, ed) -> {
                    if (event == null) return;
                    PsiElement el = pointer.getElement();
                    if (el == null) return;
                    languageContext.handleClick(event, ed, el);
                }
            );
            entry.showInMorePopup = false;
            result.add(Pair.create(elementRange, entry));
        }

        return result;
    }

    @Override
    public @Nullable CodeVisionPlaceholderCollector getPlaceholderCollector(Editor editor, @Nullable PsiFile psiFile) {
        if (psiFile == null) return null;
        VcsCodeVisionLanguageContext languageContext = VcsCodeVisionLanguageContext.forLanguage(psiFile.getLanguage());
        if (languageContext == null) return null;
        Project project = editor.getProject();
        if (project == null) return null;
        VirtualFile virtualFile = psiFile.getViewProvider().getVirtualFile();
        if (virtualFile == null) return null;
        AbstractVcs vcs = ProjectLevelVcsManager.getInstance(project).getVcsFor(virtualFile);
        if (vcs == null) return null;
        AnnotationProvider annotationProvider = vcs.getAnnotationProvider();
        if (annotationProvider == null || !annotationProvider.isCaching()) return null;

        return (BypassBasedPlaceholderCollector) (element, ed) -> {
            if (!languageContext.isAccepted(element)) return Collections.emptyList();
            return List.of(getTextRangeWithoutLeadingCommentsAndWhitespaces(element));
        };
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private static TextRange clamp(TextRange range, int maxLength) {
        int start = Math.min(range.getStartOffset(), maxLength);
        int end = Math.min(range.getEndOffset(), maxLength);
        return TextRange.create(start, end);
    }

    @RequiredReadAction
    private static TextRange getTextRangeWithoutLeadingCommentsAndWhitespaces(PsiElement element) {
        PsiElement start = element;
        for (PsiElement child : SyntaxTraverser.psiApi().children(element)) {
            if (!(child instanceof PsiComment) && !(child instanceof PsiWhiteSpace)) {
                start = child;
                break;
            }
        }
        return TextRange.create(start.getTextRange().getStartOffset(), element.getTextRange().getEndOffset());
    }

    private static void scheduleAnnotationLoad(Editor editor,
                                               Project project,
                                               VirtualFile file,
                                               AnnotationProvider provider) {
        Application.get().executeOnPooledThread(() -> {
            if (project.isDisposed() || editor.isDisposed()) return;
            try {
                FileAnnotation annotation = provider.annotate(file);
                Application.get().invokeLater(() -> {
                    if (project.isDisposed() || editor.isDisposed()) return;
                    registerAnnotationIfAbsent(annotation, editor);
                    DaemonCodeAnalyzer.getInstance(project).restart();
                });
            } catch (VcsException ignored) {
                // Silently ignore; the lens will simply not appear for this file
            }
        });
    }

    private static void registerAnnotationIfAbsent(FileAnnotation annotation, Editor editor) {
        if (editor.getUserData(VCS_CODE_AUTHOR_ANNOTATION) != null) return;
        editor.putUserData(VCS_CODE_AUTHOR_ANNOTATION, annotation);
        annotation.setCloser(() -> {
            editor.putUserData(VCS_CODE_AUTHOR_ANNOTATION, null);
            annotation.dispose();
        });
        annotation.setReloader(newAnnotation -> editor.putUserData(VCS_CODE_AUTHOR_ANNOTATION, newAnnotation));
    }

    private static VcsCodeAuthorInfo getCodeAuthorInfo(TextRange range,
                                                       Document document,
                                                       UpToDateLineNumberProviderImpl lineProvider,
                                                       @Nullable LineAnnotationAspect authorAspect) {
        if (authorAspect == null) {
            return VcsCodeAuthorInfo.NEW_CODE;
        }
        int startLine = document.getLineNumber(range.getStartOffset());
        int endLine = document.getLineNumber(range.getEndOffset());

        Map<String, Integer> authorsFrequency = new HashMap<>();
        for (int line = startLine; line <= endLine; line++) {
            String lineContent = document.getText(DocumentUtil.getLineTextRange(document, line));
            if (lineContent.isBlank()) continue;
            int vcsLine = lineProvider.getLineNumber(line);
            if (vcsLine < 0) continue;
            String author = authorAspect.getValue(vcsLine);
            if (author == null || author.isEmpty()) continue;
            authorsFrequency.merge(author, 1, Integer::sum);
        }

        if (authorsFrequency.isEmpty()) {
            return VcsCodeAuthorInfo.NEW_CODE;
        }

        int maxFrequency = authorsFrequency.values().stream().mapToInt(Integer::intValue).max().orElse(0);
        String mainAuthor = authorsFrequency.entrySet().stream()
            .filter(e -> e.getValue() == maxFrequency)
            .map(Map.Entry::getKey)
            .min(String::compareTo)
            .orElse(null);

        int otherAuthorsCount = authorsFrequency.size() - 1;
        boolean isModified = lineProvider.isRangeChanged(startLine, endLine + 1);
        return new VcsCodeAuthorInfo(mainAuthor, otherAuthorsCount, isModified);
    }

    private static String codeAuthorInfoText(VcsCodeAuthorInfo info) {
        if (info.mainAuthor() == null) {
            return VcsLocalize.codeVisionLabelNewCode().getValue();
        }
        boolean isMultiAuthor = info.otherAuthorsCount() > 0;
        if (isMultiAuthor && info.isModified()) {
            return VcsLocalize.codeVisionLabelMultiAuthorModified(info.mainAuthor(), info.otherAuthorsCount()).getValue();
        }
        if (isMultiAuthor) {
            return VcsLocalize.codeVisionLabelMultiAuthorNotModified(info.mainAuthor(), info.otherAuthorsCount()).getValue();
        }
        if (info.isModified()) {
            return VcsLocalize.codeVisionLabelSingleAuthorModified(info.mainAuthor()).getValue();
        }
        return info.mainAuthor();
    }

    private record VcsCodeAuthorInfo(@Nullable String mainAuthor, int otherAuthorsCount, boolean isModified) {
        static final VcsCodeAuthorInfo NEW_CODE = new VcsCodeAuthorInfo(null, 0, true);
    }
}
