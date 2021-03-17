/*
 * Copyright 2013-2021 consulo.io
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
package com.intellij.build.issue.quickfix;

import com.intellij.build.issue.BuildIssueQuickFix;
import com.intellij.codeInsight.highlighting.HighlightManager;
import com.intellij.codeInsight.highlighting.HighlightUsagesHandler;
import com.intellij.find.FindManager;
import com.intellij.find.FindModel;
import com.intellij.find.FindResult;
import com.intellij.openapi.actionSystem.DataProvider;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.colors.EditorColors;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * @author VISTALL
 * @since 17/01/2021
 */
public class OpenFileQuickFix implements BuildIssueQuickFix {
  private final Path myPath;
  private final String mySearch;

  public OpenFileQuickFix(Path path, String search) {
    myPath = path;
    mySearch = search;
  }

  @Override
  public String getId() {
    return myPath.toString();
  }

  @Override
  public CompletableFuture<?> runQuickFix(Project project, DataProvider dataProvider) {
    CompletableFuture<Object> future = new CompletableFuture<>();
    ApplicationManager.getApplication().invokeLater(() -> {
      try {
        showFile(project, myPath, mySearch);
        future.complete(null);
      }
      catch (Exception e) {
        future.completeExceptionally(e);
      }
    });
    return future;
  }

  public static void showFile(Project project, Path path, String search) {
    ApplicationManager.getApplication().invokeLater(() -> {
      VirtualFile file = VfsUtil.findFileByIoFile(path.toFile(), false);
      if (file == null) {
        return;
      }
      Editor editor = FileEditorManager.getInstance(project).openTextEditor(new OpenFileDescriptor(project, file), false);
      if (search == null || editor == null) return;

      FindModel findModel = new FindModel();
      FindModel.initStringToFind(findModel, search);


      FindResult findResult = FindManager.getInstance(project).findString(editor.getDocument().getCharsSequence(), 0, findModel, file);
      HighlightManager highlightManager = HighlightManager.getInstance(project);

      EditorColorsScheme globalScheme = EditorColorsManager.getInstance().getGlobalScheme();
      TextAttributes attributes = globalScheme.getAttributes(EditorColors.SEARCH_RESULT_ATTRIBUTES);

      HighlightUsagesHandler.highlightRanges(highlightManager, editor, attributes, false, List.of(findResult));
    });
  }
}
