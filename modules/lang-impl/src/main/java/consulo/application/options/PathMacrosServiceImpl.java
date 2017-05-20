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
package consulo.application.options;

import com.intellij.application.options.PathMacrosCollectorImpl;
import com.intellij.openapi.application.PathMacroFilter;
import com.intellij.openapi.application.PathMacros;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

/**
 * @author VISTALL
 * @since 06-Jun-16
 */
public class PathMacrosServiceImpl extends PathMacrosService {
  @Override
  public Set<String> getMacroNames(Element root, @Nullable PathMacroFilter filter, @NotNull PathMacros pathMacros) {
    return PathMacrosCollectorImpl.getMacroNames(root, filter, pathMacros);
  }
}
