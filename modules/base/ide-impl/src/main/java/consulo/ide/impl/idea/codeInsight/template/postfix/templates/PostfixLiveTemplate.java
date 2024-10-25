// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.codeInsight.template.postfix.templates;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.application.Application;
import consulo.application.progress.ProgressManager;
import consulo.codeEditor.Editor;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.document.Document;
import consulo.externalService.statistic.FeatureUsageTracker;
import consulo.ide.impl.idea.codeInsight.completion.OffsetTranslatorImpl;
import consulo.ide.impl.idea.codeInsight.template.postfix.completion.PostfixTemplateLookupElement;
import consulo.language.Language;
import consulo.language.editor.localize.CodeInsightLocalize;
import consulo.language.editor.postfixTemplate.PostfixTemplate;
import consulo.language.editor.postfixTemplate.PostfixTemplateProvider;
import consulo.language.editor.postfixTemplate.PostfixTemplatesSettings;
import consulo.language.editor.postfixTemplate.PostfixTemplatesUtils;
import consulo.language.editor.template.CustomLiveTemplateBase;
import consulo.language.editor.template.CustomLiveTemplateLookupElement;
import consulo.language.editor.template.CustomTemplateCallback;
import consulo.language.impl.psi.PsiFileImpl;
import consulo.language.psi.*;
import consulo.language.template.TemplateLanguageUtil;
import consulo.language.util.AttachmentFactoryUtil;
import consulo.language.util.LanguageUtil;
import consulo.logging.Logger;
import consulo.project.DumbService;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.undoRedo.CommandProcessor;
import consulo.undoRedo.util.UndoUtil;
import consulo.util.lang.StringUtil;
import consulo.util.lang.function.Condition;
import consulo.util.lang.function.Conditions;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.fileType.FileType;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

@ExtensionImpl
public class PostfixLiveTemplate extends CustomLiveTemplateBase {
    @Nonnull
    @Override
    @RequiredReadAction
    public Collection<? extends CustomLiveTemplateLookupElement> getLookupElements(
        @Nonnull PsiFile file,
        @Nonnull Editor editor,
        int offset
    ) {
        Collection<CustomLiveTemplateLookupElement> result = new HashSet<>();
        CustomTemplateCallback callback = new CustomTemplateCallback(editor, file);
        Disposable parentDisposable = Disposable.newDisposable();
        try {
            for (PostfixTemplateProvider provider : PostfixTemplateProvider.forLanguage(getLanguage(callback))) {
                ProgressManager.checkCanceled();
                String key = computeTemplateKeyWithoutContextChecking(callback);
                if (key != null && editor.getCaretModel().getCaretCount() == 1) {
                    Condition<PostfixTemplate> isApplicationTemplateFunction =
                        createIsApplicationTemplateFunction(provider, key, file, editor, parentDisposable);
                    for (PostfixTemplate postfixTemplate : PostfixTemplatesUtils.getAvailableTemplates(provider)) {
                        ProgressManager.checkCanceled();
                        if (isApplicationTemplateFunction.value(postfixTemplate)) {
                            result.add(new PostfixTemplateLookupElement(
                                this,
                                postfixTemplate,
                                postfixTemplate.getKey(),
                                provider,
                                false
                            ));
                        }
                    }
                }
            }
        }
        finally {
            Disposer.dispose(parentDisposable);
        }

        return result;
    }

    public static final String POSTFIX_TEMPLATE_ID = PostfixTemplate.POSTFIX_TEMPLATE_CUSTOM_TEMPLATE_ID;

    private static final Logger LOG = Logger.getInstance(PostfixLiveTemplate.class);

    @Nonnull
    public Set<String> getAllTemplateKeys(PsiFile file, int offset) {
        Set<String> keys = new HashSet<>();
        Language language = PsiUtilCore.getLanguageAtOffset(file, offset);
        for (PostfixTemplateProvider provider : PostfixTemplateProvider.forLanguage(language)) {
            ProgressManager.checkCanceled();
            keys.addAll(getKeys(provider));
        }
        return keys;
    }

    @Nullable
    private static String computeTemplateKeyWithoutContextChecking(
        @Nonnull PostfixTemplateProvider provider,
        @Nonnull CharSequence documentContent,
        int currentOffset
    ) {
        int startOffset = currentOffset;
        if (documentContent.length() < startOffset) {
            return null;
        }

        while (startOffset > 0) {
            ProgressManager.checkCanceled();
            char currentChar = documentContent.charAt(startOffset - 1);
            if (!Character.isJavaIdentifierPart(currentChar)) {
                if (!provider.isTerminalSymbol(currentChar)) {
                    return null;
                }
                startOffset--;
                break;
            }
            startOffset--;
        }
        return String.valueOf(documentContent.subSequence(startOffset, currentOffset));
    }

    @Nullable
    @Override
    @RequiredReadAction
    public String computeTemplateKey(@Nonnull CustomTemplateCallback callback) {
        Editor editor = callback.getEditor();
        CharSequence charsSequence = editor.getDocument().getCharsSequence();
        int offset = editor.getCaretModel().getOffset();
        for (PostfixTemplateProvider provider : PostfixTemplateProvider.forLanguage(getLanguage(callback))) {
            String key = computeTemplateKeyWithoutContextChecking(provider, charsSequence, offset);
            if (key != null && isApplicableTemplate(provider, key, callback.getFile(), editor)) {
                return key;
            }
        }
        return null;
    }

    @Nullable
    @Override
    public String computeTemplateKeyWithoutContextChecking(@Nonnull CustomTemplateCallback callback) {
        Editor editor = callback.getEditor();
        int currentOffset = editor.getCaretModel().getOffset();
        for (PostfixTemplateProvider provider : PostfixTemplateProvider.forLanguage(getLanguage(callback))) {
            ProgressManager.checkCanceled();
            String key = computeTemplateKeyWithoutContextChecking(
                provider,
                editor.getDocument().getCharsSequence(),
                currentOffset
            );
            if (key != null) {
                return key;
            }
        }
        return null;
    }

    @Override
    public boolean supportsMultiCaret() {
        return false;
    }

    @RequiredUIAccess
    @Override
    public void expand(@Nonnull final String key, @Nonnull final CustomTemplateCallback callback) {
        Application.get().assertIsDispatchThread();

        Editor editor = callback.getEditor();
        PsiFile file = callback.getContext().getContainingFile();
        for (PostfixTemplateProvider provider : PostfixTemplateProvider.forLanguage(getLanguage(callback))) {
            PostfixTemplate postfixTemplate = findApplicableTemplate(provider, key, editor, file);
            if (postfixTemplate != null) {
                expandTemplate(key, callback, editor, provider, postfixTemplate);
                return;
            }
        }

        // don't care about errors in multiCaret mode
        if (editor.getCaretModel().getAllCarets().size() == 1) {
            LOG.error(
                "Template not found by key: " + key + "; offset = " + callback.getOffset(),
                AttachmentFactoryUtil.createAttachment(callback.getFile().getVirtualFile())
            );
        }
    }

    @RequiredUIAccess
    public static void expandTemplate(
        @Nonnull String key,
        @Nonnull CustomTemplateCallback callback,
        @Nonnull Editor editor,
        @Nonnull PostfixTemplateProvider provider,
        @Nonnull PostfixTemplate postfixTemplate
    ) {
        Application.get().assertIsDispatchThread();
        FeatureUsageTracker.getInstance().triggerFeatureUsed("editing.completion.postfix");
        final PsiFile file = callback.getContext().getContainingFile();
        if (isApplicableTemplate(provider, key, file, editor, postfixTemplate)) {
            int offset = deleteTemplateKey(file, editor, key);
            try {
                provider.preExpand(file, editor);
                PsiElement context = CustomTemplateCallback.getContext(file, positiveOffset(offset));
                expandTemplate(postfixTemplate, editor, context);
            }
            finally {
                provider.afterExpand(file, editor);
            }
        }
        // don't care about errors in multiCaret mode
        else if (editor.getCaretModel().getAllCarets().size() == 1) {
            LOG.error(
                "Template not found by key: " + key + "; offset = " + callback.getOffset(),
                AttachmentFactoryUtil.createAttachment(callback.getFile().getVirtualFile())
            );
        }
    }

    @Override
    public boolean isApplicable(@Nonnull CustomTemplateCallback callback, int offset, boolean wrapping) {
        PostfixTemplatesSettings settings = PostfixTemplatesSettings.getInstance();
        if (wrapping || !settings.isPostfixTemplatesEnabled()) {
            return false;
        }
        PsiFile contextFile = callback.getFile();
        Language language = PsiUtilCore.getLanguageAtOffset(contextFile, offset);
        CharSequence fileText = callback.getEditor().getDocument().getImmutableCharSequence();
        for (PostfixTemplateProvider provider : PostfixTemplateProvider.forLanguage(language)) {
            if (StringUtil.isNotEmpty(computeTemplateKeyWithoutContextChecking(provider, fileText, offset + 1))) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean supportsWrapping() {
        return false;
    }

    @Override
    public void wrap(@Nonnull String selection, @Nonnull CustomTemplateCallback callback) {
        throw new UnsupportedOperationException();
    }

    @Nonnull
    @Override
    public String getTitle() {
        return CodeInsightLocalize.postfixLiveTemplateTitle().get();
    }

    @Override
    public char getShortcut() {
        return (char)PostfixTemplatesSettings.getInstance().getShortcut();
    }

    @Override
    public boolean hasCompletionItem(@Nonnull CustomTemplateCallback callback, int offset) {
        return true;
    }

    @RequiredUIAccess
    private static void expandTemplate(
        @Nonnull final PostfixTemplate template,
        @Nonnull final Editor editor,
        @Nonnull final PsiElement context
    ) {
        if (template.startInWriteAction()) {
            Application.get().runWriteAction(
                () -> CommandProcessor.getInstance().executeCommand(
                    context.getProject(),
                    () -> template.expand(context, editor),
                    CodeInsightLocalize.commandExpandPostfixTemplate().get(),
                    POSTFIX_TEMPLATE_ID
                )
            );
        }
        else {
            template.expand(context, editor);
        }
    }

    @RequiredUIAccess
    private static int deleteTemplateKey(
        @Nonnull final PsiFile file,
        @Nonnull final Editor editor,
        @Nonnull final String key
    ) {
        Application.get().assertIsDispatchThread();

        final int currentOffset = editor.getCaretModel().getOffset();
        final int newOffset = currentOffset - key.length();
        Application.get().runWriteAction(
            () -> CommandProcessor.getInstance().runUndoTransparentAction(
                () -> {
                    Document document = editor.getDocument();
                    document.deleteString(newOffset, currentOffset);
                    editor.getCaretModel().moveToOffset(newOffset);
                    PsiDocumentManager.getInstance(file.getProject()).commitDocument(document);
                }
            )
        );
        return newOffset;
    }

    @RequiredReadAction
    private static Condition<PostfixTemplate> createIsApplicationTemplateFunction(
        @Nonnull final PostfixTemplateProvider provider,
        @Nonnull String key,
        @Nonnull PsiFile file,
        @Nonnull Editor editor,
        @Nonnull Disposable parentDisposable
    ) {
        if (file.getFileType().isBinary()) {
            return Conditions.alwaysFalse();
        }

        int currentOffset = editor.getCaretModel().getOffset();
        final int newOffset = currentOffset - key.length();
        CharSequence fileContent = editor.getDocument().getCharsSequence();
        StringBuilder fileContentWithoutKey = new StringBuilder();
        fileContentWithoutKey.append(fileContent.subSequence(0, newOffset));
        fileContentWithoutKey.append(fileContent.subSequence(currentOffset, fileContent.length()));
        PsiFile copyFile = copyFile(file, fileContentWithoutKey);
        Document copyDocument = copyFile.getViewProvider().getDocument();
        if (copyDocument == null) {
            return Conditions.alwaysFalse();
        }

        copyFile = provider.preCheck(copyFile, editor, newOffset);
        copyDocument = copyFile.getViewProvider().getDocument();
        if (copyDocument == null) {
            return Conditions.alwaysFalse();
        }

        // The copy document doesn't contain live template key.
        // Register offset translator to make getOriginalElement() work in the copy.
        Document fileDocument = file.getViewProvider().getDocument();
        if (fileDocument != null && fileDocument.getTextLength() < currentOffset) {
            LOG.error(
                "File document length (" + fileDocument.getTextLength() + ") is less than offset (" + currentOffset + ")",
                AttachmentFactoryUtil.createAttachment(fileDocument),
                AttachmentFactoryUtil.createAttachment(editor.getDocument())
            );
        }
        Document originalDocument = editor.getDocument();
        OffsetTranslatorImpl translator =
            new OffsetTranslatorImpl(originalDocument, file, copyDocument, newOffset, currentOffset, "");
        Disposer.register(parentDisposable, translator);

        final PsiElement context = CustomTemplateCallback.getContext(copyFile, positiveOffset(newOffset));
        final Document finalCopyDocument = copyDocument;
        return template -> template != null
            && isDumbEnough(template, context)
            && template.isEnabled(provider)
            && template.isApplicable(context, finalCopyDocument, newOffset);
    }

    private static boolean isDumbEnough(@Nonnull PostfixTemplate template, @Nonnull PsiElement context) {
        DumbService dumbService = DumbService.getInstance(context.getProject());
        return !dumbService.isDumb() || DumbService.isDumbAware(template);
    }

    @Nonnull
    @RequiredReadAction
    public static PsiFile copyFile(@Nonnull PsiFile file, @Nonnull StringBuilder fileContentWithoutKey) {
        PsiFileFactory psiFileFactory = PsiFileFactory.getInstance(file.getProject());
        FileType fileType = file.getFileType();
        Language language = LanguageUtil.getLanguageForPsi(file.getProject(), file.getVirtualFile(), fileType);
        PsiFile copy = language != null
            ? psiFileFactory.createFileFromText(file.getName(), language, fileContentWithoutKey, false, true)
            : psiFileFactory.createFileFromText(file.getName(), fileType, fileContentWithoutKey);

        if (copy instanceof PsiFileImpl) {
            ((PsiFileImpl)copy).setOriginalFile(TemplateLanguageUtil.getBaseFile(file));
        }

        VirtualFile vFile = copy.getVirtualFile();
        if (vFile != null) {
            UndoUtil.disableUndoFor(vFile);
        }
        return copy;
    }

    @RequiredReadAction
    public static boolean isApplicableTemplate(
        @Nonnull PostfixTemplateProvider provider,
        @Nonnull String key,
        @Nonnull PsiFile file,
        @Nonnull Editor editor
    ) {
        return findApplicableTemplate(provider, key, editor, file) != null;
    }

    @RequiredReadAction
    private static boolean isApplicableTemplate(
        @Nonnull PostfixTemplateProvider provider,
        @Nonnull String key,
        @Nonnull PsiFile file,
        @Nonnull Editor editor,
        @Nullable PostfixTemplate template
    ) {
        Disposable parentDisposable = Disposable.newDisposable();
        try {
            return createIsApplicationTemplateFunction(provider, key, file, editor, parentDisposable).value(template);
        }
        finally {
            Disposer.dispose(parentDisposable);
        }
    }

    @Nonnull
    private static Set<String> getKeys(@Nonnull PostfixTemplateProvider provider) {
        Set<String> result = new HashSet<>();
        for (PostfixTemplate template : PostfixTemplatesUtils.getAvailableTemplates(provider)) {
            result.add(template.getKey());
        }
        return result;
    }

    @Nullable
    @RequiredReadAction
    private static PostfixTemplate findApplicableTemplate(
        @Nonnull PostfixTemplateProvider provider,
        @Nullable String key,
        @Nonnull Editor editor,
        @Nonnull PsiFile file
    ) {
        for (PostfixTemplate template : PostfixTemplatesUtils.getAvailableTemplates(provider)) {
            if (template.getKey().equals(key) && isApplicableTemplate(provider, key, file, editor, template)) {
                return template;
            }
        }
        return null;
    }

    private static Language getLanguage(@Nonnull CustomTemplateCallback callback) {
        return PsiUtilCore.getLanguageAtOffset(callback.getFile(), callback.getOffset());
    }

    private static int positiveOffset(int offset) {
        return offset > 0 ? offset - 1 : offset;
    }
}
