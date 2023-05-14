// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.util.gist;

import consulo.annotation.component.ServiceImpl;
import consulo.application.impl.internal.IdeaModalityState;
import consulo.ide.impl.idea.ide.util.PropertiesComponent;
import consulo.ide.impl.idea.util.containers.ContainerUtil;
import consulo.index.io.data.DataExternalizer;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiManager;
import consulo.language.psi.stub.gist.GistManager;
import consulo.language.psi.stub.gist.PsiFileGist;
import consulo.language.psi.stub.gist.VirtualFileGist;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.project.ProjectManager;
import consulo.ui.ex.awt.internal.GuiUtils;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.inject.Singleton;
import org.jetbrains.annotations.TestOnly;

import jakarta.annotation.Nonnull;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.Function;

@Singleton
@ServiceImpl
public final class GistManagerImpl extends GistManager {
  private static final Logger LOG = Logger.getInstance(GistManagerImpl.class);
  private static final Set<String> ourKnownIds = ContainerUtil.newConcurrentSet();
  private static final String ourPropertyName = "file.gist.reindex.count";
  private final AtomicInteger myReindexCount = new AtomicInteger(PropertiesComponent.getInstance().getInt(ourPropertyName, 0));

  @Nonnull
  @Override
  public <Data> VirtualFileGist<Data> newVirtualFileGist(@Nonnull String id, int version, @Nonnull DataExternalizer<Data> externalizer, @Nonnull BiFunction<Project, VirtualFile, Data> calcData) {
    if (!ourKnownIds.add(id)) {
      throw new IllegalArgumentException("Gist '" + id + "' is already registered");
    }

    return new VirtualFileGistImpl<>(id, version, externalizer, calcData);
  }

  @Nonnull
  @Override
  public <Data> PsiFileGist<Data> newPsiFileGist(@Nonnull String id, int version, @Nonnull DataExternalizer<Data> externalizer, @Nonnull Function<PsiFile, Data> calculator) {
    return new PsiFileGistImpl<>(id, version, externalizer, calculator);
  }

  int getReindexCount() {
    return myReindexCount.get();
  }

  @Override
  public void invalidateData() {
    invalidateGists();
    invalidateDependentCaches();
  }

  protected void invalidateGists() {
    if (LOG.isDebugEnabled()) {
      LOG.debug("Invalidating gists", new Throwable());
    }
    // Clear all cache at once to simplify and speedup this operation.
    // It can be made per-file if cache recalculation ever becomes an issue.
    PropertiesComponent.getInstance().setValue(ourPropertyName, myReindexCount.incrementAndGet(), 0);
  }

  private static void invalidateDependentCaches() {
    GuiUtils.invokeLaterIfNeeded(() -> {
      for (Project project : ProjectManager.getInstance().getOpenProjects()) {
        PsiManager.getInstance(project).dropPsiCaches();
      }
    }, IdeaModalityState.NON_MODAL);
  }

  @TestOnly
  public void resetReindexCount() {
    myReindexCount.set(0);
    PropertiesComponent.getInstance().unsetValue(ourPropertyName);
  }
}
