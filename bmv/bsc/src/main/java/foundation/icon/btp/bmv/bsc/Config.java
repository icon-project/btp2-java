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
package foundation.icon.btp.bmv.bsc;

import scorex.util.HashMap;

import java.math.BigInteger;
import java.util.Map;

// TODO Use `VarDB` instead of global object
public class Config {
    private enum Key {
        CHAIN_ID,
        EPOCH
    }

    public static Key CHAIN_ID = Key.CHAIN_ID;
    public static Key EPOCH = Key.EPOCH;

    private static Config instance = new Config();
    private Map<Key, Object> props;

    private Config() {
        props = new HashMap<>();
    }

    public static void setOnce(Key k, Object v) {
        instance.props.put(k, v);
    }

    public static Object get(Key k) {
        return instance.props.get(k);
    }

    public static BigInteger getAsBigInteger(Key k) {
        return (BigInteger) instance.get(k);
    }

    @Override
    public String toString() {
        return "Config{" +
                "props=" + props +
                '}';
    }

}
