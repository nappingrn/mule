/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.event;

import static org.mule.runtime.core.internal.event.NullEventFactory.getNullEvent;

import org.mule.api.annotation.NoImplement;
import org.mule.runtime.api.event.Event;
import org.mule.runtime.api.event.EventContext;
import org.mule.runtime.api.message.Error;
import org.mule.runtime.api.message.ItemSequenceInfo;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.api.security.SecurityContext;
import org.mule.runtime.core.api.context.notification.FlowCallStack;
import org.mule.runtime.core.api.message.GroupCorrelation;
import org.mule.runtime.core.internal.event.DefaultEventBuilder;
import org.mule.runtime.core.internal.message.InternalEvent;
import org.mule.runtime.core.privileged.event.BaseEventContext;

import java.io.Serializable;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.Function;

/**
 * Represents any data event occurring in the Mule environment. All data sent or received within the mule environment will be
 * passed between components as an Event.
 * <p>
 * Holds a Message payload and provides helper methods for obtaining the data in a format that the receiving Mule component
 * understands. The event can also maintain any number of properties that can be set and retrieved by Mule components.
 *
 * @see Message
 */
@NoImplement
public interface CoreEvent extends Serializable, Event {

  /**
   * @return a dummy event with no context and no values.
   */
  static CoreEvent nullEvent() {
    return getNullEvent();
  }

  /**
   * The security context for this session. If not null outbound, inbound and/or method invocations will be authenticated using
   * this context
   *
   * @return the context for this session or null if the request is not secure.
   */
  SecurityContext getSecurityContext();

  /**
   * Returns the correlation metadata of this message. See {@link GroupCorrelation}.
   *
   * @return the correlation metadata of this message.
   * @deprecated use {@link #getItemSequenceInfo()} instead
   */
  @Deprecated
  Optional<GroupCorrelation> getGroupCorrelation();

  /**
   * Events have a stack of executed flows (same as a call stack), so that at any given instant an application developer can
   * determine where this event came from.
   *
   * @return the flow stack associated to this event.
   * @since 3.8.0
   */
  FlowCallStack getFlowCallStack();

  /**
   * Create new {@link Builder}.
   *
   * @param context the context to create event instance with.
   * @return new builder instance.
   */
  static Builder builder(EventContext context) {
    return new DefaultEventBuilder((BaseEventContext) context);
  }

  /**
   * Create new {@link Builder} based on an existing {@link CoreEvent} instance. The existing {@link EventContext} is conserved.
   *
   * @param event existing event to use as a template to create builder instance
   * @return new builder instance.
   */
  static Builder builder(CoreEvent event) {
    return new DefaultEventBuilder((InternalEvent) event);
  }

  /**
   * Create new {@link Builder} based on an existing {@link CoreEvent} instance and and {@link EventContext}. A new
   * {@link EventContext} is used instead of the existing instance referenced by the existing {@link CoreEvent}. This builder
   * should only be used in some specific scenarios like {@code flow-ref} where a new Flow executing the same {@link CoreEvent}
   * needs a new context.
   *
   * @param event   existing event to use as a template to create builder instance
   * @param context the context to create event instance with.
   * @return new builder instance.
   */
  static Builder builder(EventContext context, CoreEvent event) {
    return new DefaultEventBuilder((BaseEventContext) context, (InternalEvent) event);
  }

  @NoImplement
  interface Builder {

    /**
     * Set the {@link Message} to construct {@link CoreEvent} with.
     *
     * @param message the message instance.
     * @return the builder instance
     */
    Builder message(Message message);

    /**
     * Set the {@link Function} that generates a {@link Message} to construct {@link CoreEvent} with.
     *
     * @param messageFactory the message factory instance.
     * @return the builder instance
     */
    Builder message(Function<EventContext, Message> messageFactory);

    /**
     * Set a map of variables. Any existing variables added to the builder will be removed.
     *
     * @param variables variables to be set.
     * @return the builder instance
     */
    Builder variables(Map<String, ?> variables);

    /**
     * Add a variable.
     *
     * @param key   the key of the variable to add.
     * @param value the value of the variable to add. {@code null} values are supported.
     * @return the builder instance.
     */
    Builder addVariable(String key, Object value);

    /**
     * Add a variable.
     *
     * @param key       the key of the variable to add.
     * @param value     the value of the variable to add. {@code null} values are supported.
     * @param mediaType additional metadata about the {@code value} type.
     * @return the builder instance
     */
    Builder addVariable(String key, Object value, DataType mediaType);

    /**
     * Add a variable.
     * <p>
     * This should be the preferred implementation when the value to be added is already held in the form of a {@link TypedValue}
     *
     * @param key   the key of the variable to add.
     * @param value the value of the variable to add. {@code null} values are supported.
     * @return the builder instance
     * @since 4.3.0
     */
    Builder addVariable(String key, TypedValue<?> value);

    /**
     * Remove a variable.
     *
     * @param key the variable key.
     * @return the builder instance
     */
    Builder removeVariable(String key);

    /**
     * Removes all variables.
     *
     * @return the builder instance
     */
    Builder clearVariables();

    /**
     * Configures the event's parameters
     *
     * @param parameters parameters to be set
     * @return {@code this} builder
     * @since 4.5.0
     */
    Builder parameters(Map<String, ?> parameters);

    /**
     * Removes all the parameters
     *
     * @return {@code this} builder
     * @since 4.5.0
     */
    Builder clearParameters();

    /**
     * Sets the group correlation information to the produced event.
     *
     * @param groupCorrelation the object containing the group correlation information to set on the produced event
     * @return the builder instance
     * @deprecated use {@link #itemSequenceInfo(Optional)}} instead
     */
    @Deprecated
    Builder groupCorrelation(Optional<GroupCorrelation> groupCorrelation);

    /**
     * Sets the event sequence information.
     *
     * @param itemSequenceInfo the object containing the sequence information of the produced event
     * @return the builder instance
     */
    default Builder itemSequenceInfo(Optional<ItemSequenceInfo> itemSequenceInfo) {
      return this;
    }

    /**
     * Sets an error related to the produced event.
     *
     * @param error the error associated with the produced event
     * @return the builder instance
     */
    Builder error(Error error);

    /**
     * The security context for this event. If not null outbound, inbound and/or method invocations will be authenticated using
     * this context.
     *
     * @param securityContext the context for this session or null if the request is not secure.
     */
    Builder securityContext(SecurityContext securityContext);

    /**
     * Build a new {@link CoreEvent} based on the state configured in the {@link Builder}.
     *
     * @return new {@link CoreEvent} instance.
     */
    CoreEvent build();

  }

  /**
   * Helper method to get the value of a given variable in a null-safe manner such that {@code null} is returned for non-existent
   * variables rather than a {@link NoSuchElementException} exception being thrown.
   *
   * @param key   the key of the variable to retrieve.
   * @param event the event from which to retrieve a variable with the given key.
   * @param <T>   the variable type
   * @return the value of the variables if it exists otherwise {@code null}.
   */
  static <T> T getVariableValueOrNull(String key, CoreEvent event) {
    if (event.getVariables().containsKey(key)) {
      return (T) event.getVariables().get(key).getValue();
    } else {
      return null;
    }
  }

}
