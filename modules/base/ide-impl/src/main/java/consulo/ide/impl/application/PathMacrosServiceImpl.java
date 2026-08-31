/*
 * Copyright 2013-2016 consulo.io
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
package consulo.ide.impl.application;

import consulo.annotation.component.ServiceImpl;
import consulo.application.macro.PathMacros;
import consulo.component.macro.PathMacroFilter;
import consulo.component.store.internal.PathMacrosService;
import consulo.component.store.internal.PathMacrosCollectorImpl;
import consulo.pathMacro.Macro;
import consulo.pathMacro.MacroManager;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;
import org.jdom.Element;
import org.jspecify.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;

/**
 * @author VISTALL
 * @since 06-Jun-16
 */
@Singleton
@ServiceImpl
public class PathMacrosServiceImpl extends PathMacrosService {
    private final Provider<PathMacros> myPathMacros;

    @Inject
    public PathMacrosServiceImpl(Provider<PathMacros> pathMacros) {
        myPathMacros = pathMacros;
    }

    @Override
    public Set<String> getMacroNames(Element root, @Nullable PathMacroFilter filter, PathMacros pathMacros) {
        return getMacroNamesImpl(root, filter, pathMacros);
    }

    @Override
    public PathMacros getPathMacros() {
        return myPathMacros.get();
    }

    public static Set<String> getMacroNamesImpl(Element root, @Nullable PathMacroFilter filter, PathMacros pathMacros) {
        PathMacrosCollectorImpl collector = new PathMacrosCollectorImpl();
        collector.substitute(root, true, false, filter);
        HashSet<String> result = new HashSet<>(collector.getMacroMap().keySet());
        result.removeAll(pathMacros.getSystemMacroNames());
        for (Macro macro : MacroManager.getInstance().getMacros()) {
            result.remove(macro.getName());
        }
        result.removeAll(pathMacros.getIgnoredMacroNames());
        return result;
    }
}
