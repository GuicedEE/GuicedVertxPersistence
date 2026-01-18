package com.guicedee.vertxpersistence.implementations;

import com.guicedee.client.services.IGuiceConfig;
import com.guicedee.client.services.lifecycle.IGuiceConfigurator;

/**
 * Configures Guice scanning behavior for the persistence module.
 * Enables classpath, annotation, field, and method scanning with relaxed visibility
 * so persistence annotations and bindings are discovered consistently.
 */
public class GuicedConfigurator implements IGuiceConfigurator
{
    /**
     * Applies scanning configuration suitable for persistence wiring.
     *
     * @param iGuiceConfig the configuration to update
     * @return the same configuration instance with scanning options enabled
     */
    @Override
    public IGuiceConfig<?> configure(IGuiceConfig<?> iGuiceConfig)
    {
        iGuiceConfig.setMethodInfo(true)
                .setFieldInfo(true)
                .setAllowPaths(true)
                .setClasspathScanning(true)
                .setAnnotationScanning(true)
                .setFieldScanning(true)
                .setIgnoreClassVisibility(true)
                .setIgnoreFieldVisibility(true)
                .setIgnoreMethodVisibility(true)
        ;
        return iGuiceConfig;
    }
}
