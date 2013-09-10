/*
 * Copyright 2013 Consulo.org
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
package org.consulo.module.extension.condition;

import com.intellij.openapi.util.text.StringUtil;
import org.consulo.module.extension.ModuleExtension;
import org.consulo.module.extension.ModuleExtensionProvider;
import org.consulo.module.extension.ModuleExtensionProviderEP;

import java.util.ArrayList;
import java.util.List;

/**
 * @author VISTALL
 * @since 18:19/10.09.13
 */
@SuppressWarnings("unchecked")
public class ModuleExtensionConvertUtil {
  private static final Class<? extends ModuleExtension<?>>[] EMPTY_CLASS_ARRAY = new Class[0];

  public static Class<? extends ModuleExtension<?>>[] toModuleExtensionClassArray(String ids) {
    List<String> split = StringUtil.split(ids, ",");

    List<Class<? extends ModuleExtension<?>>> list = new ArrayList<Class<? extends ModuleExtension<?>>>();

    for (String id : split) {
      ModuleExtensionProvider provider = ModuleExtensionProviderEP.findProvider(id);
      if(provider != null) {
        list.add(provider.getImmutableClass());
      }
    }

    return list.isEmpty() ? EMPTY_CLASS_ARRAY : list.toArray(new Class[list.size()]);
  }
}
