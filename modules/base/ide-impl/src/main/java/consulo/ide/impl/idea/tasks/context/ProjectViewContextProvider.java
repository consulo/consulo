/*
 * Copyright 2000-2009 JetBrains s.r.o.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package consulo.ide.impl.idea.tasks.context;

import consulo.annotation.component.ExtensionImpl;
import consulo.ide.impl.idea.ide.projectView.impl.AbstractProjectViewPane;
import consulo.project.Project;
import consulo.task.context.WorkingContextProvider;
import consulo.util.xml.serializer.InvalidDataException;
import consulo.util.xml.serializer.WriteExternalException;
import consulo.ui.ex.awt.tree.TreeUtil;
import jakarta.inject.Inject;
import org.jdom.Element;

import jakarta.annotation.Nonnull;

import javax.swing.*;
import java.util.List;

/**
 * @author Dmitry Avdeev
 */
@ExtensionImpl
public class ProjectViewContextProvider extends WorkingContextProvider {
    private final List<AbstractProjectViewPane> myPanes;

    @Inject
    public ProjectViewContextProvider(Project project) {
        myPanes = AbstractProjectViewPane.EP_NAME.getExtensionList(project);
    }

    @Nonnull
    @Override
    public String getId() {
        return "projectView";
    }

    @Nonnull
    @Override
    public String getDescription() {
        return "Project view state";
    }

    public void saveContext(Element toElement) throws WriteExternalException {
        for (AbstractProjectViewPane pane : myPanes) {
            Element paneElement = new Element(pane.getId());
            pane.writeExternal(paneElement);
            toElement.addContent(paneElement);
        }
    }

    public void loadContext(Element fromElement) throws InvalidDataException {
        for (AbstractProjectViewPane pane : myPanes) {
            Element paneElement = fromElement.getChild(pane.getId());
            if (paneElement != null) {
                pane.readExternal(paneElement);
                if (pane.getTree() != null) {
                    pane.restoreExpandedPaths();
                }
            }
        }
    }

    public void clearContext() {
        for (AbstractProjectViewPane pane : myPanes) {
            JTree tree = pane.getTree();
            if (tree != null) {
                TreeUtil.collapseAll(tree, -1);
            }
        }
    }
}
