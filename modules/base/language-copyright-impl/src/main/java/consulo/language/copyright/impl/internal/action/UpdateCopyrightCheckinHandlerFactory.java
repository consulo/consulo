/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package consulo.language.copyright.impl.internal.action;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.document.FileDocumentManager;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiManager;
import consulo.language.psi.PsiUtilCore;
import consulo.localize.LocalizeValue;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.versionControlSystem.change.CommitContext;
import consulo.versionControlSystem.change.CommitExecutor;
import consulo.versionControlSystem.checkin.CheckinHandler;
import consulo.versionControlSystem.checkin.CheckinHandlerFactory;
import consulo.versionControlSystem.checkin.CheckinProjectPanel;
import consulo.versionControlSystem.ui.CheckBoxRefreshableOnComponent;
import consulo.versionControlSystem.ui.RefreshableOnComponent;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * @author anna
 * @since 2008-12-08
 */
@ExtensionImpl(id = "copyright", order = "after code-cleanup")
public class UpdateCopyrightCheckinHandlerFactory extends CheckinHandlerFactory {
    @Override
    @Nonnull
    public CheckinHandler createHandler(final CheckinProjectPanel panel, CommitContext commitContext) {
        return new CheckinHandler() {
            @RequiredUIAccess
            @Override
            public RefreshableOnComponent getBeforeCheckinConfigurationPanel() {
                return new CheckBoxRefreshableOnComponent(
                    LocalizeValue.localizeTODO("Update copyright"),
                    () -> UpdateCopyrightCheckinHandlerState.getInstance(panel.getProject()).UPDATE_COPYRIGHT,
                    value -> UpdateCopyrightCheckinHandlerState.getInstance(panel.getProject()).UPDATE_COPYRIGHT = value
                );
            }

            @Override
            @RequiredUIAccess
            public ReturnResult beforeCheckin(@Nullable CommitExecutor executor, BiConsumer<Object, Object> additionalDataConsumer) {
                if (UpdateCopyrightCheckinHandlerState.getInstance(panel.getProject()).UPDATE_COPYRIGHT) {
                    new UpdateCopyrightProcessor(panel.getProject(), null, getPsiFiles()).run();
                    FileDocumentManager.getInstance().saveAllDocuments();
                }
                return super.beforeCheckin();
            }

            @RequiredReadAction
            private PsiFile[] getPsiFiles() {
                Collection<VirtualFile> files = panel.getVirtualFiles();
                List<PsiFile> psiFiles = new ArrayList<>();
                PsiManager manager = PsiManager.getInstance(panel.getProject());
                for (VirtualFile file : files) {
                    PsiFile psiFile = manager.findFile(file);
                    if (psiFile != null) {
                        psiFiles.add(psiFile);
                    }
                }
                return PsiUtilCore.toPsiFileArray(psiFiles);
            }
        };
    }
}