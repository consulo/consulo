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

import com.intellij.credentialStore.CredentialStoreKt;
import com.intellij.credentialStore.kdbx.valueCreator.ConstantValueCreator;
import com.intellij.credentialStore.kdbx.valueCreator.DateValueCreator;
import com.intellij.credentialStore.kdbx.valueCreator.UuidValueCreator;
import com.intellij.openapi.util.JDOMUtil;
import com.intellij.util.containers.ContainerUtil;
import org.bouncycastle.crypto.SkippingStreamCipher;
import org.bouncycastle.crypto.engines.ChaCha7539Engine;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.ParametersWithIV;
import org.jdom.Element;
import org.jdom.JDOMException;

import javax.annotation.Nullable;
import java.io.IOException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 2018-11-24
 */
public class KeePassDatabaseKt {
  private static final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");

  public static final String[] LOCATION_CHANGED = ContainerUtil.ar("Times", "LocationChanged");
  public static final String[] USAGE_COUNT_ELEMENT_NAME = ContainerUtil.ar("Times", "UsageCount");
  public static final String[] EXPIRES_ELEMENT_NAME = ContainerUtil.ar("Times", "Expires");
  public static final String[] ICON_ELEMENT_NAME = ContainerUtil.ar("IconID");
  public static final String[] UUID_ELEMENT_NAME = ContainerUtil.ar("UUID");
  public static final String[] LAST_MODIFICATION_TIME_ELEMENT_NAME = ContainerUtil.ar("Times", "LastModificationTime");
  public static final String[] CREATION_TIME_ELEMENT_NAME = ContainerUtil.ar("Times", "CreationTime");
  public static final String[] LAST_ACCESS_TIME_ELEMENT_NAME = ContainerUtil.ar("Times", "LastAccessTime");
  public static final String[] EXPIRY_TIME_ELEMENT_NAME = ContainerUtil.ar("Times", "ExpiryTime");

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

  public static void ensureElements(Element element, Map<String[], Supplier<String>> childElements) {
    for (Map.Entry<String[], Supplier<String>> entry : childElements.entrySet()) {
      String[] elementPath = entry.getKey();
      Supplier<String> value = entry.getValue();

      Element result = findElement(element, elementPath);
      if(result == null) {
        Element currentElement = element;

        for (String elementName : elementPath) {
          currentElement = JDOMUtil.getOrCreate(currentElement, elementName);
        }

        currentElement.setText(value.get());
      }
    }
  }

  @Nullable
  private static Element findElement(Element element, String[] elementPath) {
    Element result = element;

    for (String elementName : elementPath) {
      result = result.getChild(elementName);
      if (result == null) {
        return null;
      }
    }
    return result;
  }

  public static SkippingStreamCipher createRandomlyInitializedChaCha7539Engine(SecureRandom secureRandom) {
    ChaCha7539Engine engine = new ChaCha7539Engine();
    initCipherRandomly(secureRandom, engine);
    return engine;
  }

  public static void initCipherRandomly(SecureRandom random, SkippingStreamCipher engine) {
    KeyParameter parameter = new KeyParameter(CredentialStoreKt.generateBytes(random, 32));
    engine.init(true, new ParametersWithIV(parameter, CredentialStoreKt.generateBytes(random, 12)));
  }

  public static Element createEmptyDatabase() {
    String creationDate = formattedNow();
    try {
      return JDOMUtil.load("<KeePassFile>\n" +
                           "    <Meta>\n" +
                           "      <Generator>IJ</Generator>\n" +
                           "      <HeaderHash></HeaderHash>\n" +
                           "      <DatabaseName>New Database</DatabaseName>\n" +
                           "      <DatabaseNameChanged>" +
                           creationDate +
                           "</DatabaseNameChanged>\n" +
                           "      <DatabaseDescription>Empty Database</DatabaseDescription>\n" +
                           "      <DatabaseDescriptionChanged>" +
                           creationDate +
                           "</DatabaseDescriptionChanged>\n" +
                           "      <DefaultUserName/>\n" +
                           "      <DefaultUserNameChanged>" +
                           creationDate +
                           "</DefaultUserNameChanged>\n" +
                           "      <MaintenanceHistoryDays>365</MaintenanceHistoryDays>\n" +
                           "      <Color/>\n" +
                           "      <MasterKeyChanged>" +
                           creationDate +
                           "</MasterKeyChanged>\n" +
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
                           "      <RecycleBinChanged>" +
                           creationDate +
                           "</RecycleBinChanged>\n" +
                           "      <EntryTemplatesGroup>AAAAAAAAAAAAAAAAAAAAAA==</EntryTemplatesGroup>\n" +
                           "      <EntryTemplatesGroupChanged>" +
                           creationDate +
                           "</EntryTemplatesGroupChanged>\n" +
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

  public static String formattedNow() {
    return LocalDateTime.now(ZoneOffset.UTC).format(dateFormatter);
  }
}
