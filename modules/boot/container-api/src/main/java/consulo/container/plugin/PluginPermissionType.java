/*
 * Copyright 2013-2021 consulo.io
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
package consulo.container.plugin;

import consulo.annotation.DeprecationInfo;

/**
 * @author VISTALL
 * @since 24/10/2021
 */
public enum PluginPermissionType {
  @Deprecated(forRemoval = true)
  @DeprecationInfo("Use PROCESS_CREATE")
  PROCESS,
  /**
   * running any process in OS (and destroy if need)
   */
  PROCESS_CREATE,
  /**
   * list all processes in OS, and destroy them (protected by user space)
   */
  PROCESS_MANAGE,
  /**
   * loading native libraries into Consulo process
   */
  NATIVE_LIBRARY,
  @Deprecated(forRemoval = true)
  @DeprecationInfo("Splitting to SOCKET_BIND and SOCKET_CONNECT")
  SOCKET,
  /**
   * binding any TCP/UDP socket
   */
  SOCKET_BIND,
  /**
   * connecting to any TCP/UDP socket
   */
  SOCKET_CONNECT,
  /**
   * permission for control access to internet by url(http, https), by plain implementation.
   *
   * warning: some clients can implement own socket handling, and they will require {@link #SOCKET_CONNECT} permission, and don't checked it by this permission
   */
  INTERNET_URL_ACCESS,
  /**
   * list envs of OS, and get values of them. Also handle access to jvm properties
   */
  GET_ENV
}
