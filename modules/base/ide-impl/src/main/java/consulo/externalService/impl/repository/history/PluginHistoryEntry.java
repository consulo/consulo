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
package consulo.externalService.impl.repository.history;

/**
 * @author VISTALL
 * @since 20/11/2021
 *
 * @link https://github.com/consulo/hub.consulo.io/blob/master/backend/src/main/java/consulo/hub/backend/repository/RestPluginHistoryEntry.java
 */
public class PluginHistoryEntry {
  public String pluginVersion;

  public String repoUrl;

  //private String commitUrl;
  public String commitHash;
  public String commitMessage;
  public long commitTimestamp;
  public String commitAuthor;
}
