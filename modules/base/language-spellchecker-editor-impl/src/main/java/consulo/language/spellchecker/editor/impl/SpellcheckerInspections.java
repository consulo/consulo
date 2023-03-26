/*
 * Copyright 2013-2023 consulo.io
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
package consulo.language.spellchecker.editor.impl;

import consulo.application.Application;
import consulo.component.extension.ExtensionPointCacheKey;
import consulo.language.editor.inspection.LocalInspectionTool;
import consulo.language.spellchecker.editor.inspection.SpellcheckerInspection;
import consulo.util.collection.MultiMap;

/**
 * @author VISTALL
 * @since 26/03/2023
 */
public class SpellcheckerInspections {
  public static final ExtensionPointCacheKey<LocalInspectionTool, MultiMap<String, String>> MAPPING =
    ExtensionPointCacheKey.create("TOOLS_MAPPING", localInspectionTools -> {
      MultiMap<String, String> multiMap = new MultiMap<>();

      for (LocalInspectionTool tool : localInspectionTools) {
        if (tool instanceof SpellcheckerInspection spellcheckerInspection) {
          multiMap.putValue(spellcheckerInspection.getSpellcheckerEngineId(), spellcheckerInspection.getShortName());
        }
      }

      return multiMap;
    });

  public static boolean isOwneedSpellcheckerInspection(String spellcheckerEngineId, String toolId) {
    MultiMap<String, String> map = Application.get().getExtensionPoint(LocalInspectionTool.class).getOrBuildCache(MAPPING);
    return map.get(spellcheckerEngineId).contains(toolId);
  }
}
