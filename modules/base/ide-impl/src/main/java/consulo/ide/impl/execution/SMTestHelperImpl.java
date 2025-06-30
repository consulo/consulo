/*
 * Copyright 2013-2022 consulo.io
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
package consulo.ide.impl.execution;

import consulo.annotation.component.ServiceImpl;
import consulo.execution.test.sm.TestsLocationProviderUtil;
import consulo.execution.test.sm.internal.SMTestHelper;
import consulo.ide.navigation.ChooseByNameContributor;
import consulo.ide.navigation.GotoFileContributor;
import consulo.language.psi.PsiFile;
import consulo.navigation.NavigationItem;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.inject.Singleton;

import jakarta.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

/**
 * @author VISTALL
 * @since 21-Jul-22
 */
@Singleton
@ServiceImpl
public class SMTestHelperImpl implements SMTestHelper {
  @Nonnull
  @Override
  public List<TestsLocationProviderUtil.FileInfo> collectCandidates(Project project, String fileName, boolean includeNonProjectItems) {
    List<TestsLocationProviderUtil.FileInfo> filesInfo = new ArrayList<>();
    for (ChooseByNameContributor contributor : project.getApplication().getExtensionList(GotoFileContributor.class)) {
      // let's find files with same name in project and libraries
      NavigationItem[] navigationItems = contributor.getItemsByName(fileName, fileName, project, includeNonProjectItems);
      for (NavigationItem navigationItem : navigationItems) {
        if (navigationItem instanceof PsiFile) {
          VirtualFile itemFile = ((PsiFile)navigationItem).getVirtualFile();
          assert itemFile != null;

          filesInfo.add(new TestsLocationProviderUtil.FileInfo(itemFile));
        }
      }
    }
    return filesInfo;
  }
}
