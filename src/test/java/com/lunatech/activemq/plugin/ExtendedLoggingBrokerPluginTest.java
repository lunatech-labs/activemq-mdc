package com.lunatech.activemq.plugin;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Stream;
import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.command.ActiveMQQueue;
import org.apache.activemq.junit.EmbeddedActiveMQBroker;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

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
    System.out.println("Received msg: " + received);

    // verify if the MDC fields are added to logging
    URI resourceURI = this.getClass().getResource("/activemq.json").toURI();
    Supplier<Stream<String>> streamSupplier = () -> {
      try {
        return Files.lines(Paths.get(resourceURI));
      } catch (IOException e) {
        e.printStackTrace();
        return Stream.<String>builder().build();
      }
    };
    Assert.assertTrue(streamSupplier.get().anyMatch(
        s -> s.contains("\"message-body\":\"{\\\"propertyName\\\":\\\"propertyValue\\\"}\"")));
    Assert.assertTrue(streamSupplier.get().anyMatch(s -> s.contains("\"message-id\":\"" + messageID + "\"")));
    System.out.println("Log message with MDC: " + streamSupplier.get().filter(s -> s.contains("\"message-body\"")).findFirst().get());

  }

  @After
  public void cleanUp() throws JMSException {
    sess.close();
    conn.stop();
    conn.close();
  }

}