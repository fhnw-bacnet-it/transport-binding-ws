# Binding

##Purpose
The following example includes the BACnet/IT stack component (Stack project) and the Websocket-Transport-Binding component (WSBinding project).  
The example demonstrates how to setup and start a BACnet/IT stack on localhost with two simulated BACnet devices as communication partners.  
After initialization of an unconfirmed WHOIS-Request (represented as a byte stream) it will be sent from one device to the other device.



##Download
1. Create an new empty directory "BACnetIT" and make it the current directory
2. Download the source code of project Stack und project WSBinding  
Stack project: ```git clone https://github.com/fhnw-BACnet-IT/Stack.git```  
WSBinding project: ```git clone https://github.com/fhnw-BACnet-IT/WSBinding.git```

##Build
1. Make BACnetIT/WSBinding the current directory.
2. Note that the project WSBinding has a dependency to project Stack, so ensure that both project are stored at the same level in the BACnetIT folder.
3. Build/Download all the .jar files using Gradle Wrapper: ```./gradlew build -x test```
4. Find all needed jars under build/distributions

## Example
### Description
Setup a BACnet/IT Stack using Websocket as Transport Binding.  
This example doesn't use BACnet4j primitives, instead a WhoIsRequest is represented as a byte array.

### Preparation
Ensure the builded jars are in java class path.

#### Setup stack on localhost at port 8080

```java
final ConnectionFactory connectionFactory = new ConnectionFactory();

        final int port = 8080;
        connectionFactory.addConnectionClient("ws",
                new WSConnectionClientFactory());
        connectionFactory.addConnectionServer("ws",
                new WSConnectionServerFactory(port));
        final Channel channel1 = new Channel();

        final BACnetEID device1inStack1 = new BACnetEID(1001);
        final BACnetEID device2inStack1 = new BACnetEID(1002);
        final KeystoreConfig keystoreConfig1 = new KeystoreConfig("dummyKeystores/keyStoreDev1.jks","123456", "operationaldevcert");
        final NetworkPortObj npo1 = new NetworkPortObj("ws", 8080, keystoreConfig1);

        channel1.registerChannelListener(new ChannelListener(device1inStack1) {
            @Override
            public void onIndication(
                    final T_UnitDataIndication tUnitDataIndication,
                    final ChannelHandlerContext ctx) {
                System.out.println(this.eid.getIdentifierAsString()
                        + " got an indication" + tUnitDataIndication.getData());
            }

            @Override
            public void onError(final String cause) {
                System.err.println(cause);
            }

            @Override
            public URI getURIfromNPO() {
                return npo1.getUri();
            }
        });

        channel1.registerChannelListener(new ChannelListener(device2inStack1) {
            @Override
            public void onIndication(
                    final T_UnitDataIndication tUnitDataIndication,
                    final ChannelHandlerContext ctx) {
                System.out.println(this.eid.getIdentifierAsString()
                        + " got an indication" + tUnitDataIndication.getData());
            }

            @Override
            public void onError(final String cause) {
                System.err.println(cause);
            }

            @Override
            public URI getURIfromNPO() {
                return npo1.getUri();
            }
        });

        final BACnetEntityListener bacNetEntityHandler = new BACnetEntityListener() {

            @Override
            public void onRemoteAdded(final BACnetEID eid,
                    final URI remoteUri) {
                DirectoryService.getInstance().register(eid, remoteUri, false,
                        true);
            }

            @Override
            public void onRemoteRemove(final BACnetEID eid) {
                // TODO Auto-generated method stub
            }

            @Override
            public void onLocalRequested(final BACnetEID eid) {
                // TODO Auto-generated method stub
            }

        };
        channel1.setEntityListener(bacNetEntityHandler);

        channel1.initializeAndStart(connectionFactory);

      

        final DiscoveryConfig ds = new DiscoveryConfig(
                DirectoryBindingType.DNSSD.name(), "[IP]",
                "itb.bacnet.ch.", "bds._sub._bacnet._tcp.",
                "dev._sub._bacnet._tcp.", "obj._sub._bacnet._tcp.", false);

        try {
            DirectoryService.init();
            DirectoryService.getInstance().setDns(ds);

        } catch (final Exception e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        
       
        
        // Get the byte stream of an WhoIsRequest()
        byte[] whoIsRequest = new byte[]{(byte)0x1e,(byte)0x8e,(byte)0x8f,(byte)0x1f};
        
        final TPDU tpdu = new TPDU(device1inStack1, device2inStack1, whoIsRequest);

        final T_UnitDataRequest unitDataRequest = new T_UnitDataRequest(
                new URI("ws://localhost:8080"), tpdu, 1, true, null);

        channel1.doRequest(unitDataRequest);
```

## How to implement a new Binding
### Description
As mentioned in the previous example, to add one or many transport binings an instance of the class ConnectionFactory is needed.
```java
ConnectionFactory connectionFactory = new ConnectionFactory();
```

A connection factory holds two maps. One holds the available outgoing bindings (connectionClients) and the other holds the available incoming bindings (connectionServers).

```java
package ch.fhnw.bacnetit.stack.network.transport;
public class ConnectionFactory {
private final Map<String, ConnectionClientPipe> connectionClients = new HashMap<>();
private final Map<String, ConnectionServerPipe> connectionServers = new HashMap<>();
```

To add an outgoing binding, an implementation of the interface ConnectionClientPipe is needed.  
To add an incoming binding, an implemenation of the interface ConnectionServerPipe is needed.


The interface ConnectionClientPipe declares one method.

```java
public interface ConnectionClientPipe {
    public ConnectionClient provideConnectionClient(
            InetSocketAddress remoteAddress);
}
```

The interface ConnectionServerPipe declares two methods.

```java
public interface ConnectionServerPipe {
    public ConnectionServer createConnectionServer();

    public int getServerPort();
}
```
In case of the Websocket Binding we implemented  
- WSConnectionClientFactory implementing ConnectionClientPipe  
and  
- WSConnectionServerFactory implementing ConnectionServerPipe

```java
public class WSConnectionClientFactory implements ConnectionClientPipe {

    protected String secprotocol = null;

    /**
     * Sets secprotocol for HTTP authentication
     *
     * @param secprotocol
     */
    public void setSecprotocol(final String secprotocol) {
        this.secprotocol = secprotocol;
    }

    /**
     * Constructs a WebSocket connection to a specified address. At this point
     * the connection is not initialized or bootstrapped.
     */
    @Override
    public ConnectionClient provideConnectionClient(
            final InetSocketAddress remoteAddress) {
        return new WSConnection(remoteAddress, secprotocol);
    }

}
```

```java
public class WSConnectionServerFactory implements ConnectionServerPipe {
    protected final int port;

    public WSConnectionServerFactory(final int serverPort) {
        this.port = serverPort;
    }

    @Override
    public ConnectionServer createConnectionServer() {
        return new WSConnectionServer();
    }

    @Override
    public int getServerPort() {
        return port;
    }

}
```

Add the incoming Binding to the connectionFactory and define the scheme "ws"

```java
final int port = 8080;
connectionFactory.addConnectionServer("ws",new WSConnectionServerFactory(port));
```
Add the outgoing Binding to the connectionFactory and define the scheme "ws"

```java
connectionFactory.addConnectionClient("ws",new WSConnectionClientFactory());
```

To add several bindings keep in mind to choose an other scheme. 
