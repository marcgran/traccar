# Traccar + What3Words Integration

> Fork of [traccar/traccar](https://github.com/traccar/traccar) — adds native What3Words geocoding to the Traccar GPS platform.  
> Frontend fork: [marcgran/traccar-web](https://github.com/marcgran/traccar-web)

---

## What this fork adds

Every GPS position automatically shows the What3Words address combined with the real street address:

```
Address field:  ///alfiler.prefiero.cohetes | Calle Gran Vía 45, Madrid
```

| Feature | Description |
|---------|-------------|
| **Address display** | Each position shows `///word.word.word \| Street, City` in the address field |
| **Real street** | Street address from OpenStreetMap (Nominatim) — no extra API key needed |
| **Map search** | Search box accepts `///word.word.word` queries (requires frontend fork) |
| **REST endpoint** | `GET /api/geocoder/w3w?q=///word.word.word` → returns coordinates |

---

## How it works

```
GPS position (lat, lng)
        │
        ├── 1. What3Words API  ──►  ///word.word.word
        └── 2. Nominatim OSM   ──►  Street Name 12, City
                                │
                     ///word.word.word | Street Name 12, City
```

Two HTTP calls are made per position update:
1. **[What3Words API](https://developer.what3words.com)** — coordinates → 3-word address *(requires paid plan)*
2. **[Nominatim / OpenStreetMap](https://nominatim.org)** — coordinates → street address *(free, no key needed)*

If Nominatim is unavailable, only the What3Words address is shown — no errors.

---

## Requirements

- Traccar already installed (Linux, `/opt/traccar`)
- A **paid** What3Words API plan (see below)

### ⚠️ What3Words API — paid plan required

The free tier does **not** include the `convert-to-3wa` endpoint (coordinates → words), which is the core feature of this integration. A free API key will return an authorization error on every position update.

The `convert-to-3wa` endpoint — converting GPS coordinates into a 3-word address — is **only available on paid plans**.

Get a paid plan at [developer.what3words.com](https://developer.what3words.com/public-api).

---

## Installation

### All-in-one installer

Download the installer ZIP, extract it and run:

```bash
unzip traccar-w3w-integration.zip
sudo bash install-w3w.sh YOUR_W3W_API_KEY
```

The script will:
1. Install Java 21 JDK and Node.js if needed
2. Download and compile Traccar with the What3Words patches
3. Build the frontend with W3W map search support
4. Deploy everything to `/opt/traccar` and restart the service

### Manual configuration

Add these entries to `/opt/traccar/conf/traccar.xml`:

```xml
<entry key='geocoder.enable'>true</entry>
<entry key='geocoder.type'>what3words</entry>
<entry key='geocoder.key'>YOUR_W3W_API_KEY</entry>
<entry key='geocoder.language'>es</entry>
<entry key='geocoder.format'>%f</entry>
<entry key='geocoder.cacheSize'>1000</entry>
```

Available languages: `en`, `es`, `fr`, `de`, [and more](https://developer.what3words.com/public-api/docs#available-languages).

The `geocoder.cacheSize` entry is recommended to avoid repeated API calls when a device stays in the same location.

---

## New files

### `What3WordsGeocoder.java`
`src/main/java/org/traccar/geocoder/What3WordsGeocoder.java`

Extends `JsonGeocoder`. Calls the What3Words API for the 3-word address, then makes a second call to Nominatim for the real street address, and returns both combined.

### `GeocoderResource.java`
`src/main/java/org/traccar/api/resource/GeocoderResource.java`

REST endpoint for forward geocoding — converts a 3-word address to coordinates:

```
GET /api/geocoder/w3w?q=///word.word.word

{
  "words": "alfiler.prefiero.cohetes",
  "lat": 40.4123,
  "lng": -3.7025,
  "nearestPlace": "Madrid, Madrid",
  "country": "ES",
  "map": "https://w3w.co/alfiler.prefiero.cohetes"
}
```

### `MainModule.java` *(modified)*
Added `"what3words"` to the geocoder type switch and the corresponding import.

---

## Frontend

The map search box integration is in the companion fork:  
👉 **[marcgran/traccar-web](https://github.com/marcgran/traccar-web)**

---

## About Traccar

Traccar is an open source GPS tracking system with support for more than 200 GPS protocols and 2000+ device models.

- Website: [traccar.org](https://www.traccar.org)
- Original repository: [traccar/traccar](https://github.com/traccar/traccar)
- Web interface: [traccar/traccar-web](https://github.com/traccar/traccar-web)
- REST API docs: [traccar.org/traccar-api](https://www.traccar.org/traccar-api/)

Original authors: Anton Tananaev, Andrey Kunitsyn.

---

## License

Apache License 2.0 — same as the upstream [traccar/traccar](https://github.com/traccar/traccar) project.

New files in this fork (`What3WordsGeocoder.java`, `GeocoderResource.java`) are  
Copyright 2024 Marc Gran (marc@grantech.es), licensed under the same Apache 2.0 terms.
