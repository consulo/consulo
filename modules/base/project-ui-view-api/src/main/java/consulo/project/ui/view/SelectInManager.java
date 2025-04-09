/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package consulo.project.ui.view;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.project.DumbService;
import consulo.project.Project;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import jakarta.annotation.Nonnull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

@Singleton
@ServiceAPI(ComponentScope.PROJECT)
@ServiceImpl
public class SelectInManager {
    public static class SelectInTargetComparator implements Comparator<SelectInTarget> {
        public static final SelectInTargetComparator INSTANCE = new SelectInTargetComparator();

        @Override
        public int compare(SelectInTarget o1, SelectInTarget o2) {
            if (o1.getWeight() < o2.getWeight()) {
                return -1;
            }
            if (o1.getWeight() > o2.getWeight()) {
                return 1;
            }
            return 0;
        }
    }

    public static final String PROJECT = ProjectUIViewBundle.message("select.in.project");
    public static final String PACKAGES = ProjectUIViewBundle.message("select.in.packages");
    public static final String ASPECTS = ProjectUIViewBundle.message("select.in.aspects");
    public static final String COMMANDER = ProjectUIViewBundle.message("select.in.commander");
    public static final String NAV_BAR = ProjectUIViewBundle.message("select.in.nav.bar");
    public static final String SCOPE = ProjectUIViewBundle.message("select.in.scope");

    public static SelectInManager getInstance(Project project) {
        return project.getInstance(SelectInManager.class);
    }

    private final Project myProject;

    @Inject
    public SelectInManager(Project project) {
        myProject = project;
    }

    @Nonnull
    public List<SelectInTarget> getTargets() {
        List<SelectInTarget> targets = DumbService.getDumbAwareExtensions(myProject, myProject, SelectInTarget.EP_NAME);
        if (targets.isEmpty()) {
            return Collections.emptyList();
        }

        targets = new ArrayList<>(targets);
        targets.sort(SelectInTargetComparator.INSTANCE);
        return targets;
    }
}
