package com.ziqni.admin.bus;

import com.google.common.eventbus.DeadEvent;
import com.google.common.eventbus.EventBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ZiqniEventBus {

    private static final Logger logger = LoggerFactory.getLogger(ZiqniEventBus.class);

    public EventBus entityChangedEventBus = new EventBus();
    public EventBus entityStateChangedEventBus = new EventBus();

    public ZiqniEventBus() {
    }

    /**
     * Posts an event to all registered subscribers. This method will return successfully after the
     * event has been posted to all subscribers, and regardless of any exceptions thrown by
     * subscribers.
     *
     * <p>If no subscribers have been subscribed for {@code event}'s class, and {@code event} is not
     * already a {@link DeadEvent}, it will be wrapped in a DeadEvent and reposted.
     *
     * @param event event to post.
     */
    public void postEntityChangedEventBus(Object event) {
        this.entityChangedEventBus.post(event);
    }


    /**
     * Registers all subscriber methods on {@code object} to receive events.
     *
     * @param object object whose subscriber methods should be registered.
     */
    public void registerEntityChangedEventBus(Object object) {
        this.entityChangedEventBus.register(object);
    }

    /**
     * Posts an event to all registered subscribers. This method will return successfully after the
     * event has been posted to all subscribers, and regardless of any exceptions thrown by
     * subscribers.
     *
     * <p>If no subscribers have been subscribed for {@code event}'s class, and {@code event} is not
     * already a {@link DeadEvent}, it will be wrapped in a DeadEvent and reposted.
     *
     * @param event event to post.
     */
    public void postEntityStateChangedEventBus(Object event) {
        this.entityStateChangedEventBus.post(event);
    }


    /**
     * Registers all subscriber methods on {@code object} to receive events.
     *
     * @param object object whose subscriber methods should be registered.
     */
    public void registerEntityStateChangedEventBus(Object object) {
        this.entityStateChangedEventBus.register(object);
    }
}
