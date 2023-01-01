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
package consulo.language.codeStyle.ui.internal.arrangement;

/**
 * @author Denis Zhdanov
 * @since 8/13/12 11:48 AM
 */
public class ArrangementConstants {

  public static final String ACTION_GROUP_GROUPING_RULES_CONTROL_TOOLBAR = "Arrangement.Rule.Group.Control.ToolBar";
  public static final String GROUPING_RULES_CONTROL_TOOLBAR_PLACE = "Arrangement.Rule.Group.Control.ToolBar.Place";

  public static final String ACTION_GROUP_MATCHING_RULES_CONTEXT_MENU = "Arrangement.Rule.Match.Control.Context.Menu";
  public static final String ACTION_GROUP_MATCHING_RULES_CONTROL_TOOLBAR = "Arrangement.Rule.Match.Control.ToolBar";
  public static final String MATCHING_RULES_CONTROL_TOOLBAR_PLACE = "Arrangement.Rule.Match.Control.ToolBar.Place";
  public static final String MATCHING_RULES_CONTROL_PLACE = "Arrangement.Rule.Match.Control.Place";
  public static final String ALIAS_RULE_CONTEXT_MENU = "Arrangement.Alias.Rule.Context.Menu";
  public static final String ALIAS_RULE_CONTROL_TOOLBAR = "Arrangement.Alias.Rule.ToolBar";
  public static final String ALIAS_RULE_CONTROL_TOOLBAR_PLACE = "Arrangement.Alias.Rule.ToolBar.Place";
  public static final String ALIAS_RULE_CONTROL_PLACE = "Arrangement.Alias.Rule.Control.Place";

  public static final String MATCHING_RULE_ADD = "Arrangement.Rule.Add";
  public static final String MATCHING_RULE_REMOVE = "Arrangement.Rule.Remove";
  public static final String MATCHING_RULE_EDIT = "Arrangement.Rule.Edit";
  public static final String MATCHING_RULE_MOVE_UP = "Arrangement.Rule.Match.Condition.Move.Up";
  public static final String MATCHING_RULE_MOVE_DOWN = "Arrangement.Rule.Match.Condition.Move.Down";
  public static final String GROUPING_RULE_MOVE_UP = "Arrangement.Rule.Group.Condition.Move.Up";
  public static final String GROUPING_RULE_MOVE_DOWN = "Arrangement.Rule.Group.Condition.Move.Down";

  public static final String MATCHING_ALIAS_RULE_ADD = "Arrangement.Alias.Rule.Add";
  public static final String MATCHING_ALIAS_RULE_REMOVE = "Arrangement.Alias.Rule.Remove";
  public static final String MATCHING_ALIAS_RULE_EDIT = "Arrangement.Alias.Rule.Edit";
  public static final String MATCHING_ALIAS_RULE_MOVE_UP = "Arrangement.Alias.Rule.Match.Condition.Move.Up";
  public static final String MATCHING_ALIAS_RULE_MOVE_DOWN = "Arrangement.Alias.Rule.Match.Condition.Move.Down";

  public static final int HORIZONTAL_PADDING = 8;
  public static final int VERTICAL_PADDING = 4;
  public static final int HORIZONTAL_GAP = 5;
  public static final int VERTICAL_GAP = 3;
  public static final int CALLOUT_BORDER_HEIGHT = 10;
  public static final int BORDER_ARC_SIZE = 12;

  public static final int ANIMATION_ITERATION_PIXEL_STEP = 5;
  public static final int ANIMATION_STEPS_TIME_GAP_MILLIS = 40;
  public static final int TEXT_UPDATE_DELAY_MILLIS = 1000;

  public static final boolean LOG_RULE_MODIFICATION = Boolean.parseBoolean(System.getProperty("log.arrangement.rule.modification"));

  private ArrangementConstants() {
  }
}
