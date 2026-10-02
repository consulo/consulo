// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.language.editor.impl.internal.documentation;

import consulo.annotation.access.RequiredReadAction;
import consulo.application.AccessRule;
import consulo.application.internal.ProgressIndicatorUtils;
import consulo.application.util.DateFormatUtil;
import consulo.content.scope.NamedScope;
import consulo.content.scope.NamedScopesHolder;
import consulo.content.scope.PackageSet;
import consulo.content.scope.PackageSetBase;
import consulo.language.editor.FileColorManager;
import consulo.language.editor.documentation.DocumentationMarkup;
import consulo.language.editor.documentation.DocumentationProvider;
import consulo.language.editor.documentation.ExternalDocumentationProvider;
import consulo.language.editor.internal.DocumentationManagerHelper;
import consulo.language.editor.localize.CodeInsightLocalize;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiUtilCore;
import consulo.language.psi.SmartPsiElementPointer;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.ui.ex.awt.util.ColorUtil;
import consulo.ui.util.ColorValueUtil;
import consulo.util.lang.StringUtil;
import consulo.util.lang.ref.SimpleReference;
import consulo.versionControlSystem.change.ChangeListManager;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.fileType.FileType;
import consulo.virtualFileSystem.status.FileStatus;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import org.jspecify.annotations.Nullable;

import java.awt.Color;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collections;
import java.util.List;

public class ElementDocumentationCollector extends DocumentationCollector {
    private static final Logger LOG = Logger.getInstance(ElementDocumentationCollector.class);

    private static final long DOC_GENERATION_TIMEOUT_MILLISECONDS = 60000;
    private static final long DOC_GENERATION_PAUSE_MILLISECONDS = 100;

    private final Project myProject;
    private final @Nullable PsiElement myOriginalElement;
    private final boolean myOnHover;

    public ElementDocumentationCollector(
        Project project,
        PsiElement element,
        @Nullable PsiElement originalElement,
        @Nullable String ref,
        boolean onHover
    ) {
        super(element, null, ref, null);
        myProject = project;
        myOriginalElement = originalElement;
        myOnHover = onHover;
    }

    public @Nullable PsiElement getOriginalElement() {
        return myOriginalElement;
    }

    @Override
    public @Nullable String getDocumentation() {
        PsiElement element = getElement();
        DocumentationProvider provider =
            AccessRule.read(() -> DocumentationManagerHelper.getProviderFromElement(element, myOriginalElement));
        setProvider(provider);
        LOG.debug("Using provider ", provider);

        if (provider instanceof ExternalDocumentationProvider externalProvider) {
            List<String> urls = AccessRule.read(() -> {
                SmartPsiElementPointer originalElementPtr = element.getUserData(DocumentationManagerHelper.ORIGINAL_ELEMENT_KEY);
                PsiElement originalElement = originalElementPtr != null ? originalElementPtr.getElement() : null;
                return provider.getUrlFor(element, originalElement);
            });
            LOG.debug("External documentation URLs: ", urls);
            if (urls != null) {
                for (String url : urls) {
                    String doc = externalProvider.fetchExternalDocumentation(myProject, element, Collections.singletonList(url));
                    if (doc != null) {
                        LOG.debug("Fetched documentation from ", url);
                        setEffectiveUrl(url);
                        return doc;
                    }
                }
            }
        }

        SimpleReference<String> result = new SimpleReference<>();
        ProgressIndicatorUtils.runInReadActionWithWriteActionPriorityWithRetries(
            () -> {
                if (!element.isValid()) {
                    return;
                }
                SmartPsiElementPointer originalPointer = element.getUserData(DocumentationManagerHelper.ORIGINAL_ELEMENT_KEY);
                PsiElement originalPsi = originalPointer != null ? originalPointer.getElement() : null;
                String doc = myOnHover ? provider.generateHoverDoc(element, originalPsi) : provider.generateDoc(element, originalPsi);
                if (element instanceof PsiFile file) {
                    String fileDoc = generateFileDoc(file, doc == null);
                    if (fileDoc != null) {
                        doc = doc == null ? fileDoc : doc + fileDoc;
                    }
                }
                result.set(doc);
            },
            DOC_GENERATION_TIMEOUT_MILLISECONDS,
            DOC_GENERATION_PAUSE_MILLISECONDS
        );
        return result.get();
    }

    @RequiredReadAction
    private static @Nullable String generateFileDoc(PsiFile psiFile, boolean withUrl) {
        VirtualFile file = PsiUtilCore.getVirtualFile(psiFile);
        File ioFile = file == null || !file.isInLocalFileSystem() ? null : VirtualFileUtil.virtualToIoFile(file);
        BasicFileAttributes attr = null;
        try {
            attr = ioFile == null ? null : Files.readAttributes(Paths.get(ioFile.toURI()), BasicFileAttributes.class);
        }
        catch (Exception ignored) {
        }
        if (attr == null) {
            return null;
        }
        FileType type = file.getFileType();
        String typeName = type.getDisplayName().get();
        String languageName = type.isBinary() ? "" : psiFile.getLanguage().getDisplayName().get();
        return (
            withUrl
                ? DocumentationMarkup.DEFINITION_START + file.getPresentableUrl() + DocumentationMarkup.DEFINITION_END +
                DocumentationMarkup.CONTENT_START
                : ""
        ) +
            getVcsStatus(psiFile.getProject(), file) +
            getScope(psiFile.getProject(), file) +
            "<p><span class='grayed'>" + CodeInsightLocalize.documentationFileSizeLabel().get() + "</span> " +
            StringUtil.formatFileSize(attr.size()) +
            "<p><span class='grayed'>" + CodeInsightLocalize.documentationFileTypeLabel().get() + "</span> " +
            typeName +
            (type.isBinary() || typeName.equals(languageName) ? "" : " (" + languageName + ")") +
            "<p><span class='grayed'>" + CodeInsightLocalize.documentationFileModificationDatetimeLabel().get() + "</span> " +
            DateFormatUtil.formatDateTime(attr.lastModifiedTime().toMillis()) +
            "<p><span class='grayed'>" + CodeInsightLocalize.documentationFileCreationDatetimeLabel().get() + "</span> " +
            DateFormatUtil.formatDateTime(attr.creationTime().toMillis()) +
            (withUrl ? DocumentationMarkup.CONTENT_END : "");
    }

    @RequiredReadAction
    private static String getScope(Project project, VirtualFile file) {
        FileColorManager colorManager = FileColorManager.getInstance(project);
        Color color = colorManager.getRendererBackground(file);
        if (color == null) {
            return "";
        }
        for (NamedScopesHolder holder : NamedScopesHolder.getAllNamedScopeHolders(project)) {
            for (NamedScope scope : holder.getScopes()) {
                PackageSet packageSet = scope.getValue();
                String name = scope.getScopeId();
                if (packageSet instanceof PackageSetBase
                    && packageSet.contains(file, project, holder)
                    && colorManager.getScopeColor(name) == color) {
                    return "<p><span class='grayed'>" + CodeInsightLocalize.documentationFileScopeLabel().get() + "</span> " +
                        "<span bgcolor='" + ColorUtil.toHex(color) + "'>" + scope.getScopeId() + "</span>";
                }
            }
        }
        return "";
    }

    private static String getVcsStatus(Project project, VirtualFile file) {
        FileStatus status = ChangeListManager.getInstance(project).getStatus(file);
        return status != FileStatus.NOT_CHANGED
            ? "<p><span class='grayed'>" + CodeInsightLocalize.documentationFileVcsStatusLabel().get() + "</span> <span color='" +
            ColorValueUtil.toHex(status.getColor()) + "'>" +
            status.getText() + "</span>"
            : "";
    }
}
