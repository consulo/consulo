/*
 * Copyright 2013-2026 consulo.io
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
package consulo.continuousIntegration.impl.internal.toolWindow;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.component.persist.PersistentStateComponent;
import consulo.component.persist.State;
import consulo.component.persist.Storage;
import consulo.component.persist.StoragePathMacros;
import consulo.continuousIntegration.CIBuildStatus;
import consulo.project.Project;
import jakarta.inject.Singleton;
import org.jdom.Element;

import java.util.EnumSet;
import java.util.Set;

/**
 * View options of the CI tool window: the statuses of the jobs it hides.
 *
 * @since 2026-11-14
 */
@Singleton
@State(name = "ContinuousIntegrationToolWindow", storages = @Storage(StoragePathMacros.WORKSPACE_FILE))
@ServiceAPI(ComponentScope.PROJECT)
@ServiceImpl
public class CIToolWindowSettings implements PersistentStateComponent<Element> {
    private static final String HIDDEN = "hidden";
    private static final String STATUS = "status";

    private final Set<CIBuildStatus> myHiddenStatuses = EnumSet.noneOf(CIBuildStatus.class);

    public static CIToolWindowSettings getInstance(Project project) {
        return project.getInstance(CIToolWindowSettings.class);
    }

    public boolean isHidden(CIBuildStatus status) {
        return myHiddenStatuses.contains(status);
    }

    public void setHidden(CIBuildStatus status, boolean hidden) {
        if (hidden) {
            myHiddenStatuses.add(status);
        }
        else {
            myHiddenStatuses.remove(status);
        }
    }

    public boolean isFiltering() {
        return !myHiddenStatuses.isEmpty();
    }

    @Override
    public Element getState() {
        Element state = new Element("state");
        for (CIBuildStatus status : myHiddenStatuses) {
            state.addContent(new Element(HIDDEN).setAttribute(STATUS, status.name()));
        }
        return state;
    }

    @Override
    public void loadState(Element state) {
        myHiddenStatuses.clear();
        for (Element hidden : state.getChildren(HIDDEN)) {
            String name = hidden.getAttributeValue(STATUS);
            for (CIBuildStatus status : CIBuildStatus.values()) {
                if (status.name().equals(name)) {
                    myHiddenStatuses.add(status);
                }
            }
        }
    }
}
