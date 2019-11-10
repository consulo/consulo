package com.intellij.internal.statistic.connect;

import com.intellij.internal.statistic.StatisticsUploadAssistant;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationListener;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.application.ApplicationInfo;
import com.intellij.openapi.application.ApplicationNamesInfo;
import com.intellij.openapi.util.text.StringUtil;
import consulo.ide.webService.WebServiceApi;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;

public class RemotelyConfigurableStatisticsService implements StatisticsService {

  private StatisticsConnectionService myConnectionService;
  private StatisticsDataSender sender;
  private StatisticsUploadAssistant myAssistant;

  public RemotelyConfigurableStatisticsService(@Nonnull StatisticsConnectionService connectionService,
                                               @Nonnull StatisticsDataSender sender,
                                               @Nonnull StatisticsUploadAssistant assistant) {
    myConnectionService = connectionService;
    this.sender = sender;
    myAssistant = assistant;
  }

  @Override
  public StatisticsResult send() {
    final String serviceUrl = WebServiceApi.STATISTICS_API.buildUrl("push");


    if (!myConnectionService.isTransmissionPermitted()) {
      return new StatisticsResult(StatisticsResult.ResultCode.NOT_PERMITTED_SERVER, "NOT_PERMITTED");
    }

    String content = myAssistant.getData(myConnectionService.getDisabledGroups());

    if (StringUtil.isEmptyOrSpaces(content)) {
      return new StatisticsResult(StatisticsResult.ResultCode.NOTHING_TO_SEND, "NOTHING_TO_SEND");
    }

    try {
      sender.send(serviceUrl, content);
      StatisticsUploadAssistant.persistSentPatch(content);

      return new StatisticsResult(StatisticsResult.ResultCode.SEND, content);
    }
    catch (Exception e) {
      return new StatisticsResult(StatisticsResult.ResultCode.SENT_WITH_ERRORS, e.getMessage() != null ? e.getMessage() : "NPE");
    }
  }


  @Override
  public Notification createNotification(@Nonnull final String groupDisplayId, @Nullable NotificationListener listener) {
    final String fullProductName = ApplicationNamesInfo.getInstance().getFullProductName();
    final String companyName = ApplicationInfo.getInstance().getCompanyName();

    String text =
            "<html>Please click <a href='allow'>I agree</a> if you want to help make " + fullProductName +
            " better or <a href='decline'>I don't agree</a> otherwise. <a href='settings'>more...</a></html>";

    String title = "Help improve " + fullProductName + " by sending anonymous usage statistics to " + companyName;

    return new Notification(groupDisplayId, title,
                            text,
                            NotificationType.INFORMATION,
                            listener);
  }

  @Nullable
  @Override
  public Map<String, String> getStatisticsConfigurationLabels() {
    // Use defaults
    return null;
  }
}
