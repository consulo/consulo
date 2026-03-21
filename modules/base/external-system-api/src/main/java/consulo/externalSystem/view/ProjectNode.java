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
package consulo.externalSystem.view;

import consulo.externalSystem.model.DataNode;
import consulo.externalSystem.service.project.ProjectData;
import consulo.ui.ex.tree.PresentationData;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Tree node representing a top-level external project.
 *
 * @author Vladislav.Soroka
 */
public class ProjectNode extends ExternalSystemNode<ProjectData> {
    private String myTooltipCache;

    public ProjectNode(ExternalProjectsView externalProjectsView,
                       DataNode<ProjectData> projectDataNode) {
        super(externalProjectsView, null, projectDataNode);
        updateProject();
    }

    @Override
    protected void update(PresentationData presentation) {
        super.update(presentation);
        presentation.setIcon(getExternalProjectsView().getSystemId().getIcon());
        setNameAndTooltip(presentation, getName(), myTooltipCache);
    }

    @Nullable
    public ExternalSystemNode<?> getGroup() {
        return (ExternalSystemNode<?>) getParent();
    }

    @Override
   
    protected List<? extends ExternalSystemNode<?>> doBuildChildren() {
        final List<? extends ExternalSystemNode<?>> children = super.doBuildChildren();
        if (getExternalProjectsView().getGroupModules()) {
            List<ExternalSystemNode<?>> topLevel = new java.util.ArrayList<>();
            for (ExternalSystemNode<?> node : children) {
                if (!(node instanceof ModuleNode) || ((ModuleNode) node).getIdeParentGrouping() == null) {
                    topLevel.add(node);
                }
            }
            if (topLevel.size() == 1 && topLevel.get(0) instanceof ModuleNode) {
                return ((ModuleNode) topLevel.get(0)).doBuildChildren();
            }
            return topLevel;
        }
        return children;
    }

    void updateProject() {
        myTooltipCache = makeDescription();
        ExternalProjectsStructure structure = getStructure();
        if (structure != null) {
            structure.updateFrom(getParent() instanceof consulo.ui.ex.awt.tree.SimpleNode p ? p : null);
        }
    }

    private String makeDescription() {
        ProjectData projectData = getData();
        StringBuilder desc = new StringBuilder();
        desc.append("Project: ").append(getName());
        if (projectData != null) {
            desc.append("\n\rLocation: ").append(projectData.getLinkedExternalProjectPath());
        }
        return desc.toString();
    }

    @Override
    @Nullable
    protected String getMenuId() {
        return "ExternalSystemView.ProjectMenu";
    }
}
