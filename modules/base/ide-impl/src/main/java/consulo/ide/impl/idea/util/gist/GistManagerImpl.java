// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.util.gist;

import consulo.ide.impl.idea.ide.util.PropertiesComponent;
import consulo.application.impl.internal.IdeaModalityState;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.project.ProjectManager;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.event.BulkFileListener;
import consulo.virtualFileSystem.event.VFileEvent;
import consulo.virtualFileSystem.event.VFilePropertyChangeEvent;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiManager;
import consulo.ui.ex.awt.internal.GuiUtils;
import consulo.ide.impl.idea.util.NullableFunction;
import consulo.ide.impl.idea.util.containers.ContainerUtil;
import consulo.index.io.data.DataExternalizer;
import org.jetbrains.annotations.TestOnly;

import javax.annotation.Nonnull;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

@Singleton
public final class GistManagerImpl extends GistManager {
  private static final Logger LOG = Logger.getInstance(GistManagerImpl.class);
  private static final Set<String> ourKnownIds = ContainerUtil.newConcurrentSet();
  private static final String ourPropertyName = "file.gist.reindex.count";
  private final AtomicInteger myReindexCount = new AtomicInteger(PropertiesComponent.getInstance().getInt(ourPropertyName, 0));

  static final class MyBulkFileListener implements BulkFileListener {
    private Provider<GistManager> myGistManager;

    @Inject
    MyBulkFileListener(Provider<GistManager> gistManager) {
      myGistManager = gistManager;
    }

    @Override
    public void after(@Nonnull List<? extends VFileEvent> events) {
      if (events.stream().anyMatch(MyBulkFileListener::shouldDropCache)) {
        ((GistManagerImpl)myGistManager.get()).invalidateGists();
      }
    }

    private static boolean shouldDropCache(VFileEvent e) {
      if (!(e instanceof VFilePropertyChangeEvent)) return false;

      String propertyName = ((VFilePropertyChangeEvent)e).getPropertyName();
      return propertyName.equals(VirtualFile.PROP_NAME) || propertyName.equals(VirtualFile.PROP_ENCODING);
    }
  }

  @Nonnull
  @Override
  public <Data> VirtualFileGist<Data> newVirtualFileGist(@Nonnull String id, int version, @Nonnull DataExternalizer<Data> externalizer, @Nonnull VirtualFileGist.GistCalculator<Data> calcData) {
    if (!ourKnownIds.add(id)) {
      throw new IllegalArgumentException("Gist '" + id + "' is already registered");
    }

    return new VirtualFileGistImpl<>(id, version, externalizer, calcData);
  }

  @Nonnull
  @Override
  public <Data> PsiFileGist<Data> newPsiFileGist(@Nonnull String id, int version, @Nonnull DataExternalizer<Data> externalizer, @Nonnull NullableFunction<PsiFile, Data> calculator) {
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

  private void invalidateGists() {
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
