/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

package consulo.ide.impl.idea.codeInsight.completion;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.application.progress.ProgressManager;
import consulo.component.ProcessCanceledException;
import consulo.ide.navigation.ChooseByNameContributor;
import consulo.ide.navigation.GotoFileContributor;
import consulo.language.Language;
import consulo.language.editor.completion.CompletionContributor;
import consulo.language.editor.completion.CompletionProvider;
import consulo.language.editor.completion.CompletionResultSet;
import consulo.language.editor.completion.CompletionType;
import consulo.language.editor.completion.lookup.InsertionContext;
import consulo.language.editor.completion.lookup.LookupElement;
import consulo.language.editor.completion.lookup.LookupElementPresentation;
import consulo.language.editor.impl.internal.FileInfoManager;
import consulo.language.editor.localize.CodeInsightLocalize;
import consulo.language.file.FileTypeManager;
import consulo.language.psi.*;
import consulo.language.psi.path.*;
import consulo.language.psi.search.FilenameIndex;
import consulo.logging.Logger;
import consulo.module.Module;
import consulo.module.content.ProjectFileIndex;
import consulo.module.content.ProjectRootManager;
import consulo.project.Project;
import consulo.project.content.scope.ProjectAwareSearchScope;
import consulo.project.content.scope.ProjectScopes;
import consulo.ui.ex.action.IdeActions;
import consulo.ui.image.Image;
import consulo.util.collection.ArrayUtil;
import consulo.util.io.FileUtil;
import consulo.util.lang.Comparing;
import consulo.util.lang.Pair;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.fileType.FileNameMatcher;
import consulo.virtualFileSystem.fileType.FileType;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.*;

import static consulo.language.pattern.PlatformPatterns.psiElement;

/**
 * @author spleaner
 */
@ExtensionImpl(id = "filePath", order = "before javaClassName")
public class FilePathCompletionContributor extends CompletionContributor {
    private static final Logger LOG = Logger.getInstance(FilePathCompletionContributor.class);

    public FilePathCompletionContributor() {
        extend(
            CompletionType.BASIC,
            psiElement(),
            (parameters, context, result) -> {
                PsiReference psiReference = parameters.getPosition().getContainingFile().findReferenceAt(parameters.getOffset());
                if (getReference(psiReference) != null && parameters.getInvocationCount() == 1) {
                    String shortcut = getActionShortcut(IdeActions.ACTION_CODE_COMPLETION);
                    result.addLookupAdvertisement(CodeInsightLocalize.classCompletionFilePath(shortcut).get());
                }
            }
        );

        CompletionProvider provider = (parameters, context, _result) -> {
            if (!parameters.isExtendedCompletion()) {
                return;
            }

            @Nonnull CompletionResultSet result = _result.caseInsensitive();
            PsiElement e = parameters.getPosition();
            Project project = e.getProject();

            PsiReference psiReference = parameters.getPosition().getContainingFile().findReferenceAt(parameters.getOffset());

            Pair<FileReference, Boolean> fileReferencePair = getReference(psiReference);
            if (fileReferencePair != null) {
                FileReference first = fileReferencePair.getFirst();
                if (first == null) {
                    return;
                }

                FileReferenceSet set = first.getFileReferenceSet();
                String prefix = set.getPathString()
                    .substring(0, parameters.getOffset() - set.getElement().getTextRange().getStartOffset() - set.getStartInElement());

                List<String> pathPrefixParts = null;
                int lastSlashIndex;
                if ((lastSlashIndex = prefix.lastIndexOf('/')) != -1) {
                    pathPrefixParts = StringUtil.split(prefix.substring(0, lastSlashIndex), "/");
                    prefix = prefix.substring(lastSlashIndex + 1);
                }

                CompletionResultSet __result = result.withPrefixMatcher(prefix).caseInsensitive();

                PsiFile originalFile = parameters.getOriginalFile();
                VirtualFile contextFile = originalFile.getVirtualFile();
                if (contextFile != null) {
                    String[] fileNames = getAllNames(project);
                    Set<String> resultNames = new TreeSet<>();
                    for (String fileName : fileNames) {
                        if (filenameMatchesPrefixOrType(fileName, prefix, set.getSuitableFileTypes(), parameters.getInvocationCount())) {
                            resultNames.add(fileName);
                        }
                    }

                    ProjectFileIndex index = ProjectRootManager.getInstance(project).getFileIndex();

                    Module contextModule = index.getModuleForFile(contextFile);
                    if (contextModule != null) {
                        List<FileReferenceHelper> helpers = FileReferenceHelperRegistrar.getHelpers(originalFile);

                        ProjectAwareSearchScope scope = ProjectScopes.getProjectScope(project);
                        for (String name : resultNames) {
                            ProgressManager.checkCanceled();

                            PsiFile[] files = FilenameIndex.getFilesByName(project, name, scope);

                            if (files.length <= 0) {
                                continue;
                            }
                            for (PsiFile file : files) {
                                ProgressManager.checkCanceled();

                                VirtualFile virtualFile = file.getVirtualFile();
                                if (virtualFile == null || !virtualFile.isValid() || Comparing.equal(virtualFile, contextFile)) {
                                    continue;
                                }
                                List<FileReferenceHelper> helperList = new ArrayList<>();
                                for (FileReferenceHelper contextHelper : helpers) {
                                    ProgressManager.checkCanceled();

                                    if (contextHelper.isMine(project, virtualFile)
                                        && (pathPrefixParts == null || fileMatchesPathPrefix(
                                        contextHelper.getPsiFileSystemItem(project, virtualFile),
                                        pathPrefixParts
                                    ))) {
                                        helperList.add(contextHelper);
                                    }
                                }
                                if (!helperList.isEmpty()) {
                                    __result.addElement(new FilePathLookupItem(file, helperList));
                                }
                            }
                        }
                    }
                }

                if (set.getSuitableFileTypes().length > 0 && parameters.getInvocationCount() == 1) {
                    String shortcut = getActionShortcut(IdeActions.ACTION_CODE_COMPLETION);
                    result.addLookupAdvertisement(CodeInsightLocalize.classCompletionFilePathAllVariants(shortcut).get());
                }

                if (fileReferencePair.getSecond()) {
                    result.stopHere();
                }
            }
        };
        extend(CompletionType.BASIC, psiElement(), provider);
    }

    private static boolean filenameMatchesPrefixOrType(
        String fileName,
        String prefix,
        FileType[] suitableFileTypes,
        int invocationCount
    ) {
        boolean prefixMatched = prefix.length() == 0 || StringUtil.startsWithIgnoreCase(fileName, prefix);
        if (prefixMatched && (suitableFileTypes.length == 0 || invocationCount > 2)) {
            return true;
        }

        if (prefixMatched) {
            String extension = FileUtil.getExtension(fileName);
            if (extension.length() == 0) {
                return false;
            }

            for (FileType fileType : suitableFileTypes) {
                for (FileNameMatcher matcher : FileTypeManager.getInstance().getAssociations(fileType)) {
                    if (matcher.accept(fileName)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    @RequiredReadAction
    private static boolean fileMatchesPathPrefix(@Nullable PsiFileSystemItem file, @Nonnull List<String> pathPrefix) {
        if (file == null) {
            return false;
        }

        List<String> contextParts = new ArrayList<>();
        PsiFileSystemItem parentFile = file;
        PsiFileSystemItem parent;
        while ((parent = parentFile.getParent()) != null) {
            if (parent.getName().length() > 0) {
                contextParts.add(0, parent.getName().toLowerCase());
            }
            parentFile = parent;
        }

        String path = StringUtil.join(contextParts, "/");

        int nextIndex = 0;
        for (String s : pathPrefix) {
            if ((nextIndex = path.indexOf(s.toLowerCase(), nextIndex)) == -1) {
                return false;
            }
        }

        return true;
    }

    private static String[] getAllNames(@Nonnull Project project) {
        Set<String> names = new HashSet<>();
        for (ChooseByNameContributor contributor : project.getApplication().getExtensionList(GotoFileContributor.class)) {
            try {
                names.addAll(Arrays.asList(contributor.getNames(project, false)));
            }
            catch (ProcessCanceledException ex) {
                // index corruption detected, ignore
            }
            catch (Exception ex) {
                LOG.error(ex);
            }
        }

        return ArrayUtil.toStringArray(names);
    }

    @Nullable
    private static Pair<FileReference, Boolean> getReference(PsiReference original) {
        if (original == null) {
            return null;
        }

        if (original instanceof PsiMultiReference multiReference) {
            for (PsiReference reference : multiReference.getReferences()) {
                if (reference instanceof FileReference fileReference) {
                    if (fileReference.getFileReferenceSet().supportsExtendedCompletion()) {
                        return Pair.create(fileReference, false);
                    }
                }
            }
        }
        else if (original instanceof FileReferenceOwner fileReferenceOwner) {
            PsiFileReference psiFileReference = fileReferenceOwner.getLastFileReference();
            if (psiFileReference instanceof FileReference fileReference) {
                if (fileReference.getFileReferenceSet().supportsExtendedCompletion()) {
                    return Pair.create(fileReference, true);
                }
            }
        }

        return null;
    }

    @Nonnull
    @Override
    public Language getLanguage() {
        return Language.ANY;
    }

    public static class FilePathLookupItem extends LookupElement {
        private final String myName;
        private final String myPath;
        private final String myInfo;
        private final Image myIcon;
        private final PsiFile myFile;
        private final List<FileReferenceHelper> myHelpers;

        @RequiredReadAction
        public FilePathLookupItem(@Nonnull PsiFile file, @Nonnull List<FileReferenceHelper> helpers) {
            myName = file.getName();
            myPath = file.getVirtualFile().getPath();

            myHelpers = helpers;

            myInfo = FileInfoManager.getFileAdditionalInfo(file);
            myIcon = file.getFileType().getIcon();

            myFile = file;
        }

        @SuppressWarnings({"HardCodedStringLiteral"})
        @Override
        public String toString() {
            return String.format("%s%s", myName, myInfo == null ? "" : " (" + myInfo + ")");
        }

        @Nonnull
        @Override
        public Object getObject() {
            return myFile;
        }

        @Override
        @Nonnull
        public String getLookupString() {
            return myName;
        }

        @Override
        @RequiredReadAction
        public void handleInsert(InsertionContext context) {
            context.commitDocument();
            if (myFile.isValid()) {
                PsiReference psiReference = context.getFile().findReferenceAt(context.getStartOffset());
                Pair<FileReference, Boolean> fileReferencePair = getReference(psiReference);
                if (fileReferencePair != null) {
                    FileReference ref = fileReferencePair.getFirst();
                    context.setTailOffset(ref.getRangeInElement().getEndOffset() + ref.getElement().getTextRange().getStartOffset());
                    ref.bindToElement(myFile);
                }
            }
        }

        @Override
        @RequiredReadAction
        public void renderElement(LookupElementPresentation presentation) {
            String relativePath = getRelativePath();

            StringBuilder sb = new StringBuilder();
            if (myInfo != null) {
                sb.append(" (").append(myInfo);
            }

            if (relativePath != null && !relativePath.equals(myName)) {
                if (myInfo != null) {
                    sb.append(", ");
                }
                else {
                    sb.append(" (");
                }

                sb.append(relativePath);
            }

            if (sb.length() > 0) {
                sb.append(')');
            }

            presentation.setItemText(myName);

            if (sb.length() > 0) {
                presentation.setTailText(sb.toString(), true);
            }

            presentation.setIcon(myIcon);
        }

        @Nullable
        @RequiredReadAction
        private String getRelativePath() {
            VirtualFile virtualFile = myFile.getVirtualFile();
            LOG.assertTrue(virtualFile != null);
            for (FileReferenceHelper helper : myHelpers) {
                PsiFileSystemItem root = helper.findRoot(myFile.getProject(), virtualFile);
                String path = PsiFileSystemItemUtil.getRelativePath(root, helper.getPsiFileSystemItem(myFile.getProject(), virtualFile));
                if (path != null) {
                    return path;
                }
            }
            return null;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            FilePathLookupItem that = (FilePathLookupItem) o;

            return myName.equals(that.myName)
                && myPath.equals(that.myPath);
        }

        @Override
        public int hashCode() {
            return 31 * myName.hashCode() + myPath.hashCode();
        }
    }
}