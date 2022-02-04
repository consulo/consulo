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
package consulo.externalStorage.storage;

import consulo.external.api.InformationBean;
import consulo.ide.updateSettings.UpdateChannel;

import java.util.Base64;

/**
 * @author VISTALL
 * @since 2019-02-11
 */
public class PushFileRequestBean extends InformationBean {
  private String bytes;
  private String filePath;

  public PushFileRequestBean(UpdateChannel updateChannel, String filePath, byte[] data) {
    super(updateChannel);
    
    this.filePath = filePath;
    bytes = Base64.getEncoder().encodeToString(data);
  }
}
