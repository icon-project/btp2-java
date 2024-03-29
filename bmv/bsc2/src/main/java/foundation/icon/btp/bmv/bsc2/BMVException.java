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
package foundation.icon.btp.bmv.bsc2;

import foundation.icon.btp.lib.BTPException;

public class BMVException extends BTPException.BMV {

    public BMVException(Code c, String message) {
        super(c, message);
    }

    public static BMVException unknown(String message) {
        return new BMVException(Code.Unknown, message);
    }

    public static BMVException notVerifiable(String message) {
        return new BMVException(Code.NotVerifiable, message);
    }

    public static BMVException invalidBlockWitnessOld(String message) {
        return new BMVException(Code.InvalidBlockWitnessOld, message);
    }

    //BTPException.BMV => 25 ~ 39
    public enum Code implements Coded{
        Unknown(0),
        NotVerifiable(1),
        AlreadyVerified(2),
        InvalidBlockWitnessOld(3);

        final int code;
        Code(int code){ this.code = code; }

        @Override
        public int code() { return code; }

        static public Code of(int code) {
            for(Code c : values()) {
                if (c.code == code) {
                    return c;
                }
            }
            throw new IllegalArgumentException();
        }
    }
}
