/*
 *   $Id$
 *
 *   Copyright 2008 Glencoe Software, Inc. All rights reserved.
 *   Use is subject to license terms supplied in LICENSE.txt
 */

package ome.services.util;

import java.sql.SQLException;

import ome.conditions.InternalException;
import ome.security.SecuritySystem;
import ome.system.OmeroContext;
import ome.system.Principal;
import ome.system.ServiceFactory;
import ome.tools.spring.InternalServiceFactory;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.StatelessSession;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.springframework.aop.ProxyMethodInvocation;
import org.springframework.aop.framework.ProxyFactoryBean;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.task.TaskExecutor;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Simple execution/work interface which can be used for <em>internal</em>
 * tasks which need to have a full working implementation. The
 * {@link Executor#execute(Principal, ome.services.util.Executor.Work)} method
 * ensures that {@link SecuritySystem#login(Principal)} is called before the
 * task, that a {@link TransactionCallback} and a {@link HibernateCallback}
 * surround the call, and that subsequently {@link SecuritySystem#logout()} is
 * called.
 * 
 * @author Josh Moore, josh at glencoesoftware.com
 * @since 3.0-Beta3
 */
public class Executor implements ApplicationContextAware {

    private final static Log log = LogFactory.getLog(Executor.class);

    /**
     * Work SPI to perform actions within the server as if they were fully
     * wrapped in our service logic. Note: any results which are coming from
     * Hibernate <em>may <b>not</b></em> be assigned directly to a field,
     * rather must be returned as an {@link Object} so that Hibernate proxies
     * can be properly handled.
     */
    public interface Work {
        /**
         * Work method. Must return all results coming from Hibernate via the
         * {@link Object} return method.
         * 
         * @param status
         *            non null.
         * @param session
         *            non null.
         * @param sf
         *            non null.
         * @return Any results which will be used by non-wrapped code.
         */
        @Transactional(readOnly = false)
        Object doWork(TransactionStatus status, Session session,
                ServiceFactory sf);

    }

    /**
     * Work SPI to perform actions related to
     * {@link org.hibernate.SessionFactory#openStatelessSession() stateless}
     * sessions. This overrides <em>ALL</em> security in the server and should
     * only be used as a last resort. Currently accept locations are:
     * <ul>
     * <li>In the {@link ome.services.sessions.SessionManager} to boot strap a
     * {@link ome.model.meta.Session session}
     * <li>In the {@link ome.security.basic.EventHandler} to save
     * {@link ome.model.meta.EventLog event logs}
     * </ul>
     */
    public interface StatelessWork {
        Object doWork(StatelessSession session);

    }

    protected OmeroContext context;
    protected Scheduler scheduler;

    final protected ProxyFactoryBean proxyFactory;
    final protected SecuritySystem secSystem;
    final protected String[] proxyNames;
    final protected TransactionTemplate txTemplate;
    final protected HibernateTemplate hibTemplate;

    public Executor(SecuritySystem secSystem, TransactionTemplate tt,
            HibernateTemplate ht, String[] proxyNames) {
        this.txTemplate = tt;
        this.hibTemplate = ht;
        this.secSystem = secSystem;
        this.proxyNames = proxyNames;
        this.proxyFactory = new ProxyFactoryBean();
        this.proxyFactory.setInterceptorNames(this.proxyNames);
        try {
            this.proxyFactory.setProxyInterfaces(new Class[] { Work.class });
        } catch (Exception e) {
            throw new RuntimeException("Error working with Work.class; "
                    + "highly unlikely; " + "something is weird.", e);
        }
    }

    /**
     * Initialization method called by server (not Spring) when background
     * processing can begin.
     */
    public void init() {
        try {
            this.scheduler.start();
            this.log.info("Background schedular activated.");
        } catch (SchedulerException se) {
            throw new RuntimeException("Failed to startup scheduler.", se);
        }
    }

    public void setApplicationContext(ApplicationContext applicationContext)
            throws BeansException {
        this.context = (OmeroContext) applicationContext;
        this.proxyFactory.setBeanFactory(context);
    }

    public void setScheduler(Scheduler scheduler) {
        this.scheduler = scheduler;
    }

    /**
     * Calls
     * {@link #execute(Principal, ome.services.util.Executor.Work, boolean) with
     * readOnly set to false.
     */
    public Object execute(final Principal p, final Work work) {
        return execute(p, work, false);
    }

    /**
     * Executes a {@link Work} instance wrapped in two layers of AOP. The first
     * is intended to acquire the proper arguments for
     * {@link Work#doWork(TransactionStatus, Session, ServiceFactory)} for the
     * {@link OmeroContext}, and the second performs all the standard service
     * actions for any normal method call.
     * 
     * If the {@link Principal} argument is not null, then additionally, a
     * login/logout sequence will be performed in a try/finally block.
     * 
     * @param p
     * @param work
     */
    public Object execute(final Principal p, final Work work, boolean readOnly) {
        ProxyFactoryBean innerFactory = new ProxyFactoryBean();
        innerFactory.copyFrom(this.proxyFactory);
        innerFactory.setTarget(work);
        innerFactory.addAdvice(new Interceptor(txTemplate, hibTemplate,
                readOnly));
        Work inner = (Work) innerFactory.getObject();

        // If we've already entered Executor.execute once and applied the
        // ServiceHandler, then we might not want to re-apply all the
        // interceptors.
        // -----------------
        Work outer;
        this.proxyFactory.setTarget(inner);
        outer = (Work) this.proxyFactory.getObject();

        if (p != null) {
            this.secSystem.login(p);
        }
        try {
            // Arguments will be replaced after hibernate is in effect
            return outer.doWork(null, null, new InternalServiceFactory(
                    this.context));
        } finally {
            if (p != null) {
                this.secSystem.logout();
            }
        }
    }

    /**
     * Executes a {@link StatelessWork} in a call to
     * {@link TransactionTemplate#execute(TransactionCallback)} and
     * {@link HibernateTemplate#execute(HibernateCallback)}. No OMERO specific
     * AOP is applied. Since {@link StatelessSession} does not return proxies,
     * there is less concern about returned values, but this method
     * <em>completely</em> overrides OMERO security, and should be used
     * <b>very</em> carefully. *
     * 
     * @param work
     *            Non-null.
     * @return
     */
    public Object executeStateless(final StatelessWork work) {
        return txTemplate.execute(new TransactionCallback() {
            public Object doInTransaction(final TransactionStatus status) {
                return hibTemplate.execute(new HibernateCallback() {
                    public Object doInHibernate(final Session session)
                            throws HibernateException, SQLException {
                        StatelessSession s = hibTemplate.getSessionFactory()
                                .openStatelessSession(session.connection());
                        return work.doWork(s);
                    }
                }, true);
            }
        });
    }

    /**
     * Runs an {@link ExecutionThread} via
     * {@link TaskExecutor#execute(Runnable)}. The {@link ExecutionThread}
     * performs the necessary {@link Thread} initialization.
     * 
     * @param thread
     *            Not null.
     */
    public void trigger(String name) {
        if (scheduler == null) {
            throw new InternalException("Executor not configured for trigger.");
        }

        try {
            scheduler.triggerJob(name, "DEFAULT");
        } catch (SchedulerException e) {
            log.error("Error signaling job: " + name, e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Interceptor class which properly lookups and injects the transaction and
     * session objects in the
     * {@link Work#doWork(TransactionStatus, Session, ServiceFactory)} method.
     */
    static class Interceptor implements MethodInterceptor {

        private final boolean readOnly;
        private final TransactionTemplate txTemplate;
        private final HibernateTemplate hibTemplate;

        public Interceptor(TransactionTemplate tt, HibernateTemplate ht,
                boolean readOnly) {
            this.readOnly = readOnly;
            this.txTemplate = tt;
            this.hibTemplate = ht;
        }

        public Object invoke(MethodInvocation arg0) throws Throwable {

            // Used by EventHandler to set the readOnly status of this
            // Event. Determines whether changes can be made to the database.
            if (arg0 instanceof ProxyMethodInvocation) {
                ProxyMethodInvocation pmi = (ProxyMethodInvocation) arg0;
                pmi.setUserAttribute("readOnly", readOnly);
            }

            final Work work = (Work) arg0.getThis();
            final ServiceFactory sf = (ServiceFactory) arg0.getArguments()[2];

            return txTemplate.execute(new TransactionCallback() {
                public Object doInTransaction(final TransactionStatus status) {
                    return hibTemplate.execute(new HibernateCallback() {
                        public Object doInHibernate(final Session session)
                                throws HibernateException, SQLException {
                            return work.doWork(status, session, sf);
                        }
                    }, true);
                }
            });
        }

    }
}