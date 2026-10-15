# cordova-plugin-latinad-gps

Reads the position straight from Android's GPS provider (`LocationManager.GPS_PROVIDER`)
instead of the WebView's `navigator.geolocation` (which goes through the platform "fused"
provider and can stop delivering fixes on devices without Google Play Services).

```js
LatinadGps.getCurrentPosition({ timeout: 15000, maxAge: 60000 },
  function (p) { /* {lat, lon, accuracy, altitude, provider, ageMs} */ },
  function (e) { /* {code, message}: 1 no permission, 2 GPS disabled, 3 timeout */ });
```
