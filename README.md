Few questions about the initial design:

1. In Raw API every use case describes read operation for a `specific entity I own`:
   So, why don't we have an `owner` in a key, like:
   ```<owner>#<entity_id>#<message_type>#<negative_timestamp>```
   That will improve performance and simplify the implementation a lot.

   BTW. Do we even need an owner here. I am not sure about the use case, but
   if it is the following:

   The client wants to get an information about the device.
   - If it owns a device we return a response
   - If it does not - we return `403`

   To implement such REST API - all weed is to call Assets service before
   calling BigTable and claim the ownership.

   If you want to handle the case when the device ownership was changed
   and you do not want to expose events of the previous owner
   you can use the key described above and get the owner from Assets service.

2. In Gear Tracker API I do not see tenant information required in a request.
   Is it true?

   If it true:

   - We may leverage Assets service in case it has historical
     information about device ownership to get all owners history for a device.
   - Or, if assets service do not have such capability we may create our own version
     based on the following table:
     ```<entity_id> -> <events when owner gets changed>```

3. Do we need to finish https://zebrathings.atlassian.net/browse/VDS-618 
   before this story?
   
4. Will we have all the owners of all the devices?
