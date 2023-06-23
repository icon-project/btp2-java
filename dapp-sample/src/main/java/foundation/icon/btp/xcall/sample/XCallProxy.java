/*
 * Copyright 2023 ICON Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package foundation.icon.btp.xcall.sample;

import foundation.icon.btp.xcall.CallService;
import score.Address;
import score.Context;

import java.math.BigInteger;

public class XCallProxy implements CallService {
    private final Address address;

    public XCallProxy(Address address) {
        this.address = address;
    }

    public Address address() {
        return this.address;
    }

    public String getBtpAddress() {
        return Context.call(String.class, this.address, "getBtpAddress");
    }

    @Override
    public BigInteger sendCallMessage(String _to, byte[] _data, byte[] _rollback) {
        return sendCallMessage(BigInteger.ZERO, _to, _data, _rollback);
    }

    public BigInteger sendCallMessage(BigInteger value, String _to, byte[] _data, byte[] _rollback) {
        return Context.call(BigInteger.class, value, this.address, "sendCallMessage", _to, _data, _rollback);
    }

    @Override
    public void executeRollback(BigInteger _sn) {
        Context.call(this.address, "executeRollback", _sn);
    }

    @Override
    public void executeCall(BigInteger _reqId, byte[] _data) {
        Context.call(this.address, "executeCall", _reqId, _data);
    }
}
