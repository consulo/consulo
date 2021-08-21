/*
 * Copyright 2013-2017 consulo.io
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

import com.google.gson.Gson;
import com.intellij.errorreport.error.AuthorizationFailedException;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.components.RoamingType;
import com.intellij.openapi.components.StateStorage;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectBundle;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.wm.IdeFrame;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.util.concurrency.AppExecutorUtil;
import com.intellij.util.io.UnsyncByteArrayInputStream;
import com.intellij.util.ui.UIUtil;
import consulo.components.impl.stores.IApplicationStore;
import consulo.components.impl.stores.storage.StateStorageManager;
import consulo.ide.webService.WebServiceApi;
import consulo.ide.webService.WebServicesConfiguration;
import consulo.logging.Logger;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;

/**
 * @author VISTALL
 * @since 12-Feb-17
 */
class ExternalStorageQueue {
  static class PushFileResponse {
    private int modCount;
  }

  static class LoadItem {
    private final String myFileSpec;
    private final RoamingType myRoamingType;
    private final StateStorageManager myStateStorageManager;

    private int myModCount;

    LoadItem(String fileSpec, RoamingType roamingType, int modCount, StateStorageManager stateStorageManager) {
      myFileSpec = fileSpec;
      myRoamingType = roamingType;
      myModCount = modCount;
      myStateStorageManager = stateStorageManager;
    }
  }

  private static final Logger LOG = Logger.getInstance(ExternalStorageQueue.class);
  private static final byte[] ourNotModifiedBytes = new byte[0];

  private final ScheduledExecutorService myExecutorService = AppExecutorUtil.createBoundedScheduledExecutorService("External Storage Pool", 1);

  private final Map<String, Ref<byte[]>> myLoadedBytes = new ConcurrentHashMap<>();
  private final Map<String, LoadItem> myLoadItems = new ConcurrentHashMap<>();
  private final ExternalStorage myExternalStorage;
  private final IApplicationStore myApplicationStore;

  private Future<?> myLoadTask = CompletableFuture.completedFuture(null);

  ExternalStorageQueue(ExternalStorage externalStorage, IApplicationStore applicationStore) {
    myExternalStorage = externalStorage;
    myApplicationStore = applicationStore;
  }

  @Nullable
  public Ref<byte[]> getContent(String fileSpec, RoamingType roamingType) {
    return myLoadedBytes.remove(ExternalStorage.buildFileSpec(roamingType, fileSpec));
  }

  void wantLoad(@Nonnull String fileSpec, @Nonnull RoamingType roamingType, int mod, @Nonnull StateStorageManager stateStorageManager) {
    myLoadTask.cancel(false);

    myLoadItems.put(ExternalStorage.buildFileSpec(roamingType, fileSpec), new LoadItem(fileSpec, roamingType, mod, stateStorageManager));

    myLoadTask = myExecutorService.schedule(this::fetchFilesFromServer, 10, TimeUnit.SECONDS);
  }

  void fetchFilesFromServer() {
    if (myLoadItems.isEmpty()) {
      return;
    }

    LoadItem[] items = myLoadItems.values().toArray(new LoadItem[myLoadItems.size()]);
    myLoadItems.clear();

    Map<LoadItem, byte[]> map = new HashMap<>();

    Arrays.stream(items)/*.parallel()*/.forEach(item -> {
      URIBuilder urlBuilder;
      try {
        urlBuilder = new URIBuilder(WebServiceApi.SYNCHRONIZE_API.buildUrl("getFile"));
      }
      catch (URISyntaxException e1) {
        throw new RuntimeException(e1);
      }

      urlBuilder.addParameter("filePath", ExternalStorage.buildFileSpec(item.myRoamingType, item.myFileSpec));
      urlBuilder.addParameter("modCount", String.valueOf(item.myModCount));

      try (CloseableHttpClient client = HttpClients.createDefault()) {
        HttpGet request = new HttpGet(urlBuilder.build());
        String authKey = WebServicesConfiguration.getInstance().getOAuthKey(WebServiceApi.SYNCHRONIZE_API);
        if (authKey != null) {
          request.addHeader("Authorization", "Bearer " + authKey);
        }

        byte[] data = client.execute(request, response -> {
          int statusCode = response.getStatusLine().getStatusCode();
          switch (statusCode) {
            case HttpURLConnection.HTTP_UNAUTHORIZED:
              throw new AuthorizationFailedException();
            case HttpURLConnection.HTTP_NOT_FOUND:
              return null;
            case HttpURLConnection.HTTP_NOT_MODIFIED:
              return ourNotModifiedBytes;
            case HttpURLConnection.HTTP_OK:
              return EntityUtils.toByteArray(response.getEntity());
            default:
              throw new RuntimeException("not OK: " + statusCode);
          }
        });

        // parallel stream
        synchronized (map) {
          map.put(item, data);
        }
      }
      catch (Exception e) {
        e.printStackTrace();
      }
    });

    Map<String, StateStorage> stateStorages = new HashMap<>();
    for (Map.Entry<LoadItem, byte[]> entry : map.entrySet()) {
      try {
        LoadItem key = entry.getKey();
        byte[] value = entry.getValue();

        // not changed
        if (value == ourNotModifiedBytes) {
          continue;
        }

        // if on server data was not found - delete local file
        if (value == null) {
          // if file not deleted - there no data at server, skip if we don't have data too
          if (!myExternalStorage.deleteWithoutServer(key.myFileSpec, key.myRoamingType)) {
            continue;
          }
        }
        else {
          writeLocalFile(myExternalStorage.getProxyDirectory(), key.myFileSpec, key.myRoamingType, value);
        }

        StateStorageManager stateStorageManager = entry.getKey().myStateStorageManager;

        StateStorage storage = stateStorageManager.getStateStorage(key.myFileSpec, key.myRoamingType);

        assert storage != null;

        myLoadedBytes.put(ExternalStorage.buildFileSpec(key.myRoamingType, key.myFileSpec), Ref.create(value));

        stateStorages.put(key.myFileSpec, storage);

        if (value == null) {
          LOG.info("Dropping storage: " + key.myFileSpec);
        }
        else {
          LOG.info("Refreshing storage: " + key.myFileSpec + " from " + key.myModCount + " to " + DataCompressor.uncompress(new UnsyncByteArrayInputStream(value)).getSecond());
        }
      }
      catch (Exception e) {
        LOG.error(e);
      }
    }

    if (stateStorages.isEmpty()) {
      return;
    }

    UIUtil.invokeLaterIfNeeded(() -> {
      IApplicationStore stateStore = myApplicationStore;

      String files = StringUtil.join(stateStorages.keySet(), "<br>");

      Project project = null;
      Project[] openProjects = ProjectManager.getInstance().getOpenProjects();
      for (Project openProject : openProjects) {
        IdeFrame ideFrame = WindowManager.getInstance().getIdeFrame(openProject);
        if (ideFrame.isActive()) {
          project = openProject;
          break;
        }
      }

      new Notification("externalStorage", "External Storage", "Settings updated from server<br>" + files, NotificationType.INFORMATION).notify(project);

      try {
        WriteAction.compute(() -> stateStore.reload(stateStorages.values()));
      }
      catch (Exception e) {
        Messages.showWarningDialog(ProjectBundle.message("project.reload.failed", e.getMessage()), ProjectBundle.message("project.reload.failed.title"));
      }
    });
  }

  void deleteFromServer(String fileSpec, RoamingType roamingType) {
    myExecutorService.execute(() -> {
      try (CloseableHttpClient client = HttpClients.createDefault()) {
        URIBuilder urlBuilder;
        try {
          urlBuilder = new URIBuilder(WebServiceApi.SYNCHRONIZE_API.buildUrl("deleteFile"));
        }
        catch (URISyntaxException e1) {
          throw new RuntimeException(e1);
        }

        urlBuilder.addParameter("filePath", ExternalStorage.buildFileSpec(roamingType, fileSpec));

        HttpGet request = new HttpGet(urlBuilder.build());
        String authKey = WebServicesConfiguration.getInstance().getOAuthKey(WebServiceApi.SYNCHRONIZE_API);
        if (authKey != null) {
          request.addHeader("Authorization", "Bearer " + authKey);
        }

        client.execute(request, response -> {
          int statusCode = response.getStatusLine().getStatusCode();
          switch (statusCode) {
            case HttpURLConnection.HTTP_UNAUTHORIZED:
              throw new AuthorizationFailedException();
          }

          return null;
        });
      }
      catch (Exception e) {
        LOG.error(e);
      }
    });
  }

  private File writeLocalFile(@Nonnull File proxyDirectory, @Nonnull String fileSpec, RoamingType roamingType, byte[] compressedData) throws IOException {
    File file = new File(proxyDirectory, ExternalStorage.buildFileSpec(roamingType, fileSpec));
    FileUtil.createParentDirs(file);
    FileUtil.writeToFile(file, compressedData);
    return file;
  }

  void wantSaveToServer(@Nonnull File proxyDirectory, @Nonnull String fileSpec, RoamingType roamingType, byte[] compressedData) {
    myExecutorService.execute(() -> {
      try {
        writeLocalFile(proxyDirectory, fileSpec, roamingType, compressedData);
      }
      catch (IOException e) {
        LOG.error(e);
        return;
      }

      Gson gson = new Gson();

      try (CloseableHttpClient client = HttpClients.createDefault()) {
        URIBuilder urlBuilder = new URIBuilder(WebServiceApi.SYNCHRONIZE_API.buildUrl("pushFile"));

        String buildFileSpec = ExternalStorage.buildFileSpec(roamingType, fileSpec);

        PushFileRequestBean bean = new PushFileRequestBean(buildFileSpec, compressedData);

        HttpPost request = new HttpPost(urlBuilder.build());
        request.setHeader("Content-Type", "application/json");
        request.setEntity(new StringEntity(gson.toJson(bean)));

        String authKey = WebServicesConfiguration.getInstance().getOAuthKey(WebServiceApi.SYNCHRONIZE_API);
        if (authKey != null) {
          request.addHeader("Authorization", "Bearer " + authKey);
        }

        PushFileResponse pushFileResponse = client.execute(request, response -> {
          if (response.getStatusLine().getStatusCode() == HttpURLConnection.HTTP_UNAUTHORIZED) {
            throw new AuthorizationFailedException();
          }

          if (response.getStatusLine().getStatusCode() != HttpURLConnection.HTTP_OK) {
            throw new IllegalArgumentException(EntityUtils.toString(response.getEntity()));
          }
          return gson.fromJson(EntityUtils.toString(response.getEntity()), PushFileResponse.class);
        });

        Pair<byte[], Integer> uncompressPair = DataCompressor.uncompress(new UnsyncByteArrayInputStream(compressedData));

        byte[] newCompressedData = DataCompressor.compress(uncompressPair.getFirst(), pushFileResponse.modCount);

        writeLocalFile(proxyDirectory, fileSpec, roamingType, newCompressedData);

        // refresh mod count if settings was updated before server update
        LoadItem loadItem = myLoadItems.get(buildFileSpec);
        if (loadItem != null) {
          loadItem.myModCount = pushFileResponse.modCount;
        }

        LOG.info("Updated file at server: " + fileSpec + ", new mod count: " + pushFileResponse.modCount);
      }
      catch (Exception e) {
        LOG.error(e);
      }
    });
  }
}
