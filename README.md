# Transport Binding - WebSocket

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