// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.application.util;

import consulo.component.extension.ExtensionPointName;
import consulo.util.xml.serializer.annotation.Attribute;

/**
 * @author Konstantin Bulenkov
 */
public class DateTimeFormatterBean {
  public static final ExtensionPointName<DateTimeFormatterBean> EP_NAME = ExtensionPointName.create("consulo.dateTimeFormatter");

  @Attribute("id")
  //@RequiredElement
  public String id;

  @Attribute("name")
  //@RequiredElement
  public String name;

  @Attribute("format")
  public String format;
}
