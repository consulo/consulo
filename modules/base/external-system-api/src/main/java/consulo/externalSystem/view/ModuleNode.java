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
import consulo.externalSystem.model.project.ModuleData;
import consulo.externalSystem.util.Order;
import consulo.ui.ex.tree.PresentationData;
import consulo.util.collection.ContainerUtil;
import consulo.util.lang.StringUtil;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Tree node representing an external project module (sub-project).
 *
 * @author Vladislav.Soroka
 */
@Order(ExternalSystemNode.BUILTIN_MODULE_DATA_NODE_ORDER)
public class ModuleNode extends ExternalSystemNode<ModuleData> {
    private final boolean myIsRoot;
    private final ModuleData myData;
    private Collection<ModuleNode> myAllModules = Collections.emptyList();
    private final RunConfigurationsNode myRunConfigurationsNode;

    public ModuleNode(ExternalProjectsView externalProjectsView,
                      DataNode<ModuleData> dataNode,
                      @Nullable ExternalSystemNode<?> parent,
                      boolean isRoot) {
        super(externalProjectsView, parent, dataNode);
        myIsRoot = isRoot;
        myData = dataNode.getData();
        myRunConfigurationsNode = new RunConfigurationsNode(externalProjectsView, this);
    }

    public void setAllModules(Collection<ModuleNode> allModules) {
        myAllModules = allModules;
    }

    public boolean isRoot() {
        return myIsRoot;
    }

    @Override
    protected void update(PresentationData presentation) {
        super.update(presentation);
        presentation.setIcon(getExternalProjectsView().getSystemId().getModuleIcon());
        String hint = myIsRoot ? "root" : null;
        String tooltip = myData.toString();
        setNameAndTooltip(presentation, getName(), tooltip, hint);
    }

    @Override

    protected List<ExternalSystemNode<?>> doBuildChildren() {
        List<ExternalSystemNode<?>> myChildNodes = new ArrayList<>();
        if (getExternalProjectsView().getGroupModules()) {
            List<ModuleNode> childModules = ContainerUtil.findAll(
                myAllModules,
                module -> module != this && StringUtil.equals(module.getIdeParentGrouping(), getIdeGrouping())
            );
            myChildNodes.addAll(childModules);
        }
        //noinspection unchecked
        myChildNodes.addAll(super.doBuildChildren());
        myChildNodes.add(myRunConfigurationsNode);
        return myChildNodes;
    }

    @Override
    @Nullable
    protected String getMenuId() {
        return "ExternalSystemView.ModuleMenu";
    }

    @Override
    public int compareTo(ExternalSystemNode<?> node) {
        return myIsRoot ? -1 : (node instanceof ModuleNode && ((ModuleNode) node).myIsRoot) ? 1 : super.compareTo(node);
    }

    @Override
    public String getName() {
        if (getExternalProjectsView().getGroupModules()) {
            return myData.getExternalName();
        }
        return super.getName();
    }

    @Nullable
    public String getIdeGrouping() {
        ModuleData data = getData();
        return data != null ? data.getIdeGrouping() : null;
    }

    @Nullable
    public String getIdeParentGrouping() {
        ModuleData data = getData();
        return data != null ? data.getIdeParentGrouping() : null;
    }

    public void updateRunConfigurations() {
        myRunConfigurationsNode.updateRunConfigurations();
        childrenChanged();
        getExternalProjectsView().updateUpTo(this);
        getExternalProjectsView().updateUpTo(myRunConfigurationsNode);
    }

    @Override
    public void mergeWith(ExternalSystemNode<ModuleData> node) {
        super.mergeWith(node);
        if (node instanceof ModuleNode moduleNode) {
            myAllModules = moduleNode.myAllModules;
        }
    }
}
