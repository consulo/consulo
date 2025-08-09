/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package consulo.language.editor.inspection.scheme;

import consulo.language.editor.inspection.GlobalInspectionContext;
import consulo.language.editor.inspection.GlobalInspectionTool;
import consulo.language.editor.inspection.LocalInspectionTool;
import consulo.language.editor.inspection.reference.RefGraphAnnotator;
import consulo.language.editor.internal.RefManagerInternal;
import consulo.util.collection.ArrayUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author anna
 * @since 2005-12-28
 */
public class GlobalInspectionToolWrapper extends InspectionToolWrapper<GlobalInspectionTool> {
    public GlobalInspectionToolWrapper(@Nonnull GlobalInspectionTool globalInspectionTool) {
        super(globalInspectionTool);
    }

    private GlobalInspectionToolWrapper(@Nonnull GlobalInspectionToolWrapper other) {
        super(other);
    }

    @Nonnull
    @Override
    public GlobalInspectionToolWrapper createCopy() {
        return new GlobalInspectionToolWrapper(this);
    }

    @Override
    public void initialize(@Nonnull GlobalInspectionContext context) {
        super.initialize(context);
        RefManagerInternal refManager = (RefManagerInternal) context.getRefManager();
        RefGraphAnnotator annotator = getTool().getAnnotator(refManager, getState());
        if (annotator != null) {
            refManager.registerGraphAnnotator(annotator);
        }
        getTool().initialize(context, getState());
    }

    @Override
    @Nonnull
    public JobDescriptor[] getJobDescriptors(@Nonnull GlobalInspectionContext context) {
        JobDescriptor[] additionalJobs = getTool().getAdditionalJobs();
        if (additionalJobs == null) {
            return getTool().isGraphNeeded() ? context.getStdJobDescriptors().BUILD_GRAPH_ONLY : JobDescriptor.EMPTY_ARRAY;
        }
        else {
            return getTool().isGraphNeeded() ? ArrayUtil.append(additionalJobs, context.getStdJobDescriptors().BUILD_GRAPH) : additionalJobs;
        }
    }

    public boolean worksInBatchModeOnly() {
        return getTool().worksInBatchModeOnly();
    }

    @Nullable
    public LocalInspectionToolWrapper getSharedLocalInspectionToolWrapper() {
        LocalInspectionTool sharedTool = getTool().getSharedLocalInspectionTool();
        if (sharedTool == null) {
            return null;
        }
        return new LocalInspectionToolWrapper(sharedTool);
    }
}
