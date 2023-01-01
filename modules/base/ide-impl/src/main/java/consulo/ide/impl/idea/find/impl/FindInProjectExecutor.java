/*
 * Copyright 2013-2020 consulo.io
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
package consulo.ide.impl.idea.find.impl;

import consulo.find.FindModel;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import consulo.application.internal.ProgressIndicatorEx;
import consulo.usage.FindUsagesProcessPresentation;
import consulo.usage.UsageInfo2UsageAdapter;
import consulo.usage.UsageInfoAdapter;
import consulo.application.util.function.Processor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.table.TableCellRenderer;
import java.util.Set;

/**
 * from kotlin
 * <p>
 * service stub - we don't allow override
 */
public class FindInProjectExecutor {
  private static final FindInProjectExecutor INSTANCE = new FindInProjectExecutor();

  public static FindInProjectExecutor getInstance() {
    return INSTANCE;
  }

  @Nullable
  public TableCellRenderer createTableCellRenderer() {
    return null;
  }

  public void findUsages(@Nonnull Project project,
                         @Nonnull ProgressIndicatorEx progressIndicator,
                         @Nonnull FindUsagesProcessPresentation presentation,
                         @Nonnull FindModel findModel,
                         @Nonnull Set<VirtualFile> filesToScanInitially,
                         @Nonnull Processor<UsageInfoAdapter> onResult) {
    FindInProjectUtil.findUsages(findModel, project, presentation, filesToScanInitially, info -> {
      UsageInfoAdapter usage = (UsageInfoAdapter)UsageInfo2UsageAdapter.CONVERTER.apply(info);

      usage.getPresentation().getIcon(); // cache icon

      return onResult.process(usage);
    });
  }
}
