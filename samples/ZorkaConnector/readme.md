Zorka
=====

citation from [Zorka page](http://zorka.io/features.html):

    "Zorka is a flexible java monitoring agent with programmable bytecode instrumentation engine. <..>
    Zorka Spy is an advanced bytecode instrumentation engine using Agent Supplied Aspects technique. It is
    configured in a manner similar to Aspect Oriented Programming. Application administrator can define
    arbitrary actions at start and end of method execution, fetch and process arbitrary data
    (method arguments, current time, current thread etc.) and process data in arbitrary manner.
    There is a set of predefined components for presenting and processing obtained data"

Zorka and TNT4J-streams integration
-----------------------------------

TNT4J-streams works as Zorka's traces collector.

To work with TNT4J-streams you need to send the traces. There are some out-of the box configuration samples
that work instrumenting Tomcat Web Server, MySQL, WebServices, LDAP, JMS.

TNT4J-streams collects whole trace attributes and sends to [JKoolCloud](https://www.jkoolcloud.com).


General architecture in short
-----------------------------

```
                          JVM
                           |
    (bytecode class transformation and instrumentation)
                           |
                           v
                         ZORKA
                           |
                        (traces)
                           |
                           v
              TNT4J-Streams Zorka connector
                           |
                   (Activity events)
                           |
                           v
                       JKoolCloud
```

Zorka works as *java agent* in JVM by transforming and instrumenting your application classes the way you have
defined in Zorka scripts and Zorka configuration file `zorka.properties`.

Zorka preparing Traces, and transporting these traces by means of binding socket to TNT4J-Streams Zorka connector.
TNT4J-Streams is parsing these traces the way you have defined your tnt-data-source.xml configuration file.
Once parsed TNT4J streams activity events are generated and send over the network to [JKoolCloud](https://www.jkoolcloud.com) account store.
You define your Token in `tnt4j.properties` file.

Zorka Setup
===========

Starting application with Zorka Agent
-------------------------------------

In order to start Apache Tomcat server with Zorka agent
you need to add following command line arguments:

* `-javaagent:<zorka.path>zorka.jar -Dzorka.home.dir=<zorka.path>`


Zorka configuration
-------------------

Depending on your configuration you need to setup your Zorka agent to instrument (spy) methods
needed.

The basic approach configuring zorka:
	*	Add `*.bsh` script to zorka configuration file `zorka.properties` 
	*	Configure the script itself

For example if your Web Server is Apache's Tomcat and you use MySQL your configuration may look like:

```properties
    scripts = jvm.bsh, tnt4j_sql.bsh, tnt4j_ldap.bsh, tnt4j_jms.bsh, tnt4j_webSocket.bsh, myBusinessMethods.bsh
    sql = yes
    sql.params = yes
    sql.stats = yes
    sql.trace = yes
    tracer = yes
    tracer.min.trace.time = 0
    tracer.min.method.time = 0
    tracer.net = yes
    tracer.net.addr = 127.0.0.1
    tracer.net.port = 8640
    http.trace = yes
    http.trace.exclude = ~.*.png, ~.*.gif, ~.*.js, ~.*.css, ~.*.jpg, ~.*.jpeg, ~.*favicon.ico
```

You need to configure `tracer.min.trace.time` and `tracer.min.method.time` to exclude fast traces.

Zorka sends traces as trace records, each labeled with trace Marker. TNT4J streams looks for trace head and 
collects all attributes in trace. These attributes is sent by Zorka depending on Zorka's tracer configuration.
Trace attributes contains human readable useful process information.

### Zorka SQL configuration

SQL traces enable you to see SQL queries within application. SQL traces gives you SQL query 
application performing, database and other valuable information. Trace is retrieved attributes map.

To add SQL traces to your [JKoolCloud](https://www.jkoolcloud.com) you need to configure zorka.properties to send SQL traces.
First of all you need to add `tnt4j_sql.bsh` script to your's configuration. 

```properties
    scripts = tnt4j_sql.bsh
```

And configure script behavior itself.

```properties
    sql = yes
	sql.params = yes
	sql.stats = yes
	sql.trace = yes
	sql.trace.time = 1
```

As for `sql.trace.time` it's your choice to include traces with minimum time specified.

More information about zorka's configuration can be found on [Zorka guide](http://zorka.io/install/sql.html).

### Zorka LDAP configuration

In order you need LDAP traces in your [JKoolCloud](https://www.jkoolcloud.com), you:
* Add `tnt4j_ldap.bsh` to *scripts* in zorka.properties file
* Configure LDAP trace:

```properties
    ldap.trace = yes
    ldap.trace.time = 0
```
 
### Zorka WebServices configuration

* Add `tnt4j_webService.bsh` to *scripts* in zorka.properties file.
* Configure SOAP trace:

```properties
    soap = yes
    soap.trace = yes
    soap.fetch.xml = yes
    soap.fetch.xml.limit = 65536
    soap.fetch.xml.in = yes
    soap.fetch.xml.out = yes
```

### Zorka JMS configuration

* Add `tnt4j_jms.bsh` to *scripts* in zorka.properties file.
* Configure JMS trace:

```properties
    jms = yes
	jms.trace = yes
    jms.trace.time = 0
    jms.fetch.all = yes				;  <-- This one neabled gives you all of bellow fetched
    jms.fetch.attrs = yes			;  <-- Fetch JMS message atributes as "ID", "TSTAMP", "CORRELATION",
                                    ;	"PERSIST", "REDELIVERY", "JMSTYPE", "PRIORITY", "EXPIRATION"
                                    ; CORRELATION is particularly important and gives extra correlator available on JKoolCloud
    jms.fetch.map = yes				;  <-- Fetches JMS MAP message content to JKoolsCloud message field
    jms.fetch.text = yes			;  <-- Fetches JMS Text message to JKoolsCloud message field
```

As in other scripts property `jms.trace.time` is your choice.

To fetch actual message text use `jms.fetch.text = yes`.

NOTE: in default Zorka configuration message's text hidden.


Correlators
-----------

All TNT4J streams comes with example Zorka script to collect and share [JKoolCloud](https://www.jkoolcloud.com) correlation ID's in Tomcat 
server with MySQL database. You may need expand this script to share correlators with other services. 

Basically you will need get these Id from application server, from request's session data, where they are stored. And
you need these values saved to ThreadLocal. Each service you want to share correlators you need to find class you're
interested to trace and add these correlators. You may look for Zorka example scripts in
`zorka.jar\com\jitlogic\zorka\scripts\`

```java
    spy.add(
        spy.instrument("SQL_TNT4J_STREAMS")
        .onEnter(
            spy.tlGet("JK_CORR_SID", _JK_CORR_SID),
            spy.tlGet("JK_CORR_RID", _JK_CORR_RID),
            tracer.attr("JK_CORR_RID", "JK_CORR_RID"),
            tracer.attr("JK_CORR_SID", "JK_CORR_SID")
         )
         .include(spy.byMethod("com.mysql.jdbc.PreparedStatement", "execut*"))));

```

# TNT4J-streams configuration

You need to setup your steams to use ZorkaConnector:
```xml
    <stream name="FileStream" class="com.jkoolcloud.tnt4j.streams.inputs.ZorkaConnector">
```

ZorkaConnector Stream opens port `8640` by default, to collect Zorka's traces.


## TNT4J-streams parser configuration

### Parsers

Zorka traces are forwarded as `java.util.Map`. It is advisable to use
`com.jkoolcloud.tnt4j.streams.parsers.ActivityMapParser` parser to parse your traces.

TNT4J-Streams parser is used to transform Zorka Traces to TNT4J activity events.

In the sample configuration there are defined several out of the box parser configurations
to use with sample scripts. 

In a single `tnt-data-source.xml` configuration file there are defined several parser rules, tnt4j-stream determines
witch parser to use by **tags** property:

```xml
    <parser name="ZorkaHTTP" class="com.jkoolcloud.tnt4j.streams.parsers.ActivityMapParser" tags="HTTP">
```

The **tags** is compared to Zorka's trace *MARKER* keys Value. Once it comply's, the defined parser is used.

As for general you need no change, but as for going deeper inside Zorka scripts the *MARKER* key is inherited from
line:
```java
    tarcer.begin("YOUR_CUSTOM_MARKER_GOES_HERE")
```

### Attributes to Fields

To bind Zorka's trace attribute to JKool activity event field simply use locator of type `Label`.
Field name represents JKool activity event field name to bind, and locator value - trace attribute from witch
the field is parsed/mapped from.

```xml
    <field name="EventFieldName" locator="MARKER" locator-type="Label"/>
```

For date/time related trace attributes use one of these:

* For Unix time stamp

```xml
    <field name="StartTime" locator="CLOCK" datatype="Timestamp" locator-type="Label"/>
```

* For Formatted date

```xml
    <field name="EndTime" locator="TSTAMP" locator-type="Label" datatype="DateTime" format="yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'" timezone="GMT"/>
```

To map attributes values use: 
	
```xml
    <field name="CompCode" locator="STATUS" locator-type="Label">
    <field-map source="100" target="SUCCESS"/>
    <field-map source="101" target="SUCCESS"/>
    <...>
    <field-map source="500" target="ERROR"/>
    </field>
```
	
In Zorka scripts mapped values are defined as trace attributes `tracer.attr("ERROR", "YES")`

Where first argument of  method *attr* is the name of attribute you're getting in Zorka trace.



#### Zorka Connector

This sample shows how to stream activity events from Zorka produced traces data. Zorka connector connect to Zico
service as listener (client) depending on defined configuration. Default is `localhost:8640`.
Most basic way to use sample is to send Http request to Zorka monitored Tomcat server.

Sample files can be found in `samples/ZorkaConnector` directory.

How to use and configure Zorka, see [`samples/ZorkaConnector/readme.md`](samples/ZorkaConnector/readme.md).

Sample stream configuration: [tnt-data-source.xml](./tnt-data-source.xml)

Stream configuration states that `ZorkaConnector` referencing parsers `ZorkaHTTP`, `ZorkaSQL`, `ZorkaLDAP`,
`ZorkaWebService`, `ZorkaJMS`, `ZorkaWebSocket` and `ZorkaTrace` shall be used.

`ZorkaConnector` connects to Zico service as configured using `Host` and `Port` properties. `HaltIfNoParser` property
indicates that stream should skip unparseable entries. `ZorkaConnector` transforms received Zorka trace entries to `Map`
data structure and puts it to buffer queue to be processed by referenced parsers. Note that parsers uses attribute
`tags` to map concrete parser with received trace over trace attribute `MARKER`.

`ZorkaHTTP` parser is used to fill activity event fields from HTTP trace attributes map data. HTTP trace marker is
`HTTP`, thus parser `tags` value should be same.

`ZorkaSQL` parser is used to fill activity event fields from SQL trace attributes map data. SQL trace marker is
`SQL`.

`ZorkaLDAP` parser is used to fill activity event fields from LDAP trace attributes map data. LDAP trace marker is
`LDAP`.

`ZorkaWebService` parser is used to fill activity event fields from Web Service trace attributes map data. Web Service
 trace marker is `WS_TNT4J_STREAMS_TRACKER`.

`ZorkaJMS` parser is used to fill activity event fields from JMS trace attributes map data. JMS trace marker is
`JMS_TNT4J_STREAMS_TRACKER`.

`ZorkaWebSocket` parser is used to fill activity event fields from WebSocket trace attributes map data. WebSocket trace
marker is `WebSocket`.

`ZorkaTrace` parser is used to fill activity event fields from method call trace attributes map data. Method call trace
marker is `TRACE`.

Activity event mapped fields:

 * `EventType` is mapped from trace attribute named `EvType`. Some parsers sets static value `EVENT`.
 * `StartTime` is mapped from trace attribute named `CLOCK`. Zorka returns this field as UNIX timestamp.
 * `EventName` is mapped from trace attribute named `MARKER`.
 * `ElapsedTime` is mapped from trace attribute named `METHOD_TIME`. Zorka returns this field as timestamp in nanoseconds.
 * `Class` is mapped from trace attribute named `CLASS`. It represents class name of object trace was taken from.
 * `Method` is mapped from trace attribute named `METHOD`. It represents method name trace was taken from.
 * `Correlator` is mapped from trace attributes named `JK_CORR_RID`, `JK_CORR_SID` and `CORRELATION`. `JK_CORR_RID` and
 `JK_CORR_SID` values are retrieved from initial Http request (see ContextTracker from TNT4J API). `CORRELATION`
 value is retrieved from JMS message field `correlationId`.
 * `Message` field may be mapped from different trace attribute values. If mapping is not defined in parser configuration
 then this field is filled with trace data as string.
 * `TrackingId` is mapped from trace attribute named `TrackingId`. It represents unique identifier of activity event.
 * `ParentId` is mapped from trace attribute named `ParentId`. It represents unique identifier of parent trace activity.

Additional fields can be mapped on user demand.

Custom fields values defined in parser fields mapping can be found as activity event properties.

**NOTE:** Stream stops only when critical runtime error/exception occurs or application gets terminated.
