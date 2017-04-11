# Binding

## Example
### Description
Setup a BACnet/IT Stack using Websocket as Transport Binding.  
This example doesn't use BACnet4j primitives, instead a WhoIsRequest is represented as a byte array.

To build that example the Stack and WSBinding builds have to be in classpath.

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