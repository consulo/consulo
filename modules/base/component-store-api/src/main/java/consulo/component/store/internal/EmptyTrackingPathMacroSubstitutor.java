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
package consulo.component.store.internal;

import org.jdom.Element;

import java.util.Collection;
import java.util.Set;

/**
 * @author VISTALL
 * @since 2026-04-25
 */
public class EmptyTrackingPathMacroSubstitutor implements TrackingPathMacroSubstitutor {
    public static final EmptyTrackingPathMacroSubstitutor INSTANCE = new EmptyTrackingPathMacroSubstitutor();

    @Override
    public Set<String> getUnknownMacros(String componentName) {
        return Set.of();
    }

    @Override
    public Set<String> getComponents(Collection<String> macros) {
        return Set.of();
    }

    @Override
    public void addUnknownMacros(String componentName, Collection<String> unknownMacros) {

    }

    @Override
    public void invalidateUnknownMacros(Set<String> macros) {

    }

    @Override
    public void reset() {

    }

    @Override
    public String expandPath(String path) {
        return path;
    }

    @Override
    public String collapsePath(String path) {
        return path;
    }

    @Override
    public void expandPaths(Element element) {

    }

    @Override
    public void collapsePaths(Element element) {

    }
}
