/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package consulo.ui.ex.awt.tree;

import consulo.annotation.DeprecationInfo;
import consulo.dataContext.DataManager;
import consulo.project.Project;
import consulo.ui.ex.tree.NodeDescriptor;
import org.jspecify.annotations.Nullable;

import java.awt.event.InputEvent;

@Deprecated
@DeprecationInfo("Use consulo.ui.ex.tree.SimpleNode")
public abstract class SimpleNode extends consulo.ui.ex.tree.SimpleNode {
    protected static final SimpleNode[] NO_CHILDREN = new SimpleNode[0];

    protected SimpleNode(SimpleNode parent) {
        super(parent);
    }

    protected SimpleNode(Project project) {
        super((NodeDescriptor) null);
    }

    protected SimpleNode(Project project, @Nullable NodeDescriptor parentDescriptor) {
        super(parentDescriptor);
    }

    protected SimpleNode() {
        super();
    }

    @Override
    public SimpleNode getParent() {
        return (SimpleNode) getParentDescriptor();
    }

    @Override
    public abstract SimpleNode[] getChildren();

    @Override
    public SimpleNode getChildAt(int i) {
        return getChildren()[i];
    }

    public void handleSelection(SimpleTree tree) {
    }

    public void handleDoubleClickOrEnter(SimpleTree tree, InputEvent inputEvent) {
        handleDoubleClickOrEnter(DataManager.getInstance().getDataContext(tree), null);
    }
}
