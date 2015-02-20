/*
 * Copyright 2013-2015 must-be.org
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
package com.maddyhome.idea.copyright;

import com.intellij.openapi.extensions.AbstractExtensionPointBean;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.util.xmlb.annotations.Attribute;
import org.consulo.lombok.annotations.LazyInstance;
import org.consulo.lombok.annotations.Logger;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;

/**
 * @author VISTALL
 * @since 16.02.2015
 */
@Logger
public class PredefinedCopyrightTextEP extends AbstractExtensionPointBean {
  public static final ExtensionPointName<PredefinedCopyrightTextEP> EP_NAME = ExtensionPointName.create("com.intellij.predefinedCopyright");
  @Attribute("name")
  public String name;
  @Attribute("file")
  public String file;

  @NotNull
  @LazyInstance
  public String getText() {
    try {
      InputStream resourceAsStream = getLoaderForClass().getResourceAsStream(file);
      if(resourceAsStream == null) {
        LOGGER.error("Copyright file " + file + " not found");
        return "not find";
      }
      return FileUtil.loadTextAndClose(resourceAsStream);
    }
    catch (IOException e) {
      LOGGER.error(e);
      return "not loaded";
    }
  }
}
