// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.ide.scratch;

import consulo.annotation.access.RequiredReadAction;
import consulo.application.util.NotNullLazyValue;
import consulo.application.util.registry.Registry;
import consulo.codeEditor.Caret;
import consulo.codeEditor.Editor;
import consulo.dataContext.DataContext;
import consulo.externalService.statistic.FeatureUsageTracker;
import consulo.ide.IdeView;
import consulo.ide.impl.idea.ide.actions.NewActionGroup;
import consulo.language.Language;
import consulo.language.editor.PlatformDataKeys;
import consulo.language.file.LanguageFileType;
import consulo.language.inject.InjectedLanguageManager;
import consulo.language.plain.PlainTextLanguage;
import consulo.language.psi.*;
import consulo.language.scratch.RootType;
import consulo.language.scratch.ScratchFileService;
import consulo.language.util.LanguageUtil;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.platform.base.localize.ActionLocalize;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionPlaces;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.ui.ex.action.Presentation;
import consulo.ui.image.Image;
import consulo.ui.image.ImageEffects;
import consulo.util.collection.ArrayUtil;
import consulo.util.collection.JBIterable;
import consulo.util.io.FileUtil;
import consulo.util.io.PathUtil;
import consulo.util.lang.ObjectUtil;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.util.PerFileMappings;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import static consulo.util.lang.function.Predicates.not;
import static consulo.util.lang.function.Predicates.notNull;

/**
 * @author ignatov
 */
public class ScratchFileActions {
    private static int ourCurrentBuffer = 0;

    private static int nextBufferIndex() {
        ourCurrentBuffer = (ourCurrentBuffer % Registry.intValue("ide.scratch.buffers")) + 1;
        return ourCurrentBuffer;
    }

    public static class NewFileAction extends DumbAwareAction {
        private static final Image ICON = ImageEffects.layered(PlatformIconGroup.filetypesText(), PlatformIconGroup.actionsScratch());

        private static final String ACTION_ID = "NewScratchFile";

        private final NotNullLazyValue<LocalizeValue> myActionText = NotNullLazyValue.createValue(
            () -> NewActionGroup.isActionInNewPopupMenu(this)
                ? ActionLocalize.actionNewscratchfileText()
                : ActionLocalize.actionNewscratchfileTextWithNew()
        );

        public NewFileAction() {
            getTemplatePresentation().setIcon(ICON);
        }

        @Override
        @RequiredUIAccess
        public void update(@Nonnull AnActionEvent e) {
            getTemplatePresentation().setTextValue(myActionText.getValue());

            Project project = e.getData(Project.KEY);
            String place = e.getPlace();
            boolean enabled = project != null && (e.isFromActionToolbar() || ActionPlaces.isMainMenuOrActionSearch(place)
                || ActionPlaces.isPopupPlace(place) && e.getData(IdeView.KEY) != null);

            e.getPresentation().setEnabledAndVisible(enabled);
            updatePresentationTextAndIcon(e, e.getPresentation());
        }

        @Override
        @RequiredReadAction
        public void actionPerformed(@Nonnull AnActionEvent e) {
            Project project = e.getData(Project.KEY);
            if (project == null) {
                return;
            }

            ScratchFileCreationHelper.Context context = createContext(e, project);
            Consumer<Language> consumer = l -> {
                context.language = l;
                ScratchFileCreationHelper.forLanguage(context.language).prepareText(project, context, DataContext.EMPTY_CONTEXT);
                doCreateNewScratch(project, context);
            };
            if (context.language != null) {
                consumer.accept(context.language);
            }
            else {
                LRUPopupBuilder.forFileLanguages(project, ActionLocalize.actionNewscratchfileTextWithNew().get(), null, consumer)
                    .showCenteredInCurrentWindow(project);
            }
        }

        private void updatePresentationTextAndIcon(@Nonnull AnActionEvent e, @Nonnull Presentation presentation) {
            presentation.setTextValue(myActionText.getValue());
            presentation.setIcon(ICON);
            if (ActionPlaces.MAIN_MENU.equals(e.getPlace()) && !NewActionGroup.isActionInNewPopupMenu(this)) {
                presentation.setIcon(null);
            }
        }
    }

    public static class NewBufferAction extends DumbAwareAction {

        @Override
        @RequiredUIAccess
        public void update(@Nonnull AnActionEvent e) {
            boolean enabled = e.getData(Project.KEY) != null && Registry.intValue("ide.scratch.buffers") > 0;
            e.getPresentation().setEnabledAndVisible(enabled);
        }

        @Override
        @RequiredUIAccess
        public void actionPerformed(@Nonnull AnActionEvent e) {
            Project project = e.getData(Project.KEY);
            if (project == null) {
                return;
            }
            ScratchFileCreationHelper.Context context = createContext(e, project);
            context.filePrefix = "buffer";
            context.createOption = ScratchFileService.Option.create_if_missing;
            context.fileCounter = ScratchFileActions::nextBufferIndex;
            if (context.language == null) {
                context.language = PlainTextLanguage.INSTANCE;
            }
            doCreateNewScratch(project, context);
        }
    }

    @Nonnull
    @RequiredReadAction
    static ScratchFileCreationHelper.Context createContext(@Nonnull AnActionEvent e, @Nonnull Project project) {
        PsiFile file = e.getData(PsiFile.KEY);
        Editor editor = e.getData(Editor.KEY);
        if (file == null && editor != null) {
            // see data provider in consulo.desktop.awt.diff.impl.internal.TextEditorHolder
            file = PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument());
        }

        ScratchFileCreationHelper.Context context = new ScratchFileCreationHelper.Context();
        context.text = StringUtil.notNullize(getSelectionText(editor));
        if (!context.text.isEmpty()) {
            context.language = getLanguageFromCaret(project, editor, file);
            checkLanguageAndTryToFixText(project, context, e.getDataContext());
        }
        else {
            context.text = StringUtil.notNullize(e.getData(PlatformDataKeys.PREDEFINED_TEXT));
        }
        context.ideView = e.getData(IdeView.KEY);
        return context;
    }

    @RequiredUIAccess
    static PsiFile doCreateNewScratch(@Nonnull Project project, @Nonnull ScratchFileCreationHelper.Context context) {
        FeatureUsageTracker.getInstance().triggerFeatureUsed("scratch");
        Language language = Objects.requireNonNull(context.language);
        if (context.fileExtension == null) {
            LanguageFileType fileType = language.getAssociatedFileType();
            context.fileExtension = fileType == null ? "" : fileType.getDefaultExtension();
        }
        ScratchFileCreationHelper.forLanguage(language).beforeCreate(project, context);

        VirtualFile dir = context.ideView != null
            ? PsiUtilCore.getVirtualFile(ArrayUtil.getFirstElement(context.ideView.getDirectories())) : null;
        RootType rootType = dir == null ? null : ScratchFileService.findRootType(dir);
        String relativePath = rootType != ScratchRootType.getInstance() ? ""
            : FileUtil.getRelativePath(ScratchFileService.getInstance().getRootPath(rootType), dir.getPath(), '/');

        String fileName = (StringUtil.isEmpty(relativePath) ? "" : relativePath + "/") +
            PathUtil.makeFileName(ObjectUtil.notNull(context.filePrefix, "scratch") +
                (context.fileCounter != null ? context.fileCounter.get() : ""), context.fileExtension);
        VirtualFile file = ScratchRootType.getInstance().createScratchFile(project, fileName, language, context.text, context.createOption);
        if (file == null) {
            return null;
        }

        PsiNavigationSupport.getInstance().createNavigatable(project, file, context.caretOffset).navigate(true);
        PsiFile psiFile = PsiManager.getInstance(project).findFile(file);
        if (context.ideView != null && psiFile != null) {
            context.ideView.selectElement(psiFile);
        }
        return psiFile;
    }

    private static void checkLanguageAndTryToFixText(
        @Nonnull Project project,
        @Nonnull ScratchFileCreationHelper.Context context,
        @Nonnull DataContext dataContext
    ) {
        if (context.language == null) {
            return;
        }
        ScratchFileCreationHelper handler = ScratchFileCreationHelper.forLanguage(context.language);
        if (handler.prepareText(project, context, dataContext)) {
            return;
        }

        PsiFile psiFile = ScratchFileCreationHelper.parseHeader(project, context.language, context.text);
        PsiErrorElement firstError = SyntaxTraverser.psiTraverser(psiFile).traverse().filter(PsiErrorElement.class).first();
        // heuristics: first error must not be right under the file PSI
        // otherwise let the user choose the language manually
        if (firstError != null && firstError.getParent() == psiFile) {
            context.language = null;
        }
    }

    @Nullable
    static String getSelectionText(@Nullable Editor editor) {
        if (editor == null) {
            return null;
        }
        return editor.getSelectionModel().getSelectedText();
    }

    @Nullable
    @RequiredReadAction
    static Language getLanguageFromCaret(@Nonnull Project project, @Nullable Editor editor, @Nullable PsiFile psiFile) {
        if (editor == null || psiFile == null) {
            return null;
        }
        Caret caret = editor.getCaretModel().getPrimaryCaret();
        int offset = caret.getOffset();
        PsiElement element = InjectedLanguageManager.getInstance(project).findInjectedElementAt(psiFile, offset);
        PsiFile file = element != null ? element.getContainingFile() : psiFile;
        Language language = file.getLanguage();
        return language;
    }

    public static class LanguageAction extends DumbAwareAction {
        @Override
        @RequiredUIAccess
        public void update(@Nonnull AnActionEvent e) {
            Project project = e.getData(Project.KEY);
            JBIterable<VirtualFile> files = JBIterable.of(e.getData(VirtualFile.KEY_OF_ARRAY));
            if (project == null || files.isEmpty()) {
                e.getPresentation().setEnabledAndVisible(false);
                return;
            }

            Predicate<VirtualFile> isScratch = fileFilter(project);
            if (!files.filter(not(isScratch)).isEmpty()) {
                e.getPresentation().setEnabledAndVisible(false);
                return;
            }
            Set<Language> languages = files.filter(isScratch).map(fileLanguage(project)).filter(notNull()).addAllTo(new LinkedHashSet<>());
            LocalizeValue langName = languages.size() == 1 ? languages.iterator().next().getDisplayName() : LocalizeValue.localizeTODO(languages.size() + " different");
            e.getPresentation().setText(String.format("Change %s (%s)...", getLanguageTerm(), langName));
            e.getPresentation().setEnabledAndVisible(true);
        }

        @Nonnull
        protected String getLanguageTerm() {
            return "Language";
        }

        @Override
        @RequiredUIAccess
        public void actionPerformed(@Nonnull AnActionEvent e) {
            Project project = e.getData(Project.KEY);
            JBIterable<VirtualFile> files = JBIterable.of(e.getData(VirtualFile.KEY_OF_ARRAY)).filter(fileFilter(project));
            if (project == null || files.isEmpty()) {
                return;
            }
            actionPerformedImpl(e, project, "Change " + getLanguageTerm(), files);
        }

        @Nonnull
        protected Predicate<VirtualFile> fileFilter(Project project) {
            return file -> !file.isDirectory() && ScratchRootType.getInstance().containsFile(file);
        }

        @Nonnull
        protected Function<VirtualFile, Language> fileLanguage(@Nonnull Project project) {
            return new Function<>() {
                ScratchFileService fileService = ScratchFileService.getInstance();

                @Override
                public Language apply(VirtualFile file) {
                    Language lang = fileService.getScratchesMapping().getMapping(file);
                    return lang != null ? lang : LanguageUtil.getLanguageForPsi(project, file);
                }
            };
        }

        protected void actionPerformedImpl(
            @Nonnull AnActionEvent e,
            @Nonnull Project project,
            @Nonnull String title,
            @Nonnull JBIterable<? extends VirtualFile> files
        ) {
            ScratchFileService fileService = ScratchFileService.getInstance();
            PerFileMappings<Language> mapping = fileService.getScratchesMapping();
            LRUPopupBuilder.forFileLanguages(project, title, files, mapping).showInBestPositionFor(e.getDataContext());
        }
    }
}