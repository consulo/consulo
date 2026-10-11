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
package consulo.ide.impl.idea.codeInspection.ex;

import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.application.util.NotNullLazyValue;
import consulo.language.Language;
import consulo.language.editor.impl.internal.inspection.InspectionManagerBase;
import consulo.language.editor.inspection.*;
import consulo.language.editor.inspection.scheme.InspectionToolWrapper;
import consulo.language.editor.intention.SuppressIntentionAction;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.project.ui.wm.ToolWindowId;
import consulo.project.ui.wm.ToolWindowManager;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.content.ContentManager;
import consulo.ui.ex.localize.UILocalize;
import consulo.ui.ex.toolWindow.ContentManagerWatcher;
import consulo.ui.ex.toolWindow.ToolWindow;
import consulo.ui.ex.toolWindow.ToolWindowAnchor;
import consulo.util.collection.ContainerUtil;
import org.jspecify.annotations.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.util.*;

/**
 * @author max
 * @since 2001-10-09
 */
@Singleton
@ServiceImpl
public class InspectionManagerImpl extends InspectionManagerBase {
    private final NotNullLazyValue<ContentManager> myContentManager;
    private final Set<GlobalInspectionContextImpl> myRunningContexts = new HashSet<>();
    private GlobalInspectionContextImpl myGlobalInspectionContext;

    @Inject
    public InspectionManagerImpl(final Project project) {
        super(project);
        
        myContentManager = new NotNullLazyValue<>() {
            
            @Override
            @RequiredUIAccess
            protected ContentManager compute() {
                ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(project);
                ToolWindow toolWindow =
                    toolWindowManager.registerToolWindow(ToolWindowId.INSPECTION, true, ToolWindowAnchor.BOTTOM, project, true);
                ContentManager contentManager = toolWindow.getContentManager();
                toolWindow.setDisplayName(UILocalize.toolWindowNameInspection());
                toolWindow.setIcon(PlatformIconGroup.toolwindowsProblems());
                ContentManagerWatcher.watchContentManager(toolWindow, contentManager);
                return contentManager;
            }
        };
    }

    public static @Nullable SuppressIntentionAction[] getSuppressActions(InspectionToolWrapper toolWrapper) {
        InspectionTool tool = toolWrapper.getTool();
        if (tool instanceof CustomSuppressableInspectionTool customSuppressableInspectionTool) {
            return customSuppressableInspectionTool.getSuppressActions(null);
        }
        List<LocalQuickFix> actions = new ArrayList<>(Arrays.asList(tool.getBatchSuppressActions(null)));
        if (actions.isEmpty()) {
            Language language = toolWrapper.getLanguage();
            if (language != null) {
                List<InspectionSuppressor> suppressors = InspectionSuppressor.forLanguage(language);
                for (InspectionSuppressor suppressor : suppressors) {
                    SuppressQuickFix[] suppressActions = suppressor.getSuppressActions(null, tool.getShortName());
                    Collections.addAll(actions, suppressActions);
                }
            }
        }
        return ContainerUtil.map2Array(
            actions,
            SuppressIntentionAction.class,
            fix -> SuppressIntentionActionFromFix.convertBatchToSuppressIntentionAction((SuppressQuickFix)fix)
        );
    }

    @Override
    public GlobalInspectionContextImpl createNewGlobalContext(boolean reuse) {
        GlobalInspectionContextImpl inspectionContext;
        if (reuse) {
            if (myGlobalInspectionContext == null) {
                myGlobalInspectionContext = inspectionContext = new GlobalInspectionContextImpl(getProject(), myContentManager);
            }
            else {
                inspectionContext = myGlobalInspectionContext;
            }
        }
        else {
            inspectionContext = new GlobalInspectionContextImpl(getProject(), myContentManager);
        }
        myRunningContexts.add(inspectionContext);
        return inspectionContext;
    }

    public void setProfile(String name) {
        myCurrentProfileName = name;
    }

    public void closeRunningContext(GlobalInspectionContextImpl globalInspectionContext) {
        myRunningContexts.remove(globalInspectionContext);
    }

    public Set<GlobalInspectionContextImpl> getRunningContexts() {
        return myRunningContexts;
    }
}
