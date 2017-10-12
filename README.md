# Transactions and Statistics - APIs
-------------

**[Post]/api/transactions**
> - Description: Inovke while the transaction happened, stored the transaction to backend database and in chache as well
> - Input: Reqest within the body, Object:Transaction(member with amount and timestamp)
> - Returns: 
>> - case1, HttpStatus.CREATED(201) - assume that APIs return successfully without any error
>> - case2, HttpStatus.NO_CONTENT(204) - condition same as 201 but the transaction was 60 seconds older than previous'

**[Get]/api/statistics**
> - Description: Inovke while user want to query the statistics transactions history in last 60 seconds
> - Input: N/A
> - Returns: 
>> - case1, Statistics message in json format as below(for exmaple)
    ```
    {"sum":275.0,"avg":27.5,"max":50.0,"min":5.0,"count":10}
    ```
>> - case2, HttpStatus.NOT_FOUND(404) - Cache data was out of date, that's mean latest 60 seconds we do not have any transaction in cache

**Test case**
> - Post first transaction should return 201
> - Post two transactions(cache with 2 records history) should return 201
> - Post two transactions should return 204
> - Get statistics should return statistics message(json format)
> - Get statistics with no data in cache should return 404
> - Get statistics with out of date data should return 404
> - Get statistics that simulated multiple user perform post actions should return statistics message(json format)
