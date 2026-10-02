// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.endpoint;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.TopicAPI;
import consulo.project.Project;

@TopicAPI(ComponentScope.PROJECT)
public interface EndpointViewListener {
    enum ChangeType {
        FLUSH,
        PROVIDERS,
        ITEMS,
        VIEW
    }

    final class ChangeEvent {
        private final Project myProject;
        private final ChangeType myType;

        public ChangeEvent(Project project, ChangeType type) {
            myProject = project;
            myType = type;
        }

        public Project getProject() {
            return myProject;
        }

        public ChangeType getType() {
            return myType;
        }
    }

    void endpointsChanged(ChangeEvent e);
}
