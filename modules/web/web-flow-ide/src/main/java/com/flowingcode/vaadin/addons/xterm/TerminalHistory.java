/*-
 * #%L
 * XTerm Console Addon
 * %%
 * Copyright (C) 2020 - 2023 Flowing Code
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package com.flowingcode.vaadin.addons.xterm;

import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.shared.Registration;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

/** Manages a command history buffer for {@link XTerm}. */
@SuppressWarnings("serial")
public class TerminalHistory implements Serializable {

  private LinkedList<String> history = new LinkedList<>();

  private transient ListIterator<String> iterator;

  private final XTermBase terminal;

  private List<Registration> registrations;

  private String lastRet;

  private String initialLine;

  private Integer maxSize;

  protected <T extends XTermBase & ITerminalConsole> TerminalHistory(T terminal) {
    if (TerminalHistory.of(terminal) != null) {
      throw new IllegalArgumentException("The terminal already has a history");
    }
    this.terminal = terminal;
    ComponentUtil.setData(terminal, TerminalHistory.class, this);
  }

  /** Returns the command history of the terminal. */
  public static <T extends XTermBase & ITerminalConsole> TerminalHistory of(T xterm) {
    return ComponentUtil.getData(xterm, TerminalHistory.class);
  }

  /** Adds support for command history to the given terminal. */
  public static <T extends XTermBase & ITerminalConsole> void extend(XTerm xterm) {
    TerminalHistory history = new TerminalHistory(xterm);
    history.setEnabled(true);
  }

  /**
   * Set the number of elements to retain. If {@code null} the history is unbounded.
   *
   * @throws IllegalArgumentException if the argument is negative.
   */
  public void setMaxSize(Integer maxSize) {
    if (maxSize != null && maxSize < 0) {
      throw new IllegalArgumentException();
    }
    this.maxSize = maxSize;
    iterator = null;

    if (maxSize != null) {
      while (history.size() > maxSize) {
        history.remove(0);
      }
    }
  }

  /** Sets the enabled state of the history. */
  public void setEnabled(boolean enabled) {
    if (!enabled && registrations != null) {
      registrations.forEach(Registration::remove);
      registrations = null;
    } else if (enabled && registrations == null) {
      registrations = new ArrayList<>();
      registrations.add(((ITerminalConsole) terminal).addLineListener(ev -> {
        add(ev.getLine());
        resetIterator();
      }));
      registrations.add(terminal.addCustomKeyListener(ev -> handleArrowUp(), Key.ARROW_UP));
      registrations.add(terminal.addCustomKeyListener(ev -> handleArrowDown(), Key.ARROW_DOWN));
    }
  }

  /** Gets the enabled state of the history. */
  public boolean isEnabled() {
    return registrations != null;
  }

  private void handleArrowUp() {
    if (initialLine == null) {
      ((ITerminalConsole) terminal).getCurrentLine().thenAccept(currentLine -> {
        initialLine = currentLine;
        write(previous());
      });
    } else {
      write(previous());
    }
  }

  private void handleArrowDown() {
    write(next());
  }


  private void write(String line) {
    if (line != null) {
      // erase logical line, cursor home in logical line, cursor horizontal absolute
      String prompt = ((ITerminalConsole) terminal).getPrompt();
      terminal.write("\033[<2K\033[<H\033[G" + prompt + line);
      lastRet = line;
    }
  }

  private ListIterator<String> listIterator() {
    if (iterator == null) {
      iterator = history.listIterator(history.size());
    }
    return iterator;
  }

  private Iterator<String> forwardIterator() {
    return listIterator();
  }

  private Iterator<String> reverseIterator() {
    if (iterator == null) {
      iterator = history.listIterator(history.size());
    }
    return new Iterator<String>() {
      @Override
      public boolean hasNext() {
        return iterator.hasPrevious();
      }

      @Override
      public String next() {
        return iterator.previous();
      }
    };
  }


  /** Add a line to the history */
  public void add(String line) {
    line = line.trim();
    if (!line.isEmpty()) {
      history.add(Objects.requireNonNull(line));
      resetIterator();
      if (maxSize != null && history.size() > maxSize) {
        history.removeLast();
      }
    }
  }

  private void resetIterator()
  {
    lastRet = null;
    iterator = null;
  }

  private Optional<String> find(Iterator<String> iterator, Predicate<String> predicate) {
    while (iterator.hasNext()) {
      String line = iterator.next();
      if (predicate.test(line) && !line.equals(lastRet)) {
        return Optional.of(line);
      }
    }
    return Optional.empty();
  }

  private String previous() {
    return find(reverseIterator(), line -> true).orElse(null);
  }

  private String next() {
    return find(forwardIterator(), line -> true).orElseGet(() -> {
      String result = initialLine;
      initialLine = null;
      return result;
    });
  }

  /** Clears the history. */
  public void clear() {
    history.clear();
  }

  /** Returns the lines in the history. */
  public List<String> getLines() {
    return Collections.unmodifiableList(history);
  }

  private void writeObject(java.io.ObjectOutputStream out) throws IOException {
    int cursor = Optional.ofNullable(iterator).map(ListIterator::nextIndex).orElse(history.size());
    out.defaultWriteObject();
    out.writeInt(cursor);
  }

  private void readObject(ObjectInputStream in) throws ClassNotFoundException, IOException {
    in.defaultReadObject();
    int cursor = in.readInt();
    if (cursor != history.size()) {
      iterator = history.listIterator(cursor);
    }
  }

}
