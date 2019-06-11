# tnt4j-streams-zorka
TNT4J transaction &amp; method tracing streams for Java.

TNT4J-Streams-Zorka is an extension of TNT4J-Streams to provide the streaming of Zorka traces as activity events to [jKoolCloud](https://www.jkoolcloud.com).

TNT4J-Streams-Zorka is under GPLv3 license as Zorka itself.

This document covers only information specific to TNT4J-Streams-Zorka project.
Detailed information on TNT4J-Streams can be found in [README document](https://github.com/Nastel/tnt4j-streams/blob/master/README.md).

Importing TNT4J-Streams-Zorka project into IDE
======================================

## Eclipse
* Select File->Import...->Maven->Existing Maven Projects
* Click 'Next'
* In 'Root directory' field select path of directory where you have downloaded (checked out from git)
TNT4J-Streams project
* Click 'OK'
* Dialog fills in with project modules details
* Click 'Finish'

Running TNT4J-Streams-Zorka
======================================

Also see TNT4J-Streams README document chapter ['Running TNT4J-Streams'](https://github.com/Nastel/tnt4j-streams/blob/master/README.md#running-tnt4j-streams).

## TNT4J-Streams-Zorka can be run
* As standalone application
    * write streams configuration file. See ['Streams configuration'](https://github.com/Nastel/tnt4j-streams/blob/master/README.md#streams-configuration) chapter for more details
    * configure your loggers
    * use `bin/tnt4j-streams.bat` or `bin/tnt4j-streams.sh` to run standalone application
* As API integrated into your product
    * Write streams configuration file. See ['Streams configuration'](https://github.com/Nastel/tnt4j-streams/blob/master/README.md#streams-configuration) chapter for more details
    * use `StreamsAgent.runFromAPI(configFileName)` in your code
* As Java agent attached to running JVM
    * use `bin/zorka-attach.bat` or `bin/zorka-attach.sh` to attach to running JVM and continue as standalone application

## Samples

### Running samples
When release assemblies are built, samples are located in [`samples`](./samples/) directory, e.g.,
`../build/tnt4j-streams-zorka/tnt4j-streams-zorka-1.0.0/samples`.
To run desired sample:
* go to sample directory
* run `run.bat` or `run.sh` depending on your OS

For more detailed explanation of streams and parsers configuration and usage see chapter ['Configuring TNT4J-Streams-Zorka'](#configuring-tnt4j-streams-zorka)
and JavaDocs.

#### Zorka Connector

This sample shows how to stream activity events from Zorka produced traces data. The Zorka connector connects to the Zico [1] service 
as a  listener (client) dependent on the defined configuration. Default is `localhost:8640`. The most basic way to use the sample is to 
send an HTTP request to a Zorka monitored Tomcat server.

**NOTE [1]:** Interesting derivation of the name Zico: Zico is the nickname of the Zorka data collector, which collects data from Zorka 
agents. The collector listener port number 8640 decimal is hex 0x21C0, in which the '2' looks like the letter 'Z' and the '1' looks like the 
letter 'I'. Thus, the hex number looks like the word 'ZICO' and the collector is referred to as 'ZICO', 'Zico' or 'zico'.
See [Zico install manual](http://zorka.io/p/docs/install/monitor/zico/).

Sample files can be found in [`samples/ZorkaConnector`](./samples/ZorkaConnector/) directory.

How to use and configure Zorka, see [`samples/ZorkaConnector/readme.md`](samples/ZorkaConnector/readme.md).

Sample stream configuration:
```xml
<?xml version="1.0" encoding="utf-8"?>
<tnt-data-source
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:noNamespaceSchemaLocation="https://raw.githubusercontent.com/Nastel/tnt4j-streams/master/config/tnt-data-source.xsd">

    <parser name="JMSSendMessageParserStage1" class="com.jkoolcloud.tnt4j.streams.parsers.ActivityNameValueParser">
        <property name="EntryPattern"><![CDATA[(?<key>\w*)=(?<value>\w*)]]></property>

        <field name="jid" locator="jid" locator-type="label"/>
        <field name="type" locator="type" locator-type="label"/>
        <field name="time" locator="time" locator-type="label"/>
        <field name="id" locator="id" locator-type="label"/>
        <field name="cid" locator="cid" locator-type="label"/>
        <field name="tag" locator="tag" locator-type="label"/>
        <field name="from" locator="from" locator-type="label"/>
        <field name="mode" locator="mode" locator-type="label"/>
        <field name="pri" locator="pri" locator-type="label"/>
        <field name="ttl" locator="ttl" locator-type="label"/>
        <field name="keys" locator="keys" locator-type="label"/>
    </parser>

    <parser name="ZorkaKafka" class="com.jkoolcloud.tnt4j.streams.parsers.ActivityMapParser">
        <field name="ApplName" locator="ApplName" locator-type="Label"/>
        <field name="EventType" locator="EVENT_TYPE" locator-type="Label"/>
        <field name="StartTime" locator="CLOCK" datatype="Timestamp" locator-type="Label"/>
        <field name="ResourceName" locator="TOPIC" locator-type="Label"/>
        <field name="EventName" locator="MARKER" locator-type="Label"/>    <!-- method value-->

        <field name="TrackingId" locator="TrackingId" locator-type="Label"/>
        <field name="ParentId" locator="ParentId" locator-type="Label"/>

        <field name="value" locator="VALUE" locator-type="Label" transparent="true">
            <parser-ref name="JMSSendMessageParserStage1"/>
        </field>

        <field name="Correlator" locator="jid" locator-type="activity"/>

        <field name="parser" value="ZorkaKAFKA"/>
        <field name="all" locator="#" locator-type="Label"/>
    </parser>

    <parser name="ZorkaHTTP" class="com.jkoolcloud.tnt4j.streams.parsers.ActivityMapParser">
        <field name="ApplName" locator="ApplName" locator-type="Label"/>
        <field name="EventType" locator="EVENT_TYPE" locator-type="Label"/>
        <field name="StartTime" locator="CLOCK" datatype="Timestamp" locator-type="Label"/>
        <field name="Tag" separator=",">
            <field-locator locator="MARKER" locator-type="Label"/>
            <field-locator locator="HdrIn__cookie" locator-type="Label"/>
            <field-locator locator="HdrIn__user-agent" locator-type="Label"/>
        </field>
        <field name="ElapsedTime" locator="METHOD_TIME" datatype="Number" format="###########0" units="Nanoseconds" locator-type="Label"/>
        <field name="ResourceName" locator="HdrIn__Referer" locator-type="Label" formattingPattern="HTTP={0}"/>
        <field name="EventName" locator="MARKER" locator-type="Label"/>    <!-- method value-->

        <field name="Correlator" separator=",">
            <field-locator locator="JK_CORR_RID" locator-type="Label"/>
            <field-locator locator="JK_CORR_SID" locator-type="Label"/>
            <!--<field-locator locator="JK_ZORKA_PARENT_ID" locator-type="Label"/>-->
        </field>
        <field name="TrackingId" locator="TrackingId" locator-type="Label"/>
        <field name="ParentId" locator="ParentId" locator-type="Label"/>
        <!--<field name="ParentId" locator="JK_ZORKA_PARENT_ID" locator-type="Label"/>-->
        <field name="SESSIONID" locator="JSESSIONID" locator-type="Label"/>
        <field name="Location" separator="">
            <field-locator locator="HdrIn__host" locator-type="Label"/>
        </field>

        <field name="CompCode" locator="STATUS" locator-type="Label">
            <field-map source="100:206" target="SUCCESS" type="Range"/>
            <field-map source="300:308" target="SUCCESS" type="Range"/>
            <field-map source="400:417" target="ERROR" type="Range"/>
            <field-map source="500:511" target="ERROR" type="Range"/>
        </field>
        <field name="Severity" locator="STATUS" locator-type="Label">
            <field-map source="100:206" target="INFO" type="Range"/>
            <field-map source="300:308" target="INFO" type="Range"/>
            <field-map source="400:417" target="ERROR" type="Range"/>
            <field-map source="500:511" target="ERROR" type="Range"/>
        </field>
        <field name="ReasonCode" locator="STATUS" locator-type="Label"/>
        <field name="parser" value="ZorkaHTTP"/>
        <field name="all" locator="#" locator-type="Label"/>
    </parser>

    <parser name="ZorkaSQL" class="com.jkoolcloud.tnt4j.streams.parsers.ActivityMapParser">
        <field name="ApplName" locator="ApplName" locator-type="Label"/>

        <filter name="APPL Filter">
            <expression handle="exclude" lang="groovy"><![CDATA[
                ${ApplName} == "SERVER"
            ]]></expression>
        </filter>

        <field name="EventType" locator="EVENT_TYPE" locator-type="Label"/>
        <field name="Tag" locator="MARKER" locator-type="Label"/>
        <field name="StartTime" locator="CLOCK" datatype="Timestamp" locator-type="Label"/>
        <field name="CompCode" locator="ERROR" locator-type="Label">
            <field-map source="YES" target="ERROR"/>
            <field-map source="" target="SUCCESS"/>
        </field>

        <field name="Severity" locator="ERROR" locator-type="Label">
            <field-map source="YES" target="ERROR"/>
            <field-map source="" target="INFO"/>
        </field>

        <field name="ElapsedTime" locator="METHOD_TIME" datatype="Number" format="###########0" units="Nanoseconds" locator-type="Label"/>
        <field name="EventName" value="SQL execute"/>
        <field name="Exception" locator="EXCEPTION" locator-type="Label"/>

        <field name="Correlator" separator=",">
            <field-locator locator="JK_CORR_RID" locator-type="Label"/>
            <field-locator locator="JK_CORR_SID" locator-type="Label"/>
            <!--<field-locator locator="JK_ZORKA_PARENT_ID" locator-type="Label"/>-->
        </field>
        <field name="TrackingId" locator="TrackingId" locator-type="Label"/>
        <field name="ParentID" locator="ParentId" locator-type="Label"/>
        <!--<field name="ParentId" locator="JK_ZORKA_PARENT_ID" locator-type="Label"/>-->

        <field name="ResourceName" locator="DB" locator-type="Label" formattingPattern="DATASTORE={0}"/>

        <field name="Message" separator=",">
            <field-locator locator="SQL" locator-type="Label"/>
            <field-locator locator="DB" locator-type="Label"/>
        </field>

        <field name="SQL" locator="SQL" locator-type="Label"/>
        <field name="DB" locator="DB" locator-type="Label"/>
        <field name="parser" value="ZorkaSQL"/>
        <field name="all" locator="#" locator-type="Label"/>
    </parser>

    <parser name="ZorkaLDAP" class="com.jkoolcloud.tnt4j.streams.parsers.ActivityMapParser">
        <field name="ApplName" locator="ApplName" locator-type="Label"/>
        <field name="EventType" locator="EVENT_TYPE" locator-type="Label"/>
        <field name="StartTime" locator="CLOCK" datatype="Timestamp" locator-type="Label"/>
        <field name="Tag" locator="MARKER" locator-type="Label"/>
        <field name="ElapsedTime" locator="METHOD_TIME" datatype="Number" format="###########0" units="Nanoseconds" locator-type="Label"/>
        <field name="EventName" locator="MARKER" locator-type="Label"/> <!-- method name value-->

        <field name="Correlator" separator=",">
            <field-locator locator="JK_CORR_RID" locator-type="Label"/>
            <field-locator locator="JK_CORR_SID" locator-type="Label"/>
            <!--<field-locator locator="JK_ZORKA_PARENT_ID" locator-type="Label"/>-->
        </field>
        <field name="TrackingId" locator="TrackingId" locator-type="Label"/>
        <field name="ParentID" locator="ParentId" locator-type="Label"/>
        <!--<field name="ParentId" locator="JK_ZORKA_PARENT_ID" locator-type="Label"/>-->

        <field name="ResourceName" separator=",">
            <field-locator locator="FILTER" locator-type="Label"/>
            <field-locator locator="DC" locator-type="Label"/>
            <field-locator locator="NAME" locator-type="Label"/>
        </field>
        <field name="parser" value="ZorkaLDAP"/>
        <field name="all" locator="#" locator-type="Label"/>
    </parser>

    <parser name="ZorkaWebService" class="com.jkoolcloud.tnt4j.streams.parsers.ActivityMapParser">
        <field name="ApplName" locator="ApplName" locator-type="Label"/>
        <field name="EventType" locator="EVENT_TYPE" locator-type="Label"/>
        <field name="StartTime" locator="CLOCK" datatype="Timestamp" locator-type="Label"/>
        <field name="Tag" locator="MARKER" locator-type="Label"/>
        <field name="ElapsedTime" locator="METHOD_TIME" datatype="Number" format="###########0" units="Nanoseconds" locator-type="Label"/>
        <field name="EventName" locator="SOAP_METHOD" locator-type="Label"/>

        <field name="Correlator" separator=",">
            <field-locator locator="JK_CORR_RID" locator-type="Label"/>
            <field-locator locator="JK_CORR_SID" locator-type="Label"/>
            <!--<field-locator locator="JK_ZORKA_PARENT_ID" locator-type="Label"/>-->
        </field>
        <field name="TrackingId" locator="TrackingId" locator-type="Label"/>
        <field name="ParentID" locator="ParentId" locator-type="Label"/>
        <!--<field name="ParentId" locator="JK_ZORKA_PARENT_ID" locator-type="Label"/>-->

        <field name="ResourceName" locator="MARKER" locator-type="Label"/>  <!-- resource name value-->

        <field name="Message" separator=",">
            <field-locator locator="SOAP_ACTION" locator-type="Label"/>
            <field-locator locator="SOAP_METHOD" locator-type="Label"/>
        </field>
        <field name="parser" value="ZorkaWebServices"/>
        <field name="all" locator="#" locator-type="Label"/>
    </parser>

    <parser name="ZorkaJMS" class="com.jkoolcloud.tnt4j.streams.parsers.ActivityMapParser">
        <field name="ApplName" locator="ApplName" locator-type="Label"/>
        <field name="EventType" locator="EVENT_TYPE" locator-type="Label"/>
        <field name="StartTime" locator="CLOCK" datatype="Timestamp" locator-type="Label"/>
        <field name="Tag" locator="MARKER" locator-type="Label"/>
        <field name="Exception" locator="EXCEPTION" locator-type="Label"/>
        <field name="ElapsedTime" locator="METHOD_TIME" datatype="Number" format="###########0" units="Nanoseconds" locator-type="Label"/>
        <field name="ResourceName" locator="DESTINATION" locator-type="Label" formattingPattern="QUEUE={0}"/> <!-- queue/topic name -->

        <field name="METHOD" locator="METHOD" locator-type="Label"/>
        <field name="EventName" locator="EVENT_NAME" locator-type="Label"> <!-- send/receive -->
            <field-transform name="nameEvaluator" lang="groovy"><![CDATA[
                StringUtils.isEmpty($fieldValue) ? ${METHOD} : $fieldValue
            ]]></field-transform>
        </field>

        <field name="JanusMessageSignature" locator="JanusMessageSignature" locator-type="Label"/>

        <field name="ID" locator="ID" locator-type="Label">
            <field-transform lang="groovy"><![CDATA[
                $fieldValue == null ? null : $fieldValue - "ID:"
            ]]></field-transform>
        </field>

        <field name="Correlator" separator=",">
            <field-locator locator="JK_CORR_RID" locator-type="Label"/>
            <field-locator locator="JK_CORR_SID" locator-type="Label"/>
            <field-locator locator="JanusMessageSignature" locator-type="Activity"/>
            <field-locator locator="ID" locator-type="Activity"/>
            <!--<field-locator locator="JK_ZORKA_PARENT_ID" locator-type="Label"/>-->
        </field>
        <field name="TrackingId" locator="TrackingId" locator-type="Label"/>
        <field name="ParentID" locator="ParentId" locator-type="Label"/>
        <!--<field name="ParentId" locator="JK_ZORKA_PARENT_ID" locator-type="Label"/>-->

        <field name="Message" separator=",">
            <field-locator locator="TEXT" locator-type="Label"/>
            <field-locator locator="PRIORITY" locator-type="Label"/>
            <field-locator locator="EXPIRATION" locator-type="Label"/>
            <field-locator locator="PERSIST" locator-type="Label"/>
            <field-locator locator="REDELIVERY" locator-type="Label"/>
        </field>
        <field name="parser" value="ZorkaJMS"/>
        <field name="all" locator="#" locator-type="Label"/>
    </parser>

    <parser name="ZorkaWebSocket" class="com.jkoolcloud.tnt4j.streams.parsers.ActivityMapParser">
        <!--    <field name="EventType" locator="EVENT_TYPE" locator-type="Label"/> -->
        <field name="EventType" locator="EVENT_TYPE" locator-type="Label"/>
        <field name="StartTime" locator="CLOCK" datatype="Timestamp" locator-type="Label"/>
        <field name="Tag" locator="MARKER" locator-type="Label"/>
        <field name="CompCode" locator="ERROR" locator-type="Label">
            <field-map source="YES" target="ERROR"/>
            <field-map source="" target="SUCCESS"/>
        </field>
        <field name="Severity" locator="ERROR" locator-type="Label">
            <field-map source="YES" target="ERROR"/>
            <field-map source="" target="INFO"/>
        </field>
        <field name="ElapsedTime" locator="METHOD_TIME" datatype="Number" format="###########0" units="Nanoseconds" locator-type="Label"/>
        <field name="ResourceName" value="WebSocket"/> <!-- dynamic value-->
        <field name="EventName" value="WebSocket"/> <!-- method name value -->
        <field name="Exception" locator="EXCEPTION" locator-type="Label"/>
        <field name="Correlator" separator=",">
            <field-locator locator="SESSION" locator-type="Label"/>
            <!--<field-locator locator="MESSAGE_ID" locator-type="Label"/>-->
        </field>
        <field name="SESSIONID" locator="SESSION" locator-type="Label"/>
        <field name="ParentID" locator="ParentId" locator-type="Label"/>
        <field name="Message" locator="MSG" locator-type="Label"/>
        <field name="TrackingId" locator="TrackingId" locator-type="Label"/>
        <field name="parser" value="ZorkaWebsocket"/>
        <field name="all" locator="#" locator-type="Label"/>
    </parser>

    <parser name="ZorkaTrace" class="com.jkoolcloud.tnt4j.streams.parsers.ActivityMapParser">
        <field name="ApplName" locator="ApplName" locator-type="Label"/>
        <field name="EventType" value="CALL"/>
        <field name="StartTime" locator="CLOCK" datatype="Timestamp" locator-type="Label"/>
        <field name="Tag" locator="MARKER" locator-type="Label"/>
        <field name="ElapsedTime" locator="METHOD_TIME" datatype="Number" format="###########0" units="Nanoseconds" locator-type="Label"/>
        <!--<field name="ResourceName" locator="CLASS" locator-type="Label"/>-->
        <field name="CLASS">
            <field-locator locator="CLASS" locator-type="Label"/>
            <field-locator locator="Class" locator-type="Label"/>
        </field>

        <field name="Severity" locator="METHOD_FLAGS" locator-type="Label">
            <field-map source="0:127" target="INFO" type="Range"/>
            <field-map source="128:" target="WARNING" type="Range"/>
        </field>
        <field name="Exception" locator="EXCEPTION" locator-type="Label"/>
        <!-- <field name="Method_Flags" locator="METHOD_FLAGS" locator-type="Label"/> -->
        <field name="EventName" locator="METHOD" locator-type="Label"/>
        <field name="MethodSignature" locator="SIGNATURE" locator-type="Label"/>
        <field name="TrackingId" locator="TrackingId" locator-type="Label"/>
        <field name="ParentID" locator="ParentId" locator-type="Label"/>
        <!--<field name="Correlator" locator="JK_ZORKA_PARENT_ID" locator-type="Label"/>-->
        <field name="Level" locator="Level" locator-type="Label" datatype="Number"/>
        <!--ommit all the traces -->
        <!--<filter name="Level Filter">-->
        <!--<expression handle="exclude" lang="groovy"><![CDATA[-->
        <!--${Level} > 1-->
        <!--]]></expression>-->
        <!--</filter>-->
        <field name="parser" value="ZorkaTrace"/>
        <!--field name="ParentId" locator="JK_ZORKA_PARENT_ID" locator-type="Label"/-->
        <field name="all" locator="#" locator-type="Label"/>
    </parser>

    <stream name="ZorkaStream" class="com.jkoolcloud.tnt4j.streams.inputs.ZorkaConnector">
        <property name="HaltIfNoParser" value="false"/>

        <!-- For trace records depth filtering, define the maximum number of stack trace entry events. A value of `0` streams the whole trace. -->
        <property name="MaxTraceEvents" value="100"/>

        <!-- For trace records filtering by methods execution time, use of Bollinger Bands is required -->
        <!--<property name="Bollinger_K_times" value="3"/> -->
        <!--<property name="Bollinger_N_period" value="20"/> -->
        <!--<property name="BollingerRecalculationPeriod" value="3000"/> -->

        <property name="BuildSourceFQNFromStreamedData" value="true"/>
        <property name="SourceFQN" value="APPL=${ApplName}#SERVER=${ServerName}#NETADDR=${ServerIp}"/>

        <parser-ref name="ZorkaHTTP" tags="HTTP_CLI_SEND,HTTP_CLI_RECEIVE,HTTP"/>
        <parser-ref name="ZorkaSQL" tags="SQL"/>
        <parser-ref name="ZorkaLDAP" tags="LDAP"/>
        <parser-ref name="ZorkaWebService" tags="WS_TNT4J_STREAMS_TRACKER"/>
        <parser-ref name="ZorkaJMS" tags="JMS_SEND,JMS_SEND2,JMS_TNT4J_STREAMS_TRACKER,JMS_RECEIVE"/>
        <parser-ref name="ZorkaWebSocket" tags="WebSocket"/>
        <parser-ref name="ZorkaTrace" tags="TRACE"/>
        <parser-ref name="ZorkaKafka" tags="KAFKA_RECEIVE,KAFKA_SEND"/>
    </stream>
</tnt-data-source>
```

Stream configuration states that `ZorkaConnector` referencing parsers `ZorkaHTTP`, `ZorkaSQL`, `ZorkaLDAP`, `ZorkaWebService`, `ZorkaJMS`, 
`ZorkaWebSocket` and `ZorkaTrace` shall be used.

`ZorkaConnector` connects to Zico service as configured using `Host` and `Port` properties. `HaltIfNoParser` property indicates that the 
stream should skip unparseable entries. `ZorkaConnector` transforms received Zorka trace entries to `Map` data structure and puts it to a 
buffer queue to be processed by the referenced parsers. Note that the stream-parser mapping uses attribute `tags` to map dedicated trace 
parser over trace attribute `MARKER`.

`ZorkaHTTP` parser is used to fill activity event fields from HTTP trace attributes map data. HTTP trace marker is `HTTP`, thus the parser 
`tags` value should be the same.

`ZorkaSQL` parser is used to fill activity event fields from SQL trace attributes map data. The 'tags' value is set to match the SQL trace 
marker `SQL`.

`ZorkaLDAP` parser is used to fill activity event fields from LDAP trace attributes map data. The 'tags' value is set to match the LDAP 
trace marker `LDAP`.

`ZorkaWebService` parser is used to fill activity event fields from Web Service trace attributes map data. The 'tags' value is set to match 
the Web Service trace marker `WS_TNT4J_STREAMS_TRACKER`.

`ZorkaJMS` parser is used to fill activity event fields from JMS trace attributes map data. The 'tags' value is set to match the JMS trace 
marker `JMS_TNT4J_STREAMS_TRACKER`.

`ZorkaWebSocket` parser is used to fill activity event fields from WebSocket trace attributes map data. The 'tags' value is set to match the 
WebSocket trace marker `WebSocket`.

`ZorkaTrace` parser is used to fill activity event fields from Java method call trace attributes map data. The 'tags' value is set to match 
the Java method call trace marker 'TRACE`.

Activity event mapped fields:

 * `EventType` is mapped from trace attribute named `EvType`. Some parsers sets static value `EVENT`.
 * `StartTime` is mapped from trace attribute named `CLOCK`. Zorka returns this field as UNIX timestamp.
 * `EventName` is mapped from trace attribute named `MARKER`.
 * `ElapsedTime` is mapped from trace attribute named `METHOD_TIME`. Zorka returns this field as timestamp in nanoseconds.
 * `Class` is mapped from trace attribute named `CLASS`. It represents class name of object the trace was taken from.
 * `Method` is mapped from trace attribute named `METHOD`. It represents Java method name the trace was taken from.
 * `Correlator` is mapped from trace attributes named `JK_CORR_RID`, `JK_CORR_SID` and `CORRELATION`. `JK_CORR_RID` and `JK_CORR_SID` values 
 are retrieved from initial HTTP request (see ContextTracker from TNT4J API). `CORRELATION` value is retrieved from JMS message field 
 `correlationId`.
 * `Message` field may be mapped from different trace attribute values. If mapping is not defined in parser configuration, then this field 
 is filled with trace data as string.
 * `TrackingId` is mapped from trace attribute named `TrackingId`. It represents a unique identifier of the activity event.
 * `ParentId` is mapped from trace attribute named `ParentId`. It represents a unique identifier of the parent trace activity.

Additional fields can be mapped on user demand.

Custom fields values defined in parser fields mapping can be found as activity event properties.

**NOTE:** The stream stops only when a critical runtime error/exception occurs or an application gets terminated.

##### Zorka Connector as Java agent

To run the Zorka Connector sample using Zorka as an attached Java Agent, use `run-attach.bat` or `run-attach.sh` depending on your OS.

To change the Zorka home directory, open the `.bat` or `.sh` file and change the first parameter of `zorka-attach` executable to the actual 
path on your system.

`zorka-attach` parameters: `zorka-attach zorkaAgentPath VMDescriptor`
 * zorkaAgentPath - Zorka agent jar (`zorka*.jar`) path. (Required)
 * VMDescriptor - Java VM name fragment/pid to attach to. (Required)

Configuring TNT4J-Streams-Zorka
======================================

Details on TNT4J-Streams related configuration can be found in TNT4J-Streams README document chapter ['Configuring TNT4J-Streams'](https://github.com/Nastel/tnt4j-streams/blob/master/README.md#configuring-tnt4j-streams).

### Streams configuration

#### Zorka connector parameters

 * Host - host name of machine running the Zico collector. Default value - `localhost`. (Optional)
 * Port - listener port number of the Zico collector. Default value - `8640`. (Optional)
 * MaxTraceEvents - maximum number of events to stream for single stack trace. Default value - `100`. Value `0` (or negative) means stream 
 the whole stack trace. (Optional)
 * Bollinger_N_period - Bollinger Bands N-period moving average (EMA). It defines the number of 'method execution time' values to use for 
 averages calculation. Setting a `0` or negative value means dynamic methods execution time filtering using Bollinger Bands is disabled. 
 Default value - `0`. (Optional)
     * Bollinger_K_times - Defines an upper Bollinger Band at K times an N-period standard deviation above the exponentially moving average 
     (n-Period) and a lower band at K times an N-period standard deviation below the exponentially moving average. It means how many times 
     the average value has to change to change the bands width. Default value - `3`. (Optional; used only if `Bollinger_N_period` is set)
     * BollingerRecalculationPeriod - Bollinger Bands recalculation period in milliseconds. Default value - `3000`. (Optional; used only if 
     `Bollinger_N_period` is set)

    sample:
```xml
    <property name="Host" value="some.host.name"/>
    <property name="Port" value="8645"/>
    <property name="MaxTraceEvents" value="32"/>

    <property name="Bollinger_N_period" value="25"/>
    <property name="Bollinger_K_times" value="3"/>
    <property name="BollingerRecalculationPeriod" value="2000"/>
```

Also see ['Generic streams parameters'](https://github.com/Nastel/tnt4j-streams/blob/master/README.md#generic-streams-parameters) and ['Buffered streams parameters'](https://github.com/Nastel/tnt4j-streams/blob/master/README.md#buffered-streams-parameters).

#### JMX Zabix data puller parameters

 * JMXQuery - Zabbix JMX query expression to get desired JMX beans attributes. (Required)
 * Host - host name of the machine running the Zabix server. Default value - `localhost`. (Optional)
 * Port - listener port number of the Zabix server for receiving data from Zabix agents and proxies. Default value - `10056`. (Optional)
 * CronSchedExpr - Cron expression to define Zabbix queries invocation scheduler. Default value - `every 15sec`. (Optional)

    sample:
```xml
    <property name="Host" value="some.host.name"/>
    <property name="Port" value="9953"/>
    <property name="CronSchedExpr" value="0/15 * * 1/1 * ? *"/>
    <property name="JMXQuery">
        <![CDATA[
            "java","java.lang:type=Memory","HeapMemoryUsage","used"|
            "java","java.lang:type=Threading","ThreadCount"|
            "java","java.lang:type=GarbageCollector,name=PS MarkSweep","CollectionTime"|
            "java","java.lang:type=ClassLoading","LoadedClassCount"|
            "java","java.lang:type=Runtime","ClassPath"
        ]]>
    </property>
```

Also see ['Generic streams parameters'](https://github.com/Nastel/tnt4j-streams/blob/master/README.md#generic-streams-parameters) and ['Buffered streams parameters'](https://github.com/Nastel/tnt4j-streams/blob/master/README.md#buffered-streams-parameters).

How to Build TNT4J-Streams-Zorka
=========================================

## Requirements
* JDK 1.8+
* [Apache Maven 3](https://maven.apache.org/)
* [TNT4J-Streams](https://github.com/Nastel/tnt4j-streams) `core` module in particular

All other required dependencies are defined in project [`pom.xml`](./pom.xml) file. If maven is running online mode it should download these 
defined dependencies automatically.

### Manually installed dependencies

**NOTE:** If you have build and installed TNT4J-Streams into your local maven repository, you don't need to install
it manually.

Some of the required and optional dependencies may be not available in public [Maven Repository](http://repo.maven.apache.org/maven2/). In 
this case we would recommend to download those dependencies manually into [`lib`](./lib/) directory and install into local maven repository 
by running maven script [`lib/pom.xml`](./lib/pom.xml) using `initialize` goal.

**NOTE:** `TNT4J-Streams-Zorka` project will be ready to build only when manually downloaded libraries will be installed to local maven 
repository.

What to download manually:
* Zico-util
* Zorka

Download the above libraries and place into the `tnt4j-streams-zorka/lib` directory as follows:
```
    lib
     + zorka
         + 1.0.16
             |- zico-util.jar
         + 1.0.18
             |- zorka.jar 
```
(O) marked libraries are optional

**NOTE:** Also see TNT4J-Streams README document chapter ['Manually installed dependencies'](https://github.com/Nastel/tnt4j-streams/blob/master/README.md#manually-installed-dependencies).

## Building
   * To build the project and make release assemblies, run maven goals `clean package`
   * To build the project, make release assemblies and install to local repo, run maven goals `clean install`

Release assemblies are built in directory `../build/tnt4j-streams-zorka`.

**NOTE:** Sometimes maven fails to correctly handle dependencies. If dependency configuration looks fine, but maven still complains about 
missing dependencies, try to delete the local maven repository by hand: e.g., on MS Windows delete contents of directory 
`c:\Users\[username]\.m2\repository`.

Summarizing the build process, the quick "how to build" steps would be:
1. Download `zico-util.jar` and `zorka.jar` to directory `tnt4j-streams-zorka/lib/zorka/{version}/`.
2. Install manually managed dependencies from directory `tnt4j-streams-zorka/lib` running `mvn initialize`.
3. If `tnt4j-streams` was not built yet, build it: run `mvn clean install` from the project object model file [`pom.xml`](https://github.com/Nastel/tnt4j-streams/blob/master/pom.xml) 
located in `tnt4j-streams` directory.
4. Now you can build `tnt4j-streams-zorka`: run `mvn clean install` from file [`pom.xml`](./pom.xml) located in directory 
`tnt4j-streams-zorka`.

## Running samples

See 'Running TNT4J-Streams-Zorka' chapter section ['Samples'](#samples).

Testing of TNT4J-Streams-Zorka
=========================================

## Requirements
* [JUnit 4](http://junit.org/)
* [Mockito](http://mockito.org/)

## Testing using maven
Maven tests run is disabled by default. To enable Maven to run tests, set Maven command line argument 
`-DskipTests=false`.

## Running manually from IDE
* in `zorka` module run JUnit test suite named `AllZorkaTests`
