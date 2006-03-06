/* 
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 * 
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 * 
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file. 
 *
 */
package org.mule.providers.jms;

import edu.emory.mathcs.backport.java.util.concurrent.TimeUnit;

import org.mule.MuleException;
import org.mule.config.i18n.Messages;
import org.mule.impl.MuleMessage;
import org.mule.providers.AbstractMessageDispatcher;
import org.mule.transaction.IllegalTransactionStateException;
import org.mule.umo.UMOEvent;
import org.mule.umo.UMOException;
import org.mule.umo.UMOMessage;
import org.mule.umo.endpoint.UMOEndpointURI;
import org.mule.umo.provider.DispatchException;
import org.mule.umo.provider.UMOConnector;
import org.mule.util.PropertiesHelper;
import org.mule.util.concurrent.Latch;

import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.Session;

/**
 * <code>JmsMessageDispatcher</code> is responsible for dispatching messages
 * to Jms destinations. All Jms sematics apply and settings such as replyTo and
 * QoS properties are read from the event properties or defaults are used
 * (according to the Jms specification)
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @author Guillaume Nodet
 * @version $Revision$
 */
public class JmsMessageDispatcher extends AbstractMessageDispatcher {

    private JmsConnector connector;
    private Session delegateSession;
    private Session cachedSession;
    private boolean cacheJmsSession = false;

    public JmsMessageDispatcher(JmsConnector connector) {
        super(connector);
        this.connector = connector;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.providers.UMOConnector#dispatchEvent(org.mule.MuleEvent,
     *      org.mule.providers.MuleEndpoint)
     */
    public void doDispatch(UMOEvent event) throws Exception {
        dispatchMessage(event);
    }

    private UMOMessage dispatchMessage(UMOEvent event) throws Exception {
        if (logger.isDebugEnabled()) {
            logger.debug("dispatching on endpoint: " + event.getEndpoint().getEndpointURI() + ". Event id is: "
                    + event.getId());
        }
        Session txSession = null;
        Session session = null;
        MessageProducer producer = null;
        MessageConsumer consumer = null;

        try {

            // Retrieve the session for the current transaction
            // If there is one, this is up to the transaction to close the
            // session
            txSession = connector.getCurrentSession();

            //Should we be caching sessions
            cacheJmsSession = PropertiesHelper.getBooleanProperty(event.getProperties(),
                    JmsConstants.CACHE_JMS_SESSIONS_PROPERTY, connector.isCacheJmsSessions());
            if(txSession!=null) {
                cacheJmsSession = false;
            }
            if(cacheJmsSession) {
                if(cachedSession==null) {
                    cachedSession = connector.getSession(event.getEndpoint());
                }
                session = cachedSession;
            } else {
                // Retrieve a session from the connector
                session = connector.getSession(event.getEndpoint());
            }

            // If a transaction is running, we can not receive any messages
            // in the same transaction
            boolean remoteSync = useRemoteSync(event);
            if (txSession != null && remoteSync) {
                throw new IllegalTransactionStateException(new org.mule.config.i18n.Message("jms", 2));
            }

            UMOEndpointURI endpointUri = event.getEndpoint().getEndpointURI();
            // determine if endpointUri is a queue or topic
            // the format is topic:destination
            boolean topic = false;
            String resourceInfo = endpointUri.getResourceInfo();
            topic = (resourceInfo != null && JmsConstants.TOPIC_PROPERTY.equalsIgnoreCase(resourceInfo));
            //todo MULE20 remove resource info support
            if(!topic) {
                topic = PropertiesHelper.getBooleanProperty(event.getEndpoint().getProperties(),
                        JmsConstants.TOPIC_PROPERTY, false);
            }

            Destination dest = connector.getJmsSupport().createDestination(session, endpointUri.getAddress(), topic);
            producer = connector.getJmsSupport().createProducer(session, dest);

            Object message = event.getTransformedMessage();
            if (!(message instanceof Message)) {
                throw new DispatchException(new org.mule.config.i18n.Message(Messages.MESSAGE_NOT_X_IT_IS_TYPE_X_CHECK_TRANSFORMER_ON_X,
                        "JMS message",
                        message.getClass().getName(),
                        connector.getName()),
                        event.getMessage(),
                        event.getEndpoint());
            }

            Message msg = (Message) message;
            if (event.getMessage().getCorrelationId() != null) {
                msg.setJMSCorrelationID(event.getMessage().getCorrelationId());
            }

            Destination replyTo = null;
            // Some JMS implementations might not support the ReplyTo property.
            if (connector.getJmsSupport().supportsProperty(JmsConstants.JMS_REPLY_TO)) {

            	Object tempReplyTo = event.removeProperty(JmsConstants.JMS_REPLY_TO);
	            if (tempReplyTo != null) {
	                if (tempReplyTo instanceof Destination) {
	                    replyTo = (Destination) tempReplyTo;
	                } else {
	                    boolean replyToTopic = false;
	                    String reply = tempReplyTo.toString();
	                    int i = reply.indexOf(":");
	                    if (i > -1) {
	                        String qtype = reply.substring(0, i);
	                        replyToTopic = "topic".equalsIgnoreCase(qtype);
	                        reply = reply.substring(i + 1);
	                    }
	                    replyTo = connector.getJmsSupport().createDestination(session, reply, replyToTopic);
	                }
	            }
	            // Are we going to wait for a return event ?
	            if (remoteSync && replyTo == null) {
	                replyTo = connector.getJmsSupport().createTemporaryDestination(session, topic);
	            }
	            // Set the replyTo property
	            if (replyTo != null) {
	                msg.setJMSReplyTo(replyTo);
	            }
	            
	            // Are we going to wait for a return event ?
	            if (remoteSync) {
	                consumer = connector.getJmsSupport().createConsumer(session, replyTo);
	            }
            }

            // QoS support
            String ttlString = (String) event.removeProperty(JmsConstants.TIME_TO_LIVE_PROPERTY);
            String priorityString = (String) event.removeProperty(JmsConstants.PRIORITY_PROPERTY);
            String persistentDeliveryString = (String) event.removeProperty(JmsConstants.PERSISTENT_DELIVERY_PROPERTY);

            long ttl = Message.DEFAULT_TIME_TO_LIVE;
            int priority = Message.DEFAULT_PRIORITY;
            boolean persistent = Message.DEFAULT_DELIVERY_MODE == DeliveryMode.PERSISTENT;

            if (ttlString != null) {
                ttl = Long.parseLong(ttlString);
            }
            if (priorityString != null) {
                priority = Integer.parseInt(priorityString);
            }
            if (persistentDeliveryString != null) {
                persistent = Boolean.valueOf(persistentDeliveryString).booleanValue();
            }

            if (consumer != null && topic) {
                //need to register a listener for a topic
                Latch l = new Latch();
                ReplyToListener listener = new ReplyToListener(l);
                consumer.setMessageListener(listener);

                connector.getJmsSupport().send(producer, msg, persistent, priority, ttl);

                int timeout = event.getTimeout();
                logger.debug("Waiting for return event for: " + timeout + " ms on " + replyTo);
                l.await(timeout, TimeUnit.MILLISECONDS);
                consumer.setMessageListener(null);
                listener.release();
                Message result = listener.getMessage();
                if (result == null) {
                    logger.debug("No message was returned via replyTo destination");
                    return null;
                } else {
                    Object resultObject = JmsMessageUtils.getObjectForMessage(result);
                    return new MuleMessage(resultObject);
                }
            } else {
                connector.getJmsSupport().send(producer, msg, persistent, priority, ttl);
                if (consumer != null) {
                    int timeout = event.getTimeout();
                    logger.debug("Waiting for return event for: " + timeout + " ms on " + replyTo);
                    Message result = consumer.receive(timeout);
                    if (result == null) {
                        logger.debug("No message was returned via replyTo destination");
                        return null;
                    } else {
                        Object resultObject = JmsMessageUtils.getObjectForMessage(result);
                        return new MuleMessage(resultObject);
                    }
                }
            }
            return null;
        } finally {
            JmsUtils.closeQuietly(consumer);
            JmsUtils.closeQuietly(producer);
            if (session != null && !cacheJmsSession && !session.equals(txSession)) {
                JmsUtils.closeQuietly(session);
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.providers.UMOConnector#sendEvent(org.mule.MuleEvent,
     *      org.mule.providers.MuleEndpoint)
     */
    public UMOMessage doSend(UMOEvent event) throws Exception {
        UMOMessage message = dispatchMessage(event);
        return message;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.providers.UMOConnector#sendEvent(org.mule.MuleEvent,
     *      org.mule.providers.MuleEndpoint)
     */
    public UMOMessage receive(UMOEndpointURI endpointUri, long timeout) throws Exception {
        Session session = null;
        Destination dest = null;
        MessageConsumer consumer = null;
        try {
            boolean topic = false;
            String resourceInfo = endpointUri.getResourceInfo();
            topic = (resourceInfo != null && JmsConstants.TOPIC_PROPERTY.equalsIgnoreCase(resourceInfo));

            session = connector.getSession(false, topic);
            dest = connector.getJmsSupport().createDestination(session, endpointUri.getAddress(), topic);
            consumer = connector.getJmsSupport().createConsumer(session, dest);

            try {
                Message message = null;
                if (timeout == RECEIVE_NO_WAIT) {
                    message = consumer.receiveNoWait();
                } else if (timeout == RECEIVE_WAIT_INDEFINITELY) {
                    message = consumer.receive();
                } else {
                    message = consumer.receive(timeout);
                }
                if (message == null) {
                    return null;
                }
                return new MuleMessage(connector.getMessageAdapter(message));
            } catch (Exception e) {
                connector.handleException(e);
                return null;
            }
        } finally {
            JmsUtils.closeQuietly(consumer);
            JmsUtils.closeQuietly(session);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.umo.provider.UMOMessageDispatcher#getDelegateSession()
     */
    public synchronized Object getDelegateSession() throws UMOException {
        try {
            // Return the session bound to the current transaction
            // if possible
            Session session = connector.getCurrentSession();
            if (session != null) {
                return session;
            }
            // Else create a session for this dispatcher and
            // use it each time
            if (delegateSession == null) {
                delegateSession = connector.getSession(false, false);
            }
            return delegateSession;
        } catch (Exception e) {
            throw new MuleException(new org.mule.config.i18n.Message("jms", 3), e);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.umo.provider.UMOMessageDispatcher#getConnector()
     */
    public UMOConnector getConnector() {
        return connector;
    }

    public void doDispose() {
        logger.debug("Disposing");
        JmsUtils.closeQuietly(delegateSession);
        JmsUtils.closeQuietly(cachedSession);
    }

    private class ReplyToListener implements MessageListener {
        private Latch latch;
        private Message message;
        private boolean released = false;

        public ReplyToListener(Latch latch) {
            this.latch = latch;
        }

        public Message getMessage() {
            return message;
        }

        public void release() {
            released = true;
        }

        public void onMessage(Message message) {
            this.message = message;
            latch.unlock();
            while (!released) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {

                }
            }
        }


    }
}
