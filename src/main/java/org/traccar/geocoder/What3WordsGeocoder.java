/*
 * Copyright 2024 Marc Gran (marc@grantech.es)
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
package org.traccar.geocoder;

import jakarta.json.JsonObject;
import jakarta.ws.rs.client.Client;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;

/**
 * Geocoder que combina What3Words con Nominatim (OpenStreetMap).
 * Resultado: ///alfiler.prefiero.cohetes | Calle Gran Via 45, Madrid
 * Usar geocoder.format=%f en traccar.xml (sin cambios).
 */
public class What3WordsGeocoder extends JsonGeocoder {

    private static final Logger LOGGER = LoggerFactory.getLogger(What3WordsGeocoder.class);

    private static final String NOMINATIM_URL =
            "https://nominatim.openstreetmap.org/reverse?format=json&lat=%f&lon=%f&zoom=18&addressdetails=1";

    private static final String NOMINATIM_UA = "Traccar/What3Words-Geocoder";

    private final Client httpClient;

    private static String formatUrl(String key, String language) {
        String url = "https://api.what3words.com/v3/convert-to-3wa?coordinates=%1$f,%2$f&key=" + key;
        if (language != null) {
            url += "&language=" + language;
        }
        return url;
    }

    public What3WordsGeocoder(
            Client client, String key, String language, int cacheSize, AddressFormat addressFormat) {
        super(client, formatUrl(key, language), cacheSize, addressFormat);
        this.httpClient = client;
    }

    @Override
    public Address parseAddress(JsonObject json) {
        if (!json.containsKey("words") || json.isNull("words")) {
            return null;
        }
        String w3wWords = "///" + json.getString("words");
        String streetPart = null;
        if (json.containsKey("coordinates") && !json.isNull("coordinates")) {
            JsonObject coords = json.getJsonObject("coordinates");
            try {
                double lat = coords.getJsonNumber("lat").doubleValue();
                double lng = coords.getJsonNumber("lng").doubleValue();
                streetPart = fetchNominatimStreet(lat, lng);
            } catch (Exception e) {
                LOGGER.warn("What3Words: Nominatim enrichment failed", e);
            }
        }
        Address address = new Address();
        if (streetPart != null && !streetPart.isEmpty()) {
            address.setFormattedAddress(w3wWords + " | " + streetPart);
        } else {
            address.setFormattedAddress(w3wWords);
        }
        return address;
    }

    @Override
    protected String parseError(JsonObject json) {
        if (json.containsKey("error")) {
            JsonObject err = json.getJsonObject("error");
            if (err != null && err.containsKey("message")) {
                return err.getString("message");
            }
        }
        return null;
    }

    private String fetchNominatimStreet(double lat, double lng) {
        String url = String.format(Locale.US, NOMINATIM_URL, lat, lng);
        JsonObject nom = httpClient.target(url)
                .request()
                .header("User-Agent", NOMINATIM_UA)
                .get(JsonObject.class);
        if (nom == null || !nom.containsKey("address") || nom.isNull("address")) {
            return null;
        }
        JsonObject a = nom.getJsonObject("address");
        StringBuilder sb = new StringBuilder();
        String road = readValue(a, "road");
        String house = readValue(a, "house_number");
        if (road != null) {
            sb.append(road);
            if (house != null) {
                sb.append(" ").append(house);
            }
        }
        String city = readValue(a, "city");
        if (city == null) {
            city = readValue(a, "town");
        }
        if (city == null) {
            city = readValue(a, "village");
        }
        if (city == null) {
            city = readValue(a, "municipality");
        }
        if (city != null) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(city);
        }
        return sb.length() > 0 ? sb.toString() : null;
    }

}
