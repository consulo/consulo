/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package consulo.ide.impl.idea.openapi.application;

import consulo.annotation.component.ExtensionImpl;
import consulo.component.macro.PathMacroFilter;
import org.jdom.Attribute;

/**
 * Since SSR inspections can be stored in inspection profiles and loaded by users who don't have the SSR plugin installed, unfortunately
 * this must be in the platform and not in the SSR plugin.
 *
 * @author yole
 */
@ExtensionImpl
public class StructuralSearchPathMacroFilter extends PathMacroFilter {
  @Override
  public boolean skipPathMacros(Attribute attribute) {
    final String parentName = attribute.getParent().getName();
    if ("replaceConfiguration".equals(parentName) || "searchConfiguration".equals(parentName)) {
      return true;
    }
    return false;
  }
}
