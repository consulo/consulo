/*
 * Copyright 2000-2010 JetBrains s.r.o.
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
package consulo.ide.impl.idea.codeInspection.actions;

import consulo.ide.impl.idea.ide.util.gotoByName.SimpleChooseByNameModel;
import consulo.ide.localize.IdeLocalize;
import consulo.language.editor.impl.internal.inspection.scheme.InspectionProfileImpl;
import consulo.language.editor.inspection.scheme.InspectionProfileManager;
import consulo.language.editor.inspection.scheme.InspectionToolWrapper;
import consulo.language.editor.inspection.scheme.LocalInspectionToolWrapper;
import consulo.language.editor.internal.inspection.ScopeToolState;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.util.collection.ArrayUtil;

import javax.swing.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author Konstantin Bulenkov
 */
public class GotoInspectionModel extends SimpleChooseByNameModel {
    private static final Logger LOG = Logger.getInstance(GotoInspectionModel.class);

    private final Map<String, InspectionToolWrapper> myToolNames = new HashMap<>();
    private final Map<String, Set<InspectionToolWrapper>> myGroupNames = new HashMap<>();
    private final Map<String, InspectionToolWrapper> myToolShortNames = new HashMap<>();
    private final String[] myNames;
    private final ListCellRenderer myListCellRenderer = new InspectionListCellRenderer();

    public GotoInspectionModel(Project project) {
        super(project, IdeLocalize.promptGotoInspectionEnterName().get(), "goto.inspection.help.id");
        InspectionProfileImpl rootProfile = (InspectionProfileImpl) InspectionProfileManager.getInstance().getRootProfile();
        for (ScopeToolState state : rootProfile.getAllTools(project)) {
            try {
                InspectionToolWrapper tool = state.getTool();
                InspectionToolWrapper workingTool = tool;
                if (tool instanceof LocalInspectionToolWrapper) {
                    workingTool = LocalInspectionToolWrapper.findTool2RunInBatch(project, null, tool.getShortName());
                    if (workingTool == null) {
                        continue;
                    }
                }
                myToolNames.put(tool.getDisplayName(), workingTool);
                String groupName = tool.getJoinedGroupPath();
                Set<InspectionToolWrapper> toolsInGroup = myGroupNames.get(groupName);
                if (toolsInGroup == null) {
                    toolsInGroup = new HashSet<>();
                    myGroupNames.put(groupName, toolsInGroup);
                }
                toolsInGroup.add(workingTool);
                myToolShortNames.put(tool.getShortName(), workingTool);
            }
            catch (Throwable e) {
                LOG.error(e);
            }
        }

        Set<String> nameIds = new HashSet<>();
        nameIds.addAll(myToolNames.keySet());
        nameIds.addAll(myGroupNames.keySet());
        myNames = ArrayUtil.toStringArray(nameIds);
    }

    @Override
    public ListCellRenderer getListCellRenderer() {
        return myListCellRenderer;
    }

    @Override
    public String[] getNames() {
        return myNames;
    }

    @Override
    public Object[] getElementsByName(String id, String pattern) {
        Set<InspectionToolWrapper> result = new HashSet<>();
        InspectionToolWrapper e = myToolNames.get(id);
        if (e != null) {
            result.add(e);
        }
        e = myToolShortNames.get(id);
        if (e != null) {
            result.add(e);
        }
        Set<InspectionToolWrapper> entries = myGroupNames.get(id);
        if (entries != null) {
            result.addAll(entries);
        }
        return result.toArray(new InspectionToolWrapper[result.size()]);
    }

    @Override
    public String getElementName(Object element) {
        if (element instanceof InspectionToolWrapper) {
            InspectionToolWrapper entry = (InspectionToolWrapper) element;
            return entry.getDisplayName() + " " + entry.getJoinedGroupPath();
        }
        return null;
    }
}
