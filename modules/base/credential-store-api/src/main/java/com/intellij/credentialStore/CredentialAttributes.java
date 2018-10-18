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

import java.util.Objects;

/**
 * @author VISTALL
 * @since 2018-10-12
 */
public final class CredentialAttributes {
  private String serviceName;
  private String userName;
  private Class<?> requestor;
  private boolean isPasswordMemoryOnly;

  public CredentialAttributes(String serviceName, String userName, Class<?> requestor, boolean isPasswordMemoryOnly) {
    this.serviceName = serviceName;
    this.userName = userName;
    this.requestor = requestor;
    this.isPasswordMemoryOnly = isPasswordMemoryOnly;
  }

  public CredentialAttributes(String serviceName, String userName, Class<?> requestor) {
    this(serviceName, userName, requestor, false);
  }

  public CredentialAttributes(Class<?> requestor, String userName) {
    this(requestor.getName(), userName, requestor);
  }

  public String getUserName() {
    return userName;
  }

  public Class<?> getRequestor() {
    return requestor;
  }

  public String getServiceName() {
    return serviceName;
  }

  public boolean isPasswordMemoryOnly() {
    return isPasswordMemoryOnly;
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder("CredentialAttributes{");
    sb.append("serviceName='").append(serviceName).append('\'');
    sb.append(", userName='").append(userName).append('\'');
    sb.append(", requestor=").append(requestor);
    sb.append(", isPasswordMemoryOnly=").append(isPasswordMemoryOnly);
    sb.append('}');
    return sb.toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    CredentialAttributes that = (CredentialAttributes)o;
    return isPasswordMemoryOnly == that.isPasswordMemoryOnly && Objects.equals(serviceName, that.serviceName) && Objects.equals(userName, that.userName) && Objects.equals(requestor, that.requestor);
  }

  @Override
  public int hashCode() {
    return Objects.hash(serviceName, userName, requestor, isPasswordMemoryOnly);
  }
}
