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

import com.intellij.openapi.util.JDOMUtil;
import org.jdom.Element;
import org.jdom.JDOMException;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * from kotlin
 */
// we should on each save change protectedStreamKey for security reasons (as KeeWeb also does)
// so, this requirement (is it really required?) can force us to re-encrypt all passwords on save
public class KeePassDatabase {
  private static final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");

  public static String formattedNow() {
    return LocalDateTime.now(ZoneOffset.UTC).format(dateFormatter);
  }

  private Element rootElement;

  public KeePassDatabase() {
    this(createEmptyDatabase());
  }

  public KeePassDatabase(Element rootElement) {
     this.rootElement = rootElement;
  }

  private static Element createEmptyDatabase() {
    String creationDate = formattedNow();
    try {
      return JDOMUtil.load("<KeePassFile>\n" +
                           "    <Meta>\n" +
                           "      <Generator>IJ</Generator>\n" +
                           "      <HeaderHash></HeaderHash>\n" +
                           "      <DatabaseName>New Database</DatabaseName>\n" +
                           "      <DatabaseNameChanged>" + creationDate + "</DatabaseNameChanged>\n" +
                           "      <DatabaseDescription>Empty Database</DatabaseDescription>\n" +
                           "      <DatabaseDescriptionChanged>" + creationDate + "</DatabaseDescriptionChanged>\n" +
                           "      <DefaultUserName/>\n" +
                           "      <DefaultUserNameChanged>" + creationDate + "</DefaultUserNameChanged>\n" +
                           "      <MaintenanceHistoryDays>365</MaintenanceHistoryDays>\n" +
                           "      <Color/>\n" +
                           "      <MasterKeyChanged>" + creationDate + "</MasterKeyChanged>\n" +
                           "      <MasterKeyChangeRec>-1</MasterKeyChangeRec>\n" +
                           "      <MasterKeyChangeForce>-1</MasterKeyChangeForce>\n" +
                           "      <MemoryProtection>\n" +
                           "          <ProtectTitle>False</ProtectTitle>\n" +
                           "          <ProtectUserName>False</ProtectUserName>\n" +
                           "          <ProtectPassword>True</ProtectPassword>\n" +
                           "          <ProtectURL>False</ProtectURL>\n" +
                           "          <ProtectNotes>False</ProtectNotes>\n" +
                           "      </MemoryProtection>\n" +
                           "      <CustomIcons/>\n" +
                           "      <RecycleBinEnabled>True</RecycleBinEnabled>\n" +
                           "      <RecycleBinUUID>AAAAAAAAAAAAAAAAAAAAAA==</RecycleBinUUID>\n" +
                           "      <RecycleBinChanged>" + creationDate + "</RecycleBinChanged>\n" +
                           "      <EntryTemplatesGroup>AAAAAAAAAAAAAAAAAAAAAA==</EntryTemplatesGroup>\n" +
                           "      <EntryTemplatesGroupChanged>" + creationDate + "</EntryTemplatesGroupChanged>\n" +
                           "      <LastSelectedGroup>AAAAAAAAAAAAAAAAAAAAAA==</LastSelectedGroup>\n" +
                           "      <LastTopVisibleGroup>AAAAAAAAAAAAAAAAAAAAAA==</LastTopVisibleGroup>\n" +
                           "      <HistoryMaxItems>10</HistoryMaxItems>\n" +
                           "      <HistoryMaxSize>6291456</HistoryMaxSize>\n" +
                           "      <Binaries/>\n" +
                           "      <CustomData/>\n" +
                           "    </Meta>\n" +
                           "  </KeePassFile>");
    }
    catch (JDOMException | IOException e) {
      throw new RuntimeException(e);
    }
  }
}
