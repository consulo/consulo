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

import com.intellij.openapi.application.PathMacroFilter;
import com.intellij.openapi.application.PathMacros;
import com.intellij.openapi.components.CompositePathMacroFilter;
import consulo.lombok.annotations.ApplicationService;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;
import java.util.regex.Pattern;

/**
 * @author VISTALL
 * @since 06-Jun-16
 */
@ApplicationService
public abstract class PathMacrosService {

  public static final Pattern MACRO_PATTERN = Pattern.compile("\\$([\\w\\-\\.]+?)\\$");

  @NotNull
  public Set<String> getMacroNames(@NotNull final Element e) {
    return getMacroNames(e, new CompositePathMacroFilter(PathMacroFilter.EP_NAME.getExtensions()), PathMacros.getInstance());
  }

  public abstract Set<String> getMacroNames(Element root, @Nullable PathMacroFilter filter, @NotNull final PathMacros pathMacros);
}
