In a data lake there are multiple ways to get a location of a device.
All of them have different accuracy of the estimated location, in meters.
This represents the radius of a circle around the given location.

We will go through all the choices
starting with better accuracy:

### Devices reported GPS (~0.01% of all devices)

Some devices report their coordinates explicitly.
Accuracy radius here is: 0

EMC has  `gps_stats` table

```
SELECT _sn, _r, latitude, longitude
FROM
`es-s2dl-core-p.emc.t_zagent_gps_stats`
WHERE
DATE(_et) > DATE_SUB(CURRENT_DATE(), INTERVAL 3 DAY)
```

With
- fields: `latitude`, `longitude`
- `1537` events per day
- `61` unique devices per day


B2M has `gps_stats` table

```
SELECT sn, d.utc, d.latitude, d.longitude
FROM `es-s2dl-core-p.b2m.t_b2m_gps_stats`,
UNNEST(data.buckets) as d
WHERE
DATE(_et) > DATE_SUB(CURRENT_DATE(), INTERVAL 3 DAY)
```

With
- fields: `latitude`, `longitude`
- `3256` events per day
- `96` unique devices per day

We will serve device location as is if it has data in any of these tables.


### Devices reported Wi-Fy access point info (WLAN, ~0.05% of all devices)

NOTE: check if geo location service can find the location of Wi-Fy access point we have got!!!

Accuracy radius will be provided by a service: ~100

How that works? When the android device detects any wireless network,
it sends the BSSID (MAC address) of the router along with signal strength,
and most importantly, GPS coordinates up to Google.

Some devices report info about the Wi-Fy point they connect to.
That is possibly to get course grained location with such information
using Google Geolocation API.

EMC has `wlan_location` table

```
SELECT _sn, _r, _ip, macAddress, signalStrength
FROM
`es-s2dl-core-p.emc.t_zagent_wlan_location`
WHERE
DATE(_et) > DATE_SUB(CURRENT_DATE(), INTERVAL 3 DAY)
```

With
- fields: `macAddress`, `signalStrength` we can get a location
- `919909` events per day
- `828` unique devices per day


B2M has `wlan_info` table

```
SELECT _e, d.utc, d.bssid, d.ssid
FROM
`es-s2dl-core-p.b2m.t_b2m_wlan_info`,
UNNEST(data.buckets) as d
WHERE
DATE(_et) > DATE_SUB(CURRENT_DATE(), INTERVAL 3 DAY)
```

With
- fields: `bssid` (which is a mac-address) we can get a location
- `1026` events per day
- `81` unique devices per day


### Devices reported Cell Tower access info (WWAN, ~0.0003% of all devices)

Accuracy radius will be provided by a service: ~1000

Some devices report info about the Cell Tower they connect to.
That is possibly to get course grained location with such information
using Google Geolocation API.

EMC has `wwan_location` table

```
SELECT _e, _r, lac, mcc, mnc, carrier, signalStrength
FROM
`es-s2dl-core-p.emc.t_zagent_wwan_location`
WHERE
DATE(_et) > DATE_SUB(CURRENT_DATE(), INTERVAL 3 DAY)
```

With
- fields: `lac`, `mcc`, `mnc`, `carrier`, `signalStrength` we can get a location
- `7` unique devices per all dataset


B2M has `wlan_info` table

```
SELECT _e, d.utc, d.bssid, d.ssid
FROM
`es-s2dl-core-p.b2m.t_b2m_wlan_info`,
UNNEST(data.buckets) as d
WHERE
DATE(_et) > DATE_SUB(CURRENT_DATE(), INTERVAL 3 DAY)
```

With
- fields: `bssid` (which is a mac-address) we can get a location
- `1026` events per day
- `81` unique devices per day


### Devices reported gateway IP address

What gateway IP address our devices report???

EMC has `_ip` field in every table. Lots of data.

B2M has `ipAddress` in `wlan_info` table. There is about `327` unique devices per day.


### Reported nearby beacons or tags

What is that???

### Shipped to address in repair/deployment record

We have `receive_from_address` and `ship_to_address`
addresses.

Should be look if a device was ever rapaired and where
was it shipped after?

### Address of customer on contract

Do not see any address here.

There is `region`, but an example is like `Zebra NALA`.
Or `device.site_name`, example `WAL-MART STORES/CLUBS`

How can we use it?


### Site information pulled from blue-dns

There is a country and DNS suffix like `s00144.us.wal-mart.com s00144.us.wal-mart.com`:

How can we use it?

### Site tags from OVS (and eventually from Savanna)

MDM???
