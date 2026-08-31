// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.navigationBar.impl.internal;

import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.application.concurrent.coroutine.ReadLock;
import consulo.component.messagebus.MessageBusConnection;
import consulo.component.util.pointer.Pointer;
import consulo.dataContext.DataContext;
import consulo.disposer.Disposable;
import consulo.fileEditor.FileEditorManager;
import consulo.fileEditor.event.FileEditorManagerEvent;
import consulo.fileEditor.event.FileEditorManagerListener;
import consulo.language.editor.wolfAnalyzer.ProblemListener;
import consulo.navigation.Navigatable;
import consulo.navigationBar.NavBarItem;
import consulo.navigationBar.NavBarService;
import consulo.navigationBar.model.NavBarVmItem;
import consulo.project.Project;
import consulo.ui.UIAccess;
import consulo.util.concurrent.coroutine.Coroutine;
import consulo.util.concurrent.coroutine.CoroutineScope;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.status.FileStatusListener;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

@Singleton
@ServiceImpl
public class NavBarServiceImpl implements NavBarService {
    private final Project myProject;

    @Inject
    public NavBarServiceImpl(Project project) {
        myProject = project;
    }

    @Override
    public void subscribeActivity(Disposable disposable, Runnable fire) {
        MessageBusConnection connection = myProject.getMessageBus().connect(disposable);
        connection.subscribe(FileEditorManagerListener.class, new FileEditorManagerListener() {
            @Override
            public void fileOpened(FileEditorManager source, VirtualFile file) {
                fire.run();
            }

            @Override
            public void fileClosed(FileEditorManager source, VirtualFile file) {
                fire.run();
            }

            @Override
            public void selectionChanged(FileEditorManagerEvent event) {
                fire.run();
            }
        });
        connection.subscribe(FileStatusListener.class, new FileStatusListener() {
            @Override
            public void fileStatusesChanged() {
                fire.run();
            }

            @Override
            public void fileStatusChanged(VirtualFile virtualFile) {
                fire.run();
            }
        });
        connection.subscribe(ProblemListener.class, new ProblemListener() {
            @Override
            public void problemsAppeared(VirtualFile file) {
                fire.run();
            }

            @Override
            public void problemsDisappeared(VirtualFile file) {
                fire.run();
            }
        });
    }

    @Override
    public CompletableFuture<NavBarVmItem> defaultModel() {
        return readAsync(() -> new IdeNavBarVmItem(new ProjectNavBarItem(myProject)));
    }

    @Override
    public CompletableFuture<List<NavBarVmItem>> contextModel(DataContext ctx) {
        return readAsync(() -> {
            Pointer<? extends NavBarItem> pointer = ctx.getData(NavBarItem.NAVBAR_ITEM_KEY);
            NavBarItem contextItem = pointer == null ? null : pointer.dereference();
            if (contextItem == null) {
                return List.of();
            }
            return IdeNavBarVmItem.toVmItems(NavBarItemUtil.pathToItem(contextItem));
        });
    }

    @Override
    public CompletableFuture<Void> navigate(NavBarVmItem item) {
        if (!(item instanceof IdeNavBarVmItem ideItem)) {
            return CompletableFuture.completedFuture(null);
        }
        Pointer<? extends NavBarItem> pointer = ideItem.getPointer();
        UIAccess uiAccess = Application.get().getLastUIAccess();
        return this.<@Nullable Navigatable>readAsync(() -> {
                NavBarItem navBarItem = pointer.dereference();
                return navBarItem == null ? null : navBarItem.navigationRequest();
            })
            .whenCompleteAsync((navigatable, throwable) -> {
                if (throwable == null && navigatable != null && navigatable.canNavigate()) {
                    navigatable.navigate(true);
                }
            }, uiAccess)
            .thenApply(ignored -> null);
    }

    private <T> CompletableFuture<T> readAsync(Supplier<T> supplier) {
        CoroutineScope scope = CoroutineScope.of(Application.get().coroutineContext());
        return Coroutine.first(ReadLock.<@Nullable Object, T>apply(ignored -> supplier.get()))
            .runAsync(scope, null)
            .toFuture();
    }
}
