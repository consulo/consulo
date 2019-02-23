/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package com.intellij.idea;

import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.io.FileUtil;
import org.hamcrest.CoreMatchers;
import org.junit.*;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class LockSupportTest extends Assert{
  private File myTempDir = null;

  @Before
  public void setUp() throws IOException {
    myTempDir = FileUtil.createTempDirectory("LockSupportTest.", ".tmp", false);
  }

  @After
  public void tearDown() {
    if (myTempDir != null) {
      FileUtil.delete(myTempDir);
      myTempDir = null;
    }
  }

  @Test(timeout = 30000)
  public void testUseCanonicalPathLock() throws Exception {
    Assume.assumeThat(SystemInfo.isFileSystemCaseSensitive, CoreMatchers.is(false));

    String path1 = myTempDir.getPath();
    String path2 = path1.toUpperCase(Locale.ENGLISH);

    DesktopImportantFolderLocker lock1 = new DesktopImportantFolderLocker(path1 + "/c", path1 + "/s");
    DesktopImportantFolderLocker lock2 = new DesktopImportantFolderLocker(path2 + "/c", path2 + "/s");
    try {
      lock1.lock();
      assertThat(lock2.lock(), CoreMatchers.equalTo(DesktopImportantFolderLocker.ActivateStatus.ACTIVATED));
    }
    finally {
      lock1.dispose();
      lock2.dispose();
    }
  }

  @Test(timeout = 30000)
  public void testLock() throws Exception {
    DesktopImportantFolderLocker lock = new DesktopImportantFolderLocker(myTempDir.getPath() + "/c", myTempDir.getPath() + "/s");
    try {
      assertThat(lock.lock(), CoreMatchers.equalTo(DesktopImportantFolderLocker.ActivateStatus.NO_INSTANCE));
    }
    finally {
      lock.dispose();
    }
  }

  @Test(timeout = 30000)
  public void testTwoLocks() throws Exception {
    List<DesktopImportantFolderLocker> toClose = new ArrayList<>();
    try {
      assertThat(createLock(toClose, myTempDir, "1", "1-").lock(), CoreMatchers.equalTo(DesktopImportantFolderLocker.ActivateStatus.NO_INSTANCE));
      assertThat(createLock(toClose, myTempDir, "1.1", "1-1").lock(), CoreMatchers.equalTo(DesktopImportantFolderLocker.ActivateStatus.NO_INSTANCE));
      assertThat(createLock(toClose, myTempDir, "2", "2-").lock(), CoreMatchers.equalTo(DesktopImportantFolderLocker.ActivateStatus.NO_INSTANCE));

      assertThat(createLock(toClose, myTempDir, "2", "2-").lock(), CoreMatchers.equalTo(DesktopImportantFolderLocker.ActivateStatus.ACTIVATED));
      assertThat(createLock(toClose, myTempDir, "1", "1-").lock(), CoreMatchers.equalTo(DesktopImportantFolderLocker.ActivateStatus.ACTIVATED));
      assertThat(createLock(toClose, myTempDir, "1.1", "1-1").lock(), CoreMatchers.equalTo(DesktopImportantFolderLocker.ActivateStatus.ACTIVATED));
    }
    finally {
      toClose.forEach(DesktopImportantFolderLocker::dispose);
    }
  }

  @Nonnull
  private static DesktopImportantFolderLocker createLock(@Nonnull List<DesktopImportantFolderLocker> toClose, @Nonnull File dir, @Nonnull String c, @Nonnull String s) {
    DesktopImportantFolderLocker lock = new DesktopImportantFolderLocker(dir.getPath() + "/" + c, dir.getPath() + "/" + s);
    toClose.add(lock);
    return lock;
  }

  @Test(timeout = 30000)
  public void testDispose() throws Exception {
    DesktopImportantFolderLocker lock1 = new DesktopImportantFolderLocker(myTempDir.getPath() + "/1", myTempDir.getPath() + "/1-");
    DesktopImportantFolderLocker lock2 = new DesktopImportantFolderLocker(myTempDir.getPath() + "/1", myTempDir.getPath() + "/1-");

    assertThat(lock1.lock(), CoreMatchers.equalTo(DesktopImportantFolderLocker.ActivateStatus.NO_INSTANCE));
    assertThat(lock2.lock(), CoreMatchers.equalTo(DesktopImportantFolderLocker.ActivateStatus.ACTIVATED));

    lock1.dispose();
    assertThat(lock2.lock(), CoreMatchers.equalTo(DesktopImportantFolderLocker.ActivateStatus.NO_INSTANCE));
    lock2.dispose();
  }
}
