/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package com.intellij.openapi.editor.actionSystem;

import com.intellij.openapi.application.Application;
import com.intellij.openapi.extensions.AbstractExtensionPointBean;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.util.xmlb.annotations.Attribute;
import consulo.injecting.InjectingContainer;
import consulo.injecting.InjectingContainerBuilder;
import consulo.logging.Logger;

/**
 * @author yole
 */
public class EditorTypedHandlerBean extends AbstractExtensionPointBean {
  private static final Logger LOG = Logger.getInstance(EditorTypedHandlerBean.class);

  public static final ExtensionPointName<EditorTypedHandlerBean> EP_NAME = ExtensionPointName.create("com.intellij.editorTypedHandler");
  public static final ExtensionPointName<EditorTypedHandlerBean> RAW_EP_NAME = ExtensionPointName.create("com.intellij.rawEditorTypedHandler");

  // these must be public for scrambling compatibility
  @Attribute("implementationClass")
  public String implementationClass;

  private TypedActionHandler myHandler;

  public TypedActionHandler getHandler(TypedActionHandler originalHandler) {
    if (myHandler == null) {
      try {
        InjectingContainer container = Application.get().getInjectingContainer();

        InjectingContainerBuilder builder = container.childBuilder();
        // bind original to TypedActionHandler
        builder.bind(TypedActionHandler.class).to(originalHandler);

        myHandler = instantiate(implementationClass, builder.build());
      }
      catch(Exception e) {
        LOG.error(e);
        return null;
      }
    }
    return myHandler;
  }
}