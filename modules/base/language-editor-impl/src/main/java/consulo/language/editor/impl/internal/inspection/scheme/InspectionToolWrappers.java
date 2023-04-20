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
package consulo.language.editor.impl.internal.inspection.scheme;

import consulo.component.extension.ExtensionPointCacheKey;
import consulo.language.editor.impl.inspection.scheme.GlobalInspectionToolWrapper;
import consulo.language.editor.impl.inspection.scheme.LocalInspectionToolWrapper;
import consulo.language.editor.inspection.GlobalInspectionTool;
import consulo.language.editor.inspection.InspectionsBundle;
import consulo.language.editor.inspection.LocalInspectionTool;
import consulo.language.editor.inspection.scheme.InspectionToolWrapper;
import consulo.logging.Logger;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

/**
 * @author VISTALL
 * @since 26/03/2023
 */
public class InspectionToolWrappers {
  private static final Logger LOG = Logger.getInstance(InspectionToolWrappers.class);

  public static final ExtensionPointCacheKey<GlobalInspectionTool, List<GlobalInspectionToolWrapper>> GLOBAL_WRAPPERS =
    ExtensionPointCacheKey.create("GLOBAL_WRAPPERS", walker -> {
      List<GlobalInspectionToolWrapper> wrappers = new ArrayList<>();
      walker.walk(tool -> wrappers.add(new GlobalInspectionToolWrapper(tool)));
      return wrappers;
    });

  public static final ExtensionPointCacheKey<LocalInspectionTool, List<LocalInspectionToolWrapper>> LOCAL_WRAPPERS =
    ExtensionPointCacheKey.create("LOCAL_WRAPPERS", walker -> {
      List<LocalInspectionToolWrapper> wrappers = new ArrayList<>();
      walker.walk(tool -> {
        LocalInspectionToolWrapper wrapper = new LocalInspectionToolWrapper(tool);
        String errorMessage = checkTool(wrapper);
        if (errorMessage != null) {
          return;
        }

        wrappers.add(wrapper);
      });
      return wrappers;
    });

  private static String checkTool(@Nonnull final InspectionToolWrapper toolWrapper) {
    if (!(toolWrapper instanceof LocalInspectionToolWrapper)) {
      return null;
    }
    String message = null;
    try {
      final String id = ((LocalInspectionToolWrapper)toolWrapper).getID();
      if (id == null || !LocalInspectionTool.isValidID(id)) {
        message =
          InspectionsBundle.message("inspection.disabled.wrong.id", toolWrapper.getShortName(), id, LocalInspectionTool.VALID_ID_PATTERN);
      }
    }
    catch (Throwable t) {
      message = InspectionsBundle.message("inspection.disabled.error", toolWrapper.getShortName(), t.getMessage());
    }
    if (message != null) {
      LOG.error(message);
    }
    return message;
  }
}
