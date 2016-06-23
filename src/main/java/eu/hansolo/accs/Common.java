/*
 * Copyright (c) 2016 by Gerrit Grunwald
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

package eu.hansolo.accs;

import com.gluonhq.charm.down.common.JavaFXPlatform;
import com.gluonhq.charm.down.common.PlatformFactory;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;


/**
 * Created by hansolo on 20.06.16.
 */
public class Common {
    public  static final String PROPERTIES_FILE_NAME = "settings.properties";
    private static final Random RND                  = new Random();


    public static String getUniqueId() {
        String uniqueId;
        if (JavaFXPlatform.isDesktop()) {
            try {
                InetAddress      ip      = InetAddress.getLocalHost();
                NetworkInterface network = NetworkInterface.getByInetAddress(ip);
                byte[]           mac     = network.getHardwareAddress();
                StringBuilder    sb      = new StringBuilder();
                for (int i = 0; i < mac.length; i++) { sb.append(String.format("%02X%s", mac[i], (i < mac.length - 1) ? "-" : "")); }
                uniqueId = sb.toString();
            } catch (UnknownHostException | SocketException e) {
                uniqueId = "ACCSDesktop" + RND.nextInt(1000);
            }
        } else if (JavaFXPlatform.isAndroid()) {
            uniqueId = "ACCSAndroid" + RND.nextInt(1000);
        } else if (JavaFXPlatform.isIOS()) {
            uniqueId = "ACCSiOS" + RND.nextInt(1000);
        } else {
            uniqueId = "ACCS" + RND.nextInt(1000);
        }
        return uniqueId;
    }
}
