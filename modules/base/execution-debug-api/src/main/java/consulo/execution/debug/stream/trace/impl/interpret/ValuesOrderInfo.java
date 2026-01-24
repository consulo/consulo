package consulo.execution.debug.stream.trace.impl.interpret;

import consulo.execution.debug.stream.trace.TraceElement;
import consulo.execution.debug.stream.trace.TraceInfo;
import consulo.execution.debug.stream.wrapper.StreamCall;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ValuesOrderInfo implements TraceInfo {
  private final StreamCall streamCall;
  private final Map<Integer, TraceElement> before;
  private final Map<Integer, TraceElement> after;
  @Nullable
  private final Map<TraceElement, List<TraceElement>> direct;
  @Nullable
  private final Map<TraceElement, List<TraceElement>> reverse;

  public ValuesOrderInfo(
    @Nonnull StreamCall streamCall,
    @Nonnull Map<Integer, TraceElement> before,
    @Nonnull Map<Integer, TraceElement> after,
    @Nullable Map<TraceElement, List<TraceElement>> direct,
    @Nullable Map<TraceElement, List<TraceElement>> reverse
  ) {
    this.streamCall = streamCall;
    this.before = before;
    this.after = after;
    this.direct = direct;
    this.reverse = reverse;
  }

  public ValuesOrderInfo(
    @Nonnull StreamCall call,
    @Nonnull Map<Integer, TraceElement> before,
    @Nonnull Map<Integer, TraceElement> after
  ) {
    this(call, before, after, null, null);
  }

  @Nonnull
  @Override
  public StreamCall getCall() {
    return streamCall;
  }

  @Nonnull
  @Override
  public Map<Integer, TraceElement> getValuesOrderBefore() {
    return before;
  }

  @Nonnull
  @Override
  public Map<Integer, TraceElement> getValuesOrderAfter() {
    return after;
  }

  @Nullable
  @Override
  public Map<TraceElement, List<TraceElement>> getDirectTrace() {
    return direct;
  }

  @Nullable
  @Override
  public Map<TraceElement, List<TraceElement>> getReverseTrace() {
    return reverse;
  }

  @Nonnull
  public static TraceInfo empty(@Nonnull StreamCall call) {
    return new ValuesOrderInfo(call, Collections.emptyMap(), Collections.emptyMap());
  }
}
