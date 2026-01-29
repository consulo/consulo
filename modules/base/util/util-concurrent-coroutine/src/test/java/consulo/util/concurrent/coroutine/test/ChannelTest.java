//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
// This file is a part of the 'coroutines' project.
// Copyright 2018 Elmar Sonnenschein, esoco GmbH, Flensburg, Germany
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//	  http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
package consulo.util.concurrent.coroutine.test;

import consulo.util.concurrent.coroutine.*;
import consulo.util.concurrent.coroutine.internal.Coroutines;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static consulo.util.concurrent.coroutine.ChannelId.stringChannel;
import static consulo.util.concurrent.coroutine.CoroutineScope.launch;
import static consulo.util.concurrent.coroutine.step.ChannelReceive.receive;
import static consulo.util.concurrent.coroutine.step.ChannelSend.send;
import static consulo.util.concurrent.coroutine.step.CodeExecution.apply;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test of {@link Coroutine}.
 *
 * @author eso
 */
public class ChannelTest {

    private static final ChannelId<String> TEST_CHANNEL =
        stringChannel("TestChannel");

    private static final Coroutine<String, String> SEND =
        Coroutine.first(apply((String s) -> s + "test"))
            .then(send(TEST_CHANNEL))
            .withName("Send");

    private static final Coroutine<?, String> RECEIVE =
        Coroutine.first(receive(TEST_CHANNEL))
            .withName("Receive")
            .then(apply(s -> s.toUpperCase()));

    /**
     * Test of asynchronous channel communication.
     */
    @Test
    public void testChannel() {
        CoroutineContext context = TestCoroutineContext.newSilent();

        Coroutine<?, String> receive2 =
            RECEIVE.then(apply((String s) -> s.toLowerCase()));

        launch(context, scope -> {
            Continuation<String> r1 = RECEIVE.runAsync(scope, null);
            Continuation<String> r2 = receive2.runAsync(scope, null);

            Continuation<?> s1 = SEND.runAsync(scope, "123");
            Continuation<?> s2 = SEND.runAsync(scope, "456");

            assertEquals("123test", s1.getResult());
            assertEquals("456test", s2.getResult());

            String r1v = r1.getResult();
            String r2v = r2.getResult();

            // because of the concurrent execution it is not fixed which
            // of the values r1 and r2 will receive
            assertTrue(
                "123test".equalsIgnoreCase(r1v) || "456test".equalsIgnoreCase(
                    r1v));
            assertTrue(
                "123test".equalsIgnoreCase(r2v) || "456test".equalsIgnoreCase(
                    r2v));
            assertTrue(s1.isFinished());
            assertTrue(s2.isFinished());
            assertTrue(r1.isFinished());
            assertTrue(r2.isFinished());
        });
    }

    /**
     * Test of channel closing.
     */
    @Test
    public void testChannelClose() {
        CoroutineContext context = TestCoroutineContext.newSilent();

        launch(context, scope -> {
            Continuation<String> result = null;

            try {
                result = RECEIVE.runAsync(scope, null);

                result.getChannel(TEST_CHANNEL).close();
                result.getResult();
            }
            catch (ChannelClosedException e) {
                // expected
                result.errorHandled();
            }
        });

        launch(context, scope -> {
            Continuation<String> result = null;

            try {
                scope.getChannel(TEST_CHANNEL).close();
                result = SEND.runAsync(scope, "TEST");

                result.getResult();
                fail();
            }
            catch (ChannelClosedException e) {
                // expected
                result.errorHandled();
            }
        });
    }
}