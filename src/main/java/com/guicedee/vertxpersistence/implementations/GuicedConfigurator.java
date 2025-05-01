package com.guicedee.vertxpersistence.implementations;

import com.guicedee.guicedinjection.interfaces.IGuiceConfig;
import com.guicedee.guicedinjection.interfaces.IGuiceConfigurator;

public class GuicedConfigurator implements IGuiceConfigurator
{
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
