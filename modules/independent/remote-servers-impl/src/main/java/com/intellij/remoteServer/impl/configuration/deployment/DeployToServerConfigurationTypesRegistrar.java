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
package com.intellij.remoteServer.impl.configuration.deployment;

import com.intellij.execution.configurations.ConfigurationType;
import com.intellij.openapi.extensions.ExtensionPoint;
import com.intellij.openapi.extensions.Extensions;
import com.intellij.remoteServer.ServerType;
import consulo.annotations.NotLazy;

import javax.annotation.PostConstruct;
import javax.inject.Singleton;

/**
 * @author nik
 */
@NotLazy
@Singleton
public class DeployToServerConfigurationTypesRegistrar {
  @PostConstruct
  public void initComponent() {
    //todo[nik] improve this: configuration types should be loaded lazily
    ExtensionPoint<ConfigurationType> point = Extensions.getRootArea().getExtensionPoint(ConfigurationType.CONFIGURATION_TYPE_EP);
    for (ServerType serverType : ServerType.EP_NAME.getExtensions()) {
      point.registerExtension(new DeployToServerConfigurationType(serverType));
    }
  }
}
