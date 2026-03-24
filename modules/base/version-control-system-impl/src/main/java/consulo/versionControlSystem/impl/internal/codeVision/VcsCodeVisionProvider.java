// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.versionControlSystem.impl.internal.codeVision;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.application.Application;
import consulo.codeEditor.Editor;
import consulo.codeEditor.util.EditorUtil;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.document.Document;
import consulo.document.util.DocumentUtil;
import consulo.document.util.TextRange;
import consulo.language.Language;
import consulo.language.editor.codeVision.*;
import consulo.language.psi.*;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.ex.action.ActionManager;
import consulo.ui.ex.action.ActionPlaces;
import consulo.ui.ex.action.AnAction;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.util.dataholder.Key;
import consulo.util.lang.Pair;
import consulo.versionControlSystem.AbstractVcs;
import consulo.versionControlSystem.ProjectLevelVcsManager;
import consulo.versionControlSystem.annotate.*;
import consulo.versionControlSystem.change.VcsAnnotationLocalChangesListener;
import consulo.versionControlSystem.codeVision.VcsCodeVisionLanguageContext;
import consulo.versionControlSystem.impl.internal.annotate.AnnotationsPreloader;
import consulo.versionControlSystem.internal.UpToDateLineNumberProviderImpl;
import consulo.versionControlSystem.localize.VcsLocalize;
import consulo.versionControlSystem.util.VcsUtil;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.status.FileStatus;
import consulo.virtualFileSystem.status.FileStatusManager;
import org.jspecify.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.*;

/**
 * Code vision provider that shows the primary author and number of other authors for each
 * language element accepted by the registered {@link VcsCodeVisionLanguageContext} extensions.
 * <p>
 * Annotations are loaded by {@link AnnotationsPreloader} in the background and stored in
 * {@link VcsCacheManager}. If the annotation is not yet cached an empty list is returned and
 * the daemon will be restarted once the preloader finishes loading.
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

        Language fileLanguage = file.getLanguage();
        VcsCodeVisionLanguageContext fileContext = VcsCodeVisionLanguageContext.forLanguage(fileLanguage);

        List<VcsCodeVisionLanguageContext> additionalContexts;
        if (fileContext == null) {
            additionalContexts = Application.get().getExtensionPoint(VcsCodeVisionLanguageContext.class)
                .collectFiltered(ctx -> ctx.isCustomFileAccepted(file));
        }
        else {
            additionalContexts = Collections.emptyList();
        }

        if (fileContext == null && additionalContexts.isEmpty()) return Collections.emptyList();

        VirtualFile virtualFile = VcsUtil.resolveSymlinkIfNeeded(project, file.getViewProvider().getVirtualFile());
        AbstractVcs vcs = ProjectLevelVcsManager.getInstance(project).getVcsFor(virtualFile);
        if (vcs == null) return Collections.emptyList();

        AnnotationProvider annotationProvider = vcs.getAnnotationProvider();
        if (annotationProvider == null) return Collections.emptyList();
        // Only show code vision for VCS implementations that support annotation caching.
        if (!(annotationProvider instanceof CacheableAnnotationProvider) &&
            !(annotationProvider instanceof VcsCacheableAnnotationProvider)) {
            return Collections.emptyList();
        }

        FileStatus status = FileStatusManager.getInstance(project).getStatus(virtualFile);
        if (status == FileStatus.UNKNOWN || status == FileStatus.IGNORED) return Collections.emptyList();

        FileAnnotation annotation = editor.getUserData(VCS_CODE_AUTHOR_ANNOTATION);

        if (annotation == null) {
            if (status == FileStatus.ADDED) {
                // New file has no VCS annotation — treat as "new code"; fall through with null annotation
            }
            else {
                // JB approach: use CacheableAnnotationProvider.getFromCache() when available.
                if (annotationProvider instanceof CacheableAnnotationProvider cacheableProvider) {
                    annotation = cacheableProvider.getFromCache(virtualFile);
                }
                else {
                    annotation = VcsCacheManager.getInstance(project).getCachedAnnotation(virtualFile);
                }
                if (annotation == null) {
                    // Not yet in cache — AnnotationsPreloader will populate it and restart the daemon.
                    return Collections.emptyList();
                }
            }
        }

        LineAnnotationAspect authorAspect = null;
        if (annotation != null) {
            handleAnnotationRegistration(annotation, editor, project, virtualFile);
            for (LineAnnotationAspect aspect : annotation.getAspects()) {
                if (LineAnnotationAspect.AUTHOR.equals(aspect.getId())) {
                    authorAspect = aspect;
                    break;
                }
            }
        }

        Document document = editor.getDocument();
        UpToDateLineNumberProviderImpl lineNumberProvider = new UpToDateLineNumberProviderImpl(document, project);
        int docLength = document.getTextLength();

        List<Pair<TextRange, CodeVisionEntry>> result = new ArrayList<>();
        SyntaxTraverser<PsiElement> traverser = SyntaxTraverser.psiTraverser(file);

        for (PsiElement element : traverser) {
            VcsCodeVisionLanguageContext elementContext;
            Language elementLanguage;

            if (fileContext != null) {
                elementContext = fileContext;
                elementLanguage = fileLanguage;
            }
            else {
                elementLanguage = element.getLanguage();
                elementContext = VcsCodeVisionLanguageContext.forLanguage(elementLanguage);
                if (!additionalContexts.contains(elementContext)) continue;
            }

            if (elementContext == null || !elementContext.isAccepted(element)) continue;

            TextRange elementRange = clamp(getTextRangeWithoutLeadingCommentsAndWhitespaces(element), docLength);
            TextRange trimmedRange = clamp(elementContext.computeEffectiveRange(element), docLength);

            VcsCodeAuthorInfo codeAuthorInfo = getCodeAuthorInfo(trimmedRange, document, lineNumberProvider, authorAspect);
            String text = codeAuthorInfoText(codeAuthorInfo);
            // Show author icon when the main author is known (port of JB: AllIcons.Vcs.Author)
            javax.swing.Icon icon = codeAuthorInfo.mainAuthor() != null ? TargetAWT.to(PlatformIconGroup.actionsLoginavatar()) : null;

            Language finalElementLanguage = elementLanguage;
            SmartPsiElementPointer<PsiElement> pointer = SmartPointerManager.createPointer(element);
            ClickableTextCodeVisionEntry entry = new ClickableTextCodeVisionEntry(
                text,
                ID,
                (event, ed) -> {
                    if (event == null) return;
                    Component component = event.getComponent();
                    if (component instanceof JComponent jComponent) {
                        invokeAnnotateAction(event, jComponent);
                    }
                    PsiElement el = pointer.getElement();
                    if (el == null) return;
                    VcsCodeVisionLanguageContext ctx = VcsCodeVisionLanguageContext.forLanguage(finalElementLanguage);
                    if (ctx != null) ctx.handleClick(event, ed, el);
                },
                icon,
                text,
                text,
                Collections.emptyList()
            );
            entry.showInMorePopup = false;
            result.add(Pair.create(elementRange, entry));
        }

        return result;
    }

    @Override
    public @Nullable CodeVisionPlaceholderCollector getPlaceholderCollector(Editor editor, @Nullable PsiFile psiFile) {
        if (psiFile == null) return null;
        Language language = psiFile.getLanguage();
        VcsCodeVisionLanguageContext languageContext = VcsCodeVisionLanguageContext.forLanguage(language);
        if (languageContext == null) return null;
        Project project = editor.getProject();
        if (project == null) return null;
        VirtualFile virtualFile = psiFile.getViewProvider().getVirtualFile();
        AbstractVcs vcs = ProjectLevelVcsManager.getInstance(project).getVcsFor(virtualFile);
        if (vcs == null) return null;
        AnnotationProvider annotationProvider = vcs.getAnnotationProvider();
        if (!(annotationProvider instanceof CacheableAnnotationProvider) &&
            !(annotationProvider instanceof VcsCacheableAnnotationProvider)) return null;

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

    private static void handleAnnotationRegistration(FileAnnotation annotation, Editor editor, Project project, VirtualFile file) {
        if (editor.getUserData(VCS_CODE_AUTHOR_ANNOTATION) != null) return;

        VcsAnnotationLocalChangesListener changeListener =
            ProjectLevelVcsManager.getInstance(project).getAnnotationLocalChangesListener();

        Disposable annotationDisposable = () -> {
            changeListener.unregisterAnnotation(file, annotation);
            annotation.dispose();
        };
        annotation.setCloser(() -> {
            editor.putUserData(VCS_CODE_AUTHOR_ANNOTATION, null);
            Disposer.dispose(annotationDisposable);
            project.getInstance(AnnotationsPreloader.class).schedulePreloading(file);
        });
        annotation.setReloader(__ -> annotation.close());

        editor.putUserData(VCS_CODE_AUTHOR_ANNOTATION, annotation);
        changeListener.registerAnnotation(file, annotation);
        Application.get().invokeLater(() -> EditorUtil.disposeWithEditor(editor, annotationDisposable));
    }

    private static void invokeAnnotateAction(MouseEvent event, JComponent contextComponent) {
        AnAction action = ActionManager.getInstance().getAction("Annotate");
        if (action != null) {
            ActionManager.getInstance().tryToExecute(action, event, contextComponent, ActionPlaces.UNKNOWN, true);
        }
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
