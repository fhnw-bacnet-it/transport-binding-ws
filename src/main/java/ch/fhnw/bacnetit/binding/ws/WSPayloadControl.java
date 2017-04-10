/*******************************************************************************
 * Copyright (C) 2016 The Java BACnetITB Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package ch.fhnw.bacnetit.binding.ws;

import ch.fhnw.bacnetit.stack.encoding.UnsignedInteger31;

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
