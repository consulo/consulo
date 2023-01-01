// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.ide.util.gotoByName;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.ide.ServiceManager;
import consulo.component.persist.State;
import consulo.component.persist.Storage;
import consulo.component.persist.StoragePathMacros;
import consulo.project.Project;
import jakarta.inject.Singleton;

/**
 * Configuration for contributors filter in "Search Everywhere" popup.
 */
@Singleton
@State(name = "SearchEverywhereConfiguration", storages = @Storage(StoragePathMacros.WORKSPACE_FILE))
@ServiceAPI(ComponentScope.PROJECT)
@ServiceImpl
public class SearchEverywhereConfiguration extends ChooseByNameFilterConfiguration<String> {

  public static SearchEverywhereConfiguration getInstance(Project project) {
    return ServiceManager.getService(project, SearchEverywhereConfiguration.class);
  }

  @Override
  protected String nameForElement(String type) {
    return type;
  }
}
