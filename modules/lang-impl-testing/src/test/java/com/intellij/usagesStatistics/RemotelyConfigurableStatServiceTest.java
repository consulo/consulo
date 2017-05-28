package com.intellij.usagesStatistics;

import com.intellij.internal.statistic.StatisticsUploadAssistant;
import com.intellij.internal.statistic.connect.StatisticsHttpClientSender;
import com.intellij.internal.statistic.connect.RemotelyConfigurableStatisticsService;
import com.intellij.internal.statistic.connect.StatisticsConnectionService;
import com.intellij.internal.statistic.connect.StatisticsResult;
import junit.framework.Assert;
import junit.framework.TestCase;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

public class RemotelyConfigurableStatServiceTest extends TestCase {

  @NonNls
  private static final String STAT_URL = "http://localhost:8080/stat.jsp";

  @NonNls
  private static final String STAT_CONFIG_URL = "http://localhost:8080/config.jsp";

  public void testEmptyDataSending() {
    RemotelyConfigurableStatisticsService service = new RemotelyConfigurableStatisticsService(new StatisticsConnectionService(),
                                                                                              new StatisticsHttpClientSender(),
                                                                                              new StatisticsUploadAssistant() {
                                                                                                @Override
                                                                                                public String getData(@NotNull Set<String> disabledGroups) {
                                                                                                  return "";
                                                                                                }
                                                                                              });
    final StatisticsResult result = service.send();
    Assert.assertEquals(StatisticsResult.ResultCode.NOTHING_TO_SEND, result.getCode());
  }

  public void testIncorrectUrlSending() {
    RemotelyConfigurableStatisticsService service = new RemotelyConfigurableStatisticsService(new StatisticsConnectionService(),
                                                                                              new StatisticsHttpClientSender(),
                                                                                              new StatisticsUploadAssistant() {
                                                                                                @Override
                                                                                                public String getData(@NotNull Set<String> disabledGroups) {
                                                                                                  return "group:key1=11";
                                                                                                }
                                                                                              });
    final StatisticsResult result = service.send();
    Assert.assertEquals(StatisticsResult.ResultCode.SENT_WITH_ERRORS, result.getCode());
  }

  public void testRemotelyDisabledTransmission() {
    RemotelyConfigurableStatisticsService service = new RemotelyConfigurableStatisticsService(new StatisticsConnectionService() {
      @Override
      public Boolean isTransmissionPermitted() {
        return false;
      }
    }, new StatisticsHttpClientSender(),
    new StatisticsUploadAssistant());

    final StatisticsResult result = service.send();
    Assert.assertEquals(StatisticsResult.ResultCode.NOT_PERMITTED_SERVER, result.getCode());
  }

  public void testErrorInRemoteConfiguration() {
    RemotelyConfigurableStatisticsService service =
      new RemotelyConfigurableStatisticsService(new StatisticsConnectionService(),
                                                new StatisticsHttpClientSender(),
                                                new StatisticsUploadAssistant());
    final StatisticsResult result = service.send();
    Assert.assertEquals(StatisticsResult.ResultCode.ERROR_IN_CONFIG, result.getCode());
  }
}
