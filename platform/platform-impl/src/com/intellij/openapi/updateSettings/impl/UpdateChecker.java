/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package com.intellij.openapi.updateSettings.impl;

import com.intellij.ide.IdeBundle;
import com.intellij.ide.plugins.*;
import com.intellij.ide.reporter.ConnectionException;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.notification.*;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationInfo;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.application.ex.ApplicationInfoEx;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.*;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.StandardFileSystems;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.Function;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.io.UrlConnectionUtil;
import com.intellij.util.net.HttpConfigurable;
import com.intellij.util.text.DateFormatUtil;
import com.intellij.util.ui.UIUtil;
import consulo.lombok.annotations.Logger;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import consulo.SharedConstants;

import javax.swing.event.HyperlinkEvent;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.*;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * XML sample:
 * <pre>{@code
 * <idea>
 *   <build>456</build>
 *   <version>4.5.2</version>
 *   <title>New Intellij IDEA Version</title>
 *   <message>
 *     New version of IntelliJ IDEA is available.
 *     Please visit http://www.intellij.com/ for more info.
 *   </message>
 * </idea>
 * }</pre>
 *
 * @author mike
 *         Date: Oct 31, 2002
 */
@Logger
public final class UpdateChecker {
  private static Map<String, String> ADDITIONAL_REQUEST_OPTIONS = new HashMap<String, String>();

  @NonNls private static final String INSTALLATION_UID = "installation.uid";

  private UpdateChecker() {
  }

  public static void showConnectionErrorDialog() {
    Messages.showErrorDialog(IdeBundle.message("error.checkforupdates.connection.failed"), IdeBundle.message("title.connection.error"));
  }

  public enum DownloadPatchResult {
    SUCCESS, FAILED, CANCELED
  }

  private static boolean myVeryFirstOpening = true;

  @NonNls
  private static final String DISABLED_UPDATE = "disabled_update.txt";
  private static TreeSet<String> ourDisabledToUpdatePlugins;


  public static boolean isMyVeryFirstOpening() {
    return myVeryFirstOpening;
  }

  public static void setMyVeryFirstOpening(final boolean myVeryFirstProjectOpening) {
    myVeryFirstOpening = myVeryFirstProjectOpening;
  }

  public static boolean checkNeeded() {
    final UpdateSettings settings = UpdateSettings.getInstance();
    if (settings == null) return false;

    final long timeDelta = System.currentTimeMillis() - settings.getLastTimeChecked();
    if (Math.abs(timeDelta) < DateFormatUtil.DAY) return false;

    return settings.isCheckNeeded();
  }

  public static ActionCallback updateAndShowResult() {
    final ActionCallback result = new ActionCallback();
    final Application app = ApplicationManager.getApplication();
    final UpdateSettings updateSettings = UpdateSettings.getInstance();
    if (!updateSettings.isCheckNeeded()) {
      result.setDone();
      return result;
    }
    app.executeOnPooledThread(new Runnable() {
      @Override
      public void run() {
        final CheckForUpdateResult checkForUpdateResult = checkForUpdates(updateSettings, false);

        final List<Couple<IdeaPluginDescriptor>> updatedPlugins = loadPluginsForUpdate(false, null, null);
        app.invokeLater(new Runnable() {
          @Override
          public void run() {
            showUpdateResult(checkForUpdateResult, updatedPlugins, true, true, false);
            result.setDone();
          }
        });
      }
    });
    return result;
  }

  /**
   * Return list of couple PluginDescriptor. First is current plugin, Second is target for update
   */
  @Nullable
  public static List<Couple<IdeaPluginDescriptor>> loadPluginsForUpdate(final boolean showErrorDialog,
                                                                        final @Nullable PluginHostsConfigurable hostsConfigurable,
                                                                        @Nullable ProgressIndicator indicator) {
    final List<Couple<IdeaPluginDescriptor>> targets = new ArrayList<Couple<IdeaPluginDescriptor>>();
    final Set<String> failed = new HashSet<String>();
    List<IdeaPluginDescriptor> remotePluginDescriptors = new ArrayList<IdeaPluginDescriptor>();
    for (String host : getPluginHosts(hostsConfigurable)) {
      try {
        remotePluginDescriptors.addAll(loadPluginDescriptionsFromHost(host, indicator));
      }
      catch (ProcessCanceledException e) {
        return null;
      }
      catch (Exception e) {
        LOGGER.info(e);
        failed.add(host);
      }
    }
    try {
      remotePluginDescriptors.addAll(RepositoryHelper.loadPluginsFromRepository(indicator));
    }
    catch (ProcessCanceledException e) {
      return null;
    }
    catch (Exception e) {
      LOGGER.info(e);
    }

    final Map<PluginId, IdeaPluginDescriptor> ourPlugins = new HashMap<PluginId, IdeaPluginDescriptor>();
    final IdeaPluginDescriptor[] installedPlugins = PluginManagerCore.getPlugins();
    final List<String> disabledPlugins = PluginManagerCore.getDisabledPlugins();
    for (IdeaPluginDescriptor installedPlugin : installedPlugins) {
      if (!installedPlugin.isBundled() && !disabledPlugins.contains(installedPlugin.getPluginId().getIdString())) {
        ourPlugins.put(installedPlugin.getPluginId(), installedPlugin);
      }
    }

    final PluginManagerUISettings updateSettings = PluginManagerUISettings.getInstance();
    updateSettings.myOutdatedPlugins.clear();
    if (!ourPlugins.isEmpty()) {
      try {
        for (final Map.Entry<PluginId, IdeaPluginDescriptor> entry : ourPlugins.entrySet()) {
          final PluginId pluginId = entry.getKey();

          List<IdeaPluginDescriptor> filter = ContainerUtil.filter(remotePluginDescriptors, new Condition<IdeaPluginDescriptor>() {
            @Override
            public boolean value(IdeaPluginDescriptor ideaPluginDescriptor) {
              return pluginId.equals(ideaPluginDescriptor.getPluginId());
            }
          });

          if (filter.isEmpty()) {
            continue;
          }

          for (IdeaPluginDescriptor filtered : filter) {
            if (StringUtil.compareVersionNumbers(filtered.getVersion(), entry.getValue().getVersion()) > 0) {
              updateSettings.myOutdatedPlugins.add(pluginId.toString());
              targets.add(Couple.of(entry.getValue(), filtered));
            }
          }
        }
      }
      catch (ProcessCanceledException ignore) {
        return null;
      }
      catch (Exception e) {
        showErrorMessage(showErrorDialog, e.getMessage());
      }
    }

    if (!failed.isEmpty()) {
      showErrorMessage(showErrorDialog, IdeBundle.message("connection.failed.message", StringUtil.join(failed, ",")));
    }
    return targets;
  }

  @NotNull
  public static List<IdeaPluginDescriptor> loadPluginDescriptionsFromHost(@NotNull String host, @Nullable ProgressIndicator indicator) throws Exception {
    InputStream inputStream = loadVersionInfo(host);
    if (inputStream == null) return Collections.emptyList();

    final List<IdeaPluginDescriptor> descriptors = RepositoryHelper.loadPluginsFromDescription(inputStream, indicator);
    for (IdeaPluginDescriptor descriptor : descriptors) {
      ((PluginNode)descriptor).setRepositoryName(host);
    }
    return descriptors;
  }

  private static void showErrorMessage(boolean showErrorDialog, final String failedMessage) {
    if (showErrorDialog) {
      UIUtil.invokeLaterIfNeeded(new Runnable() {
        @Override
        public void run() {
          Messages.showErrorDialog(failedMessage, IdeBundle.message("title.connection.error"));
        }
      });
    }
    else {
      LOGGER.info(failedMessage);
    }
  }

  private static List<String> getPluginHosts(@Nullable PluginHostsConfigurable hostsConfigurable) {
    final ArrayList<String> hosts = new ArrayList<String>();
    if (hostsConfigurable != null) {
      hosts.addAll(hostsConfigurable.getPluginsHosts());
    }
    else {
      hosts.addAll(UpdateSettings.getInstance().getStoredPluginHosts());
    }
    final String pluginHosts = System.getProperty("idea.plugin.hosts");
    if (pluginHosts != null) {
      ContainerUtil.addAll(hosts, pluginHosts.split(";"));
    }
    return hosts;
  }

  public static boolean checkPluginsHost(final String host, final List<PluginDownloader> downloaded) throws Exception {
    try {
      return checkPluginsHost(host, downloaded, true, null);
    }
    catch (ProcessCanceledException e) {
      return false;
    }
  }

  public static boolean checkPluginsHost(final String host,
                                         final List<PluginDownloader> downloaded,
                                         final boolean collectToUpdate,
                                         @Nullable ProgressIndicator indicator) throws Exception {
    InputStream inputStream = loadVersionInfo(host);
    if (inputStream == null) return false;
    final Document document;
    try {
      document = JDOMUtil.loadDocument(inputStream);
    }
    catch (JDOMException e) {
      return false;
    }

    inputStream = loadVersionInfo(host);
    if (inputStream == null) return false;
    final List<IdeaPluginDescriptor> descriptors = RepositoryHelper.loadPluginsFromDescription(inputStream, indicator);
    for (IdeaPluginDescriptor descriptor : descriptors) {
      ((PluginNode)descriptor).setRepositoryName(host);
      downloaded.add(PluginDownloader.createDownloader(descriptor));
    }

    boolean success = true;
    for (Element plugin : document.getRootElement().getChildren("plugin")) {
      final String pluginId = plugin.getAttributeValue("id");
      final String pluginUrl = plugin.getAttributeValue("url");
      final String pluginVersion = plugin.getAttributeValue("version");
      final Element descriptionElement = plugin.getChild("description");
      final String description;
      if (descriptionElement != null) {
        description = descriptionElement.getText();
      }
      else {
        description = null;
      }

      final List<PluginId> dependsPlugins = new ArrayList<PluginId>();
      final List<PluginId> optionalDependsPlugins = new ArrayList<PluginId>();
      for (Element depend : plugin.getChildren("depends")) {
        String optional = depend.getAttributeValue("optional");
        if (optional != null && Boolean.parseBoolean(optional)) {
          optionalDependsPlugins.add(PluginId.getId(depend.getText()));
        }
        else {
          dependsPlugins.add(PluginId.getId(depend.getText()));
        }
      }

      if (pluginId == null) {
        LOGGER.info("plugin id should not be null");
        success = false;
        continue;
      }

      if (pluginUrl == null) {
        LOGGER.info("plugin url should not be null");
        success = false;
        continue;
      }

      final VirtualFile pluginFile = PluginDownloader.findPluginFile(pluginUrl, host);
      if (pluginFile == null) continue;

      if (collectToUpdate) {
        final String finalPluginUrl = getPluginUrl(pluginFile);
        final Runnable updatePluginRunnable = new Runnable() {
          @Override
          public void run() {
            try {
              final ProgressIndicator progressIndicator = ProgressManager.getInstance().getProgressIndicator();
              if (progressIndicator != null) {
                progressIndicator.setText2(finalPluginUrl);
              }
              final PluginDownloader downloader = new PluginDownloader(pluginId, finalPluginUrl, pluginVersion);
              if (downloader.prepareToInstall()) {
                downloaded.add(downloader);
              }
            }
            catch (IOException e) {
              LOGGER.info(e);
            }
          }
        };
        if (ApplicationManager.getApplication().isDispatchThread()) {
          ProgressManager.getInstance()
                  .runProcessWithProgressSynchronously(updatePluginRunnable, IdeBundle.message("update.uploading.plugin.progress.title"), true, null);
        }
        else {
          updatePluginRunnable.run();
        }
      }
      else {
        final PluginDownloader downloader = new PluginDownloader(pluginId, pluginUrl, pluginVersion);
        downloader.setDescription(description);
        downloader.setDepends(dependsPlugins, optionalDependsPlugins);
        downloaded.add(downloader);
      }
    }
    return success;
  }

  @NotNull
  private static String getPluginUrl(@NotNull VirtualFile pluginFile) {
    String protocol = pluginFile.getFileSystem().getProtocol();
    if (StandardFileSystems.FILE_PROTOCOL.equals(protocol) && SystemInfo.isWindows) {
      String path = pluginFile.getPath();
      if (path.length() != 0 && path.charAt(0) != '/') {
        return protocol + ":///" + path;  // fix file URI on Windows
      }
    }

    return pluginFile.getUrl();
  }

  @NotNull
  public static CheckForUpdateResult doCheckForUpdates(final UpdateSettings settings) {
    ApplicationInfo appInfo = ApplicationInfo.getInstance();
    BuildNumber currentBuild = appInfo.getBuild();
    int majorVersion = Integer.parseInt(appInfo.getMajorVersion());
    final UpdatesXmlLoader loader = new UpdatesXmlLoader(ApplicationInfoEx.getInstanceEx().getUpdatesInfoUrl());
    final Product info;
    try {
      info = loader.loadProductInfo();
      if (info == null) {
        return new CheckForUpdateResult(UpdateStrategy.State.NOTHING_LOADED);
      }
    }
    catch (ConnectionException e) {
      return new CheckForUpdateResult(UpdateStrategy.State.CONNECTION_ERROR, e);
    }

    UpdateStrategy strategy = new UpdateStrategy(majorVersion, currentBuild, info, settings);
    return strategy.checkForUpdates();
  }

  public static void addUpdateRequestParameter(String name) {
    addUpdateRequestParameter(name, "");
  }

  public static void addUpdateRequestParameter(@NotNull String name, @NotNull String value) {
    ADDITIONAL_REQUEST_OPTIONS.put(name, value);
  }

  @NotNull
  public static CheckForUpdateResult checkForUpdates(final UpdateSettings updateSettings, final boolean disregardIgnoredBuilds) {
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("enter: auto checkForUpdates()");
    }

    UserUpdateSettings settings = updateSettings;
    if (disregardIgnoredBuilds) {
      settings = new UserUpdateSettings() {
        @NotNull
        @Override
        public List<String> getKnownChannelsIds() {
          return updateSettings.getKnownChannelsIds();
        }

        @Override
        public List<String> getIgnoredBuildNumbers() {
          return Collections.emptyList();
        }

        @Override
        public void setKnownChannelIds(List<String> ids) {
          updateSettings.setKnownChannelIds(ids);
        }

        @NotNull
        @Override
        public ChannelStatus getSelectedChannelStatus() {
          return updateSettings.getSelectedChannelStatus();
        }
      };
    }

    final CheckForUpdateResult result = doCheckForUpdates(updateSettings);

    if (result.getState() == UpdateStrategy.State.LOADED) {
      updateSettings.saveLastCheckedInfo();
      settings.setKnownChannelIds(result.getAllChannelsIds());
    }

    return result;
  }

  private static boolean ourUpdateInfoDialogShown = false;

  public static void showUpdateResult(final CheckForUpdateResult checkForUpdateResult,
                                      final List<Couple<IdeaPluginDescriptor>> targetsForUpdate,
                                      final boolean showConfirmation,
                                      final boolean enableLink,
                                      final boolean alwaysShowResults) {
    UpdateChannel channelToPropose = checkForUpdateResult.getChannelToPropose();
    if (channelToPropose != null && channelToPropose.getLatestBuild() != null) {
      NewChannelDialog dialog = new NewChannelDialog(channelToPropose);
      dialog.setModal(alwaysShowResults);
      dialog.show();
    }
    else {
      final boolean showBalloonNotification = !alwaysShowResults && ProjectManager.getInstance().getOpenProjects().length > 0;
      if (checkForUpdateResult.hasNewBuildInSelectedChannel() && !ourUpdateInfoDialogShown) {
        final Runnable showUpdatesDialogRunnable = new Runnable() {
          @Override
          public void run() {
            UpdateInfoDialog dialog = new UpdateInfoDialog(true, checkForUpdateResult.getUpdatedChannel(), targetsForUpdate, enableLink) {
              @Override
              protected void dispose() {
                ourUpdateInfoDialogShown = false;
                super.dispose();
              }
            };
            dialog.setModal(alwaysShowResults);
            ourUpdateInfoDialogShown = true;
            dialog.show();
          }
        };
        if (showBalloonNotification) {
          final String message = ApplicationInfo.getInstance().getVersionName() + " is ready to <a href=\"update\">update</a>";
          showBalloonNotification(showUpdatesDialogRunnable, message);
        }
        else {
          showUpdatesDialogRunnable.run();
        }
      }
      else {
        final Runnable showPluginsUpdateDialogRunnable = new Runnable() {
          @Override
          public void run() {
            final NoUpdatesDialog dialog = new NoUpdatesDialog(true, targetsForUpdate, enableLink) {
              @Override
              protected void dispose() {
                ourUpdateInfoDialogShown = false;
                super.dispose();
              }
            };
            dialog.setShowConfirmation(showConfirmation);
            ourUpdateInfoDialogShown = true;
            dialog.show();
          }
        };
        if (showBalloonNotification && targetsForUpdate != null) {
          final String updatedPluginsList = StringUtil.join(targetsForUpdate, new Function<Couple<IdeaPluginDescriptor>, String>() {
            @Override
            public String fun(Couple<IdeaPluginDescriptor> downloader) {
              return downloader.getSecond().getName();
            }
          }, ", ");
          String message = "You have the latest version of " + ApplicationInfo.getInstance().getVersionName() + " installed.<br> ";
          message += "The following plugin" + (targetsForUpdate.size() == 1 ? " is" : "s are") + " ready to <a href=\"update\">update</a>: " + updatedPluginsList;
          showBalloonNotification(showPluginsUpdateDialogRunnable, message);
        }
        else if ((targetsForUpdate != null || alwaysShowResults) && !ourUpdateInfoDialogShown) {
          showPluginsUpdateDialogRunnable.run();
        }
      }
    }
  }

  private static void showBalloonNotification(final Runnable showUpdatesDialogRunnable, String message) {
    new NotificationGroup(IdeBundle.message("update.available.group"), NotificationDisplayType.STICKY_BALLOON, true)
            .createNotification(IdeBundle.message("updates.info.dialog.title"), message, NotificationType.INFORMATION, new NotificationListener() {
              @Override
              public void hyperlinkUpdate(@NotNull Notification notification, @NotNull HyperlinkEvent event) {
                notification.expire();
                showUpdatesDialogRunnable.run();
              }
            }).notify(null);
  }

  public static String prepareUpdateCheckArgs() {
    addUpdateRequestParameter("build", ApplicationInfo.getInstance().getBuild().asString());
    addUpdateRequestParameter("uid", getInstallationUID(PropertiesComponent.getInstance()));
    addUpdateRequestParameter("os", SystemInfo.OS_NAME + ' ' + SystemInfo.OS_VERSION);
    if (ApplicationInfoEx.getInstanceEx().isEAP()) {
      addUpdateRequestParameter("eap");
    }

    StringBuilder args = new StringBuilder();

    try {
      for (String name : ADDITIONAL_REQUEST_OPTIONS.keySet()) {
        if (args.length() > 0) {
          args.append('&');
        }

        args.append(URLEncoder.encode(name, "UTF-8"));

        String value = ADDITIONAL_REQUEST_OPTIONS.get(name);
        if (!StringUtil.isEmpty(value)) {
          args.append('=').append(URLEncoder.encode(value, "UTF-8"));
        }
      }

      return args.toString();
    }
    catch (UnsupportedEncodingException e) {
      return ""; // Can't be anyway
    }
  }

  private static InputStream loadVersionInfo(final String url) throws Exception {
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("enter: loadVersionInfo(UPDATE_URL='" + url + "' )");
    }
    final InputStream[] inputStreams = new InputStream[]{null};
    final Exception[] exception = new Exception[]{null};
    Future<?> downloadThreadFuture = ApplicationManager.getApplication().executeOnPooledThread(new Runnable() {
      @Override
      public void run() {
        try {
          URL requestUrl = new URL(url);
          if (!StandardFileSystems.FILE_PROTOCOL.equals(requestUrl.getProtocol())) {
            HttpConfigurable.getInstance().prepareURL(url);
            requestUrl = new URL(url + (url.contains("?") ? "&" : "?") + "build=" + ApplicationInfo.getInstance().getBuild().asString());
          }
          inputStreams[0] = requestUrl.openStream();
        }
        catch (IOException e) {
          exception[0] = e;
        }
      }
    });

    try {
      downloadThreadFuture.get(5, TimeUnit.SECONDS);
    }
    catch (TimeoutException e) {
      // ignore
    }

    if (!downloadThreadFuture.isDone()) {
      downloadThreadFuture.cancel(true);
      throw new ConnectionException(IdeBundle.message("updates.timeout.error"));
    }

    if (exception[0] != null) throw exception[0];
    return inputStreams[0];
  }

  public static String getInstallationUID(final PropertiesComponent propertiesComponent) {
    if (SystemInfo.isWindows) {
      String uid = getInstallationUIDOnWindows(propertiesComponent);
      if (uid != null) {
        return uid;
      }
    }
    String uid;
    if (!propertiesComponent.isValueSet(INSTALLATION_UID)) {
      uid = generateUUID();
      propertiesComponent.setValue(INSTALLATION_UID, uid);
    }
    else {
      uid = propertiesComponent.getValue(INSTALLATION_UID);
    }
    return uid;
  }

  @Nullable
  private static String getInstallationUIDOnWindows(PropertiesComponent propertiesComponent) {
    String appdata = System.getenv("APPDATA");
    if (appdata != null) {
      File jetBrainsDir = new File(appdata, "JetBrains");
      if (jetBrainsDir.exists() || jetBrainsDir.mkdirs()) {
        File permanentIdFile = new File(jetBrainsDir, "PermanentUserId");
        try {
          if (permanentIdFile.exists()) {
            return FileUtil.loadFile(permanentIdFile).trim();
          }
          String uuid;
          if (propertiesComponent.isValueSet(INSTALLATION_UID)) {
            uuid = propertiesComponent.getValue(INSTALLATION_UID);
          }
          else {
            uuid = generateUUID();
          }
          FileUtil.writeToFile(permanentIdFile, uuid);
          return uuid;
        }
        catch (IOException e) {
          // ignore
        }
      }
    }
    return null;
  }

  private static String generateUUID() {
    try {
      return UUID.randomUUID().toString();
    }
    catch (Exception ignored) {
    }
    catch (InternalError ignored) {
    }
    return "";
  }

  public static boolean install(List<PluginDownloader> downloaders) {
    boolean installed = false;
    for (PluginDownloader downloader : downloaders) {
      if (getDisabledToUpdatePlugins().contains(downloader.getPluginId())) continue;
      final IdeaPluginDescriptor descriptor = downloader.getDescriptor();
      if (descriptor != null) {
        try {
          InstalledPluginsTableModel.updateExistingPlugin(descriptor, PluginManager.getPlugin(descriptor.getPluginId()));
          downloader.install(true);
          installed = true;
        }
        catch (IOException e) {
          LOGGER.info(e);
        }
      }
    }
    return installed;
  }


  public static DownloadPatchResult downloadAndInstallPatch(final BuildInfo newVersion) {
    final DownloadPatchResult[] result = new DownloadPatchResult[]{DownloadPatchResult.CANCELED};

    if (!ProgressManager.getInstance().runProcessWithProgressSynchronously(new Runnable() {
      @Override
      public void run() {
        try {
          doDownloadAndInstallPatch(newVersion, ProgressManager.getInstance().getProgressIndicator());
          result[0] = DownloadPatchResult.SUCCESS;
        }
        catch (final IOException e) {
          LOGGER.info(e);
          result[0] = DownloadPatchResult.FAILED;
          Notifications.Bus.notify(new Notification("Updater", "Failed to download patch file", e.getMessage(), NotificationType.ERROR));
        }
      }
    }, IdeBundle.message("update.downloading.patch.progress.title"), true, null)) {
      return DownloadPatchResult.CANCELED;
    }

    return result[0];
  }

  private static void doDownloadAndInstallPatch(BuildInfo newVersion, ProgressIndicator i) throws IOException {
    PatchInfo patch = newVersion.findPatchForCurrentBuild();
    if (patch == null) throw new IOException("No patch is available for current version");

    String fromBuildNumber = patch.getFromBuild().asString();
    String toBuildNumber = newVersion.getNumber().asString();

    StringBuilder builder = new StringBuilder();
    builder.append(ApplicationInfoEx.getInstanceEx().getUpdatesDownloadUrl());
    builder.append("?fromBuild=");
    builder.append(fromBuildNumber);
    builder.append("&toBuild=");
    builder.append(toBuildNumber);
    builder.append("&os=");
    builder.append(patch.getOSSuffix());

    File tempFile = FileUtil.createTempFile("consulo", "patch", true);

    OutputStream out = new BufferedOutputStream(new FileOutputStream(tempFile));
    try {
      URLConnection connection = new URL(builder.toString()).openConnection();
      try {
        InputStream in = UrlConnectionUtil.getConnectionInputStreamWithException(connection, i);
        try {
          int total = connection.getContentLength();
          i.setIndeterminate(total > 0);

          byte[] buffer = new byte[10 * 1024];
          int count;
          int read = 0;

          while ((count = in.read(buffer)) > 0) {
            i.checkCanceled();
            out.write(buffer, 0, count);
            read += count;
            if (total > 0) {
              i.setFraction(((double)read) / total);
              i.setText2((read / 1024) + "/" + (total / 1024) + " KB");
            }
            else {
              i.setText2((read / 1024) + " KB");
            }
          }
        }
        finally {
          in.close();
        }
      }
      finally {
        if (connection instanceof HttpURLConnection) {
          ((HttpURLConnection)connection).disconnect();
        }
      }
    }
    finally {
      out.close();
    }

    File patchFile = new File(FileUtil.getTempDirectory(), SharedConstants.PATCH_FILE_NAME);
    FileUtil.copy(tempFile, patchFile);
    FileUtil.delete(tempFile);
  }

  public static Set<String> getDisabledToUpdatePlugins() {
    if (ourDisabledToUpdatePlugins == null) {
      ourDisabledToUpdatePlugins = new TreeSet<String>();
      if (!ApplicationManager.getApplication().isUnitTestMode()) {
        try {
          final File file = new File(PathManager.getConfigPath(), DISABLED_UPDATE);
          if (file.isFile()) {
            final String[] ids = FileUtil.loadFile(file).split("[\\s]");
            for (String id : ids) {
              if (id != null && id.trim().length() > 0) {
                ourDisabledToUpdatePlugins.add(id.trim());
              }
            }
          }
        }
        catch (IOException e) {
          LOGGER.error(e);
        }
      }
    }
    return ourDisabledToUpdatePlugins;
  }

  public static void saveDisabledToUpdatePlugins() {
    final File plugins = new File(PathManager.getConfigPath(), DISABLED_UPDATE);
    try {
      PluginManagerCore.savePluginsList(getDisabledToUpdatePlugins(), false, plugins);
    }
    catch (IOException e) {
      LOGGER.error(e);
    }
  }
}
