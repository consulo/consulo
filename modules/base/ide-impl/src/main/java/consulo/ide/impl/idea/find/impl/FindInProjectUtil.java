// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package consulo.ide.impl.idea.find.impl;

import consulo.annotation.access.RequiredReadAction;
import consulo.application.AccessRule;
import consulo.application.Application;
import consulo.application.dumb.IndexNotReadyException;
import consulo.application.impl.internal.progress.ProgressWrapper;
import consulo.application.internal.TooManyUsagesStatus;
import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.ProgressManager;
import consulo.application.util.registry.Registry;
import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorKeys;
import consulo.content.base.SourcesOrderRootType;
import consulo.content.library.Library;
import consulo.content.scope.SearchScope;
import consulo.dataContext.DataContext;
import consulo.dataContext.DataManager;
import consulo.dataContext.DataSink;
import consulo.dataContext.TypeSafeDataProvider;
import consulo.document.Document;
import consulo.document.FileDocumentManager;
import consulo.document.util.TextRange;
import consulo.fileEditor.FileEditor;
import consulo.find.*;
import consulo.find.localize.FindLocalize;
import consulo.ide.impl.idea.find.FindProgressIndicator;
import consulo.ide.impl.idea.find.FindUtil;
import consulo.ide.impl.idea.find.findInProject.FindInProjectManager;
import consulo.ide.impl.idea.openapi.project.DumbServiceImpl;
import consulo.ide.impl.idea.openapi.vfs.VfsUtilCore;
import consulo.language.editor.LangDataKeys;
import consulo.language.psi.*;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.language.psi.scope.GlobalSearchScopesCore;
import consulo.language.psi.scope.LocalSearchScope;
import consulo.localize.LocalizeValue;
import consulo.module.Module;
import consulo.module.ModuleManager;
import consulo.module.content.ProjectFileIndex;
import consulo.module.content.layer.orderEntry.LibraryOrderEntry;
import consulo.module.content.layer.orderEntry.OrderEntry;
import consulo.navigation.ItemPresentation;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.project.content.scope.ProjectScopes;
import consulo.ui.ex.action.ActionManager;
import consulo.ui.ex.action.KeyboardShortcut;
import consulo.ui.ex.content.Content;
import consulo.ui.image.Image;
import consulo.usage.*;
import consulo.util.dataholder.Key;
import consulo.util.io.FileUtil;
import consulo.util.lang.PatternUtil;
import consulo.util.lang.StringUtil;
import consulo.util.lang.function.Predicates;
import consulo.virtualFileSystem.LocalFileProvider;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFileManager;
import consulo.virtualFileSystem.internal.VirtualFileManagerEx;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class FindInProjectUtil {
    private static final int USAGES_PER_READ_ACTION = 100;

    private FindInProjectUtil() {
    }

    public static void setDirectoryName(@Nonnull FindModel model, @Nonnull DataContext dataContext) {
        PsiElement psiElement = null;
        Project project = dataContext.getData(Project.KEY);

        Editor editor = dataContext.getData(Editor.KEY);
        if (project != null && editor == null && !DumbServiceImpl.getInstance(project).isDumb()) {
            try {
                psiElement = dataContext.getData(PsiElement.KEY);
            }
            catch (IndexNotReadyException ignore) {
            }
        }

        String directoryName = null;

        if (psiElement instanceof PsiDirectory directory) {
            directoryName = directory.getVirtualFile().getPresentableUrl();
        }

        if (directoryName == null && psiElement instanceof PsiDirectoryContainer directoryContainer) {
            PsiDirectory[] directories = directoryContainer.getDirectories();
            directoryName = directories.length == 1 ? directories[0].getVirtualFile().getPresentableUrl() : null;
        }

        if (directoryName == null) {
            VirtualFile virtualFile = dataContext.getData(VirtualFile.KEY);
            if (virtualFile != null && virtualFile.isDirectory()) {
                directoryName = virtualFile.getPresentableUrl();
            }
        }

        Module module = dataContext.getData(LangDataKeys.MODULE_CONTEXT);
        if (module != null) {
            model.setModuleName(module.getName());
            model.setDirectoryName(null);
            model.setCustomScope(false);
        }

        if (model.getModuleName() == null || editor == null) {
            if (directoryName != null) {
                model.setDirectoryName(directoryName);
                model.setCustomScope(false); // to select "Directory: " radio button
            }
        }

        if (directoryName == null) {
            for (FindInProjectExtension extension : FindInProjectExtension.EP_NAME.getExtensionList(Application.get())) {
                boolean success = extension.initModelFromContext(model, dataContext);
                if (success) {
                    break;
                }
            }
        }

        if (directoryName == null && module == null && project != null) {
        }

        // set project scope if we have no other settings
        model.setProjectScope(model.getDirectoryName() == null && model.getModuleName() == null && !model.isCustomScope());
    }

    /**
     * @deprecated to remove in IDEA 2018
     */
    //@ApiStatus.ScheduledForRemoval(inVersion = "2018")
    @Deprecated
    @Nullable
    @RequiredReadAction
    public static PsiDirectory getPsiDirectory(@Nonnull FindModel findModel, @Nonnull Project project) {
        VirtualFile directory = getDirectory(findModel);
        return directory == null ? null : PsiManager.getInstance(project).findDirectory(directory);
    }

    @Nullable
    public static VirtualFile getDirectory(@Nonnull FindModel findModel) {
        String directoryName = findModel.getDirectoryName();
        if (findModel.isProjectScope() || StringUtil.isEmptyOrSpaces(directoryName)) {
            return null;
        }

        String path = FileUtil.toSystemIndependentName(directoryName);
        VirtualFile virtualFile = LocalFileSystem.getInstance().findFileByPath(path);
        if (virtualFile == null || !virtualFile.isDirectory()) {
            virtualFile = null;
            for (LocalFileProvider provider : ((VirtualFileManagerEx)VirtualFileManager.getInstance()).getLocalFileProviders()) {
                VirtualFile file = provider.findLocalVirtualFileByPath(path);
                if (file != null && file.isDirectory()) {
                    if (file.getChildren().length > 0) {
                        virtualFile = file;
                        break;
                    }
                    if (virtualFile == null) {
                        virtualFile = file;
                    }
                }
            }
        }
        return virtualFile;
    }

    /* filter can have form "*.js, !*_min.js", latter means except matched by *_min.js */
    @Nonnull
    public static Predicate<CharSequence> createFileMaskCondition(@Nullable String filter) throws PatternSyntaxException {
        if (filter == null) {
            return Predicates.alwaysTrue();
        }

        String pattern = "";
        String negativePattern = "";
        List<String> masks = StringUtil.split(filter, ",");

        for (String mask : masks) {
            mask = mask.trim();
            if (StringUtil.startsWith(mask, "!")) {
                negativePattern += (negativePattern.isEmpty() ? "" : "|") + "(" + PatternUtil.convertToRegex(mask.substring(1)) + ")";
            }
            else {
                pattern += (pattern.isEmpty() ? "" : "|") + "(" + PatternUtil.convertToRegex(mask) + ")";
            }
        }

        if (pattern.isEmpty()) {
            pattern = PatternUtil.convertToRegex("*");
        }
        String finalPattern = pattern;
        String finalNegativePattern = negativePattern;

        return new Predicate<>() {
            Pattern regExp = Pattern.compile(finalPattern, Pattern.CASE_INSENSITIVE);
            Pattern negativeRegExp =
                StringUtil.isEmpty(finalNegativePattern) ? null : Pattern.compile(finalNegativePattern, Pattern.CASE_INSENSITIVE);

            @Override
            public boolean test(CharSequence input) {
                return regExp.matcher(input).matches() && (negativeRegExp == null || !negativeRegExp.matcher(input).matches());
            }
        };
    }

    /**
     * @deprecated Use {@link #findUsages(FindModel, Project, Predicate, FindUsagesProcessPresentation)} instead. To remove in IDEA 16
     */
    @Deprecated
    //@ApiStatus.ScheduledForRemoval(inVersion = "2016")
    public static void findUsages(
        @Nonnull FindModel findModel,
        @Nullable PsiDirectory psiDirectory,
        @Nonnull Project project,
        @Nonnull Predicate<? super UsageInfo> consumer,
        @Nonnull FindUsagesProcessPresentation processPresentation
    ) {
        findUsages(findModel, project, consumer, processPresentation);
    }

    public static void findUsages(
        @Nonnull FindModel findModel,
        @Nonnull Project project,
        @Nonnull Predicate<? super UsageInfo> consumer,
        @Nonnull FindUsagesProcessPresentation processPresentation
    ) {
        findUsages(findModel, project, processPresentation, Collections.emptySet(), consumer);
    }

    public static void findUsages(
        @Nonnull FindModel findModel,
        @Nonnull Project project,
        @Nonnull FindUsagesProcessPresentation processPresentation,
        @Nonnull Set<? extends VirtualFile> filesToStart,
        @Nonnull Predicate<? super UsageInfo> consumer
    ) {
        new FindInProjectTask(findModel, project, filesToStart).findUsages(processPresentation, consumer);
    }

    static boolean processUsagesInFile(
        @Nonnull PsiFile psiFile,
        @Nonnull VirtualFile virtualFile,
        @Nonnull FindModel findModel,
        @Nonnull Predicate<? super UsageInfo> consumer
    ) {
        if (findModel.getStringToFind().isEmpty()) {
            return AccessRule.read(() -> consumer.test(new UsageInfo(psiFile)));
        }
        if (virtualFile.getFileType().isBinary()) {
            return true; // do not decompile .class files
        }
        Document document =
            AccessRule.read(() -> virtualFile.isValid() ? FileDocumentManager.getInstance().getDocument(virtualFile) : null);
        if (document == null) {
            return true;
        }
        int[] offsetRef = {0};
        ProgressIndicator current = ProgressManager.getInstance().getProgressIndicator();
        if (current == null) {
            throw new IllegalStateException("must find usages under progress");
        }
        ProgressIndicator indicator = ProgressWrapper.unwrapAll(current);
        TooManyUsagesStatus tooManyUsagesStatus = TooManyUsagesStatus.getFrom(indicator);
        int before;
        do {
            tooManyUsagesStatus.pauseProcessingIfTooManyUsages(); // wait for user out of read action
            before = offsetRef[0];
            boolean success = AccessRule.read(
                () -> !psiFile.isValid() || processSomeOccurrencesInFile(document, findModel, psiFile, offsetRef, consumer)
            );
            if (!success) {
                return false;
            }
        }
        while (offsetRef[0] != before);
        return true;
    }

    private static boolean processSomeOccurrencesInFile(
        @Nonnull Document document,
        @Nonnull FindModel findModel,
        @Nonnull PsiFile psiFile,
        int[] offsetRef,
        @Nonnull Predicate<? super UsageInfo> consumer
    ) {
        CharSequence text = document.getCharsSequence();
        int textLength = document.getTextLength();
        int offset = offsetRef[0];

        Project project = psiFile.getProject();

        FindManager findManager = FindManager.getInstance(project);
        int count = 0;
        while (offset < textLength) {
            FindResult result = findManager.findString(text, offset, findModel, psiFile.getVirtualFile());
            if (!result.isStringFound()) {
                break;
            }

            int prevOffset = offset;
            offset = result.getEndOffset();
            if (prevOffset == offset || offset == result.getStartOffset()) {
                // for regular expr the size of the match could be zero -> could be infinite loop in finding usages!
                ++offset;
            }

            SearchScope customScope = findModel.getCustomScope();
            if (customScope instanceof LocalSearchScope localSearchScope) {
                TextRange range = new TextRange(result.getStartOffset(), result.getEndOffset());
                if (!localSearchScope.containsRange(psiFile, range)) {
                    continue;
                }
            }
            UsageInfo info = new FindResultUsageInfo(findManager, psiFile, prevOffset, findModel, result);
            if (!consumer.test(info)) {
                return false;
            }
            count++;

            if (count >= USAGES_PER_READ_ACTION) {
                break;
            }
        }
        offsetRef[0] = offset;
        return true;
    }

    @Nonnull
    private static LocalizeValue getTitleForScope(@Nonnull FindModel findModel) {
        LocalizeValue scopeName;
        if (findModel.isProjectScope()) {
            scopeName = FindLocalize.findScopeProjectTitle();
        }
        else if (findModel.getModuleName() != null) {
            scopeName = FindLocalize.findScopeModuleTitle(findModel.getModuleName());
        }
        else if (findModel.getCustomScopeName() != null) {
            scopeName = LocalizeValue.localizeTODO(findModel.getCustomScopeName());
        }
        else {
            scopeName = FindLocalize.findScopeDirectoryTitle(findModel.getDirectoryName());
        }

        LocalizeValue result = scopeName;
        if (findModel.getFileFilter() != null) {
            result = LocalizeValue.localizeTODO(result + " " + FindLocalize.findScopeFilesWithMask(findModel.getFileFilter()));
        }

        return result;
    }

    @Nonnull
    public static UsageViewPresentation setupViewPresentation(@Nonnull FindModel findModel) {
        return setupViewPresentation(FindSettings.getInstance().isShowResultsInSeparateView(), findModel);
    }

    @Nonnull
    public static UsageViewPresentation setupViewPresentation(boolean toOpenInNewTab, @Nonnull FindModel findModel) {
        UsageViewPresentation presentation = new UsageViewPresentation();
        setupViewPresentation(presentation, toOpenInNewTab, findModel);
        return presentation;
    }

    public static void setupViewPresentation(UsageViewPresentation presentation, @Nonnull FindModel findModel) {
        setupViewPresentation(presentation, FindSettings.getInstance().isShowResultsInSeparateView(), findModel);
    }

    public static void setupViewPresentation(UsageViewPresentation presentation, boolean toOpenInNewTab, @Nonnull FindModel findModel) {
        LocalizeValue scope = getTitleForScope(findModel);
        scope = scope.map((localizeManager, value) -> Character.toLowerCase(value.charAt(0)) + value.substring(1));
        String stringToFind = findModel.getStringToFind();
        presentation.setScopeText(scope);
        if (stringToFind.isEmpty()) {
            presentation.setTabText("Files");
            presentation.setToolwindowTitle("Files in " + scope);
            presentation.setUsagesString("files");
        }
        else {
            FindSearchContext searchContext = findModel.getSearchContext();
            LocalizeValue contextText = LocalizeValue.empty();
            if (searchContext != FindSearchContext.ANY) {
                contextText = FindLocalize.findContextPresentationScopeLabel(searchContext.getName());
            }
            presentation.setTabText(FindLocalize.findUsageViewTabText(stringToFind, contextText));
            presentation.setToolwindowTitle(FindLocalize.findUsageViewToolwindowTitle(stringToFind, scope, contextText));
            presentation.setUsagesString(FindLocalize.findUsageViewUsagesText(stringToFind));
            presentation.setUsagesWord(FindLocalize.occurrence());
            presentation.setCodeUsagesString(FindLocalize.foundOccurrences());
            presentation.setContextText(contextText);
        }
        presentation.setOpenInNewTab(toOpenInNewTab);
        presentation.setCodeUsages(false);
        presentation.setUsageTypeFilteringAvailable(true);
        if (findModel.isReplaceState() && findModel.isRegularExpressions()) {
            presentation.setSearchPattern(findModel.compileRegExp());
            try {
                presentation.setReplacePattern(Pattern.compile(findModel.getStringToReplace()));
            }
            catch (Exception e) {
                presentation.setReplacePattern(null);
            }
        }
        else {
            presentation.setSearchPattern(null);
            presentation.setReplacePattern(null);
        }
        presentation.setReplaceMode(findModel.isReplaceState());
    }

    @Nonnull
    public static FindUsagesProcessPresentation setupProcessPresentation(
        @Nonnull Project project,
        @Nonnull UsageViewPresentation presentation
    ) {
        return setupProcessPresentation(project, !FindSettings.getInstance().isSkipResultsWithOneUsage(), presentation);
    }

    @Nonnull
    public static FindUsagesProcessPresentation setupProcessPresentation(
        @Nonnull Project project,
        boolean showPanelIfOnlyOneUsage,
        @Nonnull UsageViewPresentation presentation
    ) {
        FindUsagesProcessPresentation processPresentation = new FindUsagesProcessPresentation(presentation);
        processPresentation.setShowNotFoundMessage(true);
        processPresentation.setShowPanelIfOnlyOneUsage(showPanelIfOnlyOneUsage);
        processPresentation.setProgressIndicatorFactory(() -> new FindProgressIndicator(project, presentation.getScopeText()));
        return processPresentation;
    }

    @RequiredReadAction
    private static List<PsiElement> getTopLevelRegExpChars(String regExpText, Project project) {
        @SuppressWarnings("deprecation") PsiFile file = PsiFileFactory.getInstance(project).createFileFromText("A.regexp", regExpText);
        List<PsiElement> result = null;
        PsiElement[] children = file.getChildren();

        for (PsiElement child : children) {
            PsiElement[] grandChildren = child.getChildren();
            if (grandChildren.length != 1) {
                return Collections.emptyList(); // a | b, more than one branch, can not predict in current way
            }

            for (PsiElement grandGrandChild : grandChildren[0].getChildren()) {
                if (result == null) {
                    result = new ArrayList<>();
                }
                result.add(grandGrandChild);
            }
        }
        return result != null ? result : Collections.emptyList();
    }

    @Nonnull
    public static String buildStringToFindForIndicesFromRegExp(@Nonnull String stringToFind, @Nonnull Project project) {
        if (!Registry.is("idea.regexp.search.uses.indices")) {
            return "";
        }

        return AccessRule.read(() -> {
            List<PsiElement> topLevelRegExpChars = getTopLevelRegExpChars("a", project);
            if (topLevelRegExpChars.size() != 1) {
                return "";
            }

            // leave only top level regExpChars
            return StringUtil.join(
                getTopLevelRegExpChars(stringToFind, project),
                new Function<>() {
                    Class regExpCharPsiClass = topLevelRegExpChars.get(0).getClass();

                    @Override
                    public String apply(PsiElement element) {
                        if (regExpCharPsiClass.isInstance(element)) {
                            String text = element.getText();
                            if (!text.startsWith("\\")) {
                                return text;
                            }
                        }
                        return " ";
                    }
                },
                ""
            );
        });
  }

    public static void initStringToFindFromDataContext(FindModel findModel, @Nonnull DataContext dataContext) {
        Editor editor = dataContext.getData(Editor.KEY);
        FindUtil.initStringToFindWithSelection(findModel, editor);
        if (editor == null || !editor.getSelectionModel().hasSelection()) {
            FindUtil.useFindStringFromFindInFileModel(findModel, dataContext.getData(EditorKeys.EDITOR_EVEN_IF_INACTIVE));
        }
    }

    public static class StringUsageTarget implements ConfigurableUsageTarget, ItemPresentation, TypeSafeDataProvider {
        @Nonnull
        protected final Project myProject;
        @Nonnull
        protected final FindModel myFindModel;

        public StringUsageTarget(@Nonnull Project project, @Nonnull FindModel findModel) {
            myProject = project;
            myFindModel = findModel.clone();
        }

        @Override
        @Nonnull
        public String getPresentableText() {
            UsageViewPresentation presentation = setupViewPresentation(false, myFindModel);
            return presentation.getToolwindowTitle();
        }

        @Nonnull
        @Override
        public String getLongDescriptiveName() {
            return getPresentableText();
        }

        @Override
        public String getLocationString() {
            return myFindModel + "!!";
        }

        @Override
        public Image getIcon() {
            return PlatformIconGroup.actionsFind();
        }

        @Override
        public void findUsages() {
            FindInProjectManager.getInstance(myProject).startFindInProject(myFindModel);
        }

        @Override
        public void findUsagesInEditor(@Nonnull FileEditor editor) {
        }

        @Override
        public void highlightUsages(@Nonnull PsiFile file, @Nonnull Editor editor, boolean clearHighlights) {
        }

        @Override
        public boolean isValid() {
            return true;
        }

        @Override
        public boolean isReadOnly() {
            return true;
        }

        @Override
        @Nullable
        public VirtualFile[] getFiles() {
            return null;
        }

        @Override
        public void update() {
        }

        @Override
        public String getName() {
            return myFindModel.getStringToFind().isEmpty() ? myFindModel.getFileFilter() : myFindModel.getStringToFind();
        }

        @Override
        public ItemPresentation getPresentation() {
            return this;
        }

        @Override
        public void navigate(boolean requestFocus) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean canNavigate() {
            return false;
        }

        @Override
        public boolean canNavigateToSource() {
            return false;
        }

        @Override
        public void showSettings() {
            Content selectedContent = UsageViewContentManager.getInstance(myProject).getSelectedContent(true);
            JComponent component = selectedContent == null ? null : selectedContent.getComponent();
            FindInProjectManager findInProjectManager = FindInProjectManager.getInstance(myProject);
            findInProjectManager.findInProject(DataManager.getInstance().getDataContext(component), myFindModel);
        }

        @Override
        public KeyboardShortcut getShortcut() {
            return ActionManager.getInstance().getKeyboardShortcut("FindInPath");
        }

        @Override
        @RequiredReadAction
        public void calcData(@Nonnull Key key, @Nonnull DataSink sink) {
            if (UsageView.USAGE_SCOPE == key) {
                SearchScope scope = getScopeFromModel(myProject, myFindModel);
                sink.put(UsageView.USAGE_SCOPE, scope);
            }
        }
    }

    private static void addSourceDirectoriesFromLibraries(
        @Nonnull Project project,
        @Nonnull VirtualFile directory,
        @Nonnull Collection<? super VirtualFile> outSourceRoots
    ) {
        ProjectFileIndex index = ProjectFileIndex.SERVICE.getInstance(project);
        // if we already are in the sources, search just in this directory only
        if (!index.isInLibraryClasses(directory)) {
            return;
        }
        VirtualFile classRoot = index.getClassRootForFile(directory);
        if (classRoot == null) {
            return;
        }
        String relativePath = VfsUtilCore.getRelativePath(directory, classRoot);
        if (relativePath == null) {
            return;
        }

        Collection<VirtualFile> otherSourceRoots = new HashSet<>();

        // if we are in the library sources, return (to search in this directory only)
        // otherwise, if we outside sources or in a jar directory, add directories from other source roots
        searchForOtherSourceDirs:
        for (OrderEntry entry : index.getOrderEntriesForFile(directory)) {
            if (entry instanceof LibraryOrderEntry libraryOrderEntry) {
                Library library = libraryOrderEntry.getLibrary();
                if (library == null) {
                    continue;
                }
                // note: getUrls() returns jar directories too
                String[] sourceUrls = library.getUrls(SourcesOrderRootType.getInstance());
                for (String sourceUrl : sourceUrls) {
                    if (VfsUtilCore.isEqualOrAncestor(sourceUrl, directory.getUrl())) {
                        // already in this library sources, no need to look for another source root
                        otherSourceRoots.clear();
                        break searchForOtherSourceDirs;
                    }
                    // otherwise we may be inside the jar file in a library which is configured as a jar directory
                    // in which case we have no way to know whether this is a source jar or classes jar - so try to locate the source jar
                }
            }
            for (VirtualFile sourceRoot : entry.getFiles(SourcesOrderRootType.getInstance())) {
                VirtualFile sourceFile = sourceRoot.findFileByRelativePath(relativePath);
                if (sourceFile != null) {
                    otherSourceRoots.add(sourceFile);
                }
            }
        }
        outSourceRoots.addAll(otherSourceRoots);
    }

    @Nonnull
    @RequiredReadAction
    public static SearchScope getScopeFromModel(@Nonnull Project project, @Nonnull FindModel findModel) {
        SearchScope customScope = findModel.isCustomScope() ? findModel.getCustomScope() : null;
        VirtualFile directory = getDirectory(findModel);
        Module module =
            findModel.getModuleName() == null ? null : ModuleManager.getInstance(project).findModuleByName(findModel.getModuleName());
        // do not alter custom scope in any way, learn from history
        return customScope != null ? customScope :
            // we don't have to check for myProjectFileIndex.isExcluded(file) here like FindInProjectTask.collectFilesInScope() does
            // because all found usages are guaranteed to be not in excluded dir
            directory != null
                ? forDirectory(project, findModel.isWithSubdirectories(), directory)
                : module != null
                ? GlobalSearchScope.moduleContentScope(module)
                : findModel.isProjectScope()
                ? ProjectScopes.getContentScope(project)
                : GlobalSearchScope.allScope(project);
    }

    @Nonnull
    private static GlobalSearchScope forDirectory(@Nonnull Project project, boolean withSubdirectories, @Nonnull VirtualFile directory) {
        Set<VirtualFile> result = new LinkedHashSet<>();
        result.add(directory);
        addSourceDirectoriesFromLibraries(project, directory, result);
        VirtualFile[] array = result.toArray(VirtualFile.EMPTY_ARRAY);
        return GlobalSearchScopesCore.directoriesScope(project, withSubdirectories, array);
    }

    public static void initFileFilter(@Nonnull JComboBox<? super String> fileFilter, @Nonnull JCheckBox useFileFilter) {
        fileFilter.setEditable(true);
        String[] fileMasks = FindSettings.getInstance().getRecentFileMasks();
        for (int i = fileMasks.length - 1; i >= 0; i--) {
            fileFilter.addItem(fileMasks[i]);
        }
        fileFilter.setEnabled(false);

        useFileFilter.addActionListener(__ -> {
            if (useFileFilter.isSelected()) {
                fileFilter.setEnabled(true);
                fileFilter.getEditor().selectAll();
                fileFilter.getEditor().getEditorComponent().requestFocusInWindow();
            }
            else {
                fileFilter.setEnabled(false);
            }
        });
    }
}
