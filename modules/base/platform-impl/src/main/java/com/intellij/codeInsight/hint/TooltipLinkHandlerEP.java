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
package com.intellij.codeInsight.hint;

import com.intellij.codeInsight.highlighting.TooltipLinkHandler;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.extensions.AbstractExtensionPointBean;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.util.LazyInstance;
import com.intellij.util.xmlb.annotations.Attribute;
import javax.annotation.Nonnull;

import javax.annotation.Nullable;

/**
 * @author peter
 */
public class TooltipLinkHandlerEP extends AbstractExtensionPointBean {
  public static final ExtensionPointName<TooltipLinkHandlerEP> EP_NAME = ExtensionPointName.create("com.intellij.codeInsight.linkHandler");

  @Attribute("prefix")
  public String prefix;

  @Attribute("handlerClass")
  public String handlerClassName;

  private final LazyInstance<TooltipLinkHandler> myHandler = new LazyInstance<TooltipLinkHandler>() {
    @Override
    protected Class<TooltipLinkHandler> getInstanceClass() throws ClassNotFoundException {
      return findClass(handlerClassName);
    }
  };

  public static boolean handleLink(@Nonnull final String ref, @Nonnull final Editor editor) {
    for (final TooltipLinkHandlerEP handlerEP : EP_NAME.getExtensionList()) {
      if (ref.startsWith(handlerEP.prefix)) {
        final String refSuffix = ref.substring(handlerEP.prefix.length());
        return handlerEP.myHandler.getValue().handleLink(refSuffix.replaceAll("<br/>", "\n"), editor);
      }
    }
    return false;
  }

  @Nullable
  public static String getDescription(@Nonnull final String ref, @Nonnull final Editor editor) {
    for (final TooltipLinkHandlerEP handlerEP : EP_NAME.getExtensionList()) {
      if (ref.startsWith(handlerEP.prefix)) {
        final String refSuffix = ref.substring(handlerEP.prefix.length());
        return handlerEP.myHandler.getValue().getDescription(refSuffix, editor);
      }
    }
    return null;
  }

  @Nonnull
  public static String getDescriptionTitle(@Nonnull String ref, @Nonnull Editor editor) {
    for (TooltipLinkHandlerEP ep : EP_NAME.getExtensionList()) {
      if (ref.startsWith(ep.prefix)) {
        String refSuffix = ref.substring(ep.prefix.length());
        return ep.myHandler.getValue().getDescriptionTitle(refSuffix, editor);
      }
    }
    return TooltipLinkHandler.INSPECTION_INFO;
  }
}
