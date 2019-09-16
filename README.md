
1. Functional validation

```java
validate(greaterThan(arg1, 0), notEmpty(arg2))
  .then(arg1, arg2 -> {
    return Response.ok(doStuff(arg1, arg2));
  })
  .orElse(e -> {
    // Note, Validation is applicative functor - if both fails, 
    // both validation errors will be returned
    return Response.badRequest(e);
  }
```

2. Imperative preconditions

```java
try {
  greaterThan(arg1, 0);
  notEmpty(arg2)
  return Response.ok(doStuff());
} catch (ValidationException e) {
  // Note, need to handle ValidationException only 
  // and make sure nested code does not trow it
  return Response.badRequest(e);
}
```

Or we can configure special Spring error handler,
but again we may mix in validation errors which we do not expect thrown from nested code

```java
greaterThan(arg1, 0);
notEmpty(arg2)
return Response.ok(doStuff());
```

3. Try

```java
Try.product(greaterThan(arg1, 0), notEmpty(arg1, 0)).match(
  tuple -> {
    return Response.ok(doStuff(tuple._1, tuple._2));
  },
  e -> {
    // Note, Try is a monad, not applicative functor - if both fails, 
    // the first validation error will be returned
    return Response.badRequest(e);
  })
```

3. Reactor

```java
Mono.zip(greaterThan(arg1, 0), notEmpty(arg1, 0))
  .map(tuple -> {
    return Response.ok(doStuff(tuple._1, tuple._2));
  }).onErrorResume(e -> {
    return Response.badRequest(e);
  })
```

## Simple case

Imagine we have a record in a simplified latest table `battery_analytic_latest`

```
walmart#123#battery_prediction => f:c={"device_id": 567}
```
where: 
 - `walmart#123#battery_prediction` - key
 - `f` - column family, 
 - `c` - qualifier, 
 - `{"device_id": 567}` - value

And there is a need to find that battery prediction by `device_id` field value.
The only way to achieve that is to make a secondary index (first level index is a row key itself).
All the indexes for a dataset we will store in one table `<dataset>_indexes`

In our case the index would be in a table `battery_analytic_indexes` 
(`battery_analytic` is a name of a dataset).

We can create a record like
```
walmart#battery_prediction#device_id#567 => i:123=<null>
```
where:
 - `walmart#battery_prediction#device_id#567` - key in a format `<tenant>#<message_type>#<filed_name_to_index>#<index_value>`
 - `i` - index column family
 - `123` - column qualifier which is entity ID of the record index points to, you can find that record 
   by searching for a key `<tenant>#<index_value>#<message_type>` in a `latest` table

So far so good, looks like we can make a secondary index.

## Index lifecycle

What if we get an update where battery changes a device_id? 
There will be a new index and old index will remain. 
Old one may be overridden later or may not.
If index field is pretty much static that is fine, the space will not grow significantly. 

IMPORTANT: Fields which have random values SHOULD never be indexes.

IMPORTANT: When finding record by an index we need to check that record after 
we read it and check if field's value equals to one we have in index. 
If they are not - that is outdated index and shell not be used.

NOTE: We may add TTL to index tables, like 1 year or so to make index space constant.

## Indexing of a record with repeated fields

What is we have got the following record?
```json
{
  "contract_id": 123,
  "devices": [
    {
      "device_id": 1
    },
    {
      "device_id": 2
    }
  ]
}
```

And we would like to index by device_id field.
Such document should produce multiple indexes:
```
walmart#contract#device_id#1 => i:123=<null>
walmart#contract#device_id#2 => i:123=<null>
```

I would recommend to use JSONpath for index extraction, there is an example of how to
configure dataflow with such index. We may pass `secondaryIndexes` option with the following content
```json
{
  "device_id": "$.devices[*].device_id",
  "other_index": "..."
}

By applying `$.devices[*].device_id` JSONpath to a document we will get a list of indexes `[1, 2]`.

## Indexing of a record with nested fields

Same as in previous. JSONpath will handle it.

## Composite indexes

Imagine there is a document:
```json
{
  "battery_serial_number": "00165136",
  "battery_part_number": "82-111094-55 Rev.03",
  "battery_manufacture_date": "2017-09-01"
}
```

And we want to index by `battery_serial_number` + `battery_part_number`.
To handle that we should support composite indexes, like:
```json
{
  "battery_index": ["$.battery_serial_number", "$.battery_part_number"]
}
```

NOTE: The order here is important.

Such index will be stored like:
```
walmart#battery_prediction#battery_index#00165136#82-111094-55 Rev.03 => i:123=<null>
```
where we just joined the index values in specified order and added at the end of a record key.

## Real case

Let's try the same with real `latest` table.
The interesting thing about this table - it supports batches of data in one record.

There is a simplified example of a `contract` record in `support_latest` table:
```
walmart#6434#contract => t:<timestamp>=<null>, c:<timestamp>:<chunk_1>={"device_id": 1}, c:<timestamp>:<chunk_2>={"device_id": 2}, ...
```
Every chunk would make a separate index:
```
walmart#contract#device_id#1 => i:6434=<null>
walmart#contract#device_id#2 => i:6434=<null>
```
The index would point to an entire batch, later we can scan it and find the chunk we need.
However it is possible to specify an exact chuck index points to, but is is not really useful.

## Late data

There is a possibility that late arrived message may update an index.
To avoid that we may use atomic update, similar to what we do in latest table, but more simple.

We can change a way we write index a little and append an epoch timestamp to index value stored in qualifier

```
walmart#contract#device_id#1 => i:6434:<timestamp>=<null>
```

When we write every index we should use a conditional write and 
check if new qualifier value is greater than old qualifier value.
Since the first part of the value is the same, the comparison will be done by `<timestamp>`.
That way late index will not override the latest.

## Preprocessing in a dataflow

With high-load datasets like EMC which have lots of similar data inside of one batch we 
would need to use some preprocessing in a dataflow before writing indexes in BigTable.

Imagine we have got a batch and there 1000 chunks which all points to same index.
We would need to create a 1 min mini batch and reduce the number of index updates:
  - we need to leave only unique update operations (most likely there will be more duplicates)
  - if there are updates for same index but different timestamps, we need to lake only the newest one.



Retrieve last known location of a device.

## Usage examples

This service can find an approximate location of a device with certain accurracy
(represents the radius of a circle around the given location).

### Getting all known device locations

There multiple sources exist to get a location of a device, like
  - by GPS (`gps` source)
  - reported WiFi access points (`wifi` source)
  - reported Cell Towers (`cell_tower` source)
  - by IP of a gateway the devices sends data with (`ip` source)
  - by blue DNS value (Walmart device only) (`blue_dns` source)

Run:
```bash
curl -H "apikey: ***" https://api.zebra.com/v2/data-platform/private/zebra/location/16140522501076?t=walmart > out.json
```

NOTE: we used `walmart` tenant here, make sure your account has access to this tenant to reprosuce such example.

The response will be like:
```json
[
    {
        "source": "wifi",
        "location": {
            "lat": 27.853777700000002,
            "lng": -97.6334316
        },
        "confidence": 0.95,
        "accuracy": 57,
        "timestamp": "2019-09-12T08:55:46.006Z",
        "weight": 0.5202415577910159
    },
    {
        "source": "blue_dns",
        "location": {
            "lat": 27.853628,
            "lng": -97.633395
        },
        "confidence": 0.95,
        "accuracy": 202,
        "timestamp": "2019-09-12T06:42:00.089Z",
        "weight": 0.4083675173286148
    }
]
```

You can see that 2 location sources were available for a device `wifi` and `blue_dns`.
In a response they are sorted by relevance, `wifi` is supposed to be more accurate.

### Getting device location from a particular source

If you know exactly the source with the best location of a device, you may save
our service resources and narrow down the search to a particular source.

For example to get only `wifi` location you can call:
```bash
curl -H "apikey: ***" https://api.zebra.com/v2/data-platform/private/zebra/location/16140522501076/wifi?t=walmart > out.json
```

The response will be like:
```json
{
    "source": "wifi",
    "location": {
        "lat": 27.853777700000002,
        "lng": -97.6334316
    },
    "confidence": 0.95,
    "accuracy": 57,
    "timestamp": "2019-09-12T08:55:46.006Z",
    "weight": 0.5202415577910159
}
```

### Getting device location from a source with the highest relevance (`weight`)

If you are only interested in the most acccurate location you may filter out the rest
and reduce network trafic and client resources.

You can call:
```bash
curl -H "apikey: ***" https://api.zebra.com/v2/data-platform/private/zebra/location/16140522501076/finest?t=walmart > out.json
```

The response will be like:
```json
{
   "source": "wifi",
   "location": {
       "lat": 27.853777700000002,
       "lng": -97.6334316
   },
   "confidence": 0.95,
   "accuracy": 57,
   "timestamp": "2019-09-12T08:55:46.006Z",
   "weight": 0.5202415577910159
}

