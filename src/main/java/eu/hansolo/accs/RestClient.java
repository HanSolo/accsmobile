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

import com.gluonhq.charm.down.common.PlatformFactory;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Properties;

import static eu.hansolo.accs.Common.PROPERTIES_FILE_NAME;


/**
 * Created by hansolo on 15.06.16.
 */
public enum RestClient {
    INSTANCE;

    private static final MediaType    JSON = MediaType.parse("application/json; charset=utf-8");
    private              OkHttpClient client;
    private              File         localStoragePath;
    private              Properties   properties;


    // ******************** Constructors **************************************
    RestClient() {
        client = new OkHttpClient();
        try {
            localStoragePath = PlatformFactory.getPlatform().getPrivateStorage();
        } catch (IOException e) {
            String tmp = System.getProperty("java.io.tmpdir");
            localStoragePath = new File(tmp);
        }
        properties = createProperties();
        retrieveConfig();
    }


    // ******************** Public Methods ************************************
    public JSONObject addLocation(final JMapPoint LOCATION) {
        retrieveConfig();
        final String URL = properties.getProperty("url");
        if (null == URL || URL.isEmpty()) return new JSONObject();
        return postJSONObject(URL + "/add", LOCATION.toJSONString());
    }
    public JSONObject updateLocation(final JMapPoint LOCATION) {
        retrieveConfig();
        final String URL = properties.getProperty("url");
        if (null == URL || URL.isEmpty()) return new JSONObject();
        return putJSONObject(URL + "/update", LOCATION.toJSONString());
    }

    public JSONArray getLocations() {
        retrieveConfig();
        final String URL = properties.getProperty("url");
        if (null == URL || URL.isEmpty()) return new JSONArray();
        return getJSONArray(URL + "/locations");
    }
    public JSONObject getLocation(final String NAME) throws UnsupportedEncodingException {
        retrieveConfig();
        final String URL = properties.getProperty("url");
        if (null == URL || URL.isEmpty()) return new JSONObject();
        return getJSONObject(URL + "/location?name=" + URLEncoder.encode(NAME, "utf-8"));
    }


    // ******************** Private Methods ***********************************
    private JSONObject getJSONObject(final String URL) {
        try {
            Request  getRequest  = new Request.Builder().url(URL).build();
            Response getResponse = client.newCall(getRequest).execute();
            JSONObject jsonObject  = (JSONObject) JSONValue.parse(getResponse.body().string());
            return jsonObject;
        } catch (IOException e){
        }
        return new JSONObject();
    }
    private JSONArray getJSONArray(final String URL) {
        try {
            Request  getRequest  = new Request.Builder().url(URL).build();
            Response getResponse = client.newCall(getRequest).execute();
            JSONArray jsonArray  = (JSONArray) JSONValue.parse(getResponse.body().string());
            return jsonArray;
        } catch (IOException e){
        }
        return new JSONArray();
    }

    private JSONObject postJSONObject(final String URL, final String JSON_STRING) {
        try {
            RequestBody body       = RequestBody.create(JSON, JSON_STRING);
            Request     request    = new Request.Builder().url(URL).post(body).build();
            Response    response   = client.newCall(request).execute();
            JSONObject  jsonObject = (JSONObject) JSONValue.parse(response.body().string());
            return jsonObject;
        } catch (IOException e) {
        }
        return new JSONObject();
    }

    private JSONObject putJSONObject(final String URL, final String JSON_STRING) {
        try {
            RequestBody body       = RequestBody.create(JSON, JSON_STRING);
            Request     request    = new Request.Builder().url(URL).put(body).build();
            Response    response   = client.newCall(request).execute();
            JSONObject  jsonObject = (JSONObject) JSONValue.parse(response.body().string());
            return jsonObject;
        } catch (IOException e) {
        }
        return new JSONObject();
    }

    private Properties createProperties() {
        Properties p = new Properties();
        p.setProperty("url", "");
        p.setProperty("name", Common.getUniqueId());
        return p;
    }
    private void retrieveConfig() {
        Reader reader = null;
        try {
            File file = new File(localStoragePath, PROPERTIES_FILE_NAME);
            reader = new FileReader(file);
            properties.load(reader);
        } catch (IOException ex) {
        } finally {
            try { if (reader != null) { reader.close(); } } catch (IOException ex) {}
        }
    }
}
