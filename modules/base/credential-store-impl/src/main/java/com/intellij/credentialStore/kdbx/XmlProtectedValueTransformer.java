/*
 * Copyright 2013-2018 consulo.io
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
package com.intellij.credentialStore.kdbx;

import com.intellij.openapi.util.text.StringUtil;
import org.bouncycastle.crypto.SkippingStreamCipher;
import org.jdom.Content;
import org.jdom.Element;

import java.util.Base64;

/**
 * @author VISTALL
 * @since 2018-10-27
 */
public class XmlProtectedValueTransformer {
  private SkippingStreamCipher streamCipher;

  private int position;

  public XmlProtectedValueTransformer(SkippingStreamCipher streamCipher) {
    this.streamCipher = streamCipher;
  }

  public void processEntries(Element parentElement) {
    // we must process in exact order
    for (Content element : parentElement.getContent()) {
      if (!(element instanceof Element)) {
        continue;
      }

      if (KdbxDbElementNames.group.equals(((Element)element).getName())) {
        processEntries((Element)element);
      }
      else if (KdbxDbElementNames.entry.equals(((Element)element).getName())) {
        for (Element container : ((Element)element).getChildren(KdbxEntryElementNames.string)) {
          Element valueElement = container.getChild(KdbxEntryElementNames.value);
          if (valueElement == null) {
            continue;
          }

          if (isValueProtected(valueElement)) {
            byte[] value = Base64.getDecoder().decode(valueElement.getText());
            valueElement.setContent(new ProtectedValue(value, position, streamCipher));
            position += value.length;
          }
        }
      }
    }
  }

  public static boolean isValueProtected(Element valueElement) {
    return StringUtil.equalsIgnoreCase(valueElement.getAttributeValue(KdbxAttributeNames._protected), "true");
  }
}
