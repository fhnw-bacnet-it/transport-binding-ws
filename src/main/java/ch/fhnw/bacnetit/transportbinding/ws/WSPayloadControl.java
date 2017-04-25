package ch.fhnw.bacnetit.transportbinding.ws;

import ch.fhnw.bacnetit.ase.encoding.UnsignedInteger31;

public class WSPayloadControl {

    public static enum PayloadType {
        CONTROLMESSAGE(64), TBPDU(0);
        int value;

        PayloadType(final int i) {
            value = i;
        }
    }

    static enum ExpectingReply {
        EXPECTING(4), NOTEXPECTING(0);
        int value;

        ExpectingReply(final int i) {
            value = i;
        }
    }

    /**
     * Always returns version 1
     *
     * @return version 1
     */
    public static byte getVersionByte() {
        return (byte) 1;
    }

    public static byte getControlByte(final PayloadType payloadType,
            final ExpectingReply expectingReply,
            final UnsignedInteger31 networkPrority) {
        return (byte) (payloadType.value | expectingReply.value
                | networkPrority.intValue());
    }

    public static PayloadType getType(final byte controlByte) {
        if ((controlByte & PayloadType.CONTROLMESSAGE.value) > 0) {
            return PayloadType.CONTROLMESSAGE;
        } else {
            return PayloadType.TBPDU;
        }
    }

}
