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
package com.intellij.credentialStore.gpg;

import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.util.text.StringUtilRt;
import com.intellij.util.SmartList;

import javax.annotation.Nonnull;
import java.util.Iterator;
import java.util.List;

/**
 * @author VISTALL
 * @since 2018-10-28
 */
public class Pgp {
  private final GpgToolWrapper gpgTool;

  public Pgp() {
    this(GpgToolWrapper.createGpg());
  }

  public Pgp(GpgToolWrapper gpgTool) {
    this.gpgTool = gpgTool;
  }

  @Nonnull
  public List<PgpKey> listKeys() {
    List<PgpKey> result = new SmartList<>();
    String keyId = null;

    for (String line : StringUtil.split(StringUtilRt.convertLineSeparators(gpgTool.listSecretKeys()), "\n")) {
      Iterator<String> fields = StringUtil.split(line, ":").iterator();

      if(!fields.hasNext()) {
        continue;
      }

      String tag = fields.next();

      switch (tag) {
        case "sec":{
          for (int i = 2; i < 5; i++) {
            fields.next();
          }

          // Field 5 - KeyID
          keyId = fields.next();
          break;
        }
        case "uid": {
          for (int i = 2; i < 10; i++) {
            fields.next();
          }

          // Field 10 - User-ID
          // The value is quoted like a C string to avoid control characters (the colon is quoted =\x3a=).
          result.add(new PgpKey(keyId, StringUtil.replace(fields.next(), "=\\x3a=", ":")));
          keyId = null;
          break;
        }
      }
    }

    return result;
  }

  public byte[] encrypt(byte[] data, String recipient) {
    return gpgTool.encrypt(data, recipient);
  }

  public byte[] decrypt(byte[] data) {
    return gpgTool.decrypt(data);
  }
}
