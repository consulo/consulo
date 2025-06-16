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
package consulo.execution.test.sm.ui;

import consulo.execution.test.AbstractTestProxy;
import consulo.execution.test.TestTreeViewStructure;
import consulo.execution.test.sm.runner.SMTestProxy;
import consulo.project.Project;
import consulo.ui.ex.tree.NodeDescriptor;

import jakarta.annotation.Nonnull;

import java.util.List;

/**
 * @author: Roman Chernyatchik
 */
public class SMTRunnerTreeStructure extends TestTreeViewStructure<SMTestProxy> {
    private final Object myRootNode;
    private final Project myProject;

    public SMTRunnerTreeStructure(final Project project, final Object rootNode) {
        myProject = project;
        myRootNode = rootNode;
    }

    @Override
    public void commit() {
    }

    @Override
    public boolean hasSomethingToCommit() {
        return false;
    }

    @Nonnull
    @Override
    public SMTRunnerNodeDescriptor createDescriptor(final Object element, final NodeDescriptor parentDesc) {
        //noinspection unchecked
        return new SMTRunnerNodeDescriptor(myProject, (SMTestProxy) element, (NodeDescriptor<SMTestProxy>) parentDesc);
    }

    @Override
    public Object[] getChildElements(final Object element) {
        final List<? extends SMTestProxy> results = ((SMTestProxy) element).getChildren(getFilter());

        return results.toArray(new AbstractTestProxy[results.size()]);
    }

    @Override
    public Object getParentElement(final Object element) {
        return ((AbstractTestProxy) element).getParent();
    }


    @Override
    public Object getRootElement() {
        return myRootNode;
    }
}
