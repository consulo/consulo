/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package consulo.ide.impl.idea.remote.ext;

import consulo.ide.impl.idea.remote.RemoteCredentials;
import consulo.ide.impl.idea.remote.RemoteCredentialsHolder;
import org.jdom.Element;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public class SshCredentialsHandler extends RemoteCredentialsHandlerBase<RemoteCredentialsHolder> {

  public SshCredentialsHandler(RemoteCredentialsHolder credentials) {
    super(credentials);
  }

  @Override
  public String getId() {
    return constructSshCredentialsFullPath();
  }

  @Override
  public void save(@Nonnull Element rootElement) {
    getCredentials().save(rootElement);
  }

  @Override
  public String getPresentableDetails(String interpreterPath) {
    return "(" + constructSshCredentialsFullPath() + interpreterPath + ")";
  }

  @Override
  public void load(@Nullable Element rootElement) {
    if (rootElement != null) {
      getCredentials().load(rootElement);
    }
  }

  @Nonnull
  private String constructSshCredentialsFullPath() {
    RemoteCredentials cred = getCredentials();
    return RemoteCredentialsHolder.SSH_PREFIX + cred.getUserName() + "@" + cred.getHost() + ":" + cred.getLiteralPort();
  }
}
