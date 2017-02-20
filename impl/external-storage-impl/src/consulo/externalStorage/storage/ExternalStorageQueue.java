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
import com.intellij.openapi.components.RoamingType;
import com.intellij.openapi.components.StateStorage;
import com.intellij.openapi.components.impl.stores.StateStorageManager;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.util.concurrency.AppExecutorUtil;
import consulo.externalStorage.ExternalStorageUtil;
import consulo.ide.webService.WebServiceApi;
import consulo.ide.webService.WebServicesConfiguration;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.util.*;
import java.util.concurrent.*;

/**
 * @author VISTALL
 * @since 12-Feb-17
 */
class ExternalStorageQueue {
  static class PushFile {
    private int modCount;
  }

  static class LoadItem {
    private final String myFileSpec;
    private final RoamingType myRoamingType;
    private final int myModCount;
    private final StateStorageManager myStateStorageManager;

    LoadItem(String fileSpec, RoamingType roamingType, int modCount, StateStorageManager stateStorageManager) {
      myFileSpec = fileSpec;
      myRoamingType = roamingType;
      myModCount = modCount;
      myStateStorageManager = stateStorageManager;
    }
  }

  private static final byte[] ourNotModifiedBytes = new byte[0];

  private final List<LoadItem> myLoadItems = new CopyOnWriteArrayList<>();
  private final Map<String, Ref<byte[]>> myLoadedBytes = new ConcurrentHashMap<>();

  private Future<?> myLoadTask = CompletableFuture.completedFuture(null);

  @Nullable
  public Ref<byte[]> getContent(String fileSpec, RoamingType roamingType) {
    return myLoadedBytes.remove(ExternalStorage.buildFileSpec(roamingType, fileSpec));
  }

  void wantLoad(@NotNull String fileSpec, @NotNull RoamingType roamingType, int mod, @NotNull StateStorageManager stateStorageManager) {
    myLoadTask.cancel(false);

    myLoadItems.add(new LoadItem(fileSpec, roamingType, mod, stateStorageManager));

    myLoadTask = AppExecutorUtil.getAppScheduledExecutorService().schedule(this::executeLoad, 1, TimeUnit.MINUTES);
  }

  void executeLoad() {
    if (myLoadItems.isEmpty()) {
      return;
    }

    LoadItem[] items = myLoadItems.toArray(new LoadItem[myLoadItems.size()]);
    myLoadItems.removeAll(Arrays.asList(items));

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
          request.addHeader("Authorization", authKey);
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
      catch (AuthorizationFailedException ae) {
        ae.printStackTrace();
      }
      catch (Exception e) {
        e.printStackTrace();
      }
    });

    Set<StateStorage> stateStorages = new HashSet<>();
    for (Map.Entry<LoadItem, byte[]> entry : map.entrySet()) {
      try {
        LoadItem key = entry.getKey();
        byte[] value = entry.getValue();

        // not changed
        if (value == ourNotModifiedBytes) {
          continue;
        }

        StateStorageManager stateStorageManager = entry.getKey().myStateStorageManager;

        StateStorage storage = stateStorageManager.getStateStorage(key.myFileSpec, key.myRoamingType);

        assert storage != null;

        myLoadedBytes.put(ExternalStorage.buildFileSpec(key.myRoamingType, key.myFileSpec), Ref.create(value));

        stateStorages.add(storage);
        System.out.println("changed" + key.myFileSpec);
      }
      catch (Exception e) {
        e.printStackTrace();
      }
    }

   /* ComponentStoreImpl.ReloadComponentStoreStatus status =
            ComponentStoreImpl.reloadStore(stateStorages, ((ApplicationEx2)ApplicationManager.getApplication()).getStateStore());

    if (status == ComponentStoreImpl.ReloadComponentStoreStatus.RESTART_AGREED) {
      ApplicationManagerEx.getApplicationEx().restart(true);
    } */
  }

  void wantSave(@NotNull File proxyDirectory, @NotNull String fileSpec, RoamingType roamingType, byte[] content) throws IOException {
    File file = new File(proxyDirectory, ExternalStorage.buildFileSpec(roamingType, fileSpec));
    FileUtil.createParentDirs(file);
    FileUtil.writeToFile(file, content);

    try (CloseableHttpClient client = HttpClients.createDefault()) {
      URIBuilder urlBuilder = new URIBuilder(WebServiceApi.SYNCHRONIZE_API.buildUrl("pushFile"));
      urlBuilder.addParameter("filePath", ExternalStorage.buildFileSpec(roamingType, fileSpec));

      HttpPost request = new HttpPost(urlBuilder.build());
      request.setEntity(new ByteArrayEntity(content));
      String authKey = WebServicesConfiguration.getInstance().getOAuthKey(WebServiceApi.SYNCHRONIZE_API);
      if (authKey != null) {
        request.addHeader("Authorization", authKey);
      }

      PushFile pushFile = client.execute(request, response -> {
        if (response.getStatusLine().getStatusCode() == HttpURLConnection.HTTP_UNAUTHORIZED) {
          throw new AuthorizationFailedException();
        }
        return new Gson().fromJson(EntityUtils.toString(response.getEntity()), PushFile.class);
      });

      File modCountFile = ExternalStorageUtil.getModCountFile(file);
      FileUtil.writeToFile(modCountFile, String.valueOf(pushFile.modCount));
    }
    catch (URISyntaxException e) {
      throw new IOException(e);
    }
  }
}
