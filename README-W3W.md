# Traccar + What3Words Integration

> Fork of [traccar/traccar](https://github.com/traccar/traccar) with native What3Words geocoding support.  
> Frontend counterpart: [marcgran/traccar-web](https://github.com/marcgran/traccar-web/tree/feature/what3words-integration)

---

## What this adds

| Feature | Description |
|---------|-------------|
| **Reverse geocoding** | Every GPS position shows `///word.word.word \| Street Name 12, City` in the address field |
| **Combined address** | What3Words code + real street address from OpenStreetMap (Nominatim) — no extra API needed |
| **Forward search** | REST endpoint to convert a 3-word address to coordinates |
| **Map search** | Search box in the Traccar map accepts `///word.word.word` queries (requires frontend fork) |

### Example

```
Address field:  ///alfiler.prefiero.cohetes | Calle Gran Vía 45, Madrid
```

---

## How it works

```
GPS position (lat, lng)
        │
        ├── 1st call → What3Words API ──► ///word.word.word
        │
        └── 2nd call → Nominatim OSM  ──► Street Name 12, City
                                │
                          Combined result
                    ///word.word.word | Street Name 12, City
```

The `What3WordsGeocoder` class makes two HTTP calls per position:
1. **[What3Words API](https://developer.what3words.com/public-api)** — converts coordinates to a 3-word address (requires API key, free tier available)
2. **[Nominatim / OpenStreetMap](https://nominatim.org/)** — converts coordinates to a real street address (free, no API key needed)

If Nominatim is unavailable, only the What3Words address is shown — no errors.

---

## Installation

### Requirements

- Traccar already installed at `/opt/traccar`
- Ubuntu / Debian Linux
- Internet access (for What3Words and Nominatim APIs)
- A free What3Words API key from [developer.what3words.com](https://developer.what3words.com)

### Quick install (recommended)

Download the all-in-one installer from the [Releases](../../releases) page or build it yourself:

```bash
wget https://github.com/marcgran/traccar/releases/download/latest/traccar-w3w-integration.zip
unzip traccar-w3w-integration.zip
sudo bash install-w3w.sh YOUR_API_KEY
```

The script will:
1. Install Java 21 JDK and Node.js if needed
2. Download and compile the Traccar source with the W3W patches
3. Build the frontend with W3W map search
4. Deploy everything and restart the service

### Manual configuration

If you already have a custom Traccar build, add these entries to `/opt/traccar/conf/traccar.xml`:

```xml
<entry key='geocoder.enable'>true</entry>
<entry key='geocoder.type'>what3words</entry>
<entry key='geocoder.key'>YOUR_W3W_API_KEY</entry>
<entry key='geocoder.language'>es</entry>
<entry key='geocoder.format'>%f</entry>
```

Available languages: `en` (English), `es` (Spanish), `fr` (French), [and more](https://developer.what3words.com/public-api/docs#available-languages).

---

## New files

### `What3WordsGeocoder.java`
`src/main/java/org/traccar/geocoder/What3WordsGeocoder.java`

Extends `JsonGeocoder`. Calls the What3Words API for the 3-word address, then enriches it with a Nominatim call for the real street address.

### `GeocoderResource.java`
`src/main/java/org/traccar/api/resource/GeocoderResource.java`

New REST endpoint for forward geocoding (words → coordinates):

```
GET /api/geocoder/w3w?q=///word.word.word

Response:
{
  "words": "alfiler.prefiero.cohetes",
  "lat": 40.4123,
  "lng": -3.7025,
  "nearestPlace": "Madrid, Madrid",
  "country": "ES",
  "map": "https://w3w.co/alfiler.prefiero.cohetes"
}
```

### `MainModule.java` (modified)
Added `"what3words"` case to the geocoder type switch, so Traccar recognizes the new geocoder type.

---

## API key

> ⚠️ **A paid What3Words API plan is required for production use.**  
> The free tier is limited to development/testing only and will return `QuotaExceeded` errors almost immediately with a real GPS tracker.

### Why the free tier is not enough

A GPS device reporting every 30 seconds generates **~2,880 requests/day per device**.  
The What3Words free tier quota is exhausted within minutes of real use.

### Getting a paid plan

1. Go to [developer.what3words.com](https://developer.what3words.com/public-api)
2. Sign up and choose a **paid plan** suited for your number of devices and reporting frequency
3. Copy your API key and set it in `traccar.xml`

### Reduce API calls with caching

Even with a paid plan, enable the Traccar geocoder cache to avoid duplicate requests when a device stays in the same location:

```xml
<entry key='geocoder.cacheSize'>1000</entry>
```

With caching, a stationary device only makes **1 request** instead of one every 30 seconds.

---

## Frontend

The map search box integration is in the companion fork:  
👉 **[marcgran/traccar-web — feature/what3words-integration](https://github.com/marcgran/traccar-web/tree/feature/what3words-integration)**

It modifies `MapGeocoder.jsx` to detect `///word.word.word` queries and route them through the `/api/geocoder/w3w` endpoint instead of Nominatim.

---

## License

Apache 2.0 — same as the upstream [traccar/traccar](https://github.com/traccar/traccar) project.
