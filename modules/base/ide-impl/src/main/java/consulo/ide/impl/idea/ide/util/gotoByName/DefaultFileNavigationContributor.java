/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package consulo.ide.impl.idea.ide.util.gotoByName;

import consulo.annotation.component.ExtensionImpl;
import consulo.ide.navigation.ChooseByNameContributorEx;
import consulo.language.psi.search.FilenameIndex;
import consulo.ide.navigation.GotoFileContributor;
import consulo.language.psi.search.FindSymbolParameters;
import consulo.application.dumb.DumbAware;
import consulo.application.util.function.Processor;
import consulo.application.util.registry.Registry;
import consulo.content.scope.SearchScope;
import consulo.language.psi.PsiFileSystemItem;
import consulo.language.psi.stub.IdFilter;
import consulo.logging.Logger;
import consulo.navigation.NavigationItem;
import consulo.project.ProjectCoreUtil;

import jakarta.annotation.Nonnull;

@ExtensionImpl
public class DefaultFileNavigationContributor implements GotoFileContributor, ChooseByNameContributorEx, DumbAware {
  private static final Logger LOG = Logger.getInstance(DefaultFileNavigationContributor.class);

  @Override
  public void processNames(@Nonnull Processor<String> processor, @Nonnull SearchScope scope, IdFilter filter) {
    long started = System.currentTimeMillis();
    FilenameIndex.processAllFileNames(processor, scope, filter);
    if (LOG.isDebugEnabled()) {
      LOG.debug("All names retrieved:" + (System.currentTimeMillis() - started));
    }
  }

  @Override
  public void processElementsWithName(@Nonnull String name, @Nonnull Processor<NavigationItem> _processor, @Nonnull FindSymbolParameters parameters) {
    boolean globalSearch = parameters.getSearchScope().isSearchInLibraries();
    Processor<PsiFileSystemItem> processor = item -> {
      if (!globalSearch && ProjectCoreUtil.isProjectOrWorkspaceFile(item.getVirtualFile())) {
        return true;
      }
      return _processor.process(item);
    };

    boolean directoriesOnly = isDirectoryOnlyPattern(parameters);
    if (!directoriesOnly) {
      FilenameIndex.processFilesByName(name, false, processor, parameters.getSearchScope(), parameters.getProject(), parameters.getIdFilter());
    }

    if (directoriesOnly || Registry.is("ide.goto.file.include.directories")) {
      FilenameIndex.processFilesByName(name, true, processor, parameters.getSearchScope(), parameters.getProject(), parameters.getIdFilter());
    }
  }

  private static boolean isDirectoryOnlyPattern(@Nonnull FindSymbolParameters parameters) {
    String completePattern = parameters.getCompletePattern();
    return completePattern.endsWith("/") || completePattern.endsWith("\\");
  }
}
