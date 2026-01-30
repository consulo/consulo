package consulo.util.concurrent.coroutine.test;

import consulo.util.concurrent.coroutine.Continuation;
import consulo.util.concurrent.coroutine.Coroutine;
import consulo.util.concurrent.coroutine.CoroutineContext;
import consulo.util.concurrent.coroutine.step.Delay;
import org.junit.jupiter.api.Test;

import static consulo.util.concurrent.coroutine.CoroutineScope.launch;
import static consulo.util.concurrent.coroutine.step.CodeExecution.apply;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CoroutineDelayTest {

    public static final long DELAY = 100L;

    @Test
    public void testDelay() {
        CoroutineContext context = TestCoroutineContext.newSilent();

        Coroutine<String, String> sleep = Coroutine.first(Delay.sleep(DELAY));

        Coroutine<String, String> r =
            sleep.then(apply((String s) -> s.toUpperCase()));

        launch(context, scope -> {
            long start = System.currentTimeMillis();
            Continuation<String> ca = r.runAsync(scope, "test");

            assertEquals("TEST", ca.getResult());
            long delay = System.currentTimeMillis() - start;
            assertTrue(delay > DELAY, () -> "Failed to check delay. Delay: " + delay);
            assertTrue(ca.isFinished());
        });
    }
}