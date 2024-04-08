package dev.vality.webhook.dispatcher.prototype;

import lombok.AllArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

class WebhookDispatcherPrototypeTest {

    LinkedBlockingQueue<Model> forwardQueue = new LinkedBlockingQueue<>();
    LinkedBlockingQueue<Model> firstRetryQueue = new LinkedBlockingQueue<>();
    LinkedBlockingQueue<Model> secondRetryQueue = new LinkedBlockingQueue<>();
    LinkedBlockingQueue<Model> thirdRetryQueue = new LinkedBlockingQueue<>();

    Map<String, String> commitMap = new HashMap<>();

    @Mock
    RemoteClient remoteClient;

    @BeforeEach
    public void init() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    void prototypeModel() throws InterruptedException, TimeoutException {
        forwardQueue.put(new Model("1", 1));
        forwardQueue.put(new Model("1", 2));
        forwardQueue.put(new Model("1", 3));

        iterateThreeMessage(forwardQueue, firstRetryQueue);

        assertThat(commitMap.keySet()).containsExactly("11", "12", "13");
    }

    private void iterateThreeMessage(LinkedBlockingQueue<Model> forwardQueue,
                                     LinkedBlockingQueue<Model> firstRetryQueue) throws InterruptedException {
        Model event;
        for (int i = 0; i < 3; i++) {
            event = forwardQueue.poll();
            checkAndCommit(event, firstRetryQueue);
        }
    }

    @Test
    void prototypeModelRetry() throws InterruptedException, TimeoutException {

        Mockito.when(remoteClient.invoke()).thenThrow(new TimeoutException());

        forwardQueue.put(new Model("1", 1));
        forwardQueue.put(new Model("1", 2));
        forwardQueue.put(new Model("1", 3));


        iterateThreeMessage(forwardQueue, firstRetryQueue);

        assertEquals(firstRetryQueue.size(), 3);

        Model event = firstRetryQueue.poll();
        checkAndCommit(event, secondRetryQueue);

        // Switch on URL
        Mockito.reset(remoteClient);
        Mockito.when(remoteClient.invoke()).thenReturn(true);

        event = firstRetryQueue.poll();
        checkAndCommit(event, secondRetryQueue);

        event = firstRetryQueue.poll();
        checkAndCommit(event, secondRetryQueue);

        assertEquals(3, secondRetryQueue.size());

        iterateThreeMessage(secondRetryQueue, thirdRetryQueue);

        assertThat(commitMap.keySet()).containsExactly("11", "12", "13");
    }

    private void checkAndCommit(Model event, LinkedBlockingQueue<Model> retryQueue) throws InterruptedException {
        if (isCanFastForward(event)) {
            try {
                remoteClient.invoke();
                commitMap.put(event.id + event.sequence, event.toString());
            } catch (TimeoutException e) {
                retryQueue.put(event);
            }
        } else {
            retryQueue.put(event);
        }
    }

    private boolean isCanFastForward(Model event) {
        if (event.sequence == 1) {
            return true;
        }
        int parentSequence = event.sequence - 1;
        return commitMap.containsKey(event.id + parentSequence);
    }

    interface RemoteClient {
        Boolean invoke() throws TimeoutException;
    }

    @AllArgsConstructor
    class Model {
        public String id;
        public int sequence;
    }

}
