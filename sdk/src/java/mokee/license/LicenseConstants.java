/**
 * Copyright (C) 2019 The MoKee Open Source Project
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

package mokee.license;

import com.mokee.center.misc.ConstantsBase;

public class LicenseConstants extends ConstantsBase {
    public static final String LICENSE_PUB_KEY =
            "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCwN8FdvNOu5A8yP2Bfb7rk1o6N" +
                    "dXik/DO+Kw6+q7nIZjTh4qpPL3Gyoa7A3MI01gTRKaM+MU2+zkiZND8qoB8EGlF6" +
                    "BfDfi9BLyFyx+nOTgz3KDEYutLJhopS18DfrdZTohNXsM7+MEsk5y+GHFjYHePXN" +
                    "oE4fjtfCg3xbtwU29wIDAQAB";

    /**
     * Broadcast action: license changed
     */
    public static final String ACTION_LICENSE_CHANGED = "mokee.intent.action.LICENSE_CHANGED";
}
