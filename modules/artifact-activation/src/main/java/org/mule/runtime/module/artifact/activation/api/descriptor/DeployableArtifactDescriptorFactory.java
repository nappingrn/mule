/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.activation.api.descriptor;

import org.mule.runtime.api.deployment.meta.MulePluginModel;
import org.mule.runtime.module.artifact.activation.api.deployable.DeployableProjectModel;
import org.mule.runtime.module.artifact.activation.api.plugin.PluginDescriptorResolver;
import org.mule.runtime.module.artifact.activation.api.plugin.PluginModelResolver;
import org.mule.runtime.module.artifact.activation.internal.descriptor.DefaultDeployableArtifactDescriptorFactory;
import org.mule.runtime.module.artifact.api.descriptor.ApplicationDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactPluginDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.BundleDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.DomainDescriptor;

import java.util.Map;

/**
 * Provides methods to create the descriptors of deployable artifacts (i.e. applications and domains with the descriptors for
 * their plugins within) from a {@link DeployableProjectModel}.
 *
 * @since 4.5
 */
public interface DeployableArtifactDescriptorFactory {

  static DeployableArtifactDescriptorFactory defaultArtifactDescriptorFactory() {
    return new DefaultDeployableArtifactDescriptorFactory();
  }

  /**
   * Creates a descriptor for a domain, including its plugin descriptors.
   *
   * @param model                    model describing the structure of the domain with all the necessary information to build its
   *                                 descriptor.
   * @param deploymentProperties     properties that affect how the artifact is deployed.
   * @param pluginModelResolver      resolves {@link MulePluginModel} from a dependency. Default implementation is
   *                                 {@link PluginModelResolver#mavenDeployablePluginModelResolver()}.
   * @param pluginDescriptorResolver a wrapper function around the logic to extract an {@link ArtifactPluginDescriptor} from the
   *                                 jar described by the {@link BundleDescriptor}, otherwise it will be created.
   * @return a descriptor for a domain.
   */
  DomainDescriptor createDomainDescriptor(DeployableProjectModel model,
                                          Map<String, String> deploymentProperties,
                                          PluginModelResolver pluginModelResolver,
                                          PluginDescriptorResolver pluginDescriptorResolver);

  /**
   * Creates a descriptor for an application, including its plugin descriptors.
   *
   * @param model                    model describing the structure of the application with all the necessary information to build
   *                                 its descriptor.
   * @param deploymentProperties     properties that affect how the artifact is deployed.
   * @param pluginModelResolver      resolves {@link MulePluginModel} from a dependency. Default implementation is
   *                                 {@link PluginModelResolver#mavenDeployablePluginModelResolver()}.
   * @param pluginDescriptorResolver a wrapper function around the logic to extract an {@link ArtifactPluginDescriptor} from the
   *                                 jar described by the {@link BundleDescriptor}, otherwise it will be created.
   * @return a descriptor for an application.
   */
  ApplicationDescriptor createApplicationDescriptor(DeployableProjectModel model,
                                                    Map<String, String> deploymentProperties,
                                                    PluginModelResolver pluginModelResolver,
                                                    PluginDescriptorResolver pluginDescriptorResolver);

}