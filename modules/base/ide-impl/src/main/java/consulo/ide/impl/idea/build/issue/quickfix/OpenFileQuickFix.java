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
package consulo.ide.impl.idea.build.issue.quickfix;

import consulo.application.ApplicationManager;
import consulo.build.ui.issue.BuildIssueQuickFix;
import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorColors;
import consulo.dataContext.DataProvider;
import consulo.fileEditor.FileEditorManager;
import consulo.fileEditor.impl.internal.OpenFileDescriptorImpl;
import consulo.find.FindManager;
import consulo.find.FindModel;
import consulo.find.FindResult;
import consulo.ide.impl.idea.codeInsight.highlighting.HighlightUsagesHandler;
import consulo.ide.impl.idea.openapi.vfs.VfsUtil;
import consulo.language.editor.highlight.HighlightManager;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;

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
      Editor editor = FileEditorManager.getInstance(project).openTextEditor(new OpenFileDescriptorImpl(project, file), false);
      if (search == null || editor == null) return;

      FindModel findModel = new FindModel();
      FindModel.initStringToFind(findModel, search);

      FindResult findResult = FindManager.getInstance(project).findString(editor.getDocument().getCharsSequence(), 0, findModel, file);
      HighlightManager highlightManager = HighlightManager.getInstance(project);

      HighlightUsagesHandler.highlightRanges(highlightManager, editor, EditorColors.SEARCH_RESULT_ATTRIBUTES, false, List.of(findResult));
    });
  }
}
