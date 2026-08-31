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
package consulo.ide.impl.base;

import consulo.annotation.component.ServiceImpl;
import consulo.configurable.Configurable;
import consulo.configurable.ConfigurableService;
import consulo.configurable.ConfigurableSession;
import consulo.configurable.internal.ConfigurableSessionHolder;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * @author VISTALL
 * @since 2026-05-03
 */
@ServiceImpl
@Singleton
public class ConfigurableServiceImpl implements ConfigurableService {
    private static final String PATH_SEPARATOR = " \uD83E\uDC92 ";

    @Override
    public LocalizeValue getDisplayConfigurablePath(String configurableId, @Nullable Project project) {
        return LocalizeValue.lazy(() -> {
            ConfigurableSession session = ConfigurableSessionHolder.getNullable();
            if (session != null) {
                return session.getDisplayConfigurablePath(configurableId);
            }

            return buildDisplayPath(configurableId, BaseShowSettingsUtil.buildConfigurables(project));
        });
    }

    public static LocalizeValue buildDisplayPath(String configurableId, Configurable[] configurables) {
        List<LocalizeValue> path = new ArrayList<>();
        if (!findConfigurablePath(configurableId, configurables, path)) {
            return LocalizeValue.of(configurableId);
        }

        return LocalizeValue.join(PATH_SEPARATOR, path.toArray(LocalizeValue[]::new));
    }

    private static boolean findConfigurablePath(String id, Configurable[] configurables, List<LocalizeValue> path) {
        for (Configurable c : configurables) {
            path.add(c.getDisplayName());

            if (id.equals(c.getId())) {
                return true;
            }

            if (c instanceof Configurable.Composite composite
                && findConfigurablePath(id, composite.getConfigurables(), path)) {
                return true;
            }

            path.removeLast();
        }
        return false;
    }
}
