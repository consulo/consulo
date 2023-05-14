/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package consulo.ide.impl.idea.diff.tools.external;

import consulo.ide.impl.idea.diff.DiffManagerEx;
import consulo.diff.content.DiffContent;
import consulo.diff.content.DocumentContent;
import consulo.diff.content.FileContent;
import consulo.diff.merge.MergeRequest;
import consulo.ide.impl.idea.diff.merge.ThreesideMergeRequest;
import consulo.process.ExecutionException;
import consulo.component.ProcessCanceledException;
import consulo.project.Project;
import consulo.ui.ex.awt.Messages;
import consulo.logging.Logger;

import jakarta.annotation.Nonnull;

import java.io.IOException;
import java.util.List;

public class ExternalMergeTool {
  public static final Logger LOG = Logger.getInstance(ExternalMergeTool.class);

  public static boolean isDefault() {
    return ExternalDiffSettings.getInstance().isMergeEnabled();
  }

  public static boolean isEnabled() {
    return ExternalDiffSettings.getInstance().isMergeEnabled();
  }

  public static void show(@jakarta.annotation.Nullable final Project project,
                          @Nonnull final MergeRequest request) {
    try {
      if (canShow(request)) {
        showRequest(project, request);
      }
      else {
        DiffManagerEx.getInstance().showMergeBuiltin(project, request);
      }
    }
    catch (ProcessCanceledException ignore) {
    }
    catch (Throwable e) {
      LOG.error(e);
      Messages.showErrorDialog(project, e.getMessage(), "Can't Show Merge In External Tool");
    }
  }

  public static void showRequest(@jakarta.annotation.Nullable Project project, @Nonnull MergeRequest request)
          throws ExecutionException, IOException {
    ExternalDiffSettings settings = ExternalDiffSettings.getInstance();

    ExternalDiffToolUtil.executeMerge(project, settings, (ThreesideMergeRequest)request);
  }

  public static boolean canShow(@Nonnull MergeRequest request) {
    if (request instanceof ThreesideMergeRequest) {
      DiffContent outputContent = ((ThreesideMergeRequest)request).getOutputContent();
      if (!canProcessOutputContent(outputContent)) return false;

      List<? extends DiffContent> contents = ((ThreesideMergeRequest)request).getContents();
      if (contents.size() != 3) return false;
      for (DiffContent content : contents) {
        if (!ExternalDiffToolUtil.canCreateFile(content)) return false;
      }
      return true;
    }
    return false;
  }

  private static boolean canProcessOutputContent(@Nonnull DiffContent content) {
    if (content instanceof DocumentContent) return true;
    if (content instanceof FileContent && ((FileContent)content).getFile().isInLocalFileSystem()) return true;
    return false;
  }
}
