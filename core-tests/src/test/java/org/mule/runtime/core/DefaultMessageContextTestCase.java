/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core;

import static org.mule.runtime.api.message.Message.of;
import static org.mule.runtime.core.api.event.EventContextFactory.create;
import static org.mule.tck.util.MuleContextUtils.mockContextWithServices;

import static java.util.Optional.empty;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

import org.mule.runtime.api.event.EventContext;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.message.GroupCorrelation;
import org.mule.runtime.core.internal.message.InternalEvent;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.util.Optional;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

public class DefaultMessageContextTestCase extends AbstractMuleTestCase {

  private static final String GENERATED_CORRELATION_ID = "generatedCorrelationIdValue";
  private static final String CUSTOM_CORRELATION_ID = "customCorrelationIdValue";
  private static final String SERVER_ID = "serverId";

  @Rule
  public MockitoRule rule = MockitoJUnit.rule();

  private final MuleContext muleContext = mockContextWithServices();

  @Mock(lenient = true)
  private FlowConstruct flow;

  private EventContext executionContext;
  private EventContext executionContextWithCorrelation;

  @Before
  public void before() {
    when(flow.getMuleContext()).thenReturn(muleContext);
    when(flow.getUniqueIdString()).thenReturn(GENERATED_CORRELATION_ID);
    when(flow.getServerId()).thenReturn(SERVER_ID);

    executionContext = create(flow, TEST_CONNECTOR_LOCATION);
    executionContextWithCorrelation = create(flow, TEST_CONNECTOR_LOCATION, CUSTOM_CORRELATION_ID);
  }

  @Test
  public void noCorrelationIdInContext() {
    final Message message = of(TEST_PAYLOAD);
    final CoreEvent event = InternalEvent.builder(executionContext).message(message).build();

    assertThat(event.getCorrelationId(), is(GENERATED_CORRELATION_ID));
  }

  @Test
  public void correlationIdInContext() {
    final Message message = of(TEST_PAYLOAD);
    final CoreEvent event = InternalEvent.builder(executionContextWithCorrelation).message(message).build();

    assertThat(event.getCorrelationId(), is(CUSTOM_CORRELATION_ID));
  }

  @Test
  public void overrideCorrelationIdInContext() {
    final Message message = of(TEST_PAYLOAD);
    final CoreEvent event = InternalEvent.builder(executionContextWithCorrelation).message(message)
        .groupCorrelation(empty()).build();

    assertThat(event.getCorrelationId(), is(CUSTOM_CORRELATION_ID));
  }

  @Test
  public void overrideCorrelationIdInContextSequence() {
    final Message message = of(TEST_PAYLOAD);
    final CoreEvent event =
        InternalEvent.builder(executionContextWithCorrelation).message(message).correlationId(CUSTOM_CORRELATION_ID)
            .groupCorrelation(Optional.of(GroupCorrelation.of(6))).build();

    assertThat(event.getCorrelationId(), is(CUSTOM_CORRELATION_ID));
  }

}
