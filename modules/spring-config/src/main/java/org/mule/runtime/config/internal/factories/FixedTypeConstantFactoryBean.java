/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.factories;

/**
 * A specialization of {@link ConstantFactoryBean} which always returns a fix type when {@link #getObjectType()} is invoked,
 * regardless of the actual value
 *
 * @since 4.2
 */
public class FixedTypeConstantFactoryBean<T> extends ConstantFactoryBean<T> {

  private final Class<?> type;

  public FixedTypeConstantFactoryBean(T value, Class<?> type) {
    super(value);
    this.type = type;
  }

  @Override
  public Class<?> getObjectType() {
    return type;
  }
}
