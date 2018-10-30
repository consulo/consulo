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

import com.intellij.util.containers.ContainerUtil;
import consulo.util.jdom.JbXmlOutputter;
import org.bouncycastle.crypto.SkippingStreamCipher;
import org.jdom.Content;
import org.jdom.Element;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.Writer;

/**
 * from kotlin
 */
public class ProtectedXmlWriter extends JbXmlOutputter {
  private final SkippingStreamCipher myStreamCipher;

  public ProtectedXmlWriter(SkippingStreamCipher streamCipher) {
    super("\n", null, null, null);
    myStreamCipher = streamCipher;
  }

  @Override
  protected boolean writeContent(@Nonnull Writer out, @Nonnull Element element, int level) throws IOException {
    if (KdbxEntryElementNames.value.equals(element.getName())) {
      Content value = ContainerUtil.getFirstItem(element.getContent());
      if (value instanceof SecureString) {
        ProtectedValue protectedValue;
        if (value instanceof ProtectedValue){
          ((ProtectedValue)value).setNewStreamCipher(myStreamCipher);
          protectedValue = (ProtectedValue)value;
        }
        else{
          byte[] bytes = ((UnsavedProtectedValue)value).getSecureString().getAsByteArray();
          int position = (int)myStreamCipher.getPosition();
          myStreamCipher.processBytes(bytes, 0, bytes.length, bytes, 0);
          protectedValue = new ProtectedValue(bytes, position, myStreamCipher);
          element.setContent(protectedValue);
        }

        out.write((int)'>');
        out.write(escapeElementEntities(protectedValue.encodeToBase64()));
        return true;
      }
    } return super.writeContent(out, element, level);
  }
}
