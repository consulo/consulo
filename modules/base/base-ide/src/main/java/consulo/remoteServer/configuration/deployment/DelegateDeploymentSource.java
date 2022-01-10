/*
 * Copyright 2013-2016 consulo.io
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
package consulo.remoteServer.configuration.deployment;

import com.intellij.remoteServer.configuration.deployment.DeploymentSource;
import com.intellij.remoteServer.configuration.deployment.DeploymentSourceType;
import consulo.ui.image.Image;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;

/**
 * @author VISTALL
 * @since 17:54/28.09.13
 */
public class DelegateDeploymentSource implements DeploymentSource {
  private final DeploymentSource myDelegate;

  public DelegateDeploymentSource(DeploymentSource delegate) {
    myDelegate = delegate;
  }

  @Nullable
  @Override
  public File getFile() {
    return myDelegate.getFile();
  }

  @Nullable
  @Override
  public String getFilePath() {
    return myDelegate.getFilePath();
  }

  @Nonnull
  @Override
  public String getPresentableName() {
    return myDelegate.getPresentableName();
  }

  @Nullable
  @Override
  public Image getIcon() {
    return myDelegate.getIcon();
  }

  @Override
  public boolean isValid() {
    return myDelegate.isValid();
  }

  @Override
  public boolean isArchive() {
    return myDelegate.isArchive();
  }

  @Nonnull
  @Override
  public DeploymentSourceType<?> getType() {
    return myDelegate.getType();
  }

  @Nonnull
  public DeploymentSource getDelegate() {
    return myDelegate;
  }
}
