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
package com.intellij.openapi.vfs;

import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.util.Ref;
import com.intellij.testFramework.PlatformLangTestCase;

import javax.annotation.Nonnull;
import java.io.IOException;

/**
 * @author nik
 */
public abstract class VirtualFileListenerTest extends PlatformLangTestCase {
  public void testFireEvent() throws IOException {
    final VirtualFile dir = getVirtualFile(createTempDir("vDir"));
    assertNotNull(dir);
    dir.getChildren();
    final Ref<Boolean> eventFired = Ref.create(false);
    VirtualFileManager.getInstance().addVirtualFileListener(new VirtualFileAdapter() {
      @Override
      public void fileCreated(@Nonnull VirtualFileEvent event) {
        eventFired.set(true);
      }
    }, myTestRootDisposable);

    WriteAction.run(() -> {
      try {
        dir.createChildData(this, "x.txt");
      }
      catch (IOException e) {
        throw new RuntimeException(e);
      }
    });

    assertTrue(eventFired.get());
  }
}
