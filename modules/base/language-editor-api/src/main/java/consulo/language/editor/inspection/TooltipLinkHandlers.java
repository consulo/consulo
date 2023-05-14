/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package consulo.language.editor.inspection;

import consulo.application.Application;
import consulo.codeEditor.Editor;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author peter
 */
public class TooltipLinkHandlers {
  public static boolean handleLink(@Nonnull final String ref, @Nonnull final Editor editor) {
    for (final TooltipLinkHandler handler : Application.get().getExtensionList(TooltipLinkHandler.class)) {
      if (ref.startsWith(handler.getPrefix())) {
        final String refSuffix = ref.substring(handler.getPrefix().length());
        return handler.handleLink(refSuffix.replaceAll("<br/>", "\n"), editor);
      }
    }
    return false;
  }

  @Nullable
  public static String getDescription(@Nonnull final String ref, @Nonnull final Editor editor) {
    for (final TooltipLinkHandler handler : Application.get().getExtensionList(TooltipLinkHandler.class)) {
      if (ref.startsWith(handler.getPrefix())) {
        final String refSuffix = ref.substring(handler.getPrefix().length());
        return handler.getDescription(refSuffix, editor);
      }
    }
    return null;
  }

  @Nonnull
  public static String getDescriptionTitle(@Nonnull String ref, @Nonnull Editor editor) {
    for (final TooltipLinkHandler handler : Application.get().getExtensionList(TooltipLinkHandler.class)) {
      if (ref.startsWith(handler.getPrefix())) {
        String refSuffix = ref.substring(handler.getPrefix().length());
        return handler.getDescriptionTitle(refSuffix, editor);
      }
    }
    return TooltipLinkHandler.INSPECTION_INFO;
  }
}
