package com.guicedee.vertxpersistence.implementations;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.guicedee.guicedservlets.servlets.services.scopes.CallScope;
import com.guicedee.vertxpersistence.bind.JtaPersistService;
import com.guicedee.vertxpersistence.bind.JtaUnitOfWork;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.reactive.mutiny.Mutiny;

@CallScope
@AllArgsConstructor
@NoArgsConstructor
public class SqlClientProvider implements Provider<Mutiny.SessionFactory>
{
    @Inject
    private JtaUnitOfWork unitOfWork;
    @Setter
    private JtaPersistService persistService;

    @Override
    public Mutiny.SessionFactory get()
    {
        if (persistService != null && persistService.getEmFactory() == null)
        {
            persistService.start();
        }
        else if (persistService != null && persistService.getEmFactory() != null)
        {
            var sessionFactory = unitOfWork.getSessionFactory();
            return sessionFactory;
        }
        return null;
    }
}
