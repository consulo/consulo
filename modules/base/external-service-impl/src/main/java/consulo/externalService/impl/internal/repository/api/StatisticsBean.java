/*
 * Copyright 2013-2020 consulo.io
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
package consulo.externalService.impl.internal.repository.api;

/**
 * @author VISTALL
 * @since 2020-05-30
 */
public class StatisticsBean {
  public static class UsageGroup {
    public String id;

    public UsageGroupValue[] values = new UsageGroupValue[0];
  }

  public static class UsageGroupValue {
    public String id;

    public int count;

    public UsageGroupValue() {
    }

    public UsageGroupValue(String id, int count) {
      this.id = id;
      this.count = count;
    }
  }

  public String key;

  public String installationID;

  public UsageGroup[] groups = new UsageGroup[0];
}
