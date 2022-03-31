// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.actions.searcheverywhere;

import com.google.common.collect.Lists;
import consulo.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.impl.EditorHistoryManager;
import consulo.application.progress.ProgressIndicator;
import consulo.application.impl.internal.progress.ProgressIndicatorUtils;
import consulo.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import consulo.virtualFileSystem.VirtualFile;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiManager;
import consulo.language.editor.internal.matcher.MinusculeMatcher;
import consulo.language.editor.internal.matcher.NameUtil;
import consulo.application.util.function.Processor;
import com.intellij.util.containers.ContainerUtil;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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
  public void fetchWeightedElements(@Nonnull String pattern, @Nonnull ProgressIndicator progressIndicator, @Nonnull Processor<? super FoundItemDescriptor<Object>> consumer) {
    if (myProject == null) {
      return; //nothing to search
    }

    String searchString = filterControlSymbols(pattern);
    MinusculeMatcher matcher = NameUtil.buildMatcher("*" + searchString).build();
    List<VirtualFile> opened = Arrays.asList(FileEditorManager.getInstance(myProject).getSelectedFiles());
    List<VirtualFile> history = Lists.reverse(EditorHistoryManager.getInstance(myProject).getFileList());

    List<FoundItemDescriptor<Object>> res = new ArrayList<>();
    ProgressIndicatorUtils.yieldToPendingWriteActions();
    ProgressIndicatorUtils.runInReadActionWithWriteActionPriority(() -> {
      PsiManager psiManager = PsiManager.getInstance(myProject);
      Stream<VirtualFile> stream = history.stream();
      if (!StringUtil.isEmptyOrSpaces(searchString)) {
        stream = stream.filter(file -> matcher.matches(file.getName()));
      }
      res.addAll(stream.filter(vf -> !opened.contains(vf) && vf.isValid()).distinct().map(vf -> {
        PsiFile f = psiManager.findFile(vf);
        return f == null ? null : new FoundItemDescriptor<Object>(f, matcher.matchingDegree(vf.getName()));
      }).filter(file -> file != null).collect(Collectors.toList()));

      ContainerUtil.process(res, consumer);
    }, progressIndicator);
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
