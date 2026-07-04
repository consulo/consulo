// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.navigationBar;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.dataContext.DataContext;
import consulo.disposer.Disposable;
import consulo.navigationBar.model.NavBarVmItem;
import consulo.project.Project;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@ServiceAPI(ComponentScope.PROJECT)
public interface NavBarService {
    @Deprecated
    static NavBarService getInstance(Project project) {
        return project.getInstance(NavBarService.class);
    }

    /**
     * Subscribes {@code fire} to all model-affecting activity until {@code disposable} is disposed.
     */
    void subscribeActivity(Disposable disposable, Runnable fire);

    CompletableFuture<NavBarVmItem> defaultModel();

    CompletableFuture<List<NavBarVmItem>> contextModel(DataContext ctx);

    CompletableFuture<Void> navigate(NavBarVmItem item);
}
