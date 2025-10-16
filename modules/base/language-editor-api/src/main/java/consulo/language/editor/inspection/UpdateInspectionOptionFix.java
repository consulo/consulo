/*
 * Copyright 2013-2025 consulo.io
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
package consulo.language.editor.inspection;

import consulo.annotation.access.RequiredReadAction;
import consulo.language.editor.inspection.scheme.InspectionProfile;
import consulo.language.editor.inspection.scheme.InspectionProjectProfileManager;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import jakarta.annotation.Nonnull;

import java.util.function.Consumer;

/**
 * @author VISTALL
 * @since 2025-10-16
 */
public final class UpdateInspectionOptionFix<T extends InspectionTool, S extends InspectionToolState<S>> implements LocalQuickFix {
    @Nonnull
    private final T myInspectionTool;
    @Nonnull
    private final LocalizeValue myActionText;
    @Nonnull
    private final Consumer<S> myToolConsumer;

    public UpdateInspectionOptionFix(@Nonnull T inspectionTool,
                                     @Nonnull LocalizeValue actionText,
                                     @Nonnull Consumer<S> toolConsumer) {
        myInspectionTool = inspectionTool;

        myActionText = actionText;
        myToolConsumer = toolConsumer;
    }

    @Nonnull
    @Override
    public LocalizeValue getName() {
        return myActionText;
    }

    @Override
    @RequiredReadAction
    public void applyFix(@Nonnull Project project, @Nonnull ProblemDescriptor descriptor) {
        InspectionProfile profile = InspectionProjectProfileManager.getInstance(project).getInspectionProfile();
        profile.<T, S>modifyToolSettings(
            myInspectionTool.getShortName(),
            descriptor.getPsiElement(),
            (inspectionTool, state) -> myToolConsumer.accept(state)
        );
    }
}
