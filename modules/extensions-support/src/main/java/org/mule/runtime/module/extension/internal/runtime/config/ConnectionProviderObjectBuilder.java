/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.config;

import static java.util.Collections.emptyList;

import static org.mule.runtime.core.api.retry.ReconnectionConfig.defaultReconnectionConfig;
import static org.mule.runtime.core.internal.connection.ConnectionUtils.getInjectionTarget;
import static org.mule.runtime.module.extension.internal.loader.parser.java.connection.SdkConnectionProviderAdapter.from;
import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.getConnectionProviderFactory;

import org.mule.runtime.api.config.PoolingProfile;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.connection.ConnectionProviderModel;
import org.mule.runtime.api.util.Pair;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.el.ExpressionManager;
import org.mule.runtime.core.api.retry.ReconnectionConfig;
import org.mule.runtime.module.extension.internal.loader.parser.java.connection.SdkConnectionProviderAdapter;
import org.mule.runtime.module.extension.internal.runtime.objectbuilder.ResolverSetBasedObjectBuilder;
import org.mule.runtime.module.extension.internal.runtime.resolver.ResolverSet;
import org.mule.runtime.module.extension.internal.runtime.resolver.ResolverSetResult;
import org.mule.runtime.module.extension.internal.util.ValueSetter;

import java.util.List;

/**
 * Implementation of {@link ResolverSetBasedObjectBuilder} which produces instances of {@link ConnectionProviderModel}
 *
 * @since 4.0
 */
public abstract class ConnectionProviderObjectBuilder<C>
    extends ResolverSetBasedObjectBuilder<Pair<ConnectionProvider<C>, ResolverSetResult>> {

  protected final ConnectionProviderModel providerModel;
  protected final ReconnectionConfig reconnectionConfig;
  protected final PoolingProfile poolingProfile;
  protected final ExtensionModel extensionModel;
  protected final MuleContext muleContext;

  private volatile boolean firstBuild = true;
  protected String ownerConfigName;

  /**
   * Creates a new instances which produces instances based on the given {@code providerModel} and {@code resolverSet}
   *
   * @param providerModel the {@link ConnectionProviderModel} which describes the instances to be produced
   * @param resolverSet   a {@link ResolverSet} to populate the values
   */
  public ConnectionProviderObjectBuilder(ConnectionProviderModel providerModel,
                                         ResolverSet resolverSet,
                                         ExtensionModel extensionModel,
                                         ExpressionManager expressionManager,
                                         MuleContext muleContext) {
    this(providerModel, resolverSet, null, null, extensionModel, expressionManager, muleContext);
  }

  public ConnectionProviderObjectBuilder(Class<?> prototypeClass,
                                         ConnectionProviderModel providerModel,
                                         ResolverSet resolverSet,
                                         PoolingProfile poolingProfile,
                                         ReconnectionConfig reconnectionConfig,
                                         ExtensionModel extensionModel,
                                         ExpressionManager expressionManager,
                                         MuleContext muleContext) {
    super(prototypeClass, providerModel, resolverSet, expressionManager, muleContext);
    this.providerModel = providerModel;
    this.poolingProfile = poolingProfile;
    this.extensionModel = extensionModel;
    this.muleContext = muleContext;
    this.reconnectionConfig = computeReconnectionConfig(reconnectionConfig);
  }

  public ConnectionProviderObjectBuilder(ConnectionProviderModel providerModel,
                                         ResolverSet resolverSet,
                                         PoolingProfile poolingProfile,
                                         ReconnectionConfig reconnectionConfig,
                                         ExtensionModel extensionModel,
                                         ExpressionManager expressionManager,
                                         MuleContext muleContext) {
    this(getConnectionProviderFactory(providerModel).getObjectType(), providerModel, resolverSet, poolingProfile,
         reconnectionConfig, extensionModel, expressionManager, muleContext);
  }

  private ReconnectionConfig computeReconnectionConfig(ReconnectionConfig reconnectionConfig) {
    return reconnectionConfig != null ? reconnectionConfig : defaultReconnectionConfig();
  }

  public ConnectionProviderObjectBuilder(ConnectionProviderModel providerModel,
                                         Class<?> prototypeClass,
                                         ResolverSet resolverSet,
                                         PoolingProfile poolingProfile,
                                         ReconnectionConfig reconnectionConfig,
                                         ExtensionModel extensionModel,
                                         ExpressionManager expressionManager,
                                         MuleContext muleContext) {
    super(prototypeClass, providerModel, resolverSet, expressionManager, muleContext);
    this.providerModel = providerModel;
    this.poolingProfile = poolingProfile;
    this.extensionModel = extensionModel;
    this.muleContext = muleContext;
    this.reconnectionConfig = computeReconnectionConfig(reconnectionConfig);
  }

  /**
   * {@inheritDoc}
   */
  protected Pair<ConnectionProvider<C>, ResolverSetResult> instantiateObject() {
    return new Pair<>((ConnectionProvider<C>) getConnectionProviderFactory(providerModel).newInstance(), null);
  }

  @Override
  public Pair<ConnectionProvider<C>, ResolverSetResult> build(ResolverSetResult result) throws MuleException {
    final ConnectionProvider<C> value = from(instantiateObject().getFirst());
    Object injectionTarget = getInjectionTarget(value);

    if (firstBuild) {
      synchronized (this) {
        if (firstBuild) {
          singleValueSetters = super.createSingleValueSetters(injectionTarget.getClass(), resolverSet);
          firstBuild = false;
        }
      }
    }

    populate(result, injectionTarget);
    return new Pair<>(value, result);
  }

  /**
   * In order to support {@link org.mule.sdk.api.connectivity.ConnectionProvider} instances, introspection needs to be deferred to
   * the actual instantiation process so that {@link SdkConnectionProviderAdapter} can be unwrapped.
   * <p>
   * Therefore, this method always returns an empty list so that no introspection happens on the setup of this builder but
   * deferred to the first execution of the {@link #build(ResolverSetResult)} method.
   */
  @Override
  protected List<ValueSetter> createSingleValueSetters(Class<?> prototypeClass, ResolverSet resolverSet) {
    return emptyList();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isDynamic() {
    return resolverSet.isDynamic();
  }

  public void setOwnerConfigName(String ownerConfigName) {
    this.ownerConfigName = ownerConfigName;
  }
}
