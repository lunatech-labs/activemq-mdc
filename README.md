## A plugin to add our own MDC fields to logging
* This is an external plugin which extends ActiveMQ's default LoggingBrokerPlugin and will be added to ActiveMQ's broker filter chain.
* Using this plugin, it is now possible to add our own MDC values into the ActiveMQ logging which is not supported natively.

## Usage
#### Build
* Running `mvn clean package` will produce two artifacts `activemq-mdc.jar` and `activemq-mdc.zip`

#### Add runtime dependency to `lib` directory
* Add these `activemq-mdc.jar`,`jsonevent-layout-1.7.jar`, `json-smart-2.3.jar`, `accessors-smart-1.2.jar` to `${ACTIVEME_HOME}/lib` directory
* For convenience, the required jars are assembled in this `activemq-mdc.zip` archive which can be extracted inside `/lib`.
* The three other jars except `activemq-mdc.jar` are required to produce logging in JSON layout. Further, the JSON file can be fed directly into JSON log parsers like `logstash` and viewed using ElasticSearch. Those jars can be skipped if JSON layout is not required.

#### Modify ActiveMQ broker configuration
* Add below bean configuration to `activemq.xml` inside `<plugins>` section.
* The properties can be modified according to our needs.
* Sample configuration is [here](src/test/resources/my-activemq.xml)

```xml
        <bean xmlns="http://www.springframework.org/schema/beans"
                   id="extendedLoggingPlugin"
                   class="com.lunatech.activemq.plugin.ExtendedLoggingBrokerPlugin">
               <property name="logAll" value="false"/>
               <property name="logConnectionEvents" value="false"/>
               <property name="logTransactionEvents" value="true"/>
               <property name="logConsumerEvents" value="true"/>
               <property name="logProducerEvents" value="true"/>
         </bean>
```

#### Configure file appender
* Use this File appender configuration in `log4j.properties` under `${ActiveMQ_HOME}/conf` directory.
* Do not forget to add this appender to `logger.rootLogger`.
* Sample configuration is [here](src/test/resources/log4j.properties)

```xml
log4j.appender.jsonfile=org.apache.log4j.RollingFileAppender
log4j.appender.jsonfile.file=/var/log/activemq/activemq.json
log4j.appender.jsonfile.encoding=UTF-8
log4j.appender.jsonfile.maxFileSize=10MB
log4j.appender.jsonfile.maxBackupIndex=5
log4j.appender.jsonfile.append=true
log4j.appender.jsonfile.DatePattern=.yyyy-MM-dd
log4j.appender.jsonfile.layout=net.logstash.log4j.JSONEventLayoutV1
```

#### What can you expect?
You should be able to find the following additional MDC fields in your log message.

```json
  "mdc": {
    "sent-to-DLQ": "false",
    "redelivery-count": "0",
    "producer-id": "ID:Sasikumars-MacBook-Pro.local-59587-1538170787867-4:1:1:1",
    "message-body": "{\"propertyName\":\"propertyValue\"}",
    "client-id": "ID:Sasikumars-MacBook-Pro.local-59587-1538170787867-3:1",
    "message-id": "ID:Sasikumars-MacBook-Pro.local-59587-1538170787867-4:1:1:1:1",
    "activemq.connector": "vm:\/\/unit-test",
    "destination": "unit.test.q",
    "is-redelivered": "false"
  }
```