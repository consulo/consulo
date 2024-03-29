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
package consulo.ide.newModule.ui;

import consulo.annotation.DeprecationInfo;
import consulo.application.Application;
import consulo.application.CommonBundle;
import consulo.ide.IdeBundle;
import consulo.ui.ex.awt.Messages;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import org.jetbrains.annotations.NonNls;

import java.io.File;
import java.io.IOException;

/**
 * @author cdr
 */
@Deprecated(forRemoval = true)
@DeprecationInfo("Desktop util for ProjectOrModuleNameStep. Use UnifiedProjectOrModuleNameStep")
public class ProjectWizardUtil {
  private ProjectWizardUtil() {
  }

  public static String findNonExistingFileName(String searchDirectory, @NonNls String preferredName, String extension){
    for (int idx = 0; ; idx++){
      final String fileName = (idx > 0? preferredName + idx : preferredName) + extension;
      if(!new File(searchDirectory + File.separator + fileName).exists()) {
        return fileName;
      }
    }
  }

  public static boolean createDirectoryIfNotExists(final String promptPrefix, String directoryPath, boolean promptUser) {
    File dir = new File(directoryPath);
    if (!dir.exists()) {
      if (promptUser) {
        final int answer = Messages.showOkCancelDialog(IdeBundle.message("promot.projectwizard.directory.does.not.exist", promptPrefix,
                                                                         dir.getPath(), Application.get().getName().get()),
                                                       IdeBundle.message("title.directory.does.not.exist"), Messages.getQuestionIcon());
        if (answer != 0) {
          return false;
        }
      }
      try {
        VirtualFileUtil.createDirectories(dir.getPath());
      }
      catch (IOException e) {
        Messages.showErrorDialog(IdeBundle.message("error.failed.to.create.directory", dir.getPath()), CommonBundle.getErrorTitle());
        return false;
      }
    }
    return true;
  }
}
