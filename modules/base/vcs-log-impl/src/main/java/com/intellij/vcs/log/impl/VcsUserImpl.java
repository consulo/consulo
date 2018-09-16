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
package com.intellij.vcs.log.impl;

import com.intellij.vcs.log.VcsUser;
import com.intellij.vcs.log.util.VcsUserUtil;
import javax.annotation.Nonnull;

import java.util.Objects;

/**
 * Note: users are considered equal if they have the same name and email. Emails are converted to lower case in constructor.
 */
public class VcsUserImpl implements VcsUser {

  @Nonnull
  private final String myName;
  @Nonnull
  private final String myEmail;

  public VcsUserImpl(@Nonnull String name, @Nonnull String email) {
    myName = name;
    myEmail = VcsUserUtil.emailToLowerCase(email);
  }

  @Nonnull
  @Override
  public String getName() {
    return myName;
  }

  @Nonnull
  @Override
  public String getEmail() {
    return myEmail;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    VcsUserImpl user = (VcsUserImpl)o;

    if (!myName.equals(user.myName)) return false;
    if (!myEmail.equals(user.myEmail)) return false;

    return true;
  }

  @Override
  public int hashCode() {
    return Objects.hash(myName, myEmail);
  }

  @Override
  public String toString() {
    return VcsUserUtil.toExactString(this);
  }
}
