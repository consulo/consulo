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

import consulo.application.macro.PathMacros;
import consulo.component.macro.PathMacroFilter;
import org.jdom.Element;

import java.util.HashSet;
import java.util.Set;

/**
 * @author VISTALL
 * @since 2026-04-26
 */
public class SystemOnlyPathMacrosService extends PathMacrosService {
    private final SystemOnlyPathMacros myMacros;

    public SystemOnlyPathMacrosService(SystemOnlyPathMacros macros) {
        myMacros = macros;
    }

    @Override
    public PathMacros getPathMacros() {
        return myMacros;
    }

    @Override
    public Set<String> getMacroNames(Element root, PathMacroFilter filter, PathMacros pathMacros) {
        PathMacrosCollectorImpl collector = new PathMacrosCollectorImpl();
        collector.substitute(root, true, false, filter);
        HashSet<String> result = new HashSet<>(collector.getMacroMap().keySet());

        result.removeAll(pathMacros.getSystemMacroNames());

        return result;
    }
}
