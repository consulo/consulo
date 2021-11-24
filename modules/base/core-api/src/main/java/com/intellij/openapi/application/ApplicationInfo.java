/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package com.intellij.openapi.application;

import com.intellij.openapi.util.BuildNumber;
import com.intellij.openapi.util.NotNullLazyValue;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.PathUtil;
import consulo.annotation.DeprecationInfo;
import consulo.application.ApplicationProperties;
import consulo.logging.Logger;
import consulo.platform.Platform;

import javax.annotation.Nonnull;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

/**
 * I don't think changed random thread name is good idea.
 * <p>
 * Thread currentThread = Thread.currentThread();
 * currentThread.setName(getFullApplicationName());
 */
public class ApplicationInfo {
  private static final NotNullLazyValue<ApplicationInfo> ourValue = NotNullLazyValue.createValue(ApplicationInfo::new);

  @Nonnull
  public static ApplicationInfo getInstance() {
    return ourValue.getValue();
  }

  private BuildNumber myBuild;
  private Calendar myBuildDate;

  private ApplicationInfo() {
    String jarPathForClass = PathUtil.getJarPathForClass(Application.class);

    try (JarFile jarFile = new JarFile(jarPathForClass)) {
      Manifest manifest = jarFile.getManifest();

      Attributes attributes = manifest.getMainAttributes();

      String buildNumberFromJvm = Platform.current().jvm().getRuntimeProperty("consulo.build.number");
      if (buildNumberFromJvm != null) {
        myBuild = BuildNumber.fromString(buildNumberFromJvm);
      }
      else {
        String buildNumber = attributes.getValue("Consulo-Build-Number");
        if (buildNumber != null) {
          myBuild = BuildNumber.fromString(buildNumber);
        }
      }

      // yyyyMMddHHmm
      String buildDate = attributes.getValue("Consulo-Build-Date");
      if (buildDate != null) {
        myBuildDate = parseDate(buildDate);
      }
    }
    catch (Throwable e) {
      Logger.getInstance(ApplicationInfo.class).error(e);
    }

    if (myBuild == null) {
      myBuild = BuildNumber.fallback();
    }

    if (myBuildDate == null) {
      myBuildDate = Calendar.getInstance();
    }
  }

  private static GregorianCalendar parseDate(final String dateString) {
    int year = 0, month = 0, day = 0, hour = 0, minute = 0;
    try {
      year = Integer.parseInt(dateString.substring(0, 4));
      month = Integer.parseInt(dateString.substring(4, 6));
      day = Integer.parseInt(dateString.substring(6, 8));
      if (dateString.length() > 8) {
        hour = Integer.parseInt(dateString.substring(8, 10));
        minute = Integer.parseInt(dateString.substring(10, 12));
      }
    }
    catch (Exception ignore) {
    }
    if (month > 0) month--;
    return new GregorianCalendar(year, month, day, hour, minute);
  }

  @Nonnull
  public Calendar getBuildDate() {
    return myBuildDate;
  }

  @Deprecated
  @DeprecationInfo("Use #getBuild()")
  public String getBuildNumber() {
    return getBuild().asString();
  }

  @Nonnull
  public BuildNumber getBuild() {
    return myBuild;
  }

  @Nonnull
  public final String getMajorVersion() {
    return String.valueOf(myBuildDate.get(Calendar.YEAR));
  }

  @Nonnull
  public final String getMinorVersion() {
    return String.valueOf(myBuildDate.get(Calendar.MONTH) + 1);
  }

  @Deprecated
  @DeprecationInfo("Use #getName()")
  public String getVersionName() {
    return getName();
  }

  @Nonnull
  public final String getFullApplicationName() {
    StringBuilder buffer = new StringBuilder();
    buffer.append(getName());
    buffer.append(" ");
    buffer.append(getMajorVersion());

    String minorVersion = getMinorVersion();
    if (!StringUtil.isEmpty(minorVersion)) {
      buffer.append(".");
      buffer.append(getMinorVersion());
    }

    return buffer.toString();
  }

  @Nonnull
  public final String getName() {
    return "Consulo";
  }

  @Nonnull
  public final String getCompanyName() {
    return "consulo.io";
  }

  @Nonnull
  public final String getSupportUrl() {
    return "https://discuss.consulo.io";
  }

  public String getFullVersion() {
    final String majorVersion = getMajorVersion();
    if (majorVersion.trim().length() > 0) {
      final String minorVersion = getMinorVersion();
      if (minorVersion.trim().length() > 0) {
        return majorVersion + "." + minorVersion;
      }
      else {
        return majorVersion + ".0";
      }
    }
    else {
      return getName();
    }
  }

  @Deprecated
  @DeprecationInfo("Do not use this method. Use SandboxUtil.getAppIcon()")
  public String getIconUrl() {
    return getUrl("/icon32");
  }

  @Deprecated
  @DeprecationInfo("Do not use this method. Use SandboxUtil.getAppIcon()")
  public String getSmallIconUrl() {
    return getUrl("/icon16");
  }

  private static String getUrl(String prefix) {
    return (ApplicationProperties.isInSandbox() ? prefix + "-sandbox" : prefix) + ".png";
  }
}
