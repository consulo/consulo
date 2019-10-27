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
package consulo.external.api;

import com.intellij.openapi.extensions.PluginId;
import com.intellij.util.ExceptionUtil;
import consulo.logging.attachment.Attachment;
import org.jetbrains.annotations.NonNls;

import javax.annotation.Nonnull;
import java.util.*;

@SuppressWarnings("unused")
public class ErrorReportBean extends InformationBean {
  private static class AttachmentBean {
    private final String name;
    private final String path;
    private final String encodedText;

    private AttachmentBean(String name, String path, String encodedText) {
      this.name = name;
      this.path = path;
      this.encodedText = encodedText;
    }
  }

  private static class AffectedPlugin implements Comparable<AffectedPlugin> {
    private final String pluginId;
    private final String pluginVersion;

    private AffectedPlugin(String pluginId, String pluginVersion) {
      this.pluginId = pluginId;
      this.pluginVersion = pluginVersion;
    }

    @Override
    public int compareTo(@Nonnull AffectedPlugin o) {
      return pluginId.compareToIgnoreCase(o.pluginId);
    }
  }

  private String lastAction;
  private String previousException;
  private String message;
  private String stackTrace;
  private String description;
  private Integer assigneeId;

  private final Set<AffectedPlugin> affectedPlugins = new TreeSet<>();

  private List<AttachmentBean> attachments = Collections.emptyList();

  public ErrorReportBean(Throwable throwable, String lastAction) {
    if (throwable != null) {
      message = throwable.getMessage();
      stackTrace = ExceptionUtil.getThrowableText(throwable);
    }
    this.lastAction = lastAction;
  }

  public void addAffectedPlugin(@Nonnull PluginId pluginId, @Nonnull String version) {
    affectedPlugins.add(new AffectedPlugin(pluginId.toString(), version));
  }

  public String getPreviousException() {
    return previousException;
  }

  public void setPreviousException(String previousException) {
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
