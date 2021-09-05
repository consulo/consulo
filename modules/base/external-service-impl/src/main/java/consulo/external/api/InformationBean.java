/*
 * Copyright 2013-2019 consulo.io
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
package consulo.external.api;

import com.intellij.openapi.application.ApplicationInfo;
import com.intellij.util.SystemProperties;
import consulo.ide.updateSettings.UpdateChannel;

import java.util.Locale;

/**
 * @author VISTALL
 * @since 2019-02-11
 */
@SuppressWarnings("unused")
public abstract class InformationBean {
  private final String osName = SystemProperties.getOsName();
  private final String osVersion = SystemProperties.getOsVersion();
  private final String javaVersion = SystemProperties.getJavaVersion();
  private final String javaVmVendor = SystemProperties.getJavaVmVendor();
  private final String locale = Locale.getDefault().toString();

  private final String appName;
  private final UpdateChannel appUpdateChannel;
  private final String appBuild;
  private final String appVersionMajor;
  private final String appVersionMinor;
  private final String appBuildDate;

  public InformationBean(UpdateChannel appUpdateChannel) {
    this.appUpdateChannel = appUpdateChannel;

    ApplicationInfo appInfo = ApplicationInfo.getInstance();
    appName = appInfo.getName();
    appBuild = appInfo.getBuild().asString();
    appVersionMajor = appInfo.getMajorVersion();
    appVersionMinor = appInfo.getMinorVersion();
    appBuildDate = appInfo.getBuildDate() == null ? null : String.valueOf(appInfo.getBuildDate().getTimeInMillis());
  }
}
