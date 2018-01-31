package com.lunatech.activemq.plugin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;
import org.apache.activemq.broker.ProducerBrokerExchange;
import org.apache.activemq.broker.util.LoggingBrokerPlugin;
import org.apache.activemq.command.ActiveMQTextMessage;
import org.apache.activemq.command.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

/**
 * This plugin extends the default {@link LoggingBrokerPlugin} and adds MDC fields before a message is sent to a logging event.
 */
public class ExtendedLoggingBrokerPlugin extends LoggingBrokerPlugin {

    private static final Logger LOG = LoggerFactory.getLogger(ExtendedLoggingBrokerPlugin.class);

    @Override
    public void send(ProducerBrokerExchange producerExchange, Message messageSend) throws Exception {
        MDC.put("client-id", producerExchange.getConnectionContext().getClientId());
        if (Optional.fromNullable(producerExchange.getConnectionContext().getUserName()).isPresent())
            MDC.put("user-name", producerExchange.getConnectionContext().getUserName());
        MDC.put("message-id", messageSend.getMessageId().toString());
        MDC.put("producer-id", messageSend.getProducerId().toString());
        if (Optional.fromNullable(messageSend.getTargetConsumerId()).isPresent())
            MDC.put("target-consumer-id", messageSend.getTargetConsumerId().toString());
        if (Optional.fromNullable(messageSend.getGroupID()).isPresent())
            MDC.put("group-id", messageSend.getGroupID());
        if (Optional.fromNullable(messageSend.getCorrelationId()).isPresent()) {
            MDC.put("correlation-id", messageSend.getCorrelationId());
        }
        if (Optional.fromNullable(messageSend.getOriginalDestination()).isPresent()) {
            MDC.put("original-destination", messageSend.getOriginalDestination().getPhysicalName());
        }
        MDC.put("destination", messageSend.getDestination().getPhysicalName());
        MDC.put("sent-to-DLQ", String.valueOf(messageSend.getDestination().isDLQ()));
        MDC.put("redelivery-count", String.valueOf(messageSend.getRedeliveryCounter()));

        try {
            ActiveMQTextMessage textMsg = (ActiveMQTextMessage) messageSend;
            if (Optional.fromNullable(textMsg).isPresent()) {
                MDC.put("message-body", String.valueOf(new ObjectMapper().readTree(textMsg.getText())));
            }
            MDC.put("is-redelivered", String.valueOf(textMsg.getJMSRedelivered()));
        } catch (ClassCastException e) {
            // class cast exception for worker queues and json parse exception are expected here if the message is of type ActiveMQObjectMessage.
            // like 'java.lang.ClassCastException: org.apache.activemq.command.ActiveMQObjectMessage cannot be cast to org.apache.activemq.command.ActiveMQTextMessage'
            LOG.warn("Something went wrong while casting message into ActiveMQTextMessage: " + e);
        } catch (Exception e) {
            // Swallow any other exception and deal with it afterwards from logging
            // this should not block the Activemq's default process
            LOG.error("Something went wrong", e);
        } finally {
            // delegate the LoggingEvent
            super.send(producerExchange, messageSend);
        }

        MDC.remove("client-id");
        MDC.remove("user-name");
        MDC.remove("producer-id");
        MDC.remove("message-id");
        MDC.remove("correlation-id");
        MDC.remove("original-destination");
        MDC.remove("destination");
        MDC.remove("sent-to-DLQ");
        MDC.remove("redelivery-count");
        MDC.remove("message-body");
        MDC.remove("is-redelivered");

    }

}
