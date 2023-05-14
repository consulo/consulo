// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.vcs.changes;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.util.registry.Registry;
import consulo.content.internal.scope.CustomScopesProviderEx;
import consulo.content.scope.NamedScope;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.versionControlSystem.ProjectLevelVcsManager;
import consulo.versionControlSystem.change.ChangeList;
import consulo.versionControlSystem.change.ChangeListManager;
import consulo.versionControlSystem.change.ChangesUtil;
import consulo.versionControlSystem.change.LocalChangeList;
import jakarta.inject.Inject;

import jakarta.annotation.Nonnull;
import java.util.List;
import java.util.function.Consumer;

@ExtensionImpl(order = "last")
public final class ChangeListsScopesProvider extends CustomScopesProviderEx {
  @Nonnull
  private final Project myProject;

  public static ChangeListsScopesProvider getInstance(Project project) {
    return CUSTOM_SCOPES_PROVIDER.findExtension(project, ChangeListsScopesProvider.class);
  }

  @Inject
  public ChangeListsScopesProvider(@Nonnull Project project) {
    myProject = project;
  }

  @Override
  public void acceptScopes(@Nonnull Consumer<NamedScope> consumer) {
    if (myProject.isDefault() || !ProjectLevelVcsManager.getInstance(myProject).hasActiveVcss()) return ;
    final ChangeListManager changeListManager = ChangeListManager.getInstance(myProject);

    consumer.accept(new ChangeListScope(changeListManager));

    if (ChangesUtil.hasMeaningfulChangelists(myProject)) {
      List<LocalChangeList> changeLists = changeListManager.getChangeLists();
      boolean skipSingleDefaultCL = Registry.is("vcs.skip.single.default.changelist") && changeLists.size() == 1 && changeLists.get(0).isBlank();
      if (!skipSingleDefaultCL) {
        for (ChangeList list : changeLists) {
          consumer.accept(new ChangeListScope(changeListManager, list.getName(), LocalizeValue.of(list.getName())));
        }
      }
    }
  }

  @Override
  public NamedScope getCustomScope(@Nonnull String name) {
    if (myProject.isDefault()) return null;
    final ChangeListManager changeListManager = ChangeListManager.getInstance(myProject);
    if (ChangeListScope.ALL_CHANGED_FILES_SCOPE_NAME.equals(name)) {
      return new ChangeListScope(changeListManager);
    }
    if (ChangesUtil.hasMeaningfulChangelists(myProject)) {
      final LocalChangeList changeList = changeListManager.findChangeList(name);
      if (changeList != null) {
        return new ChangeListScope(changeListManager, changeList.getName(), LocalizeValue.of(changeList.getName()));
      }
    }
    return null;
  }

  @Override
  public boolean isVetoed(NamedScope scope, ScopePlace place) {
    if (place == ScopePlace.SETTING) {
      if (myProject.isDefault()) return false;
      final ChangeListManager changeListManager = ChangeListManager.getInstance(myProject);
      return changeListManager.findChangeList(scope.getScopeId()) != null;
    }
    return false;
  }
}
