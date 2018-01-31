package com.lunatech.activemq.plugin;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.command.ActiveMQQueue;
import org.apache.activemq.junit.EmbeddedActiveMQBroker;
import org.junit.*;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import javax.jms.*;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;

@RunWith(JUnit4.class)
public class ExtendedLoggingBrokerPluginTest {

    @Rule
    public EmbeddedActiveMQBroker customizedBroker;
    private Connection conn;
    private Session sess;

    @Before
    public void setUp() throws JMSException {
        customizedBroker = new EmbeddedActiveMQBroker("xbean:my-activemq.xml");
        conn = new ActiveMQConnectionFactory("vm://unit-test?create=false").createConnection();
        sess = conn.createSession(false, Session.AUTO_ACKNOWLEDGE);
        conn.start();
    }

    @Test
    public void testSend() throws Exception {

        // send message
        TextMessage msg = sess.createTextMessage("{\"propertyName\":\"propertyValue\"}");
        MessageProducer producer = sess.createProducer(new ActiveMQQueue("unit.test.q"));
        producer.send(msg);
        producer.close();

        String messageID = msg.getJMSMessageID();

        // receive message(just for sanity)
        MessageConsumer consumer = sess.createConsumer(new ActiveMQQueue("unit.test.q"));
        Message received = consumer.receive(5000);
        consumer.close();
        System.out.println("Received msg: -> " + received);

        // verify if the MDC fields are added to logging
        URI resourceURI = this.getClass().getResource("/activemq.json").toURI();
        Assert.assertTrue(Files.lines(Paths.get(resourceURI)).anyMatch(s -> s.contains("\"message-body\":\"{\\\"propertyName\\\":\\\"propertyValue\\\"}\"")));
        Assert.assertTrue(Files.lines(Paths.get(resourceURI)).anyMatch(s -> s.contains("\"message-id\":\"" + messageID + "\"")));

    }

    @After
    public void cleanUp() throws JMSException {
        sess.close();
        conn.stop();
        conn.close();
    }

}