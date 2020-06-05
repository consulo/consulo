/*
 * Copyright 2013-2018 consulo.io
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
package consulo.vfs.newvfs;

import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.TransactionId;
import com.intellij.openapi.vfs.AsyncFileListener;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.newvfs.AsyncEventSupport;
import com.intellij.openapi.vfs.newvfs.RefreshQueueImpl;
import com.intellij.openapi.vfs.newvfs.RefreshSessionImpl;
import com.intellij.openapi.vfs.newvfs.events.VFileCreateEvent;
import com.intellij.openapi.vfs.newvfs.events.VFileEvent;
import com.intellij.util.containers.ContainerUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;

/**
 * @author VISTALL
 * @since 2018-05-13
 *
 * Implementation of {@link RefreshQueueImpl} without sync write version
 */
@Singleton
public class AsyncRefreshQueueImpl extends RefreshQueueImpl {
  @Inject
  public AsyncRefreshQueueImpl(@Nonnull Application application) {
    super(application);
  }
}
