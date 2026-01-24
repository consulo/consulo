// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.execution.debug.impl.internal.stream.ui;

import consulo.execution.debug.stream.trace.TraceElement;
import consulo.execution.debug.stream.ui.ValueWithPosition;
import jakarta.annotation.Nonnull;

/**
 * @author Vitaliy.Bibaev
 */
public class ValueWithPositionImpl implements ValueWithPosition {
  public static final int INVALID_POSITION = Integer.MIN_VALUE;
  public static final boolean DEFAULT_VISIBLE_VALUE = false;
  public static final boolean DEFAULT_HIGHLIGHTING_VALUE = false;

  private final TraceElement myTraceElement;
  private int myPosition = INVALID_POSITION;
  private boolean myIsVisible = DEFAULT_VISIBLE_VALUE;
  private boolean myIsHighlighted = DEFAULT_HIGHLIGHTING_VALUE;

  public ValueWithPositionImpl(@Nonnull TraceElement traceElement) {
    myTraceElement = traceElement;
  }

  @Override
  public boolean equals(Object other) {
    return other != null && other instanceof ValueWithPosition && myTraceElement.equals(((ValueWithPosition)other).getTraceElement());
  }

  @Override
  public int hashCode() {
    return myTraceElement.hashCode();
  }

  @Nonnull
  @Override
  public TraceElement getTraceElement() {
    return myTraceElement;
  }

  @Override
  public boolean isVisible() {
    return myIsVisible;
  }

  @Override
  public int getPosition() {
    return myPosition;
  }

  @Override
  public boolean isValid() {
    return myPosition != INVALID_POSITION;
  }

  @Override
  public boolean isHighlighted() {
    return myIsHighlighted;
  }

  public boolean updateToInvalid() {
    return updateProperties(INVALID_POSITION, DEFAULT_VISIBLE_VALUE, DEFAULT_HIGHLIGHTING_VALUE);
  }

  public void setInvalid() {
    setProperties(INVALID_POSITION, DEFAULT_VISIBLE_VALUE, DEFAULT_HIGHLIGHTING_VALUE);
  }

  public boolean updateProperties(int position, boolean isVisible, boolean isHighlighted) {
    boolean changed = myPosition != position || myIsVisible != isVisible || myIsHighlighted != isHighlighted;
    setProperties(position, isVisible, isHighlighted);
    return changed;
  }

  public void setProperties(int position, boolean isVisible, boolean isHighlighted) {
    myPosition = position;
    myIsHighlighted = isHighlighted;
    myIsVisible = isVisible;
  }
}
