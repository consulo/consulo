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
package consulo.execution.test;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.application.util.concurrent.AppExecutorUtil;
import consulo.application.util.function.ThrowableComputable;
import consulo.container.boot.ContainerPathManager;
import consulo.disposer.Disposable;
import consulo.index.io.EnumeratorStringDescriptor;
import consulo.index.io.PersistentHashMap;
import consulo.index.io.data.DataExternalizer;
import consulo.index.io.data.IOUtil;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.util.io.FileUtil;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * @author Dmitry Avdeev
 */
@Singleton
@ServiceAPI(ComponentScope.PROJECT)
@ServiceImpl
public class TestStateStorage implements Disposable {

  private static final File TEST_HISTORY_PATH = new File(ContainerPathManager.get().getSystemPath(), "testHistory");

  private static final int CURRENT_VERSION = 1;

  private final File myFile;

  public static File getTestHistoryRoot(Project project) {
    return new File(TEST_HISTORY_PATH, project.getLocationHash());
  }

  public static class Record {
    public final int magnitude;
    public final long configurationHash;
    public final Date date;

    public Record(int magnitude, Date date, long configurationHash) {
      this.magnitude = magnitude;
      this.date = date;
      this.configurationHash = configurationHash;
    }
  }

  private static final Logger LOG = Logger.getInstance(TestStateStorage.class);
  @Nullable
  private PersistentHashMap<String, Record> myMap;
  private volatile ScheduledFuture<?> myMapFlusher;

  public static TestStateStorage getInstance(@Nonnull Project project) {
    return project.getInstance(TestStateStorage.class);
  }

  @Inject
  public TestStateStorage(Project project) {
    String directoryPath = getTestHistoryRoot(project).getPath();

    myFile = new File(directoryPath + "/testStateMap");
    FileUtil.createParentDirs(myFile);

    try {
      myMap = initializeMap();
    }
    catch (IOException e) {
      LOG.error(e);
    }
    myMapFlusher = AppExecutorUtil.getAppScheduledExecutorService().scheduleWithFixedDelay(this::flushMap, 5, 5, TimeUnit.SECONDS);
  }

  private PersistentHashMap<String, Record> initializeMap() throws IOException {
    return IOUtil.openCleanOrResetBroken(getComputable(myFile), myFile);
  }

  private synchronized void flushMap() {
    if (myMapFlusher == null) return; // disposed
    if (myMap != null && myMap.isDirty()) myMap.force();
  }

  @Nonnull
  private static ThrowableComputable<PersistentHashMap<String, Record>, IOException> getComputable(File file) {
    return () -> new PersistentHashMap<>(file, EnumeratorStringDescriptor.INSTANCE, new DataExternalizer<Record>() {
      @Override
      public void save(@Nonnull DataOutput out, Record value) throws IOException {
        out.writeInt(value.magnitude);
        out.writeLong(value.date.getTime());
        out.writeLong(value.configurationHash);
      }

      @Override
      public Record read(@Nonnull DataInput in) throws IOException {
        return new Record(in.readInt(), new Date(in.readLong()), in.readLong());
      }
    }, 4096, CURRENT_VERSION);
  }

  @Nullable
  public synchronized Record getState(String testUrl) {
    try {
      return myMap == null ? null : myMap.get(testUrl);
    }
    catch (IOException e) {
      thingsWentWrongLetsReinitialize(e, "Can't get state for " + testUrl);
      return null;
    }
  }

  public synchronized void removeState(String url) {
    if (myMap != null) {
      try {
        myMap.remove(url);
      }
      catch (IOException e) {
        thingsWentWrongLetsReinitialize(e, "Can't remove state for " + url);
      }
    }
  }

  @Nullable
  public synchronized Map<String, Record> getRecentTests(int limit, Date since) {
    if (myMap == null) return null;

    Map<String, Record> result = new HashMap<>();
    try {
      for (String key : myMap.getAllKeysWithExistingMapping()) {
        Record record = myMap.get(key);
        if (record != null && record.date.compareTo(since) > 0) {
          result.put(key, record);
          if (result.size() >= limit) {
            break;
          }
        }
      }
    }
    catch (IOException e) {
      thingsWentWrongLetsReinitialize(e, "Can't get recent tests");
    }

    return result;
  }

  public synchronized void writeState(@Nonnull String testUrl, Record record) {
    if (myMap == null) return;
    try {
      myMap.put(testUrl, record);
    }
    catch (IOException e) {
      thingsWentWrongLetsReinitialize(e, "Can't write state for " + testUrl);
    }
  }

  @Override
  public synchronized void dispose() {
    myMapFlusher.cancel(false);
    myMapFlusher = null;
    if (myMap == null) return;
    try {
      myMap.close();
    }
    catch (IOException e) {
      LOG.error(e);
    }
    finally {
      myMap = null;
    }
  }

  private void thingsWentWrongLetsReinitialize(IOException e, String message) {
    try {
      if (myMap != null) {
        try {
          myMap.close();
        }
        catch (IOException ignore) {
        }
        IOUtil.deleteAllFilesStartingWith(myFile);
      }
      myMap = initializeMap();
      LOG.error(message, e);
    }
    catch (IOException e1) {
      LOG.error("Cannot repair", e1);
      myMap = null;
    }
  }
}
