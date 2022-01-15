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
package consulo.copyright.impl;

import consulo.logging.Logger;
import com.intellij.openapi.extensions.AbstractExtensionPointBean;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.util.NotNullLazyValue;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.util.xmlb.annotations.Attribute;
import javax.annotation.Nonnull;

import java.io.IOException;
import java.io.InputStream;

/**
 * @author VISTALL
 * @since 16.02.2015
 */
public class PredefinedCopyrightTextEP extends AbstractExtensionPointBean {
  private static final Logger LOG = Logger.getInstance(PredefinedCopyrightTextEP.class);

  public static final ExtensionPointName<PredefinedCopyrightTextEP> EP_NAME = ExtensionPointName.create("com.intellij.predefinedCopyright");
  @Attribute("name")
  public String name;
  @Attribute("file")
  public String file;

  private NotNullLazyValue<String> myText = NotNullLazyValue.createValue(() -> {
    try {
      InputStream resourceAsStream = getLoaderForClass().getResourceAsStream(file);
      if (resourceAsStream == null) {
        LOG.error("Copyright file " + file + " not found");
        return "not find file: " + file;
      }
      return FileUtil.loadTextAndClose(resourceAsStream, true);
    }
    catch (IOException e) {
      LOG.error(e);
      return "not loaded file: " + file;
    }
  });

  @Nonnull
  public String getText() {
    return myText.getValue();
  }
}
