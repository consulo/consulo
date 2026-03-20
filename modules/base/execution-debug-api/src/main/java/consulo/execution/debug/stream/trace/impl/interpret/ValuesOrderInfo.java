package consulo.execution.debug.stream.trace.impl.interpret;

import consulo.execution.debug.stream.trace.TraceElement;
import consulo.execution.debug.stream.trace.TraceInfo;
import consulo.execution.debug.stream.wrapper.StreamCall;

import org.jspecify.annotations.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ValuesOrderInfo implements TraceInfo {
  private final StreamCall streamCall;
  private final Map<Integer, TraceElement> before;
  private final Map<Integer, TraceElement> after;
  private final @Nullable Map<TraceElement, List<TraceElement>> direct;
  private final @Nullable Map<TraceElement, List<TraceElement>> reverse;

  public ValuesOrderInfo(
    StreamCall streamCall,
    Map<Integer, TraceElement> before,
    Map<Integer, TraceElement> after,
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
    StreamCall call,
    Map<Integer, TraceElement> before,
    Map<Integer, TraceElement> after
  ) {
    this(call, before, after, null, null);
  }

  
  @Override
  public StreamCall getCall() {
    return streamCall;
  }

  
  @Override
  public Map<Integer, TraceElement> getValuesOrderBefore() {
    return before;
  }

  
  @Override
  public Map<Integer, TraceElement> getValuesOrderAfter() {
    return after;
  }

  @Override
  public @Nullable Map<TraceElement, List<TraceElement>> getDirectTrace() {
    return direct;
  }

  @Override
  public @Nullable Map<TraceElement, List<TraceElement>> getReverseTrace() {
    return reverse;
  }

  
  public static TraceInfo empty(StreamCall call) {
    return new ValuesOrderInfo(call, Collections.emptyMap(), Collections.emptyMap());
  }
}
