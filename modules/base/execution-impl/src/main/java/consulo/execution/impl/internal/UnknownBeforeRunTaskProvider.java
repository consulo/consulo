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

package consulo.execution.impl.internal;

import consulo.application.AllIcons;
import consulo.dataContext.DataContext;
import consulo.execution.BeforeRunTask;
import consulo.execution.BeforeRunTaskProvider;
import consulo.execution.configuration.RunConfiguration;
import consulo.execution.localize.ExecutionLocalize;
import consulo.execution.runner.ExecutionEnvironment;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.image.Image;
import consulo.util.concurrent.AsyncResult;
import consulo.util.dataholder.Key;
import consulo.util.jdom.JDOMUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jdom.Attribute;
import org.jdom.Element;

import java.util.List;

/**
 * @author Eugene Zhuravlev
 * @since 2009-09-15
 */
@SuppressWarnings("ExtensionImplIsNotAnnotated")
public class UnknownBeforeRunTaskProvider extends BeforeRunTaskProvider<UnknownBeforeRunTaskProvider.UnknownTask> {
  private final Key<UnknownTask> myId;

  public UnknownBeforeRunTaskProvider(String mirrorProviderName) {
    myId = Key.create(mirrorProviderName);
  }

  @Nonnull
  @Override
  public Key<UnknownTask> getId() {
    return myId;
  }

  @Nonnull
  @Override
  public String getName() {
    return ExecutionLocalize.beforeLaunchRunUnknownTask().get();
  }

  @Nullable
  @Override
  public Image getIcon() {
    return AllIcons.Actions.Help;
  }

  @Nonnull
  @Override
  public String getDescription(UnknownTask task) {
    return ExecutionLocalize.beforeLaunchRunUnknownTask() + " " + myId.toString();
  }

  @Nonnull
  @RequiredUIAccess
  @Override
  public AsyncResult<Void> configureTask(RunConfiguration runConfiguration, UnknownTask task) {
    return AsyncResult.rejected();
  }

  @Override
  public boolean canExecuteTask(RunConfiguration configuration, UnknownTask task) {
    return false;
  }

  @Nonnull
  @Override
  public AsyncResult<Void> executeTaskAsync(UIAccess uiAccess, DataContext context, RunConfiguration configuration, ExecutionEnvironment env, UnknownTask task) {
    return AsyncResult.resolved();
  }

  @Override
  public UnknownTask createTask(RunConfiguration runConfiguration) {
    return new UnknownTask(getId());
  }

  public static final class UnknownTask extends BeforeRunTask<UnknownTask> {
    private Element myConfig;

    public UnknownTask(Key<UnknownTask> providerId) {
      super(providerId);
    }

    @Override
    public void readExternal(Element element) {
      myConfig = element;
    }

    @Override
    public void writeExternal(Element element) {
      if (myConfig != null) {
        element.removeContent();
        List attributes = myConfig.getAttributes();
        for (Object attribute : attributes) {
         element.setAttribute((Attribute)((Attribute)attribute).clone());
        }
        for (Object child : myConfig.getChildren()) {
          element.addContent((Element)((Element)child).clone());
        }
      }
    }

    @Override
    public BeforeRunTask clone() {
      return super.clone();
    }

    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      if (!super.equals(o)) return false;

      UnknownTask that = (UnknownTask)o;

      if (!JDOMUtil.areElementsEqual(myConfig, that.myConfig)) return false;

      return true;
    }

    public int hashCode() {
      int result = super.hashCode();
      result = 31 * result + (myConfig != null ? myConfig.hashCode() : 0);
      return result;
    }
  }
}
