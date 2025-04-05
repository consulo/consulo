// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.ide.actions.searcheverywhere;

import com.google.common.collect.Lists;
import consulo.application.impl.internal.progress.ProgressIndicatorUtils;
import consulo.application.progress.ProgressIndicator;
import consulo.application.util.matcher.MinusculeMatcher;
import consulo.application.util.matcher.NameUtil;
import consulo.fileEditor.FileEditorManager;
import consulo.fileEditor.impl.internal.EditorHistoryManagerImpl;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiManager;
import consulo.project.Project;
import consulo.util.collection.ContainerUtil;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class RecentFilesSEContributor extends FileSearchEverywhereContributor {
    public RecentFilesSEContributor(@Nullable Project project, @Nullable PsiElement context) {
        super(project, context);
    }

    @Nonnull
    @Override
    public String getSearchProviderId() {
        return RecentFilesSEContributor.class.getSimpleName();
    }

    @Nonnull
    @Override
    public String getGroupName() {
        return "Recent Files";
    }

    @Override
    public int getSortWeight() {
        return 70;
    }

    @Override
    public int getElementPriority(@Nonnull Object element, @Nonnull String searchPattern) {
        return super.getElementPriority(element, searchPattern) + 5;
    }

    @Override
    public void fetchWeightedElements(
        @Nonnull String pattern,
        @Nonnull ProgressIndicator progressIndicator,
        @Nonnull Predicate<? super FoundItemDescriptor<Object>> predicate
    ) {
        if (myProject == null) {
            return; //nothing to search
        }

        String searchString = filterControlSymbols(pattern);
        MinusculeMatcher matcher = NameUtil.buildMatcher("*" + searchString).build();
        List<VirtualFile> opened = Arrays.asList(FileEditorManager.getInstance(myProject).getSelectedFiles());
        List<VirtualFile> history = Lists.reverse(EditorHistoryManagerImpl.getInstance(myProject).getFileList());

        List<FoundItemDescriptor<Object>> res = new ArrayList<>();
        ProgressIndicatorUtils.yieldToPendingWriteActions();
        ProgressIndicatorUtils.runInReadActionWithWriteActionPriority(
            () -> {
                PsiManager psiManager = PsiManager.getInstance(myProject);
                Stream<VirtualFile> stream = history.stream();
                if (!StringUtil.isEmptyOrSpaces(searchString)) {
                    stream = stream.filter(file -> matcher.matches(file.getName()));
                }
                res.addAll(
                    stream.filter(vf -> !opened.contains(vf) && vf.isValid())
                        .distinct()
                        .map(vf -> {
                            PsiFile f = psiManager.findFile(vf);
                            return f == null ? null : new FoundItemDescriptor<Object>(f, matcher.matchingDegree(vf.getName()));
                        })
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList())
                );

                ContainerUtil.process(res, predicate);
            },
            progressIndicator
        );
    }

    @Override
    public boolean isEmptyPatternSupported() {
        return true;
    }

    @Override
    public boolean isShownInSeparateTab() {
        return false;
    }
}
