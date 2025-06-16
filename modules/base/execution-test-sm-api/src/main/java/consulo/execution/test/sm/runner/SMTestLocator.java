/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package consulo.execution.test.sm.runner;

import consulo.application.dumb.DumbAware;
import consulo.execution.action.Location;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.project.DumbService;
import consulo.project.Project;
import consulo.util.lang.Pair;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A parser for location URLs reported by test runners.
 * See {@link SMTestProxy#getLocation(Project, GlobalSearchScope)} for details.
 */
public interface SMTestLocator {
    /**
     * Creates the <code>Location</code> list from <code>protocol</code> and <code>path</code> in <code>scope</code>.
     */
    @Nonnull
    List<Location> getLocation(@Nonnull String protocol, @Nonnull String path, @Nonnull Project project, @Nonnull GlobalSearchScope scope);

    /**
     * Creates the <code>Location</code> list from <code>protocol</code>, <code>path</code>, and <code>metainfo</code> in <code>scope</code>.
     * Implementation of test framework can provide additional information in <code>metainfo</code> parameter,
     * The <code>metainfo</code> parameter simplifies the search for locations, but can not be used to identify the test.
     * A good example for code>metainfo</code> is the line number of the beginning of the test. It can speed up the search procedure,
     * but it changes when editing.
     */
    @Nonnull
    default List<Location> getLocation(
        @Nonnull String protocol,
        @Nonnull String path,
        @Nullable String metainfo,
        @Nonnull Project project,
        @Nonnull GlobalSearchScope scope
    ) {
        return getLocation(protocol, path, project, scope);
    }

    /**
     * @deprecated consoles should provide specific locators; the implementation is trivial (to be removed in IDEA 18)
     */
    class Composite implements SMTestLocator, DumbAware {
        private final Map<String, SMTestLocator> myLocators;

        public Composite(@Nonnull Pair<String, ? extends SMTestLocator> first, @Nonnull Pair<String, ? extends SMTestLocator>... rest) {
            myLocators = new HashMap<>();
            myLocators.put(first.getFirst(), first.getSecond());
            for (Pair<String, ? extends SMTestLocator> pair : rest) {
                myLocators.put(pair.getFirst(), pair.getSecond());
            }
        }

        public Composite(@Nonnull Map<String, ? extends SMTestLocator> locators) {
            myLocators = new HashMap<>(locators);
        }

        @Nonnull
        @Override
        public List<Location> getLocation(
            @Nonnull String protocol,
            @Nonnull String path,
            @Nonnull Project project,
            @Nonnull GlobalSearchScope scope
        ) {
            SMTestLocator locator = myLocators.get(protocol);

            if (locator != null && (!DumbService.isDumb(project) || DumbService.isDumbAware(locator))) {
                return locator.getLocation(protocol, path, project, scope);
            }

            return Collections.emptyList();
        }
    }
}