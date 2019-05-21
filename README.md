I have reviewed a design document one more time. There are few questions left:

1. There is a statement: 
   **_Looking for a data store from which we can retrieve data from the last approximately 30 days    AND last known value no matter how long ago it was written_**. 

   Without a right upfront schema that is pretty difficult to remove old data without removing the last information about the device which timestamp is older than 30 days. Let's try:
We may create a dataflow which would scan all the data, group that by an entity ID, sort and produce delete command for all the records < 30 days or not last. Not sure how much time and resources that will take.

   In case we maintain 2 separate tables where the first one is purelly historical data and the second one is for gear treaker API where we only keep the latest event per message type. For the first table we can leverage standard TTl feature, the second will never grow.

2. The next one **_`negative_timestamp` is zero minus the integer timestamp converted to a hexadecimal string. Assuming a 64 bit integer timestamp, that would give a 16 character hex string. The newest timestamp (i.e. largest integer value) will be sorted as the first element for each `entity_id/message_type`._**.

   Any reason you do not want to use `Long.MAX - timestamp`, to save 3 characters? BTW, nothing stops us to base32 or base64 that to get even shorter result.
   
