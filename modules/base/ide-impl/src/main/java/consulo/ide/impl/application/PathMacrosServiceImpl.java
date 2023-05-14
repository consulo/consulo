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
import consulo.ide.impl.idea.application.options.PathMacrosCollectorImpl;
import consulo.component.macro.PathMacroFilter;
import consulo.application.macro.PathMacros;
import consulo.component.store.impl.internal.PathMacrosService;
import jakarta.inject.Singleton;
import org.jdom.Element;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Set;

/**
 * @author VISTALL
 * @since 06-Jun-16
 */
@Singleton
@ServiceImpl
public class PathMacrosServiceImpl extends PathMacrosService {
  @Override
  public Set<String> getMacroNames(Element root, @Nullable PathMacroFilter filter, @Nonnull PathMacros pathMacros) {
    return PathMacrosCollectorImpl.getMacroNames(root, filter, pathMacros);
  }
}
