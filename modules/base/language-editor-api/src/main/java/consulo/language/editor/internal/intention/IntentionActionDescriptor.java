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
package consulo.language.editor.internal.intention;

import consulo.codeEditor.Editor;
import consulo.language.editor.annotation.HighlightSeverity;
import consulo.language.editor.annotation.ProblemGroup;
import consulo.language.editor.annotation.SuppressableProblemGroup;
import consulo.language.editor.inspection.CustomSuppressableInspectionTool;
import consulo.language.editor.inspection.InspectionTool;
import consulo.language.editor.inspection.SuppressIntentionActionFromFix;
import consulo.language.editor.inspection.SuppressQuickFix;
import consulo.language.editor.inspection.scheme.InspectionProfile;
import consulo.language.editor.inspection.scheme.InspectionProjectProfileManager;
import consulo.language.editor.inspection.scheme.InspectionToolWrapper;
import consulo.language.editor.intention.IntentionAction;
import consulo.language.editor.intention.IntentionManager;
import consulo.language.editor.internal.inspection.AnnotatorBasedInspection;
import consulo.language.editor.inspection.scheme.GlobalInspectionToolWrapper;
import consulo.language.editor.inspection.scheme.LocalInspectionToolWrapper;
import consulo.language.editor.rawHighlight.HighlightDisplayKey;
import consulo.language.psi.PsiElement;
import consulo.localize.LocalizeValue;
import consulo.ui.image.Image;
import consulo.util.collection.ContainerUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class IntentionActionDescriptor {
    private final IntentionAction myAction;
    private volatile List<IntentionAction> myOptions;
    private volatile HighlightDisplayKey myKey;
    private final ProblemGroup myProblemGroup;
    private final HighlightSeverity mySeverity;
    private final LocalizeValue myDisplayName;
    private final Image myIcon;
    private Boolean myCanCleanup;

    public IntentionActionDescriptor(@Nonnull IntentionAction action, List<IntentionAction> options, @Nonnull LocalizeValue displayName) {
        this(action, options, displayName, null);
    }

    public IntentionActionDescriptor(@Nonnull IntentionAction action, Image icon) {
        this(action, null, LocalizeValue.of(), icon);
    }

    public IntentionActionDescriptor(
        @Nonnull IntentionAction action,
        @Nullable List<IntentionAction> options,
        @Nonnull LocalizeValue displayName,
        @Nullable Image icon
    ) {
        this(action, options, displayName, icon, null, null, null);
    }

    public IntentionActionDescriptor(
        @Nonnull IntentionAction action,
        @Nullable List<IntentionAction> options,
        @Nonnull LocalizeValue displayName,
        @Nullable Image icon,
        @Nullable HighlightDisplayKey key,
        @Nullable ProblemGroup problemGroup,
        @Nullable HighlightSeverity severity
    ) {
        myAction = action;
        myOptions = options;
        myDisplayName = displayName;
        myIcon = icon;
        myKey = key;
        myProblemGroup = problemGroup;
        mySeverity = severity;
    }

    @Nonnull
    public IntentionAction getAction() {
        return myAction;
    }

    public boolean isError() {
        return mySeverity == null || mySeverity.compareTo(HighlightSeverity.ERROR) >= 0;
    }

    public boolean isInformation() {
        return HighlightSeverity.INFORMATION.equals(mySeverity);
    }

    public boolean canCleanup(@Nonnull PsiElement element) {
        if (myCanCleanup == null) {
            InspectionProfile profile = InspectionProjectProfileManager.getInstance(element.getProject()).getCurrentProfile();
            HighlightDisplayKey key = myKey;
            if (key == null) {
                myCanCleanup = false;
            }
            else {
                InspectionToolWrapper toolWrapper = profile.getInspectionTool(key.toString(), element);
                myCanCleanup = toolWrapper != null && toolWrapper.isCleanupTool();
            }
        }
        return myCanCleanup;
    }

    @Nullable
    public List<IntentionAction> getOptions(@Nonnull PsiElement element, @Nullable Editor editor) {
        if (editor != null && Boolean.FALSE.equals(editor.getUserData(IntentionManager.SHOW_INTENTION_OPTIONS_KEY))) {
            return null;
        }
        List<IntentionAction> options = myOptions;
        HighlightDisplayKey key = myKey;
        if (myProblemGroup != null) {
            String problemName = myProblemGroup.getProblemName();
            HighlightDisplayKey problemGroupKey = problemName != null ? HighlightDisplayKey.findById(problemName) : null;
            if (problemGroupKey != null) {
                key = problemGroupKey;
            }
        }
        if (options != null || key == null) {
            return options;
        }
        IntentionManager intentionManager = IntentionManager.getInstance();
        List<IntentionAction> newOptions = intentionManager.getStandardIntentionOptions(key, element);
        InspectionProfile profile = InspectionProjectProfileManager.getInstance(element.getProject()).getCurrentProfile();
        InspectionToolWrapper toolWrapper = profile.getInspectionTool(key.toString(), element);
        if (!(toolWrapper instanceof LocalInspectionToolWrapper)) {
            HighlightDisplayKey idKey = HighlightDisplayKey.findById(key.toString());
            if (idKey != null) {
                toolWrapper = profile.getInspectionTool(idKey.toString(), element);
            }
        }

        if (toolWrapper != null) {
            myCanCleanup = toolWrapper.isCleanupTool();

            IntentionAction fixAllIntention = intentionManager.createFixAllIntention(toolWrapper, myAction);
            InspectionTool wrappedTool = toolWrapper instanceof LocalInspectionToolWrapper localInspectionToolWrapper
                ? localInspectionToolWrapper.getTool()
                : ((GlobalInspectionToolWrapper)toolWrapper).getTool();
            if (wrappedTool instanceof AnnotatorBasedInspection) {
                List<IntentionAction> actions = Collections.emptyList();
                if (myProblemGroup instanceof SuppressableProblemGroup suppressableProblemGroup) {
                    actions = Arrays.asList(suppressableProblemGroup.getSuppressActions(element));
                }
                if (fixAllIntention != null) {
                    if (actions.isEmpty()) {
                        return Collections.singletonList(fixAllIntention);
                    }
                    else {
                        actions = new ArrayList<>(actions);
                        actions.add(fixAllIntention);
                    }
                }
                return actions;
            }
            ContainerUtil.addIfNotNull(newOptions, fixAllIntention);
            if (wrappedTool instanceof CustomSuppressableInspectionTool customSuppressableInspectionTool) {
                IntentionAction[] suppressActions = customSuppressableInspectionTool.getSuppressActions(element);
                if (suppressActions != null) {
                    ContainerUtil.addAll(newOptions, suppressActions);
                }
            }
            else {
                SuppressQuickFix[] suppressFixes = wrappedTool.getBatchSuppressActions(element);
                if (suppressFixes.length > 0) {
                    newOptions.addAll(ContainerUtil.map(
                        suppressFixes,
                        SuppressIntentionActionFromFix::convertBatchToSuppressIntentionAction
                    ));
                }
            }

        }
        if (myProblemGroup instanceof SuppressableProblemGroup suppressableProblemGroup) {
            IntentionAction[] suppressActions = suppressableProblemGroup.getSuppressActions(element);
            ContainerUtil.addAll(newOptions, suppressActions);
        }

        //noinspection SynchronizeOnThis
        synchronized (this) {
            options = myOptions;
            if (options == null) {
                myOptions = options = newOptions;
            }
            myKey = null;
        }

        return options;
    }

    @Nullable
    public LocalizeValue getDisplayName() {
        return myDisplayName;
    }

    @Override
    public String toString() {
        LocalizeValue text = getAction().getText();
        return "descriptor: " + (text == LocalizeValue.of() ? getAction().getClass() : text);
    }

    @Nullable
    public Image getIcon() {
        return myIcon;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof IntentionActionDescriptor descriptor && myAction.equals(descriptor.myAction);
    }

    @Override
    public int hashCode() {
        return myAction.hashCode();
    }
}
