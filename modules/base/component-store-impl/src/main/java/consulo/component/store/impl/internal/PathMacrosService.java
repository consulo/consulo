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
package consulo.component.store.impl.internal;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.application.macro.PathMacros;
import consulo.component.macro.CompositePathMacroFilter;
import consulo.component.macro.PathMacroFilter;
import org.jdom.Element;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * @author VISTALL
 * @since 06-Jun-16
 */
@ServiceAPI(ComponentScope.APPLICATION)
public abstract class PathMacrosService {
  public static final Pattern MACRO_PATTERN = Pattern.compile("\\$([\\w\\-\\.]+?)\\$");

  @Nonnull
  public Set<String> getMacroNames(@Nonnull final Element e) {
    return getMacroNames(e, new CompositePathMacroFilter(PathMacroFilter.EP_NAME.getExtensionList()), PathMacros.getInstance());
  }

  public abstract Set<String> getMacroNames(Element root, @Nullable PathMacroFilter filter, @Nonnull final PathMacros pathMacros);
}
