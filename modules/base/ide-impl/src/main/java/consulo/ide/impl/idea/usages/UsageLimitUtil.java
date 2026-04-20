/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package consulo.ide.impl.idea.usages;

import consulo.component.ProcessCanceledException;
import consulo.project.Project;
import consulo.ui.ex.awt.Messages;
import consulo.ui.ex.awt.internal.GuiUtils;
import consulo.usage.UsageViewBundle;
import consulo.usage.UsageViewPresentation;
import consulo.usage.localize.UsageLocalize;
import consulo.util.lang.StringUtil;

import java.util.function.Supplier;

/**
 * @author cdr
 */
public class UsageLimitUtil {
  public static final int USAGES_LIMIT = 1000;

  public static void showAndCancelIfAborted(
    Project project,
    String message,
    UsageViewPresentation usageViewPresentation
  ) {
    Result retCode = showTooManyUsagesWarning(project, message, usageViewPresentation);

    if (retCode != Result.CONTINUE) {
      throw new ProcessCanceledException();
    }
  }

  public enum Result {
    CONTINUE, ABORT
  }

  public static Result showTooManyUsagesWarning(
    Project project,
    String message,
    UsageViewPresentation usageViewPresentation
  ) {
    int result = runOrInvokeAndWait(() -> {
      String title = UsageViewBundle.message("find.excessive.usages.title", StringUtil.capitalize(StringUtil.pluralize(usageViewPresentation.getUsagesWord())));
      return Messages.showOkCancelDialog(
        project,
        message,
        title,
        UsageLocalize.buttonTextContinue().get(),
        UsageLocalize.buttonTextAbort().get(),
        Messages.getWarningIcon()
      );
    });
    return result == Messages.OK ? Result.CONTINUE : Result.ABORT;
  }

  private static int runOrInvokeAndWait(Supplier<Integer> f) {
    int[] answer = new int[1];
    try {
      GuiUtils.runOrInvokeAndWait(() -> answer[0] = f.get());
    }
    catch (Exception e) {
      answer[0] = 0;
    }

    return answer[0];
  }
}
