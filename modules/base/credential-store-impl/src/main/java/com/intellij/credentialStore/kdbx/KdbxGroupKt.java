/*
 * Copyright 2013-2018 consulo.io
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
package com.intellij.credentialStore.kdbx;

import com.intellij.credentialStore.kdbx.valueCreator.ConstantValueCreator;
import com.intellij.credentialStore.kdbx.valueCreator.DateValueCreator;
import com.intellij.credentialStore.kdbx.valueCreator.UuidValueCreator;
import org.jdom.Element;

import javax.annotation.Nonnull;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * from kotlin
 */
public class KdbxGroupKt {
  private static final Map<String[], Supplier<String>> mandatoryGroupElements = new LinkedHashMap<>();

  static {
    mandatoryGroupElements.put(KeePassDatabaseKt.UUID_ELEMENT_NAME, new UuidValueCreator());
    mandatoryGroupElements.put(new String[]{"Notes"}, new ConstantValueCreator(""));
    mandatoryGroupElements.put(KeePassDatabaseKt.ICON_ELEMENT_NAME, new ConstantValueCreator("0"));
    mandatoryGroupElements.put(KeePassDatabaseKt.CREATION_TIME_ELEMENT_NAME, new DateValueCreator());
    mandatoryGroupElements.put(KeePassDatabaseKt.LAST_MODIFICATION_TIME_ELEMENT_NAME, new DateValueCreator());
    mandatoryGroupElements.put(KeePassDatabaseKt.LAST_ACCESS_TIME_ELEMENT_NAME, new DateValueCreator());
    mandatoryGroupElements.put(KeePassDatabaseKt.EXPIRY_TIME_ELEMENT_NAME, new DateValueCreator());
    mandatoryGroupElements.put(KeePassDatabaseKt.EXPIRES_ELEMENT_NAME, new ConstantValueCreator("False"));
    mandatoryGroupElements.put(KeePassDatabaseKt.USAGE_COUNT_ELEMENT_NAME, new ConstantValueCreator("0"));
    mandatoryGroupElements.put(KeePassDatabaseKt.LOCATION_CHANGED, new DateValueCreator());
  }

  @Nonnull
  public static KdbxGroup createGroup(KeePassDatabase db, KdbxGroup parent) {
    Element element = new Element(KdbxDbElementNames.group);
    KeePassDatabaseKt.ensureElements(element, mandatoryGroupElements);
    return new KdbxGroup(element, db, parent);
  }

  public static long parseTime(String value) {
    try {
      return ZonedDateTime.parse(value).toEpochSecond();
    }
    catch (DateTimeParseException e) {
      return 0;
    }
  }
}
