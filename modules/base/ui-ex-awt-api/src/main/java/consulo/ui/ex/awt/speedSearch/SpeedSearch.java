/*
 * Copyright 2000-2017 JetBrains s.r.o.
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
package consulo.ui.ex.awt.speedSearch;

import consulo.application.util.matcher.*;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.keymap.util.KeymapUtil;
import kava.beans.PropertyChangeListener;
import kava.beans.PropertyChangeSupport;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

public class SpeedSearch extends SpeedSearchSupply implements KeyListener {
  public static final String PUNCTUATION_MARKS = "*_-\"'/.#$>: ,;?!@%^&";

  private final PropertyChangeSupport myChangeSupport = new PropertyChangeSupport(this);
  private final boolean myMatchAllOccurrences;

  private String myString = "";
  private boolean myEnabled;
  private Matcher myMatcher;

  public SpeedSearch() {
    this(false);
  }

  public SpeedSearch(boolean matchAllOccurrences) {
    myMatchAllOccurrences = matchAllOccurrences;
  }

  public void type(String letter) {
    updatePattern(myString + letter);
  }

  public void backspace() {
    if (myString.length() > 0) {
      updatePattern(myString.substring(0, myString.length() - 1));
    }
  }

  public boolean shouldBeShowing(String string) {
    return string == null || myString.length() == 0 || (myMatcher != null && myMatcher.matches(string));
  }

  public void processKeyEvent(KeyEvent e) {
    if (e.isConsumed() || !myEnabled) return;

    String old = myString;
    if (e.getID() == KeyEvent.KEY_PRESSED) {
      if (KeymapUtil.isEventForAction(e, "EditorDeleteToWordStart")) {
        if (isHoldingFilter()) {
          while (!myString.isEmpty() && !Character.isWhitespace(myString.charAt(myString.length() - 1))) {
            backspace();
          }
          e.consume();
        }
      }
      else if (e.getKeyCode() == KeyEvent.VK_BACK_SPACE) {
        backspace();
        e.consume();
      }
      else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
        if (isHoldingFilter()) {
          updatePattern("");
          e.consume();
        }
      }
    }
    else if (e.getID() == KeyEvent.KEY_TYPED) {
      if (!UIUtil.isReallyTypedEvent(e)) return;
      // key-char is good only on KEY_TYPED
      // for example: key-char on ctrl-J PRESSED is \n
      // see https://en.wikipedia.org/wiki/Control_character
      char ch = e.getKeyChar();
      if (Character.isLetterOrDigit(ch) || !startedWithWhitespace(ch) && PUNCTUATION_MARKS.indexOf(ch) != -1) {
        type(Character.toString(ch));
        e.consume();
      }
    }

    if (!old.equalsIgnoreCase(myString)) {
      update();
    }
  }

  private boolean startedWithWhitespace(char ch) {
    return !isHoldingFilter() && Character.isWhitespace(ch);
  }

  public void update() {

  }

  public void noHits() {
  }

  public boolean isHoldingFilter() {
    return myEnabled && myString.length() > 0;
  }

  public void setEnabled(boolean enabled) {
    myEnabled = enabled;
  }

  public void reset() {
    if (isHoldingFilter()) {
      updatePattern("");
    }

    if (myEnabled) {
      update();
    }
  }

  public String getFilter() {
    return myString;
  }

  public void updatePattern(final String string) {
    String prevString = myString;
    myString = string;
    try {
      String pattern = "*" + string;
      NameUtil.MatchingCaseSensitivity caseSensitivity = NameUtil.MatchingCaseSensitivity.NONE;
      String separators = "";
      myMatcher = myMatchAllOccurrences ? AllOccurrencesMatcher.create(pattern, caseSensitivity, separators) : new FixingLayoutMatcher(pattern, caseSensitivity, separators);
    }
    catch (Exception e) {
      myMatcher = null;
    }
    fireStateChanged(prevString);
  }

  @Nullable
  public Matcher getMatcher() {
    return myMatcher;
  }

  @Nullable
  @Override
  public Iterable<MatcherTextRange> matchingFragments(@Nonnull String text) {
    if (myMatcher instanceof MinusculeMatcher) {
      return ((MinusculeMatcher)myMatcher).matchingFragments(text);
    }
    return null;
  }

  @Override
  public void refreshSelection() {
  }

  @Override
  public boolean isPopupActive() {
    return isHoldingFilter();
  }

  @Nullable
  @Override
  public String getEnteredPrefix() {
    return myString;
  }

  @Override
  public void addChangeListener(@Nonnull PropertyChangeListener listener) {
    myChangeSupport.addPropertyChangeListener(listener);
  }

  @Override
  public void removeChangeListener(@Nonnull PropertyChangeListener listener) {
    myChangeSupport.removePropertyChangeListener(listener);
  }

  private void fireStateChanged(String prevString) {
    myChangeSupport.firePropertyChange(SpeedSearchSupply.ENTERED_PREFIX_PROPERTY_NAME, prevString, getEnteredPrefix());
  }

  @Override
  public void findAndSelectElement(@Nonnull String searchQuery) {

  }

  @Override
  public void keyTyped(KeyEvent e) {
    processKeyEvent(e);
  }

  @Override
  public void keyPressed(KeyEvent e) {
    processKeyEvent(e);
  }

  @Override
  public void keyReleased(KeyEvent e) {
    processKeyEvent(e);
  }
}
