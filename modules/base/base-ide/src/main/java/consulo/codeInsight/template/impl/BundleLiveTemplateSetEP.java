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
package consulo.codeInsight.template.impl;

import com.intellij.openapi.extensions.AbstractExtensionPointBean;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.util.xmlb.annotations.Attribute;

/**
 * @author VISTALL
 * @since 16.06.14
 */
public class BundleLiveTemplateSetEP extends AbstractExtensionPointBean {
  public static final ExtensionPointName<BundleLiveTemplateSetEP> EP_NAME = ExtensionPointName.create("com.intellij.bundleLiveTemplateSet");

  @Attribute("path")
  public String path;

  @Attribute("register")
  public boolean register = true;
}
