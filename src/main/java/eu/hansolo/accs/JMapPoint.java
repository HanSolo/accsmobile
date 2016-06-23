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

import com.gluonhq.maps.MapPoint;
import org.json.simple.JSONObject;

import java.time.Instant;


/**
 * Created by hansolo on 21.06.16.
 */
public class JMapPoint extends MapPoint{

    public JMapPoint(JSONObject JSON) {
        super(null == JSON.get("name") ? "" : JSON.get("name").toString(),
              null == JSON.get("latitude") ? 0 : Double.parseDouble(JSON.get("latitude").toString()),
              null == JSON.get("longitude") ? 0 : Double.parseDouble(JSON.get("longitude").toString()));
        setTimestamp(null == JSON.get("timestamp") ? Instant.now() : Instant.ofEpochSecond(Long.parseLong(JSON.get("timestamp").toString())));
        setInfo(null == JSON.get("info") ? "" : JSON.get("info").toString());
    }
    public JMapPoint(final double LATITUDE, final double LONGITUDE) {
        super(LATITUDE, LONGITUDE);
    }
    public JMapPoint(final String ID, final double LATITUDE, final double LONGITUDE) {
        super(ID, LATITUDE, LONGITUDE);
    }


    public JSONObject toJSON() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("name", getId());
        jsonObject.put("timestamp", new Long(getTimestamp().getEpochSecond()));
        jsonObject.put("latitude", new Double(getLatitude()));
        jsonObject.put("longitude", new Double(getLongitude()));
        jsonObject.put("info", new String(getInfo()));
        return jsonObject;
    }
    public String toJSONString() { return toJSON().toJSONString(); }
}
