// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.openapi.project.impl;

import consulo.application.ReadAction;
import consulo.module.Module;
import consulo.module.ModuleManager;
import consulo.project.Project;
import consulo.project.ProjectManager;
import consulo.project.event.ProjectManagerListener;
import consulo.module.content.ModuleRootManager;
import consulo.module.content.ProjectRootManager;
import consulo.ide.impl.idea.util.ArrayUtilRt;
import consulo.ide.impl.idea.util.containers.ContainerUtil;
import consulo.component.messagebus.MessageBusConnection;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;

// provides list of all excluded folders across all opened projects, fast.
final class ExcludeRootsCache {
  private volatile CachedUrls myCache;

  private static class CachedUrls {
    private final long myModificationCount;
    @Nonnull
    private final String[] myUrls;

    private CachedUrls(long count, @Nonnull String[] urls) {
      myModificationCount = count;
      myUrls = urls;
    }
  }

  ExcludeRootsCache(@Nonnull MessageBusConnection connection) {
    connection.subscribe(ProjectManager.TOPIC, new ProjectManagerListener() {
      @Override
      public void projectOpened(@Nonnull Project project) {
        myCache = null;
      }

      @Override
      public void projectClosed(@Nonnull Project project) {
        myCache = null;
      }
    });
  }

  @Nonnull
  String[] getExcludedUrls() {
    return ReadAction.compute(() -> {
      CachedUrls cache = myCache;
      long actualModCount = Arrays.stream(ProjectManager.getInstance().getOpenProjects()).map(ProjectRootManager::getInstance).mapToLong(ProjectRootManager::getModificationCount).sum();
      String[] urls;
      if (cache != null && actualModCount == cache.myModificationCount) {
        urls = cache.myUrls;
      }
      else {
        Collection<String> excludedUrls = new HashSet<>();
        for (Project project : ProjectManager.getInstance().getOpenProjects()) {
          for (Module module : ModuleManager.getInstance(project).getModules()) {
            urls = ModuleRootManager.getInstance(module).getExcludeRootUrls();
            ContainerUtil.addAll(excludedUrls, urls);
          }
        }
        urls = ArrayUtilRt.toStringArray(excludedUrls);
        Arrays.sort(urls);
        myCache = new CachedUrls(actualModCount, urls);
      }
      return urls;
    });
  }
}
