/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package consulo.execution.debug.ui;

import consulo.application.AllIcons;
import consulo.execution.debug.localize.XDebuggerLocalize;
import consulo.localize.LocalizeValue;
import consulo.project.ui.notification.NotificationGroup;
import consulo.project.ui.wm.ToolWindowId;
import consulo.ui.ex.DarculaColors;
import consulo.ui.ex.JBColor;
import consulo.ui.ex.SimpleTextAttributes;
import consulo.ui.image.Image;

import java.awt.*;

/**
 * @author nik
 */
public class XDebuggerUIConstants {
  public static final LocalizeValue COLLECTING_DATA_MESSAGE = XDebuggerLocalize.xdebuggerBuildingTreeNodeMessage();
  public static final LocalizeValue EVALUATING_EXPRESSION_MESSAGE = XDebuggerLocalize.xdebuggerEvaluatingExpressionNodeMessage();
  public static final LocalizeValue MODIFYING_VALUE_MESSAGE = XDebuggerLocalize.xdebuggerModifiyngValueNodeMessage();

  public static final Image ERROR_MESSAGE_ICON = AllIcons.General.Error;
  public static final Image INFORMATION_MESSAGE_ICON = AllIcons.General.Information;

  public static final SimpleTextAttributes COLLECTING_DATA_HIGHLIGHT_ATTRIBUTES = get(new JBColor(Color.lightGray, Color.lightGray));
  public static final SimpleTextAttributes EVALUATING_EXPRESSION_HIGHLIGHT_ATTRIBUTES = get(new JBColor(Color.lightGray, Color.lightGray));
  public static final SimpleTextAttributes MODIFYING_VALUE_HIGHLIGHT_ATTRIBUTES = get(JBColor.blue);
  public static final SimpleTextAttributes CHANGED_VALUE_ATTRIBUTES = get(JBColor.blue);
  public static final SimpleTextAttributes EXCEPTION_ATTRIBUTES = get(JBColor.red);
  public static final SimpleTextAttributes VALUE_NAME_ATTRIBUTES = get(new JBColor(new Color(128, 0, 0), DarculaColors.RED.brighter()));
  public static final SimpleTextAttributes ERROR_MESSAGE_ATTRIBUTES = get(JBColor.red);
  public static final String EQ_TEXT = " = ";

  public static final SimpleTextAttributes TYPE_ATTRIBUTES = SimpleTextAttributes.GRAY_ATTRIBUTES;
  public static final String LAYOUT_VIEW_BREAKPOINT_CONDITION = "breakpoint";

  public static final NotificationGroup NOTIFICATION_GROUP = NotificationGroup.toolWindowGroup("Debugger messages", ToolWindowId.DEBUG, false);

  private static SimpleTextAttributes get(JBColor c) {
    return new SimpleTextAttributes(Font.PLAIN, c);
  }

  private XDebuggerUIConstants() {
  }
}
