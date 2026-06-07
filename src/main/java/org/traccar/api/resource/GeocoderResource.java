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
package org.traccar.api.resource;

import jakarta.annotation.security.PermitAll;
import jakarta.inject.Inject;
import jakarta.json.JsonObject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.traccar.api.BaseResource;
import org.traccar.config.Config;
import org.traccar.config.Keys;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Endpoint para búsqueda directa por dirección What3Words.
 *
 * GET /api/geocoder/w3w?q=///word.word.word
 * GET /api/geocoder/w3w?q=word.word.word
 *
 * Devuelve: { lat, lng, words, nearestPlace, country, map }
 */
@Path("geocoder")
@Produces(MediaType.APPLICATION_JSON)
public class GeocoderResource extends BaseResource {

    @Inject
    private Config config;

    @Inject
    private Client client;

    @PermitAll
    @GET
    @Path("w3w")
    public Response lookupW3W(@QueryParam("q") String query) {

        if (query == null || query.isBlank()) {
            return errorResponse(400, "Parámetro 'q' requerido. Ejemplo: ?q=///palabra.palabra.palabra");
        }

        // Eliminar el prefijo /// si viene incluido
        String words = query.startsWith("///") ? query.substring(3) : query.trim();

        // Validar formato básico: tres palabras separadas por puntos
        if (!words.matches("[^.\\s]+\\.[^.\\s]+\\.[^.\\s]+")) {
            return errorResponse(400,
                    "Formato inválido. Usa tres palabras separadas por punto: palabra.palabra.palabra");
        }

        String key = config.getString(Keys.GEOCODER_KEY);
        if (key == null || key.isBlank()) {
            return errorResponse(503, "Geocoder no configurado en traccar.xml (geocoder.key)");
        }

        String url = "https://api.what3words.com/v3/convert-to-coordinates"
                + "?words=" + words
                + "&key=" + key;

        try {
            JsonObject json = client.target(url).request().get(JsonObject.class);

            // Si la API devuelve error, propagarlo
            if (json.containsKey("error")) {
                JsonObject err = json.getJsonObject("error");
                String code = err.containsKey("code") ? err.getString("code") : "unknown";
                String msg  = err.containsKey("message") ? err.getString("message") : "Error desconocido";
                return errorResponse(400, code + ": " + msg);
            }

            // Construir respuesta simplificada
            JsonObject coords = json.getJsonObject("coordinates");
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("words",        json.containsKey("words")        ? json.getString("words")        : words);
            result.put("lat",          coords.getJsonNumber("lat").doubleValue());
            result.put("lng",          coords.getJsonNumber("lng").doubleValue());
            result.put("nearestPlace", json.containsKey("nearestPlace") ? json.getString("nearestPlace") : null);
            result.put("country",      json.containsKey("country")      ? json.getString("country")      : null);
            result.put("map",          json.containsKey("map")          ? json.getString("map")          : null);

            return Response.ok(result).build();

        } catch (Exception e) {
            return errorResponse(502, "Error al contactar la API de What3Words: " + e.getMessage());
        }
    }

    private Response errorResponse(int status, String message) {
        Map<String, String> body = new LinkedHashMap<>();
        body.put("error", message);
        return Response.status(status).entity(body).build();
    }
}
