/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package com.intellij.errorreport.bean;

import com.intellij.openapi.application.ApplicationInfo;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ApplicationNamesInfo;
import com.intellij.openapi.application.ex.ApplicationInfoEx;
import com.intellij.openapi.diagnostic.Attachment;
import com.intellij.util.SystemProperties;
import consulo.ide.updateSettings.UpdateChannel;
import org.jetbrains.annotations.NonNls;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.*;

/**
 * @author stathik
 * @since May 5, 2003
 */
@SuppressWarnings("unused")
public class ErrorBean {
  private static class AttachmentBean {
    private String name;
    private String path;
    private String encodedText;

    private AttachmentBean(String name, String path, String encodedText) {
      this.name = name;
      this.path = path;
      this.encodedText = encodedText;
    }
  }

  private final String osName = SystemProperties.getOsName();
  private final String javaVersion = SystemProperties.getJavaVersion();
  private final String javaVmVendor = SystemProperties.getJavaVmVendor();

  private final String appName = ApplicationNamesInfo.getInstance().getFullProductName();
  private final UpdateChannel appUpdateChannel;
  private final String appBuild;
  private final String appVersionMajor;
  private final String appVersionMinor;
  private final String appBuildDate;
  private final boolean appIsInternal;

  private String lastAction;
  private Integer previousException;
  private String message;
  private String stackTrace;
  private String description;
  private Integer assigneeId;

  private final Map<String, String> affectedPluginIds = new TreeMap<>();

  private List<AttachmentBean> attachments = Collections.emptyList();

  public ErrorBean(Throwable throwable, String lastAction) {
    appIsInternal = ApplicationManager.getApplication().isInternal();
    appUpdateChannel = consulo.ide.updateSettings.UpdateSettings.getInstance().getChannel();

    ApplicationInfoEx appInfo = (ApplicationInfoEx)ApplicationInfo.getInstance();
    appBuild = appInfo.getBuild().asString();
    appVersionMajor = appInfo.getMajorVersion();
    appVersionMinor = appInfo.getMinorVersion();
    appBuildDate = appInfo.getBuildDate() == null ? null : String.valueOf(appInfo.getBuildDate().getTimeInMillis());

    if (throwable != null) {
      message = throwable.getMessage();

      ByteArrayOutputStream stream = new ByteArrayOutputStream();
      //noinspection IOResourceOpenedButNotSafelyClosed
      throwable.printStackTrace(new PrintStream(stream, true));
      stackTrace = stream.toString();
    }
    this.lastAction = lastAction;
  }

  public Map<String, String> getAffectedPluginIds() {
    return affectedPluginIds;
  }

  public Integer getPreviousException() {
    return previousException;
  }

  public void setPreviousException(Integer previousException) {
    this.previousException = previousException;
  }

  public String getLastAction() {
    return lastAction;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(@NonNls String description) {
    this.description = description;
  }

  public String getStackTrace() {
    return stackTrace;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public void setAttachments(List<Attachment> attachments) {
    this.attachments = new ArrayList<>(attachments.size());
    for (Attachment attachment : attachments) {
      this.attachments.add(new AttachmentBean(attachment.getName(), attachment.getPath(), attachment.getEncodedBytes()));
    }
  }

  public Integer getAssigneeId() {
    return assigneeId;
  }

  public void setAssigneeId(Integer assigneeId) {
    this.assigneeId = assigneeId;
  }
}
