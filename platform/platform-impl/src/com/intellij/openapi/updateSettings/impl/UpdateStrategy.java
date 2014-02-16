/*
 * Copyright 2000-2011 JetBrains s.r.o.
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


import com.intellij.openapi.util.BuildNumber;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class UpdateStrategy {
  public static enum State {LOADED, CONNECTION_ERROR, NOTHING_LOADED}

  private UserUpdateSettings myUpdateSettings;
  private int myMajorVersion;
  private BuildNumber myCurrentBuild;

  private ChannelStatus myChannelStatus;
  private Product myProduct;


  public UpdateStrategy(int majorVersion, @NotNull BuildNumber currentBuild, @NotNull Product product,
                        @NotNull UserUpdateSettings updateSettings) {
    myMajorVersion = majorVersion;
    myProduct = product;
    myUpdateSettings = updateSettings;
    myCurrentBuild = currentBuild;
    myChannelStatus = updateSettings.getSelectedChannelStatus();
  }

  public final CheckForUpdateResult checkForUpdates() {
    if (myProduct.getChannels().isEmpty()) {
      return new CheckForUpdateResult(State.NOTHING_LOADED);
    }

    UpdateChannel updatedChannel = null;
    BuildInfo newBuild = null;
    List<UpdateChannel> activeChannels = getActiveChannels(myProduct);
    for (UpdateChannel channel : activeChannels) {
      if (hasNewVersion(channel)) {
        updatedChannel = channel;
        newBuild = updatedChannel.getLatestBuild();
        break;
      }
    }

    CheckForUpdateResult result = new CheckForUpdateResult(updatedChannel, newBuild, myProduct.getAllChannelIds());

    UpdateChannel channelToPropose = null;
    for (UpdateChannel channel : myProduct.getChannels()) {
      if (!myUpdateSettings.getKnownChannelsIds().contains(channel.getId()) &&
          channel.getMajorVersion() >= myMajorVersion &&
          channel.getStatus().compareTo(myChannelStatus) >= 0 &&
          hasNewVersion(channel)) {
        result.addNewChannel(channel);
        if (channelToPropose == null || isBetter(channelToPropose, channel)) {
          channelToPropose = channel;
        }
      }
    }
    result.setChannelToPropose(channelToPropose);
    return result;
  }

  private static boolean isBetter(UpdateChannel channelToPropose, UpdateChannel channel) {
    return channel.getMajorVersion() > channelToPropose.getMajorVersion() ||
           (channel.getMajorVersion() == channelToPropose.getMajorVersion() &&
            channel.getStatus().compareTo(channelToPropose.getStatus()) > 0);
  }

  private List<UpdateChannel> getActiveChannels(Product product) {
    List<UpdateChannel> channels = product.getChannels();
    List<UpdateChannel> result = new ArrayList<UpdateChannel>();
    for (UpdateChannel channel : channels) {
      if ((channel.getMajorVersion() == myMajorVersion && channel.getStatus().compareTo(myChannelStatus) >= 0) ||
          (channel.getMajorVersion() > myMajorVersion && channel.getStatus() == ChannelStatus.EAP && myChannelStatus == ChannelStatus.EAP)) {
        result.add(channel);
      }
    }
    return result;
  }

  private boolean hasNewVersion(@NotNull UpdateChannel channel) {
    BuildInfo latestBuild = channel.getLatestBuild();
    if (latestBuild == null || latestBuild.getNumber() == null ||
        myUpdateSettings.getIgnoredBuildNumbers().contains(latestBuild.getNumber().asString())) {
      return false;
    }
    return myCurrentBuild.compareTo(latestBuild.getNumber()) < 0;
  }
}
