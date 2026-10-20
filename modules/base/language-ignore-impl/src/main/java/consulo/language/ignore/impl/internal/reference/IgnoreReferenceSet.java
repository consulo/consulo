/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2018 hsz Jakub Chrzanowski <jakub@hsz.mobi>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package consulo.language.ignore.impl.internal.reference;

import consulo.annotation.access.RequiredReadAction;
import consulo.application.progress.ProgressManager;
import consulo.document.util.TextRange;
import consulo.language.Language;
import consulo.language.ignore.IgnoreLanguage;
import consulo.language.ignore.cache.PatternCache;
import consulo.language.ignore.impl.internal.cache.IgnorePatternsMatchedFilesCache;
import consulo.language.ignore.psi.IgnoreEntry;
import consulo.language.ignore.psi.IgnoreFile;
import consulo.language.ignore.util.RegexUtil;
import consulo.language.psi.PsiDirectory;
import consulo.language.psi.PsiElementResolveResult;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiFileSystemItem;
import consulo.language.psi.PsiManager;
import consulo.language.psi.ResolveResult;
import consulo.language.psi.path.FileReference;
import consulo.language.psi.path.FileReferenceSet;
import consulo.project.Project;
import consulo.util.collection.ContainerUtil;
import consulo.util.lang.StringUtil;
import consulo.versionControlSystem.util.VcsUtil;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;

public class IgnoreReferenceSet extends FileReferenceSet {
    private final IgnorePatternsMatchedFilesCache myIgnorePatternsMatchedFilesCache;

    private final PatternCache myPatternCache;

    public IgnoreReferenceSet(IgnoreEntry element) {
        super(element);
        myIgnorePatternsMatchedFilesCache = IgnorePatternsMatchedFilesCache.getInstance(element.getProject());
        myPatternCache = PatternCache.getInstance(element.getProject());
    }

    /**
     * Creates {@link IgnoreReference} instance basing on passed text value.
     *
     * @param range text range
     * @param index start index
     * @param text  string text
     * @return file reference
     */
    @Override
    public FileReference createFileReference(TextRange range, int index, String text) {
        return new IgnoreReference(this, range, index, text);
    }

    /**
     * Sets ending slash as allowed.
     *
     * @return <code>false</code>
     */
    @Override
    public boolean isEndingSlashNotAllowed() {
        return false;
    }

    /**
     * Computes current element's parent context.
     *
     * @return contexts collection
     */
    @Override
    @RequiredReadAction
    public Collection<PsiFileSystemItem> computeDefaultContexts() {
        PsiFile containingFile = getElement().getContainingFile();
        if (containingFile == null) {
            return super.computeDefaultContexts();
        }
        PsiDirectory containingDirectory =
            containingFile.getParent() != null ? containingFile.getParent() : containingFile.getOriginalFile().getContainingDirectory();
        if (containingDirectory == null) {
            Language language = containingFile.getLanguage();
            VirtualFile originalVirtualFile = containingFile.getOriginalFile().getVirtualFile();
            if (language instanceof IgnoreLanguage ignoreLanguage && originalVirtualFile != null) {
                VirtualFile affectedRoot = ignoreLanguage.getAffectedRoot(containingFile.getProject(), originalVirtualFile);
                if (affectedRoot != null) {
                    containingDirectory = containingFile.getManager().findDirectory(affectedRoot);
                }
            }
        }
        return containingDirectory != null ? Collections.singletonList(containingDirectory) : super.computeDefaultContexts();
    }

    @Override
    public Predicate<PsiFileSystemItem> getReferenceCompletionFilter() {
        return item -> {
            Project project = item.getProject();
            PsiFile elementFile = getElement().getContainingFile();
            if (elementFile == null) {
                return false;
            }
            PsiFile originalFile = elementFile.getOriginalFile();
            VirtualFile ignoreFile = originalFile.getVirtualFile();
            VirtualFile itemFile = item.getVirtualFile();
            Language language = originalFile.getLanguage();
            if (!(language instanceof IgnoreLanguage ignoreLanguage) || ignoreFile == null || itemFile == null) {
                return false;
            }
            VirtualFile ignoreFileAffectedRoot = ignoreLanguage.getAffectedRoot(project, ignoreFile);
            if (ignoreFileAffectedRoot == null) {
                return false;
            }
            VirtualFile ignoreFileVcsRoot = getRootFor(project, ignoreFileAffectedRoot);
            return isFileUnderSameRoot(project, ignoreFileVcsRoot, ignoreFileAffectedRoot, itemFile);
        };
    }

    /**
     * Returns last reference of the current element's references.
     *
     * @return last {@link FileReference}
     */
    @Override
    public @Nullable FileReference getLastReference() {
        FileReference lastReference = super.getLastReference();
        if (lastReference != null && lastReference.getCanonicalText().endsWith(getSeparatorString())) {
            FileReference[] references = myReferences;
            return references != null && references.length > 1 ? references[references.length - 2] : null;
        }
        return lastReference;
    }

    /**
     * Disallows conversion to relative reference.
     *
     * @param relative is ignored
     * @return <code>false</code>
     */
    @Override
    public boolean couldBeConvertedTo(boolean relative) {
        return false;
    }

    /**
     * Parses entry, searches for file references and stores them in {@link #myReferences}.
     */
    @Override
    protected void reparse() {
        ProgressManager.checkCanceled();
        String str = StringUtil.trimEnd(getPathString(), getSeparatorString());
        List<FileReference> referencesList = new ArrayList<>();

        String separatorString = getSeparatorString(); // separator's length can be more then 1 char
        int sepLen = separatorString.length();
        int currentSlash = -sepLen;
        int startInElement = getStartInElement();

        // skip white space
        while (currentSlash + sepLen < str.length() && Character.isWhitespace(str.charAt(currentSlash + sepLen))) {
            currentSlash++;
        }

        if (currentSlash + sepLen + sepLen < str.length()
            && str.substring(currentSlash + sepLen, currentSlash + sepLen + sepLen).equals(separatorString)) {
            currentSlash += sepLen;
        }
        int index = 0;

        if (str.equals(separatorString)) {
            FileReference fileReference = createFileReference(
                new TextRange(startInElement, startInElement + sepLen),
                index++,
                separatorString
            );
            referencesList.add(fileReference);
        }

        while (true) {
            ProgressManager.checkCanceled();
            int nextSlash = str.indexOf(separatorString, currentSlash + sepLen);
            String subReferenceText = nextSlash > 0 ? str.substring(0, nextSlash) : str;
            TextRange range = new TextRange(
                startInElement + currentSlash + sepLen,
                startInElement + (nextSlash > 0 ? nextSlash : str.length())
            );
            FileReference ref = createFileReference(range, index++, subReferenceText);
            referencesList.add(ref);
            if ((currentSlash = nextSlash) < 0) {
                break;
            }
        }

        myReferences = referencesList.toArray(FileReference.EMPTY);
    }

    @Override
    @RequiredReadAction
    protected String getNewAbsolutePath(PsiFileSystemItem root, String relativePath) {
        PsiFile ignoreFile = getContainingFile();
        VirtualFile rootVF = root.getVirtualFile();
        VirtualFile ignoreVirtualFile = ignoreFile != null ? ignoreFile.getVirtualFile() : null;
        VirtualFile ignoreFileParent = ignoreVirtualFile != null ? ignoreVirtualFile.getParent() : null;
        if (rootVF != null && ignoreFileParent != null && !rootVF.equals(ignoreFileParent)) {
            VirtualFile relativeFile = rootVF.findFileByRelativePath(relativePath);
            if (relativeFile != null) {
                String relativeToIgnoreFileParent = VirtualFileUtil.getRelativePath(relativeFile, ignoreFileParent);
                if (relativeToIgnoreFileParent != null) {
                    return absoluteUrlNeedsStartSlash() ? "/" + relativeToIgnoreFileParent : relativeToIgnoreFileParent;
                }
            }
        }
        return super.getNewAbsolutePath(root, relativePath);
    }

    /**
     * Custom definition of {@link FileReference}.
     */
    private final class IgnoreReference extends FileReference {
        private IgnoreReference(FileReferenceSet fileReferenceSet, TextRange range, int index, String text) {
            super(fileReferenceSet, range, index, text);
        }

        /**
         * Resolves reference to the filesystem.
         *
         * @param text          entry
         * @param context       filesystem context
         * @param result        result references collection
         * @param caseSensitive is ignored
         */
        @Override
        @RequiredReadAction
        protected void innerResolveInContext(
            String text,
            PsiFileSystemItem context,
            Collection<ResolveResult> result,
            boolean caseSensitive
        ) {
            ProgressManager.checkCanceled();
            super.innerResolveInContext(text, context, result, caseSensitive);

            PsiFile containingFile = getContainingFile();
            if (!(containingFile instanceof IgnoreFile) || containingFile.getVirtualFile() == null) {
                return;
            }

            Project project = context.getProject();
            VirtualFile ignoreFileAffectedRoot =
                ((IgnoreLanguage) containingFile.getLanguage()).getAffectedRoot(project, containingFile.getVirtualFile());
            if (ignoreFileAffectedRoot == null) {
                return;
            }

            VirtualFile ignoreFileVcsRoot = getRootFor(project, ignoreFileAffectedRoot);

            VirtualFile contextVirtualFile = context.getVirtualFile();
            if (contextVirtualFile != null) {
                IgnoreEntry entry = (IgnoreEntry) getFileReferenceSet().getElement();
                String current = getCanonicalText();
                Pattern pattern = myPatternCache.createPattern(current, entry.getSyntax());
                if (pattern != null) {
                    PsiFile elementFile = getElement().getContainingFile();
                    PsiDirectory parent = elementFile != null ? elementFile.getParent() : null;
                    VirtualFile root = parent != null ? parent.getVirtualFile() : null;
                    PsiManager psiManager = getElement().getManager();

                    List<VirtualFile> files = new ArrayList<>(myIgnorePatternsMatchedFilesCache.getFilesForPattern(pattern));
                    VirtualFile[] children = contextVirtualFile.getChildren();
                    if (files.isEmpty() && children != null) {
                        files.addAll(ContainerUtil.filter(
                            children,
                            virtualFile -> isFileUnderSameRoot(project, ignoreFileVcsRoot, ignoreFileAffectedRoot, virtualFile)
                        ));
                    }

                    for (VirtualFile file : files) {
                        ProgressManager.checkCanceled();
                        if (!isFileUnderSameRoot(project, ignoreFileVcsRoot, ignoreFileAffectedRoot, file)) {
                            continue;
                        }

                        VirtualFile baseRoot = ignoreFileVcsRoot != null ? ignoreFileVcsRoot : ignoreFileAffectedRoot;
                        String relativeToIgnoreFileVcsRoot = VirtualFileUtil.getRelativePath(file, baseRoot);
                        String name = root != null
                            ? VirtualFileUtil.getRelativePath(file, root)
                            : relativeToIgnoreFileVcsRoot != null ? relativeToIgnoreFileVcsRoot : file.getName();
                        if (RegexUtil.match(pattern, name)) {
                            PsiFileSystemItem psiFileSystemItem = getPsiFileSystemItem(psiManager, file);
                            if (psiFileSystemItem == null) {
                                continue;
                            }
                            result.add(new PsiElementResolveResult(psiFileSystemItem));
                        }
                    }
                }
            }
        }

        /**
         * Searches for directory or file using {@link PsiManager}.
         *
         * @param manager {@link PsiManager} instance
         * @param file    working file
         * @return Psi item
         */
        @RequiredReadAction
        private static @Nullable PsiFileSystemItem getPsiFileSystemItem(PsiManager manager, VirtualFile file) {
            if (!file.isValid()) {
                return null;
            }

            return file.isDirectory() ? manager.findDirectory(file) : manager.findFile(file);
        }
    }

    /**
     * Returns the VCS root of the given ignore file root, or <code>null</code> if it is not under version control.
     */
    private static @Nullable VirtualFile getRootFor(Project project, VirtualFile ignoreFileAffectedRoot) {
        return VcsUtil.getVcsRootFor(project, ignoreFileAffectedRoot);
    }

    /**
     * Checks that the file is under the same VCS root as the ignore file. Ignore files outside of version control
     * (i.e. .aiignore in a project without VCS) are matched against their affected root instead.
     */
    private static boolean isFileUnderSameRoot(
        Project project,
        @Nullable VirtualFile vcsRoot,
        VirtualFile affectedRoot,
        VirtualFile file
    ) {
        if (vcsRoot == null) {
            return VirtualFileUtil.isAncestor(affectedRoot, file, false);
        }
        VirtualFile fileVcsRoot = VcsUtil.getVcsRootFor(project, file);
        return fileVcsRoot != null && vcsRoot.equals(fileVcsRoot);
    }
}
