/*
 * Copyright 2013-2026 consulo.io
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
package consulo.ide.impl.projectView;

import consulo.dataContext.DataSink;
import consulo.disposer.Disposable;
import consulo.language.psi.PsiElement;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.virtualFileSystem.VirtualFile;
import org.jdom.Element;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

public interface UnifiedProjectViewPaneView extends Disposable {
    Component getComponent();

    @RequiredUIAccess
    void uiDataSnapshot(DataSink sink);

    @RequiredUIAccess
    PsiElement[] getSelectedPsiElements();

    @RequiredUIAccess
    @Nullable PsiElement getParentOfCurrentSelection();

    @RequiredUIAccess
    void onShow();

    @RequiredUIAccess
    void onHide();

    @RequiredUIAccess
    CompletableFuture<?> rebuild();

    @RequiredUIAccess
    CompletableFuture<?> reRestoreExpandedPaths();

    void queueUpdate();

    @RequiredUIAccess
    CompletableFuture<?> select(@Nullable Object element, @Nullable VirtualFile file, boolean requestFocus);

    void readExternal(Element paneElement);

    void writeExternal(Element paneElement);

    @Override
    default void dispose() {
    }
}
