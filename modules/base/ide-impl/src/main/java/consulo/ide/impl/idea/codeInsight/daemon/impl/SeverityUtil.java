/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package consulo.ide.impl.idea.codeInsight.daemon.impl;

import consulo.colorScheme.EditorColorsManager;
import consulo.colorScheme.EditorColorsScheme;
import consulo.colorScheme.TextAttributes;
import consulo.language.editor.impl.internal.rawHighlight.SeverityRegistrarImpl;
import consulo.language.editor.rawHighlight.HighlightInfoType;

import consulo.logging.Logger;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Collection;

public class SeverityUtil {
  private static final Logger LOG = Logger.getInstance(SeverityUtil.class);
  @Nonnull
  public static Collection<SeverityRegistrarImpl.SeverityBasedTextAttributes> getRegisteredHighlightingInfoTypes(@Nonnull SeverityRegistrarImpl registrar) {
    Collection<SeverityRegistrarImpl.SeverityBasedTextAttributes> collection = registrar.allRegisteredAttributes();
    for (HighlightInfoType type : registrar.standardSeverities()) {
      SeverityRegistrarImpl.SeverityBasedTextAttributes attributes = getSeverityBasedTextAttributes(registrar, type);
      if (attributes != null) {
        collection.add(attributes);
      } else {
        LOG.error("Can't find severity for type: " + type);
      }
    }
    return collection;
  }

  @Nullable
  private static SeverityRegistrarImpl.SeverityBasedTextAttributes getSeverityBasedTextAttributes(@Nonnull SeverityRegistrarImpl registrar, @Nonnull HighlightInfoType type) {
    final EditorColorsScheme scheme = EditorColorsManager.getInstance().getGlobalScheme();
    final TextAttributes textAttributes = scheme.getAttributes(type.getAttributesKey());
    if (textAttributes != null) {
      return new SeverityRegistrarImpl.SeverityBasedTextAttributes(textAttributes, (HighlightInfoType.HighlightInfoTypeImpl)type);
    }
    
    TextAttributes severity = registrar.getTextAttributesBySeverity(type.getSeverity(null));
    if (severity == null) {
      return null;
    }
    return new SeverityRegistrarImpl.SeverityBasedTextAttributes(severity, (HighlightInfoType.HighlightInfoTypeImpl)type);
  }
}
