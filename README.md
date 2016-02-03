# tnt4j-streams-zorka
TNT4J transaction &amp; method tracing streams for Java.

TNT4J-Streams-Zorka is extension of TNT4J-Streams to give ability of streaming Zorka traces as activity events to
JKoolCloud.

TNT4J-Streams-Zorka is under LGPLv3 license as Zorka itself.

This document covers just information specific to TNT4J-Streams-Zorka project.
Detailed information on TNT4J-Streams can be found at
(https://github.com/Nastel/tnt4j-streams/blob/master/README.md).

Importing TNT4J-Streams-Zorka project into IDE
======================================

## Eclipse
* Select File->Import...->Maven->Existing Maven Projects
* Click 'Next'
* In 'Root directory' field select path of directory where You have downloaded (checked out from git)
TNT4J-Streams project
* Click 'OK'
* Dialog fills in with project modules details
* Click 'Finish'

Running TNT4J-Streams-Zorka
======================================

Also see (https://github.com/Nastel/tnt4j-streams/blob/master/README.md) chapter 'Running TNT4J-Streams'.

## TNT4J-Streams-Zorka can be run:
* As standalone application
    * write streams configuration file. See 'Streams configuration' chapter for more details
    * configure Your loggers
    * use `bin\tnt4j-streams.bat` or `bin\tnt4j-streams.sh` to run standalone application
* As API integrated into Your product
    * Write streams configuration file. See 'Streams configuration' chapter for more details
    * use `StreamsAgent.runFromAPI(configFileName)` in your code

## Samples:

### Running samples
When release assembly is built samples are located in `samples` directory i.e. `../build/tnt4j-streams-zorka/tnt4j-streams-zorka-1.0.0/samples`.
To run desired sample:
* go to sample directory
* run `run.bat` or `run.sh` depending on Your OS

For more detailed explanation of streams and parsers configuration and usage see chapter 'Configuring TNT4J-Streams-Zorka'
and JavaDocs.

#### Zorka Connector

This sample shows how to stream activity events from Zorka produced traces data. Zorka connector connect to Zico
service as listener (client) depending on defined configuration. Default is `localhost:8640`.

Sample files can be found in `samples\ZorkaConnector` directory.

How to use and configure Zorka, see `samples\ZorkaConnector\readme.md`

Sample stream configuration:
```xml
<?xml version="1.0" encoding="utf-8"?>
<tnt-data-source
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:noNamespaceSchemaLocation="../../config/tnt-data-source.xsd">

    <parser name="ZorkaHTTP" class="com.jkool.tnt4j.streams.parsers.ActivityMapParser" tags="HTTP">
        <field name="StartTime" locator="CLOCK" datatype="Timestamp" locator-type="Label"/>
        <field name="EventName" locator="MARKER" locator-type="Label"/>
        <field name="ElapsedTime" locator="METHOD_TIME" datatype="Number" format="###########0" units="Nanoseconds"
               locator-type="Label"/>
        <field name="Class" locator="CLASS" locator-type="Label"/>
        <field name="Method" locator="METHOD" locator-type="Label"/>

        <field name="Correlator" locator="JK_CORR_RID" locator-type="Label"/>
        <field name="Correlator" locator="JK_CORR_SID" locator-type="Label"/>

        <field name="Location" locator="HdrIn__host" locator-type="Label"/>

        <field name="EventType" value="SEND"/>
        <field name="ResourceName" locator="URI" locator-type="Label"/>
        <field name="CompCode" locator="STATUS" locator-type="Label">
            <field-map source="100" target="SUCCESS"/>
            <field-map source="101" target="SUCCESS"/>
            <field-map source="103" target="SUCCESS"/>
            <field-map source="200" target="SUCCESS"/>
            <field-map source="201" target="SUCCESS"/>
            <field-map source="202" target="SUCCESS"/>
            <field-map source="203" target="SUCCESS"/>
            <field-map source="204" target="SUCCESS"/>
            <field-map source="205" target="SUCCESS"/>
            <field-map source="206" target="SUCCESS"/>
            <field-map source="300" target="WARNING"/>
            <field-map source="301" target="WARNING"/>
            <field-map source="302" target="WARNING"/>
            <field-map source="303" target="WARNING"/>
            <field-map source="304" target="WARNING"/>
            <field-map source="306" target="WARNING"/>
            <field-map source="307" target="WARNING"/>
            <field-map source="308" target="WARNING"/>
            <field-map source="400" target="ERROR"/>
            <field-map source="401" target="ERROR"/>
            <field-map source="402" target="ERROR"/>
            <field-map source="403" target="ERROR"/>
            <field-map source="404" target="ERROR"/>
            <field-map source="405" target="ERROR"/>
            <field-map source="406" target="ERROR"/>
            <field-map source="407" target="ERROR"/>
            <field-map source="408" target="ERROR"/>
            <field-map source="409" target="ERROR"/>
            <field-map source="410" target="ERROR"/>
            <field-map source="411" target="ERROR"/>
            <field-map source="412" target="ERROR"/>
            <field-map source="413" target="ERROR"/>
            <field-map source="414" target="ERROR"/>
            <field-map source="415" target="ERROR"/>
            <field-map source="416" target="ERROR"/>
            <field-map source="417" target="ERROR"/>
            <field-map source="500" target="ERROR"/>
            <field-map source="501" target="ERROR"/>
            <field-map source="502" target="ERROR"/>
            <field-map source="503" target="ERROR"/>
            <field-map source="504" target="ERROR"/>
            <field-map source="505" target="ERROR"/>
            <field-map source="511" target="ERROR"/>
        </field>

        <field name="Tag" separator=",">
            <field-locator locator="HdrIn__cookie" locator-type="Label"/>
            <field-locator locator="HdrIn__user-agent" locator-type="Label"/>
        </field>
    </parser>


    <parser name="ZorkaSQL" class="com.jkool.tnt4j.streams.parsers.ActivityMapParser" tags="SQL">
        <field name="EventType" value="CALL"/>
        <field name="EventName" locator="MARKER" locator-type="Label"/>
        <field name="StartTime" locator="CLOCK" datatype="Timestamp" locator-type="Label"/>
        <field name="CompCode" locator="ERROR" locator-type="Label">
            <field-map source="YES" target="ERROR"/>
            <field-map source="" target="SUCCESS"/>
        </field>
        <field name="ElapsedTime" locator="METHOD_TIME" datatype="Number" format="###########0" units="Nanoseconds"
               locator-type="Label"/>
        <field name="Class" locator="CLASS" locator-type="Label"/>
        <field name="Method" locator="METHOD" locator-type="Label"/>
        <field name="Exception" locator="EXCEPTION" locator-type="Label"/>

        <field name="Correlator" locator="JK_CORR_RID" locator-type="Label"/>
        <field name="Correlator" locator="JK_CORR_SID" locator-type="Label"/>

        <field name="Message" separator=",">
            <field-locator locator="SQL" locator-type="Label"/>
            <field-locator locator="DB" locator-type="Label"/>
        </field>

        <field name="Tag" locator="MARKER" locator-type="Label"/>
    </parser>

    <parser name="ZorkaLDAP" class="com.jkool.tnt4j.streams.parsers.ActivityMapParser" tags="LDAP">
        <field name="EventType" value="CALL"/>
        <field name="StartTime" locator="CLOCK" datatype="Timestamp" locator-type="Label"/>
        <field name="EventName" locator="MARKER" locator-type="Label"/>
        <field name="ElapsedTime" locator="METHOD_TIME" datatype="Number" format="###########0" units="Nanoseconds"
               locator-type="Label"/>
        <field name="Class" locator="CLASS" locator-type="Label"/>
        <field name="Method" locator="METHOD" locator-type="Label"/>

        <field name="Correlator" locator="JK_CORR_RID" locator-type="Label"/>
        <field name="Correlator" locator="JK_CORR_SID" locator-type="Label"/>

        <field name="Message" separator=",">
            <field-locator locator="FILTER" locator-type="Label"/>
            <field-locator locator="DC" locator-type="Label"/>
            <field-locator locator="NAME" locator-type="Label"/>
        </field>
    </parser>

    <parser name="ZorkaWebService" class="com.jkool.tnt4j.streams.parsers.ActivityMapParser"
            tags="WS_TNT4J_STREAMS_TRACKER">
        <field name="EventType" value="CALL"/>
        <field name="StartTime" locator="CLOCK" datatype="Timestamp" locator-type="Label"/>
        <field name="EventName" locator="MARKER" locator-type="Label"/>
        <field name="ElapsedTime" locator="METHOD_TIME" datatype="Number" format="###########0" units="Nanoseconds"
               locator-type="Label"/>
        <field name="Class" locator="CLASS" locator-type="Label"/>
        <field name="Method" locator="METHOD" locator-type="Label"/>

        <field name="Correlator" locator="JK_CORR_RID" locator-type="Label"/>
        <field name="Correlator" locator="JK_CORR_SID" locator-type="Label"/>

        <field name="Message" separator=",">
            <field-locator locator="SOAP_ACTION" locator-type="Label"/>
            <field-locator locator="SOAP_METHOD" locator-type="Label"/>
        </field>
    </parser>

    <parser name="ZorkaJMS" class="com.jkool.tnt4j.streams.parsers.ActivityMapParser" tags="JMS_TNT4J_STREAMS_TRACKER">
        <field name="EventType" value="CALL"/>
        <field name="StartTime" locator="CLOCK" datatype="Timestamp" locator-type="Label"/>
        <field name="EventName" locator="MARKER" locator-type="Label"/>
        <field name="ElapsedTime" locator="METHOD_TIME" datatype="Number" format="###########0" units="Nanoseconds"
               locator-type="Label"/>
        <field name="Class" locator="CLASS" locator-type="Label"/>
        <field name="Method" locator="METHOD" locator-type="Label"/>

        <field name="Correlator" locator="JK_CORR_RID" locator-type="Label"/>
        <field name="Correlator" locator="JK_CORR_SID" locator-type="Label"/>
        <field name="Correlator" locator="CORRELATION" locator-type="Label"/>

        <field name="Message" separator=",">
            <field-locator locator="TEXT" locator-type="Label"/>
            <field-locator locator="PRIORITY" locator-type="Label"/>
            <field-locator locator="EXPIRATION" locator-type="Label"/>
            <field-locator locator="PERSIST" locator-type="Label"/>
            <field-locator locator="REDELIVERY" locator-type="Label"/>
        </field>
    </parser>


    <stream name="FileStream" class="com.jkool.tnt4j.streams.inputs.ZorkaConnector">
        <property name="HaltIfNoParser" value="false"/>

        <parser-ref name="ZorkaHTTP"/>
        <parser-ref name="ZorkaSQL"/>
        <parser-ref name="ZorkaLDAP"/>
        <parser-ref name="ZorkaWebService"/>
        <parser-ref name="ZorkaJMS"/>
    </stream>
</tnt-data-source>
```

Stream configuration states that `ZorkaConnector` referencing parsers `ZorkaHTTP`, `ZorkaSQL`, `ZorkaLDAP`,
`ZorkaWebService` and `ZorkaJMS` shall be used.

`CharacterStream` starts server socket on port defined using `Port` property. `HaltIfNoParser` property indicates that
stream should skip unparseable entries.

`JSONEnvelopeParser` transforms received JSON data package to Map with entries `MsgBody`, `sinkName`, `chanelName` and
`headers`. `MsgBody` entry value is passed to stacked parser named `AccessLogParserCommon`.  Note that activity event
will contain all fields processed by all stacked parsers. Custom fields values can be found as activity event properties.

Details on `AccessLogParserCommon` (or `ApacheAccessLogParser` in general) can be found in section
'Apache Access log single file' and 'Parsers configuration # Apache access log parser'.

NOTE: Stream stops only when critical runtime error/exception occurs or application gets terminated.

Configuring TNT4J-Streams-Zorka
======================================

Details on TNT4J-Streams related configuration can be found at
(https://github.com/Nastel/tnt4j-streams/blob/master/README.md) chapter 'Configuring TNT4J-Streams'.

#### Zorka connector parameters:

* Host - host name of machine running Zico service to listen. Default value - `localhost`. (Optional)
* Port - port number of machine running Zico service to listen. Default value - `8640`. (Optional)

    sample:
```xml
    <property name="Host" value="some.host.name"/>
    <property name="Port" value="8645"/>
```

How to Build TNT4J-Streams
=========================================

## Requirements
* JDK 1.6+
* Apache Maven 3 (https://maven.apache.org/)
* TNT4J-Streams (https://github.com/Nastel/tnt4j-streams) `core` module in particular

All other required dependencies are defined in project modules `pom.xml` files. If maven is running
online mode it should download these defined dependencies automatically.

### Manually installed dependencies

NOTE: If you have build and installed TNT4J-Streams into Your local maven repository, you dont need to install
it manually.

Some of required and optional dependencies may be not available in public Maven Repository (http://mvnrepository.com/).
In this case we would recommend to download those dependencies manually into `lib` directory and install into local
maven repository by running `mvn install` command. See `lib\mvn-install.bat` how to do this.

So what to download manually:
* TNT4J-Streams (also see (https://github.com/Nastel/tnt4j-streams/blob/master/README.md) chapter 'Manually installed
dependencies')
* Zico-util

Download the above libraries and place into the `tnt4j-streams-zorka/lib directory` directory like this:
```
    lib
     tnt4j-streams-core-[version].jar
     zico-util.jar
```

## Building
   * to build project run maven goals `clean package`
   * to make release assembly run maven goals `clean package javadoc:aggregate install`

Release assembly is built to `../build/tnt4j-streams-zorka` directory.

NOTE: sometimes maven fails to correctly handle dependencies. If dependency configuration looks
fine, but maven still complains about missing dependencies try to delete local maven repository
by hand: i.e. delete contents of `c:\Users\[username]\.m2\repository` directory.

## Running samples

See 'Running TNT4J-Streams-Zorka' chapter section 'Samples'.

Testing of TNT4J-Streams-Zorka
=========================================

## Requirements
* JUnit 4 (http://junit.org/)
* Mockito (http://mockito.org/)

## Testing using maven
Maven runs tests automatically while building project. To skip test phase add Maven parameter `-Dmaven.test.skip=true`
or select 'Skip tests' UI element in your IDE  'Maven Run' configuration.

## Running manually from IDE
* in `zorka` module run JUnit test suite named `AllZorkaTests`