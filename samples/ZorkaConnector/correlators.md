How tnt4j-streams-zorka correlators work?
=========================================

```
          Web application
                |
                | (JK_CORR_SID & _RID in attributes)
                v
             Request
                |-------------------------->----------------------------------------|
                v                                                                   |
          Processing Chain                                                          |   SQL, LDAP, JMS (Optional)
                |                                                                   |     corresponding spy
                |  StandardContextValve.process Spy                                 |
                v                                                                   |
    Zorka Thread local variable                                                     |
                |                                                                   |
                |--------->----------JK_CORR_SID & _RID varibales---->--------------|
                v                                                                   |
              Zorka-------------------------<--------------------------<------------|
                |
                |   (Zorka trace)
                v
   TNT4J-Streams-Zorka connector
                |
                |   (field correlator mapping)
                v
            jKoolCloud
```

[jKoolCloud](https://www.jkoolcloud.com) accepts variety of correlators into field `correlator`. These correlators user to relate your 
event data into connected events bundle.

You set your correlators in the application as session attributes in backing bean:

```java
    if ((String)session.getAttribute("JK_CORR_SID") == null) {
        id = UUID.randomUUID().toString();
        session.setAttribute("JK_CORR_SID", id);
        this.sid = id;
    }
    else {
        id = (String)session.getAttribute("JK_CORR_SID");
    }
    String rid = null;
    rid = UUID.randomUUID().toString();
    this.rid = rid;
    session.setAttribute("JK_CORR_RID", rid);
```

`JK_CORR_SID` it's a session correlator, as `JK_CORR_RID` - request correlator.

Zorka intercepts these correlators, as soon as script `tnt4j_base_tomcat.bsh` is loaded.
Zorka attaches spy to on of the Tomcat's request flow class  - `org.apache.catalina.core.StandardContextValve` and extracts these 
attributes in `attributes_processor()` and adds attributes by calling method _attributeTntCorrIds():

```java
    spy.add(spy.instrument("CATALINA_TNT4J_STREAMS_TRACKER")
        .onEnter(
            spy.fetchArg("REQ", 1),
            spy.fetchArg("RESP", 2),
            attributes_processor(),
            <..>
            attributeTntCorrIds()
        )
        .include(spy.byMethod("org.apache.catalina.core.StandardContextValve", "invoke")));
```

Attributes processor fetches `JK_CORR_RID` and `JK_CORR_SID` and put them to ThreadLocal for later use.
And method _attributeTntCorrIds() is called in every script supplied by tnt4j-streams-zorka to attribute all traces by application services 
(SQL, LDAP, JMS etc.) to attribute these traces with correct correlator.

JMS message correlator is retrieved over JMS spy as message attribute. Mapping into TNT4J activity event is performed
over JMS traces parser configuration:

```xml
    <parser name="ZorkaJMS" class="com.jkoolcloud.tnt4j.streams.parsers.ActivityMapParser">
        ...
        <field name="Correlator" locator="JK_CORR_RID" locator-type="Label"/>
        <field name="Correlator" locator="JK_CORR_SID" locator-type="Label"/>
        <field name="Correlator" locator="CORRELATION" locator-type="Label"/>
        ...
    </parser>
    
    <stream name="ZorkaStream" class="com.jkoolcloud.tnt4j.streams.inputs.ZorkaConnector">
        ...
        <parser-ref name="ZorkaJMS" tags="JMS_TNT4J_STREAMS_TRACKER"/>
    </stream>
```
