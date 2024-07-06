// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.fileEditor.history;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;

import java.util.List;

@ServiceAPI(value = ComponentScope.PROJECT, lazy = false)
public abstract class IdeDocumentHistory {
  public static IdeDocumentHistory getInstance(Project project) {
    return project.getInstance(IdeDocumentHistory.class);
  }

  public abstract void includeCurrentCommandAsNavigation();

  public abstract void setCurrentCommandHasMoves();

  public abstract void includeCurrentPlaceAsChangePlace();

  public abstract void clearHistory();

  public abstract void back();

  public abstract void forward();

  public abstract boolean isBackAvailable();

  public abstract boolean isForwardAvailable();

  public abstract void navigatePreviousChange();

  public abstract void navigateNextChange();

  public abstract boolean isNavigatePreviousChangeAvailable();

  public abstract boolean isNavigateNextChangeAvailable();

  public abstract VirtualFile[] getChangedFiles();

  public abstract List<PlaceInfo> getChangePlaces();

  public abstract List<PlaceInfo> getBackPlaces();

  public abstract void removeChangePlace(@Nonnull PlaceInfo placeInfo);

  public abstract void removeBackPlace(@Nonnull PlaceInfo placeInfo);

  public abstract void gotoPlaceInfo(@Nonnull PlaceInfo info);
}
