package com.guicedee.vertxpersistence.bind;

import com.guicedee.client.IGuiceContext;
import com.guicedee.guicedservlets.rest.services.RestInterceptor;
import io.vertx.core.Future;
import io.vertx.core.Vertx;

public class RestServiceInterceptor implements RestInterceptor
{
    @Override
    public Future<Boolean> onStart()
    {
        return Vertx.currentContext().executeBlocking(()->{
            JtaUnitOfWork unitOfWork = IGuiceContext.get(JtaUnitOfWork.class);
            if (unitOfWork != null)
            {
                unitOfWork.begin();
            }
            else
            {
                throw new RuntimeException("No JTA Unit of Work found");
            }
            return true;
        });
    }

    @Override
    public Future<Boolean> onEnd()
    {
        JtaUnitOfWork unitOfWork = IGuiceContext.get(JtaUnitOfWork.class);
        if (unitOfWork != null)
        {
            unitOfWork.end();
        }
        else
        {
            throw new RuntimeException("No JTA Unit of Work found");
        }
        return Future.succeededFuture(true);
    }
}
