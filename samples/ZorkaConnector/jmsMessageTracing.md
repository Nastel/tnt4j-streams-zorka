# JMS message tracing

There is a possibility to alter zorka instrumented applications sent JMS messages. This is done to get a whole picture of an instrumented 
application and correlate with data with Streams monitored.

```
  +-------------------+
  |                   |  JMS      +---------------------------+
  |    Instrumented   |  messages |                           |
  |    Application    | --------> |    IBM MQ queue manager   |
  |                   |           |                           |
  +---------+---------+           +-------------+-------------+
            |                                   |
            |                                   v
            v                     +---------------------------+
  +-------------------+           |                           |
  |                   |           |       TNT4J-Streams       |
  |       Zorka       |           |                           |
  |                   |           +-+-------------------------+
  +---------+---------+             |
            |                       |
            |                       |
            v                       |
+-----------------------+           |
|                       |           |
|  TNT4J-Streams-Zorka  |           |
|                       |           |
+--------------------+--+           |
                     |              |
                     v              v
                  +---------------------------------------+
                  |                                       |
                  |        Data collector (JKool)         |
                  |                                       |
                  +---------------------------------------+
```

## Enabling Zorka to trace JMS messages

Current sample [`tnt4j_jms.bsh`](./zorka/scripts/tnt4j_jms.bsh) script contains a feature to enhance Send message with additional UUID 
correlator. 

The property in file `zorka.properties` determines the name of the messages custom named property.
The default custom property name is `JanusMessageSignature`.

1) To enable this behavior you need to include script [`tnt4j_jms.bsh`](./zorka/scripts/tnt4j_jms.bsh) in your `zorka.properties`.
2) Select he name of the property `tnt4j.jms.correlator.name` (or use default).
3) Start application server with zorka (see [`readme.md`](./readme.md)) 
4) Collect traces with tnt4j-streams-zorka (see [`readme.md`](./readme.md)) 
5) Collect wmq-traces with tnt4j-streams from the `SYSTEM.ADMIN.TRACE.ACTIVITY.QUEUE` 
  (the sample configuration is provided in file [`wmqTrace\tnt-data-source.xml`](./wmqTrace/tnt-data-source.xml))
