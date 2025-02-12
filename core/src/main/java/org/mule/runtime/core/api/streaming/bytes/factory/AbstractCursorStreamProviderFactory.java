/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.streaming.bytes.factory;

import static java.lang.Boolean.getBoolean;
import static org.mule.runtime.api.util.MuleSystemProperties.TRACK_CURSOR_PROVIDER_CLOSE_PROPERTY;
import static org.mule.runtime.core.privileged.util.EventUtils.getRoot;

import org.mule.api.annotation.NoExtend;
import org.mule.runtime.api.component.AbstractComponent;
import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.event.EventContext;
import org.mule.runtime.api.streaming.bytes.CursorStream;
import org.mule.runtime.api.streaming.bytes.CursorStreamProvider;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.streaming.StreamingManager;
import org.mule.runtime.core.api.streaming.bytes.ByteBufferManager;
import org.mule.runtime.core.api.streaming.bytes.CursorStreamProviderFactory;
import org.mule.runtime.core.internal.streaming.CursorManager;

import java.io.InputStream;

/**
 * Base implementation of {@link CursorStreamProviderFactory} which contains all the base behaviour and template methods.
 * <p>
 * It interacts with the {@link CursorManager} in order to track all allocated resources and make sure they're properly disposed
 * of once they're no longer necessary.
 *
 * @since 4.0
 */
@NoExtend
public abstract class AbstractCursorStreamProviderFactory extends AbstractComponent implements CursorStreamProviderFactory {

  protected final ByteBufferManager bufferManager;
  protected final StreamingManager streamingManager;
  protected static final boolean trackCursorProviderClose = getBoolean(TRACK_CURSOR_PROVIDER_CLOSE_PROPERTY);

  /**
   * Creates a new instance
   *
   * @param bufferManager the {@link ByteBufferManager} that will be used to allocate all buffers
   */
  protected AbstractCursorStreamProviderFactory(ByteBufferManager bufferManager, StreamingManager streamingManager) {
    this.bufferManager = bufferManager;
    this.streamingManager = streamingManager;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public final Object of(EventContext eventContext, InputStream inputStream, ComponentLocation originatingLocation) {
    if (inputStream instanceof CursorStream) {
      return streamingManager.manage(((CursorStream) inputStream).getProvider(), eventContext);
    }

    Object value = resolve(inputStream, eventContext, originatingLocation);
    if (value instanceof CursorStreamProvider) {
      value = streamingManager.manage((CursorStreamProvider) value, eventContext);
    }

    return value;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public final Object of(EventContext eventContext, InputStream inputStream) {
    return of(eventContext, inputStream, null);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public final Object of(CoreEvent event, InputStream inputStream, ComponentLocation originatingLocation) {
    return of(getRoot(event.getContext()), inputStream, originatingLocation);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public final Object of(CoreEvent event, InputStream inputStream) {
    return of(getRoot(event.getContext()), inputStream, null);
  }

  /**
   * @return the {@link ByteBufferManager} that <b>MUST</b> to be used to allocate byte buffers
   */
  protected ByteBufferManager getBufferManager() {
    return bufferManager;
  }

  /**
   * Implementations should use this method to actually create the output value
   *
   * @param inputStream  a stream
   * @param eventContext the context of the event on which the stream was opened
   * @return a resolved value
   */
  protected abstract Object resolve(InputStream inputStream, EventContext eventContext, ComponentLocation originatingLocation);

  /**
   * Implementations should use this method to actually create the output value
   *
   * @param inputStream a stream
   * @param event       the event on which the stream was opened
   * @return a resolved value
   * @deprecated Use {@link #resolve(InputStream, EventContext, ComponentLocation)} instead.
   */
  @Deprecated
  protected abstract Object resolve(InputStream inputStream, CoreEvent event, ComponentLocation originatingLocation);
}
