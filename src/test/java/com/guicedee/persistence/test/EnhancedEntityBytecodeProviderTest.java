package com.guicedee.persistence.test;

import com.guicedee.persistence.implementations.EnhancedEntityBytecodeProvider;
import org.hibernate.bytecode.spi.BytecodeProvider;
import org.hibernate.bytecode.spi.ReflectionOptimizer;
import org.hibernate.bytecode.enhance.spi.EnhancementContext;
import org.hibernate.engine.spi.PersistentAttributeInterceptable;
import org.junit.jupiter.api.Test;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.hibernate.boot.registry.StandardServiceInitiator;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.cfg.BytecodeSettings;
import com.guicedee.persistence.implementations.VertxServiceContributor;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SuppressWarnings("removal")
class EnhancedEntityBytecodeProviderTest {
    abstract static class Enhanced implements PersistentAttributeInterceptable {}

    @Test void enhancedEntitiesUseStandardAccessWithoutInitializingOrResettingByteBuddy() {
        var count = new AtomicInteger();
        var provider = new EnhancedEntityBytecodeProvider(() -> { count.incrementAndGet(); return mock(BytecodeProvider.class); });
        assertNull(provider.getReflectionOptimizer(Enhanced.class, Map.of()));
        assertNull(provider.getReflectionOptimizer(Enhanced.class, new String[0], new String[0], new Class[0]));
        provider.resetCaches();
        assertEquals(0, count.get());
    }

    @Test void ordinaryClassesProxiesAndEnhancementKeepTheOriginalProvider() {
        var upstream = mock(BytecodeProvider.class);
        var optimizer = mock(ReflectionOptimizer.class);
        when(upstream.getReflectionOptimizer(String.class, Map.of())).thenReturn(optimizer);
        var count = new AtomicInteger();
        var provider = new EnhancedEntityBytecodeProvider(() -> { count.incrementAndGet(); return upstream; });
        assertSame(optimizer, provider.getReflectionOptimizer(String.class, Map.of()));
        provider.getProxyFactoryFactory();
        var context = mock(EnhancementContext.class);
        provider.getEnhancer(context);
        provider.resetCaches();
        assertEquals(1, count.get());
        verify(upstream).getProxyFactoryFactory();
        verify(upstream).getEnhancer(context);
        verify(upstream).resetCaches();
    }

    private StandardServiceInitiator<BytecodeProvider> initiator() {
        var builder = mock(StandardServiceRegistryBuilder.class);
        var selected = new AtomicReference<StandardServiceInitiator<BytecodeProvider>>();
        doAnswer(call -> { selected.set(call.getArgument(0)); return builder; })
                .when(builder).addInitiator(any());
        new VertxServiceContributor().contribute(builder);
        return selected.get();
    }

    @Test void explicitProviderIsUsedWithoutDiscovery() {
        var custom = mock(BytecodeProvider.class);
        var registry = mock(ServiceRegistryImplementor.class);
        assertSame(custom, initiator().initiateService(Map.of(BytecodeSettings.BYTECODE_PROVIDER_INSTANCE, custom), registry));
        verifyNoInteractions(registry);
        assertThrows(IllegalArgumentException.class, () -> initiator().initiateService(
                Map.of(BytecodeSettings.BYTECODE_PROVIDER_INSTANCE, "invalid"), registry));
    }

    @Test void standardLoaderDefersTheDefaultProviderWithoutConsultingTheRegistry() {
        var registry = mock(ServiceRegistryImplementor.class);
        assertInstanceOf(EnhancedEntityBytecodeProvider.class,
                initiator().initiateService(Map.of("guicedee.persistence.standard_loader", true), registry));
        verifyNoInteractions(registry);
    }

    @Test void optOutUsesHibernatesNormalProviderDiscovery() {
        var custom = mock(BytecodeProvider.class);
        var registry = mock(ServiceRegistryImplementor.class);
        var loader = mock(ClassLoaderService.class);
        when(registry.requireService(ClassLoaderService.class)).thenReturn(loader);
        when(loader.loadJavaServices(BytecodeProvider.class)).thenReturn(java.util.List.of(custom));
        assertSame(custom, initiator().initiateService(Map.of("guicedee.persistence.optimize_enhanced_entities", "false"), registry));
        verify(loader).loadJavaServices(BytecodeProvider.class);
    }

    @Test void unknownLoaderArrangementPreservesTheDiscoveredProvider() {
        var custom = mock(BytecodeProvider.class);
        var registry = mock(ServiceRegistryImplementor.class);
        var loader = mock(ClassLoaderService.class);
        when(registry.requireService(ClassLoaderService.class)).thenReturn(loader);
        when(loader.loadJavaServices(BytecodeProvider.class)).thenReturn(java.util.List.of(custom));
        assertSame(custom, initiator().initiateService(Map.of(), registry));
        verify(loader).loadJavaServices(BytecodeProvider.class);
        verifyNoInteractions(custom);
    }
}
