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

import android.content.Context;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Log;

import mokee.app.MKContextConstants;

public class LicenseInterface {

    private static final String TAG = "LicenseInterface";

    private static ILicenseInterface sService;
    private static LicenseInterface sInstance;

    private Context mContext;

    private LicenseInterface(Context context) {
        Context appContext = context.getApplicationContext();
        mContext = appContext == null ? context : appContext;
        sService = getService();
        if (context.getPackageManager().hasSystemFeature(
                MKContextConstants.Features.LICENSE) && sService == null) {
            throw new RuntimeException("Unable to get LicenseInterfaceService. The service" +
                    " either crashed, was not started, or the interface has been called to early" +
                    " in SystemServer init");
        }
    }

    /**
     * Get or create an instance of the {@link mokee.license.LicenseInterface}
     *
     * @param context Used to get the service
     * @return {@link LicenseInterface}
     */
    public static LicenseInterface getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new LicenseInterface(context);
        }
        return sInstance;
    }

    /** @hide **/
    public static ILicenseInterface getService() {
        if (sService != null) {
            return sService;
        }
        IBinder b = ServiceManager.getService(MKContextConstants.MK_LICENSE_INTERFACE);
        sService = ILicenseInterface.Stub.asInterface(b);

        if (b == null) {
            Log.e(TAG, "null service. SAD!");
            return null;
        }

        sService = ILicenseInterface.Stub.asInterface(b);
        return sService;
    }

    public void licenseVerification() {
        if (sService != null) {
            try {
                sService.licenseVerification();
            } catch (RemoteException e) {
                Log.e(TAG, e.getLocalizedMessage(), e);
            }
        }
    }
}
