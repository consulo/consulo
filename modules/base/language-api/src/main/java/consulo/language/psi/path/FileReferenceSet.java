/*
 * Copyright 2000-2016 JetBrains s.r.o.
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

package consulo.language.psi.path;

import consulo.annotation.access.RequiredReadAction;
import consulo.document.util.TextRange;
import consulo.language.inject.InjectedLanguageManager;
import consulo.language.psi.*;
import consulo.language.util.ModuleUtilCore;
import consulo.logging.Logger;
import consulo.module.Module;
import consulo.project.Project;
import consulo.util.collection.ContainerUtil;
import consulo.util.lang.StringUtil;
import consulo.util.lang.function.Predicates;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.fileType.FileType;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * @author Maxim.Mossienko
 */
public class FileReferenceSet {
    private static final Logger LOG = Logger.getInstance(FileReferenceSet.class);

    private static final FileType[] EMPTY_FILE_TYPES = {};

    public static final CustomizableReferenceProvider.CustomizationKey<Function<PsiFile, Collection<PsiFileSystemItem>>>
        DEFAULT_PATH_EVALUATOR_OPTION =
        new CustomizableReferenceProvider.CustomizationKey<>(PsiBundle.message("default.path.evaluator.option"));
    public static final Function<PsiFile, Collection<PsiFileSystemItem>> ABSOLUTE_TOP_LEVEL =
        FileReferenceSet::getAbsoluteTopLevelDirLocations;

    public static final Predicate<PsiFileSystemItem> FILE_FILTER = item -> item instanceof PsiFile;

    public static final Predicate<PsiFileSystemItem> DIRECTORY_FILTER = item -> item instanceof PsiDirectory;

    protected FileReference[] myReferences;
    private PsiElement myElement;
    private final int myStartInElement;
    private final boolean myCaseSensitive;
    private final String myPathStringNonTrimmed;
    private final String myPathString;
    private Collection<PsiFileSystemItem> myDefaultContexts;
    private final boolean myEndingSlashNotAllowed;
    private boolean myEmptyPathAllowed;
    @Nullable
    private Map<CustomizableReferenceProvider.CustomizationKey, Object> myOptions;
    @Nullable
    private FileType[] mySuitableFileTypes;

    public FileReferenceSet(
        String str,
        @Nonnull PsiElement element,
        int startInElement,
        PsiReferenceProvider provider,
        boolean caseSensitive,
        boolean endingSlashNotAllowed,
        @Nullable FileType[] suitableFileTypes
    ) {
        this(str, element, startInElement, provider, caseSensitive, endingSlashNotAllowed, suitableFileTypes, true);
    }

    public FileReferenceSet(
        String str,
        @Nonnull PsiElement element,
        int startInElement,
        PsiReferenceProvider provider,
        boolean caseSensitive,
        boolean endingSlashNotAllowed,
        @Nullable FileType[] suitableFileTypes,
        boolean init
    ) {
        myElement = element;
        myStartInElement = startInElement;
        myCaseSensitive = caseSensitive;
        myPathStringNonTrimmed = str;
        myPathString = str.trim();
        myEndingSlashNotAllowed = endingSlashNotAllowed;
        myEmptyPathAllowed = !endingSlashNotAllowed;
        myOptions = provider instanceof CustomizableReferenceProvider customizableRefProvider ? customizableRefProvider.getOptions() : null;
        mySuitableFileTypes = suitableFileTypes;

        if (init) {
            reparse();
        }
    }

    protected String getNewAbsolutePath(PsiFileSystemItem root, String relativePath) {
        return absoluteUrlNeedsStartSlash() ? "/" + relativePath : relativePath;
    }

    public String getSeparatorString() {
        return "/";
    }

    protected int findSeparatorLength(@Nonnull CharSequence sequence, int atOffset) {
        return StringUtil.startsWith(sequence, atOffset, getSeparatorString()) ? getSeparatorString().length() : 0;
    }

    protected int findSeparatorOffset(@Nonnull CharSequence sequence, int startingFrom) {
        return StringUtil.indexOf(sequence, getSeparatorString(), startingFrom);
    }

    /**
     * This should be removed. Please use {@link FileReference#getContexts()} instead.
     */
    @Deprecated
    protected Collection<PsiFileSystemItem> getExtraContexts() {
        return Collections.emptyList();
    }

    @RequiredReadAction
    public static FileReferenceSet createSet(
        @Nonnull PsiElement element,
        boolean soft,
        boolean endingSlashNotAllowed,
        boolean urlEncoded
    ) {
        ElementManipulator<PsiElement> manipulator = ElementManipulators.getManipulator(element);
        assert manipulator != null;
        TextRange range = manipulator.getRangeInElement(element);
        int offset = range.getStartOffset();
        String text = range.substring(element.getText());
        for (FileReferenceHelper helper : FileReferenceHelperRegistrar.getHelpers()) {
            text = helper.trimUrl(text);
        }

        return new FileReferenceSet(text, element, offset, null, true, endingSlashNotAllowed) {
            @Override
            public boolean isUrlEncoded() {
                return urlEncoded;
            }

            @Override
            protected boolean isSoft() {
                return soft;
            }
        };
    }


    public FileReferenceSet(
        String str,
        @Nonnull PsiElement element,
        int startInElement,
        @Nullable PsiReferenceProvider provider,
        boolean isCaseSensitive
    ) {
        this(str, element, startInElement, provider, isCaseSensitive, true);
    }

    public FileReferenceSet(
        @Nonnull String str,
        @Nonnull PsiElement element,
        int startInElement,
        PsiReferenceProvider provider,
        boolean isCaseSensitive,
        boolean endingSlashNotAllowed
    ) {
        this(str, element, startInElement, provider, isCaseSensitive, endingSlashNotAllowed, null);
    }

    public FileReferenceSet(@Nonnull PsiElement element) {
        myElement = element;
        TextRange range = ElementManipulators.getValueTextRange(element);
        myStartInElement = range.getStartOffset();
        myPathStringNonTrimmed = range.substring(element.getText());
        myPathString = myPathStringNonTrimmed.trim();
        myEndingSlashNotAllowed = true;
        myCaseSensitive = false;

        reparse();
    }

    @Nonnull
    public PsiElement getElement() {
        return myElement;
    }

    void setElement(@Nonnull PsiElement element) {
        myElement = element;
    }

    public boolean isCaseSensitive() {
        return myCaseSensitive;
    }

    public boolean isEndingSlashNotAllowed() {
        return myEndingSlashNotAllowed;
    }

    public int getStartInElement() {
        return myStartInElement;
    }

    public FileReference createFileReference(TextRange range, int index, String text) {
        return new FileReference(this, range, index, text);
    }

    protected void reparse() {
        List<FileReference> referencesList = reparse(myPathStringNonTrimmed, myStartInElement);
        myReferences = referencesList.toArray(new FileReference[referencesList.size()]);
    }

    protected List<FileReference> reparse(String str, int startInElement) {
        int wsHead = 0;
        int wsTail = 0;

        LiteralTextEscaper<? extends PsiLanguageInjectionHost> escaper;
        TextRange valueRange;
        CharSequence decoded;
        // todo replace @param str with honest @param rangeInElement; and drop the following startsWith(..)
        if (myElement instanceof PsiLanguageInjectionHost languageInjectionHost
            && !StringUtil.startsWith(myElement.getText(), startInElement, str)) {
            escaper = languageInjectionHost.createLiteralTextEscaper();
            valueRange = ElementManipulators.getValueTextRange(languageInjectionHost);
            StringBuilder sb = new StringBuilder();
            escaper.decode(valueRange, sb);
            decoded = sb;
            wsHead += Math.max(0, startInElement - valueRange.getStartOffset());
        }
        else {
            escaper = null;
            decoded = str;
            valueRange = TextRange.from(startInElement, decoded.length());
        }
        List<FileReference> referencesList = ContainerUtil.newArrayList();

        for (int i = wsHead; i < decoded.length() && Character.isWhitespace(decoded.charAt(i)); i++) {
            wsHead++;     // skip head white spaces
        }
        for (int i = decoded.length() - 1; i >= 0 && Character.isWhitespace(decoded.charAt(i)); i--) {
            wsTail++;     // skip tail white spaces
        }

        int index = 0;
        int curSep = findSeparatorOffset(decoded, wsHead);
        int sepLen = curSep >= wsHead ? findSeparatorLength(decoded, curSep) : 0;

        if (curSep >= 0 && decoded.length() == wsHead + sepLen + wsTail) {
            // add extra reference for the only & leading "/"
            TextRange r = TextRange.create(startInElement, offset(curSep + Math.max(0, sepLen - 1), escaper, valueRange) + 1);
            referencesList.add(createFileReference(r, index++, decoded.subSequence(curSep, curSep + sepLen).toString()));
        }
        curSep = curSep == wsHead ? curSep + sepLen : wsHead; // reset offsets & start again for simplicity
        sepLen = 0;
        while (curSep >= 0) {
            int nextSep = findSeparatorOffset(decoded, curSep + sepLen);
            int start = curSep + sepLen;
            int endTrimmed = nextSep > 0 ? nextSep : Math.max(start, decoded.length() - wsTail);
            int endInclusive = nextSep > 0 ? nextSep : Math.max(start, decoded.length() - 1 - wsTail);
            // todo move ${placeholder} support (the str usage below) to a reference implementation
            // todo reference-set should be bound to exact range & text in a file, consider: ${slash}path${slash}file&amp;.txt
            String refText =
                index == 0 && nextSep < 0 && !StringUtil.contains(decoded, str) ? str : decoded.subSequence(start, endTrimmed).toString();
            TextRange r = new TextRange(
                offset(start, escaper, valueRange),
                offset(endInclusive, escaper, valueRange) + (nextSep < 0 && refText.length() > 0 ? 1 : 0)
            );
            referencesList.add(createFileReference(r, index++, refText));
            curSep = nextSep;
            sepLen = curSep > 0 ? findSeparatorLength(decoded, curSep) : 0;
        }

        return referencesList;
    }

    private static int offset(int offset, LiteralTextEscaper<? extends PsiLanguageInjectionHost> escaper, TextRange valueRange) {
        return escaper == null ? offset + valueRange.getStartOffset() : escaper.getOffsetInHost(offset, valueRange);
    }

    public FileReference getReference(int index) {
        return myReferences[index];
    }

    @Nonnull
    public FileReference[] getAllReferences() {
        return myReferences;
    }

    protected boolean isSoft() {
        return false;
    }

    public boolean isUrlEncoded() {
        return false;
    }

    @Nonnull
    public Collection<PsiFileSystemItem> getDefaultContexts() {
        if (myDefaultContexts == null) {
            myDefaultContexts = computeDefaultContexts();
        }
        return myDefaultContexts;
    }

    @Nonnull
    public Collection<PsiFileSystemItem> computeDefaultContexts() {
        PsiFile file = getContainingFile();
        if (file == null) {
            return Collections.emptyList();
        }

        if (myOptions != null) {
            Function<PsiFile, Collection<PsiFileSystemItem>> value = DEFAULT_PATH_EVALUATOR_OPTION.getValue(myOptions);
            if (value != null) {
                Collection<PsiFileSystemItem> roots = value.apply(file);
                if (roots != null) {
                    for (PsiFileSystemItem root : roots) {
                        if (root == null) {
                            LOG.error("Default path evaluator " + value + " produced a null root for " + file);
                        }
                    }
                    return roots;
                }
            }
        }

        if (isAbsolutePathReference()) {
            return getAbsoluteTopLevelDirLocations(file);
        }

        return getContextByFile(file);
    }

    @Nullable
    protected PsiFile getContainingFile() {
        PsiFile cf = myElement.getContainingFile();
        PsiFile file = InjectedLanguageManager.getInstance(cf.getProject()).getTopLevelFile(cf);
        if (file != null) {
            return file.getOriginalFile();
        }
        LOG.error("Invalid element: " + myElement);
        return null;
    }

    @Nonnull
    private Collection<PsiFileSystemItem> getContextByFile(@Nonnull PsiFile file) {
        PsiElement context = file.getContext();
        if (context != null) {
            file = context.getContainingFile();
        }

        if (useIncludingFileAsContext()) {
            FileContextProvider contextProvider = FileContextProvider.getProvider(file);
            if (contextProvider != null) {
                Collection<PsiFileSystemItem> folders = contextProvider.getContextFolders(file);
                if (!folders.isEmpty()) {
                    return folders;
                }
                PsiFile contextFile = contextProvider.getContextFile(file);
                if (contextFile != null) {
                    return Collections.singleton(contextFile.getParent());
                }
            }
        }

        return getContextByFileSystemItem(file.getOriginalFile());
    }

    @Nonnull
    protected Collection<PsiFileSystemItem> getContextByFileSystemItem(@Nonnull PsiFileSystemItem file) {
        VirtualFile virtualFile = file.getVirtualFile();
        if (virtualFile != null) {
            List<FileReferenceHelper> helpers = FileReferenceHelperRegistrar.getHelpers();
            ArrayList<PsiFileSystemItem> list = new ArrayList<>();
            Project project = file.getProject();
            for (FileReferenceHelper helper : helpers) {
                if (helper.isMine(project, virtualFile)) {
                    if (!list.isEmpty() && helper.isFallback()) {
                        continue;
                    }
                    list.addAll(helper.getContexts(project, virtualFile));
                }
            }
            if (!list.isEmpty()) {
                return list;
            }
            return getParentDirectoryContext();
        }
        return Collections.emptyList();
    }

    @Nonnull
    protected Collection<PsiFileSystemItem> getParentDirectoryContext() {
        PsiFile file = getContainingFile();
        VirtualFile virtualFile = file == null ? null : file.getOriginalFile().getVirtualFile();
        VirtualFile parent = virtualFile == null ? null : virtualFile.getParent();
        PsiDirectory directory = parent == null ? null : file.getManager().findDirectory(parent);
        return directory != null ? Collections.singleton(directory) : Collections.emptyList();
    }

    public String getPathString() {
        return myPathString;
    }

    public boolean isAbsolutePathReference() {
        return myPathString.startsWith(getSeparatorString());
    }

    protected boolean useIncludingFileAsContext() {
        return true;
    }

    @Nullable
    public PsiFileSystemItem resolve() {
        FileReference lastReference = getLastReference();
        return lastReference == null ? null : lastReference.resolve();
    }

    @Nullable
    public FileReference getLastReference() {
        return myReferences == null || myReferences.length == 0 ? null : myReferences[myReferences.length - 1];
    }

    @Nonnull
    public static Collection<PsiFileSystemItem> getAbsoluteTopLevelDirLocations(@Nonnull PsiFile file) {
        VirtualFile virtualFile = file.getVirtualFile();
        if (virtualFile == null) {
            return Collections.emptyList();
        }

        PsiDirectory parent = file.getParent();
        Module module = ModuleUtilCore.findModuleForPsiElement(parent == null ? file : parent);
        if (module == null) {
            return Collections.emptyList();
        }

        List<PsiFileSystemItem> list = new ArrayList<>();
        Project project = file.getProject();
        for (FileReferenceHelper helper : FileReferenceHelperRegistrar.getHelpers()) {
            if (helper.isMine(project, virtualFile)) {
                if (helper.isFallback() && !list.isEmpty()) {
                    continue;
                }
                Collection<PsiFileSystemItem> roots = helper.getRoots(module);
                for (PsiFileSystemItem root : roots) {
                    if (root == null) {
                        LOG.error("Helper " + helper + " produced a null root for " + file);
                    }
                }
                list.addAll(roots);
            }
        }
        return list;
    }

    @Nonnull
    protected Collection<PsiFileSystemItem> toFileSystemItems(VirtualFile... files) {
        return toFileSystemItems(Arrays.asList(files));
    }

    @Nonnull
    protected Collection<PsiFileSystemItem> toFileSystemItems(@Nonnull Collection<VirtualFile> files) {
        PsiManager manager = getElement().getManager();
        return ContainerUtil.mapNotNull(files, file -> file != null ? manager.findDirectory(file) : null);
    }

    public Predicate<PsiFileSystemItem> getReferenceCompletionFilter() {
        return Predicates.alwaysTrue();
    }

    public <Option> void addCustomization(CustomizableReferenceProvider.CustomizationKey<Option> key, Option value) {
        if (myOptions == null) {
            myOptions = new HashMap<>(5);
        }
        myOptions.put(key, value);
    }

    public boolean couldBeConvertedTo(boolean relative) {
        return true;
    }

    public boolean absoluteUrlNeedsStartSlash() {
        return true;
    }

    @Nonnull
    public FileType[] getSuitableFileTypes() {
        return mySuitableFileTypes == null ? EMPTY_FILE_TYPES : mySuitableFileTypes;
    }

    public boolean isEmptyPathAllowed() {
        return myEmptyPathAllowed;
    }

    public void setEmptyPathAllowed(boolean emptyPathAllowed) {
        myEmptyPathAllowed = emptyPathAllowed;
    }

    public boolean supportsExtendedCompletion() {
        return true;
    }
}