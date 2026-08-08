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
package consulo.ai.impl.internal;

import consulo.ai.AITool;
import consulo.ai.AIToolManager;
import consulo.ai.AIToolProvider;
import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.logging.Logger;
import consulo.project.Project;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * @author VISTALL
 * @since 2026-08-04
 */
@Singleton
@ServiceImpl
public class AIToolManagerImpl implements AIToolManager {
    private static final Logger LOG = Logger.getInstance(AIToolManagerImpl.class);

    private final Application myApplication;

    @Inject
    public AIToolManagerImpl(Application application) {
        myApplication = application;
    }

    @Override
    public List<AITool> getTools(Project project) {
        List<AITool> tools = new ArrayList<>();
        Set<String> names = new LinkedHashSet<>();

        myApplication.getExtensionPoint(AIToolProvider.class).forEach(provider -> {
            try {
                for (AITool tool : provider.getTools(project)) {
                    // a model cannot disambiguate two tools with the same name, so the first one wins
                    if (names.add(tool.getName())) {
                        tools.add(tool);
                    }
                    else {
                        LOG.warn("Duplicate AI tool name '" + tool.getName() + "' from " + provider.getClass().getName());
                    }
                }
            }
            catch (Throwable e) {
                LOG.error("Failed to collect AI tools of " + provider.getClass().getName(), e);
            }
        });

        return tools;
    }

    @Override
    public @Nullable AITool findTool(Project project, String name) {
        for (AITool tool : getTools(project)) {
            if (tool.getName().equals(name)) {
                return tool;
            }
        }
        return null;
    }
}
