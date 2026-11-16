package com.guicedee.persistence.test;

import java.util.Map;
import org.hibernate.jpa.boot.spi.PersistenceUnitDescriptor;
import org.hibernate.reactive.provider.ReactivePersistenceProvider;
import org.hibernate.reactive.provider.impl.ReactiveProviderChecker;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Exercises Jakarta provider selection in the installed, shaded Reactive artifact. */
class HibernateReactiveProviderSelectionTest {
    private final String provider = ReactivePersistenceProvider.class.getName();

    @Test void jakartaIntegrationPropertyTakesPrecedenceOverDescriptor() {
        var descriptor = mock(PersistenceUnitDescriptor.class);
        when(descriptor.getProviderClassName()).thenReturn("another.Provider");
        assertTrue(ReactiveProviderChecker.isProvider(descriptor,
                Map.of("jakarta.persistence.provider", " " + provider + " ")));
    }

    @Test void explicitlySelectingAnotherProviderIsNotIgnored() {
        var descriptor = mock(PersistenceUnitDescriptor.class);
        when(descriptor.getProviderClassName()).thenReturn(provider);
        assertFalse(ReactiveProviderChecker.isProvider(descriptor,
                Map.of("jakarta.persistence.provider", "another.Provider")));
    }

    @Test void descriptorStillSelectsTheProviderWithoutAnOverride() {
        var descriptor = mock(PersistenceUnitDescriptor.class);
        when(descriptor.getProviderClassName()).thenReturn(provider);
        assertTrue(ReactiveProviderChecker.isProvider(descriptor, Map.of()));
    }
}
