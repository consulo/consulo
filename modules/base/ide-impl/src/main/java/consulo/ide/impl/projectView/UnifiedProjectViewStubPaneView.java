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
import consulo.ide.impl.idea.ide.projectView.impl.AbstractProjectViewPane;
import consulo.language.psi.PsiElement;
import consulo.localize.LocalizeValue;
import consulo.ui.Component;
import consulo.ui.Label;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.DockLayout;
import consulo.virtualFileSystem.VirtualFile;
import org.jdom.Element;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

public class UnifiedProjectViewStubPaneView implements UnifiedProjectViewPaneView {
    private final DockLayout myLayout;

    @RequiredUIAccess
    public UnifiedProjectViewStubPaneView(AbstractProjectViewPane pane) {
        myLayout = DockLayout.create();
        myLayout.center(Label.create(LocalizeValue.localizeTODO("'" + pane.getTitle().get() + "' view is not supported by this frontend")));
    }

    @Override
    public Component getComponent() {
        return myLayout;
    }

    @Override
    @RequiredUIAccess
    public void uiDataSnapshot(DataSink sink) {
    }

    @Override
    @RequiredUIAccess
    public PsiElement[] getSelectedPsiElements() {
        return PsiElement.EMPTY_ARRAY;
    }

    @Override
    @RequiredUIAccess
    public @Nullable PsiElement getParentOfCurrentSelection() {
        return null;
    }

    @Override
    @RequiredUIAccess
    public void onShow() {
    }

    @Override
    @RequiredUIAccess
    public void onHide() {
    }

    @Override
    @RequiredUIAccess
    public CompletableFuture<?> rebuild() {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    @RequiredUIAccess
    public CompletableFuture<?> reRestoreExpandedPaths() {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public void queueUpdate() {
    }

    @Override
    @RequiredUIAccess
    public CompletableFuture<?> select(@Nullable Object element, @Nullable VirtualFile file, boolean requestFocus) {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public void readExternal(Element paneElement) {
    }

    @Override
    public void writeExternal(Element paneElement) {
    }
}
