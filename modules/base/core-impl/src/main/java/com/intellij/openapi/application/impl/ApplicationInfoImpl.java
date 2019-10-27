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
package com.intellij.openapi.application.impl;

import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationInfo;
import com.intellij.openapi.application.ApplicationNamesInfo;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.application.ex.ApplicationInfoEx;
import com.intellij.openapi.util.BuildNumber;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.JDOMUtil;
import com.intellij.openapi.util.text.StringUtil;
import consulo.annotations.DeprecationInfo;
import consulo.logging.Logger;
import org.jdom.Document;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;

import javax.inject.Singleton;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

@Singleton
public class ApplicationInfoImpl extends ApplicationInfoEx {
  private static final Logger LOG = Logger.getInstance("#com.intellij.openapi.application.impl.ApplicationInfoImpl");

  @NonNls
  @Deprecated
  @DeprecationInfo(value = "Use consulo.ide.webService.WebServiceApi")
  private static final String DEFAULT_STATISTICS_HOST = "http://must-be.org/consulo/statistics/";

  private String myMajorVersion = null;
  private String myMinorVersion = null;
  private String myBuildNumber = null;
  private String myCompanyName = "consulo.io";
  private String myCompanyUrl = "https://consulo.io";

  private Calendar myBuildDate = null;
  private String myDocumentationUrl;
  private String mySupportUrl;
  private String myReleaseFeedbackUrl;

  private String myStatisticsUrl;
  private String myWhatsNewUrl;
  private String myWinKeymapUrl;
  private String myMacKeymapUrl;
  private boolean myHasHelp = true;
  private boolean myHasContextHelp = true;
  @NonNls
  private String myHelpFileName = "ideahelp.jar";
  @NonNls
  private String myHelpRootName = "idea";
  @NonNls
  private String myWebHelpUrl = "https://github.com/consulo/consulo/wiki";

  @NonNls
  private static final String ELEMENT_VERSION = "version";
  @NonNls
  private static final String ATTRIBUTE_MAJOR = "major";
  @NonNls
  private static final String ATTRIBUTE_MINOR = "minor";
  @NonNls
  private static final String ATTRIBUTE_NAME = "name";
  @NonNls
  private static final String ELEMENT_BUILD = "build";
  @NonNls
  private static final String ELEMENT_COMPANY = "company";
  @NonNls
  private static final String ATTRIBUTE_NUMBER = "number";
  @NonNls
  private static final String ATTRIBUTE_DATE = "date";
  @NonNls
  private static final String ATTRIBUTE_URL = "url";
  @NonNls
  private static final String HELP_ELEMENT_NAME = "help";
  @NonNls
  private static final String ATTRIBUTE_HELP_FILE = "file";
  @NonNls
  private static final String ATTRIBUTE_HELP_ROOT = "root";
  @NonNls
  private static final String ELEMENT_DOCUMENTATION = "documentation";
  @NonNls
  private static final String ELEMENT_SUPPORT = "support";
  @NonNls
  private static final String ELEMENT_FEEDBACK = "feedback";
  @NonNls
  private static final String ATTRIBUTE_RELEASE_URL = "release-url";
  @NonNls
  private static final String ATTRIBUTE_WEBHELP_URL = "webhelp-url";
  @NonNls
  private static final String ATTRIBUTE_HAS_HELP = "has-help";
  @NonNls
  private static final String ATTRIBUTE_HAS_CONTEXT_HELP = "has-context-help";
  @NonNls
  private static final String ELEMENT_WHATSNEW = "whatsnew";
  @NonNls
  private static final String ELEMENT_KEYMAP = "keymap";
  @NonNls
  private static final String ATTRIBUTE_WINDOWS_URL = "win";
  @NonNls
  private static final String ATTRIBUTE_MAC_URL = "mac";

  public ApplicationInfoImpl() {
    load();
  }

  @Override
  public Calendar getBuildDate() {
    return myBuildDate;
  }

  @Override
  public BuildNumber getBuild() {
    return BuildNumber.fromString(myBuildNumber);
  }

  @Override
  public String getMajorVersion() {
    return myMajorVersion;
  }

  @Override
  public String getMinorVersion() {
    return myMinorVersion;
  }

  @Override
  public String getVersionName() {
    return ApplicationNamesInfo.getInstance().getFullProductName();
  }

  @Override
  @NonNls
  public String getHelpURL() {
    return "jar:file:///" + getHelpJarPath() + "!/" + myHelpRootName;
  }

  @Override
  public String getCompanyName() {
    return myCompanyName;
  }

  @Override
  public String getCompanyURL() {
    return myCompanyUrl;
  }

  @NonNls
  private String getHelpJarPath() {
    return PathManager.getHomePath() + File.separator + "help" + File.separator + myHelpFileName;
  }

  @Override
  public String getDocumentationUrl() {
    return myDocumentationUrl;
  }

  @Override
  public String getSupportUrl() {
    return mySupportUrl;
  }

  @Override
  public String getReleaseFeedbackUrl() {
    return myReleaseFeedbackUrl;
  }

  @Override
  public String getStatisticsUrl() {
    return myStatisticsUrl;
  }

  @Override
  public String getWebHelpUrl() {
    return myWebHelpUrl;
  }

  @Override
  public boolean hasHelp() {
    return myHasHelp;
  }

  @Override
  public boolean hasContextHelp() {
    return myHasContextHelp;
  }

  @Override
  public String getWhatsNewUrl() {
    return myWhatsNewUrl;
  }

  @Override
  public String getWinKeymapUrl() {
    return myWinKeymapUrl;
  }

  @Override
  public String getMacKeymapUrl() {
    return myMacKeymapUrl;
  }

  @Override
  public String getFullApplicationName() {
    @NonNls StringBuilder buffer = new StringBuilder();
    buffer.append(getVersionName());
    buffer.append(" ");
    buffer.append(getMajorVersion());

    String minorVersion = getMinorVersion();
    if (!StringUtil.isEmpty(minorVersion)) {
      buffer.append(".");
      buffer.append(getMinorVersion());
    }

    return buffer.toString();
  }

  private static ApplicationInfoImpl ourShadowInstance;

  public static ApplicationInfoEx getShadowInstance() {
    if (ourShadowInstance == null) {
      ourShadowInstance = new ApplicationInfoImpl();
    }
    return ourShadowInstance;
  }

  private void load() {
    try {
      Document doc = JDOMUtil.loadDocument(ApplicationInfoImpl.class, ApplicationInfo.ABSOLUTE_APPLICATION_INFO_XML);

      readExternal(doc.getRootElement());
    }
    catch (FileNotFoundException e) {
      LOG.error("Resource is not in classpath", e);
    }
    catch (Exception e) {
      LOG.error(e);
    }
  }

  private void readExternal(Element parentNode) throws InvalidDataException {
    Element versionElement = parentNode.getChild(ELEMENT_VERSION);
    if (versionElement != null) {
      myMajorVersion = versionElement.getAttributeValue(ATTRIBUTE_MAJOR);
      myMinorVersion = versionElement.getAttributeValue(ATTRIBUTE_MINOR);
    }

    Element companyElement = parentNode.getChild(ELEMENT_COMPANY);
    if (companyElement != null) {
      myCompanyName = companyElement.getAttributeValue(ATTRIBUTE_NAME, myCompanyName);
      myCompanyUrl = companyElement.getAttributeValue(ATTRIBUTE_URL, myCompanyUrl);
    }

    Element buildElement = parentNode.getChild(ELEMENT_BUILD);
    if (buildElement != null) {
      myBuildNumber = buildElement.getAttributeValue(ATTRIBUTE_NUMBER);
      String dateString = buildElement.getAttributeValue(ATTRIBUTE_DATE);
      if (dateString.equals("__BUILD_DATE__")) {
        myBuildDate = new GregorianCalendar();
        try {
          String jarPathForClass = PathManager.getJarPathForClass(Application.class);
          try (JarFile bootJar = new JarFile(jarPathForClass)) {
            final JarEntry jarEntry = bootJar.entries().nextElement(); // /META-INF is always updated on build
            myBuildDate.setTime(new Date(jarEntry.getTime()));
          }
        }
        catch (Exception ignore) {
        }
      }
      else {
        myBuildDate = parseDate(dateString);
      }
    }

    String consuloBuildNumber = System.getProperty("consulo.build.number");
    if (consuloBuildNumber != null) {
      myBuildNumber = consuloBuildNumber;
    }

    Thread currentThread = Thread.currentThread();
    currentThread.setName(currentThread.getName() + " " + myMajorVersion + "." + myMinorVersion + "#" + myBuildNumber);

    Element helpElement = parentNode.getChild(HELP_ELEMENT_NAME);
    if (helpElement != null) {
      myHelpFileName = helpElement.getAttributeValue(ATTRIBUTE_HELP_FILE);
      myHelpRootName = helpElement.getAttributeValue(ATTRIBUTE_HELP_ROOT);
      final String webHelpUrl = helpElement.getAttributeValue(ATTRIBUTE_WEBHELP_URL);
      if (webHelpUrl != null) {
        myWebHelpUrl = webHelpUrl;
      }

      String attValue = helpElement.getAttributeValue(ATTRIBUTE_HAS_HELP);
      myHasHelp = attValue == null || Boolean.parseBoolean(attValue); // Default is true

      attValue = helpElement.getAttributeValue(ATTRIBUTE_HAS_CONTEXT_HELP);
      myHasContextHelp = attValue == null || Boolean.parseBoolean(attValue); // Default is true
    }

    Element documentationElement = parentNode.getChild(ELEMENT_DOCUMENTATION);
    if (documentationElement != null) {
      myDocumentationUrl = documentationElement.getAttributeValue(ATTRIBUTE_URL);
    }

    Element supportElement = parentNode.getChild(ELEMENT_SUPPORT);
    if (supportElement != null) {
      mySupportUrl = supportElement.getAttributeValue(ATTRIBUTE_URL);
    }

    Element feedbackElement = parentNode.getChild(ELEMENT_FEEDBACK);
    if (feedbackElement != null) {
      myReleaseFeedbackUrl = feedbackElement.getAttributeValue(ATTRIBUTE_RELEASE_URL);
    }

    Element whatsnewElement = parentNode.getChild(ELEMENT_WHATSNEW);
    if (whatsnewElement != null) {
      myWhatsNewUrl = whatsnewElement.getAttributeValue(ATTRIBUTE_URL);
    }

    myStatisticsUrl = StringUtil.notNullize(System.getProperty("consulo.statistics.host"), DEFAULT_STATISTICS_HOST) + "post";

    Element keymapElement = parentNode.getChild(ELEMENT_KEYMAP);
    if (keymapElement != null) {
      myWinKeymapUrl = keymapElement.getAttributeValue(ATTRIBUTE_WINDOWS_URL);
      myMacKeymapUrl = keymapElement.getAttributeValue(ATTRIBUTE_MAC_URL);
    }
  }

  private static GregorianCalendar parseDate(final String dateString) {
    @SuppressWarnings("MultipleVariablesInDeclaration") int year = 0, month = 0, day = 0, hour = 0, minute = 0;
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

  private static volatile boolean myInPerformanceTest;

  public static boolean isInPerformanceTest() {
    return myInPerformanceTest;
  }

  public static void setInPerformanceTest(boolean inPerformanceTest) {
    myInPerformanceTest = inPerformanceTest;
  }
}
