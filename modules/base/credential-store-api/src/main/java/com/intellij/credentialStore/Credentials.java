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
package com.intellij.credentialStore;

import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.text.StringUtil;

import javax.annotation.Nullable;
import java.nio.charset.CharacterCodingException;

/**
 * from kotlin
 */
public class Credentials {
  public static boolean isFulfilled(@Nullable Credentials credentials) {
    return credentials != null && credentials.getUserName() != null && !StringUtil.isEmpty(credentials.password);
  }

  @Nullable
  private final String user;
  @Nullable
  private final OneTimeString password;

  public Credentials(@Nullable String user, @Nullable OneTimeString password) {
    this.user = user;
    this.password = password;
  }

  public Credentials(String user, String password) {
    this(user, password == null ? null : new OneTimeString(password));
  }

  public Credentials(String user, char[] password) {
    this(user, password == null ? null : new OneTimeString(password));
  }

  public Credentials(String user, byte[] password) throws CharacterCodingException {
    this(user, password == null ? null : OneTimeString.from(password));
  }

  @Nullable
  public String getUserName() {
    return StringUtil.nullize(user);
  }

  @Nullable
  public String getPasswordAsString() {
    return password == null ? null : password.toString();
  }

  @Override
  public boolean equals(Object obj) {
    if(!(obj instanceof Credentials)) {
      return false;
    }
    return Comparing.equal(user, ((Credentials)obj).user) && Comparing.equal(password, ((Credentials)obj).password);
  }

  @Override
  public int hashCode() {
    return (user == null ? 0 : user.hashCode()) * 37 + (password == null ? 0 : password.hashCode());
  }

  @Override
  public String toString() {
    return "userName: " + getUserName() + ", password size: " + (password == null ? 0 : password.length());
  }
}
