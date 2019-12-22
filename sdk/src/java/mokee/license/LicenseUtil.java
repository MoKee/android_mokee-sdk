/**
 * Copyright (C) 2019 The MoKee Open Source Project
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package mokee.license;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.text.TextUtils;

import com.mokee.os.Build;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Locale;

public class LicenseUtil {

  public static boolean isPremiumVersion() {
    return TextUtils.equals(Build.RELEASE_TYPE.toLowerCase(Locale.ENGLISH), "premium");
  }

  public static void copyLicenseFile(Context context, Uri uri) {
    ContentResolver contentResolver = context.getContentResolver();
    try {
      InputStream inputStream = contentResolver.openInputStream(uri);
      if (inputStream != null) {
        Files.copy(inputStream, new File(getLicenseFilePath()).toPath(), StandardCopyOption.REPLACE_EXISTING);
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public static String getLicenseFilePath() {
    return String.join("/", Environment.getDataSystemDirectory().getAbsolutePath(), "mokee.lic");
  }
}
