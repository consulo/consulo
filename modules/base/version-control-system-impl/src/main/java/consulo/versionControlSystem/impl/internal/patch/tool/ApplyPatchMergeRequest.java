/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package consulo.versionControlSystem.impl.internal.patch.tool;

import consulo.application.Application;
import consulo.application.util.function.Computable;
import consulo.diff.content.DocumentContent;
import consulo.diff.merge.MergeRequest;
import consulo.diff.merge.MergeResult;
import consulo.versionControlSystem.impl.internal.patch.apply.AppliedTextPatch;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.undoRedo.CommandProcessor;
import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;

public class ApplyPatchMergeRequest extends MergeRequest implements ApplyPatchRequest {
    @Nullable
    private final Project myProject;

    
    private final DocumentContent myResultContent;
    
    private final AppliedTextPatch myAppliedPatch;

    
    private final CharSequence myOriginalContent;
    
    private final String myLocalContent;

    @Nullable
    private final String myWindowTitle;
    
    private final String myLocalTitle;
    
    private final String myResultTitle;
    
    private final String myPatchTitle;

    @Nullable
    private final Consumer<MergeResult> myCallback;

    public ApplyPatchMergeRequest(
        @Nullable Project project,
        DocumentContent resultContent,
        AppliedTextPatch appliedPatch,
        String localContent,
        @Nullable String windowTitle,
        String localTitle,
        String resultTitle,
        String patchTitle,
        @Nullable Consumer<MergeResult> callback
    ) {
        myProject = project;
        myResultContent = resultContent;
        myAppliedPatch = appliedPatch;

        myOriginalContent =
            Application.get().runReadAction((Computable<CharSequence>)()-> myResultContent.getDocument().getImmutableCharSequence());
        myLocalContent = localContent;

        myWindowTitle = windowTitle;
        myLocalTitle = localTitle;
        myResultTitle = resultTitle;
        myPatchTitle = patchTitle;

        myCallback = callback;
    }

    @Nullable
    public Project getProject() {
        return myProject;
    }

    @Override
    
    public DocumentContent getResultContent() {
        return myResultContent;
    }

    @Override
    
    public String getLocalContent() {
        return myLocalContent;
    }

    @Override
    
    public AppliedTextPatch getPatch() {
        return myAppliedPatch;
    }

    @Nullable
    @Override
    public String getTitle() {
        return myWindowTitle;
    }

    @Override
    
    public String getLocalTitle() {
        return myLocalTitle;
    }

    @Override
    
    public String getResultTitle() {
        return myResultTitle;
    }

    @Override
    
    public String getPatchTitle() {
        return myPatchTitle;
    }

    @Override
    @RequiredUIAccess
    public void applyResult(MergeResult result) {
        CharSequence applyContent = switch (result) {
            case CANCEL -> myOriginalContent;
            case LEFT -> myLocalContent;
            case RIGHT -> PatchChangeBuilder.getPatchedContent(myAppliedPatch, myLocalContent);
            case RESOLVED -> null;
            default -> throw new IllegalArgumentException(result.name());
        };

        if (applyContent != null) {
            CommandProcessor.getInstance().newCommand()
                .project(myProject)
                .inWriteAction()
                .run(() -> myResultContent.getDocument().setText(applyContent));
        }

        if (myCallback != null) {
            myCallback.accept(result);
        }
    }
}
