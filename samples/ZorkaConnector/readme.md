Zorka
=====

"Zorka is a flexible java monitoring agent with programmable bytecode instrumentation engine. <..> 
Zorka Spy is an advanced bytecode instrumentation engine using Agent Supplied Aspects technique. It is 
configured in a manner similiar to Aspect Oriented Programming. Application administrator can define 
arbitrary actions at start and end of method execution, fetch and process arbitrary data 
(method arguments, current time, current thread etc.) and process data in arbitrary manner. 
There is a set of predefined components for presenting and processing obtained data"
	citation from http://zorka.io/features.html

Zorka and TNT4J-streams integration
-----------------------------------

TNT4J-streams works as Zorka's traces collector.

To work with TNT4J-streams you need to send the traces. There are some out-of the box configuration samples
that work intrumenting Tomcat Web Server, MySQL, WebServices, LDAP, JMS
TNT4J-streams collects whole trace attributes and sends to JKool Cloud.


General architecture in short
-----------------------------

JMV ----(bytecode class transformation and instrumentation)----> ZORKA ----(traces)----> TNT4J-Streams Zorka connector -----(Events)----> JKoolCloud

Zorka works as *javaagent* in JMV by transforming and instrumenting your application classes the way you have defined in 
Zorka scripts and Zorka configuration file `zorka.properties`. 

Zorka preparing Traces, and transporting these traces by means of binding socket to TNT4J-Streams Zorka connector. TNT4J-Streams is parsing these traces
the way you have defined your tnt-data-source.xml configuration file. Once parsed TNT4J streams Events are generated and send over the network to JKoolCloud 
account's store. You define your Token in `tnt4j.properties` file.



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

For example if your Web Server is Apache's Tomcat and you use MySQL your configuration may look like:

```properties

    scripts = jvm.bsh, tnt4j_sql.bsh, tnt4j_ldap.bsh\
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
collects all attributes in trace. These attributes is sent by Zorka depending on zorca's tracer configuration. 
Trace attributes contains human readable useful process information.

### Correlators

TNT4J streams comes with example Zorka script to collect and share JKoolCloud correlation ID's in Tomcat server with 
MySQL database. You may need expand this script to share correlators with other services. 

Basically you will need get these Id from application server, from request's session data, where they are stored. And
you need these values saved to ThreadLocal. Each service you want to share correlators you need to find class you're
interested spy'íng and add these correlators. You may look for Zorca example scripts in
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

### Zorka SQL configuration 

SQL traces enable you to see SQL queries within application. SQL traces gives you SQL query 
application performing, database and other valuable information. Traces are attri 
To enable SQL traces with'in application
include tnt4j_jms.bsh in your `zorka.properties`:
```
    scripts = jvm.bsh, tnt4j_sql.bsh
```


### Zorka JMS configuration

To enable JMS traces and include correlators in your JMS message trace please use `tnt4j_jms.bsh`
script:
```
    scripts = jvm.bsh, tnt4j_jms.bsh
```

in `zorka.proeperties` file please specify:
```properties
    jms = yes
    jms.trace = yes
    jms.trace.time = 0
    jms.fetch.text = yes
```



As in other scripts property `jms.trace.time` is your choise.

To fetch actual message text use `jms.fetch.text = yes`.

NOTE: in default zorka configuration message's text hidden.


# TNT4J-streams configuration

You need to setup your steams to use ZorkaConnector:
```xml
    <stream name="FileStream" class="com.jkool.tnt4j.streams.inputs.ZorkaConnector">
```

ZorcaConnector Stream opens port `8640` by default, to collect Zorka's traces.


In you are interested in single Zorka's trace you can put ZorcaMarker value to exclude other markers:
```xml
    <property name="ZorkaMarker" value="HTTP"/>
```

## TNT4J-streams parser configuration

### Parsers

Zorka traces are forwarded as `java.util.Map`. It is advisable to use `com.jkool.tnt4j.streams.parsers.ActivityMapParser`
parser to parse your traces. 

TNT4J-Streams parser is used to transform Zorka Traces to JKoolCloud events. 

In the sample configuration there are defined several out of the box parser configurations
to use with sample scripts. 

In a single `tnt-data-source.xml` configuration file there are defined several parser rules, tnt4j-stream determines witch parser to
use by **tags** property: 
	`<parser name="ZorkaHTTP" class="com.jkool.tnt4j.streams.parsers.ActivityMapParser" tags="HTTP">`

The **tags** is compared to Zorka's trace *MARKER* keys Value. Once it comply's, the defined parser is used.

As for general you need no change, but as for going deeper inside Zorka scripts the *MARKER* key is inherited from
line: 
	`tarcer.begin("YOUR_CUSTOM_MARKER_GOES_HERE")`
	
	
### Attributes to Fields

To bind Zorka's trace attribute to JKool's cloud field simple use locator of type
label. Where field name represents JKolls field, and locator it's an attribute from witch 
the field is parsed/mapped from.   
  
	`<field name="EventName" locator="MARKER" locator-type="Label"/>`

For date's depending on date getting with attribute you use 

	* For Unix time stamp `<field name="StartTime" locator="CLOCK" datatype="Timestamp" locator-type="Label"/>`
	* For Formatted date `<field name="EndTime" locator="TSTAMP" locator-type="Label" datatype="DateTime" format="yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'" timezone="GMT"/>

To map attributes values use: 
	
	```     <field name="CompCode" locator="STATUS" locator-type="Label">
            <field-map source="100" target="SUCCESS"/>
            <field-map source="101" target="SUCCESS"/>
            <...>
            <field-map source="500" target="ERROR"/>
            </field> ```
	
In Zorka scripts mapped values are defined as trace attributes
	`tracer.attr("ERROR", "YES")`

Where first argument of  method *attr* is the name of attribute you're getting in Zorka trace.


	 

