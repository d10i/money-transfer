# Money transfer

## Usage
1. Install Scala and SBT
2. To run tests, run `sbt test`
3. To run the application, run `sbt run`

## Endpoints
##### Create an account
###### Request
`POST` to `/accounts` with payload (example):
```json
{
    "firstName": "firstName",
    "lastName": "lastName",
    "balance": 30.0
}
```

###### Response
`HTTP 201 Created` with payload (example):
```json
{
    "id": "6ec33eed-a63f-4018-b94a-d3d7e0b42500"
}
```

##### Retrieve an account
###### Request
`GET` to `/accounts/6ec33eed-a63f-4018-b94a-d3d7e0b42500`.

###### Response
`HTTP 200 OK` with payload (example):
```json
{
    "id": "6ec33eed-a63f-4018-b94a-d3d7e0b42500",
    "firstName": "firstName",
    "lastName": "lastName",
    "balance": 30.0
}
```

OR

`HTTP 404 Not Found` with payload:
```json
{
    "message": "Unable to find account with the specified ID"
}
```

##### Update an account
###### Request
`PUT` to `/accounts/6ec33eed-a63f-4018-b94a-d3d7e0b42500` with payload (example):
```json
{
    "firstName": "firstName2",
    "lastName": "lastName2",
    "balance": 40.0
}
```

###### Response
`HTTP 204 No Content`.

##### Delete an account
###### Request
`DELETE` to `/accounts/6ec33eed-a63f-4018-b94a-d3d7e0b42500`.

###### Response
`HTTP 204 No Content`.

##### Transfer money
###### Request
`PUT` to `/accounts/6ec33eed-a63f-4018-b94a-d3d7e0b42500` with payload (example):
```json
{
    "fromAccountId": "4353706e-8f3e-4b13-a9cd-b770a4e6ab0c",
    "toAccountId": "a90ea363-be01-4ab6-a4d0-69b8b6se6ca79",
    "amount": 10.0
}
```

###### Response
`HTTP 204 No Content`.

##### Errors
Errors are returned via an HTTP 5xx response with a JSON payload with `message` and optional `code`, for example:
```json
{
    "message": "Can't find account with ID a90ea363-be01-4ab6-a4d0-69b8b6se6ca79",
    "code": "UnknownAccount"
}
```

## Code structure

The application is organised in 3 different layers listed bottom to top:
##### Repositories (aka DAOs)
This layer is responsible for storing data and/or defining classes that interface with a persistent storage.
In this example everything is stored in memory in a `TrieMap` to ensure thread-safety. Repositories don't contain any
business logic, all they do is query the storage (create, retrieve, update and delete).

##### Services
This layer contains all the business logic and it interfaces with the repositories to manage the data. Sometimes this
layer simply proxies requests to the repositories but sometimes, like for the `transferMoney` method, it can contain
logic that manipulates the data before hitting any repository. Services are not HTTP-specific and could be reused in any
other non-HTTP application.

##### Resources

This layer is very thin and is generally responsible for:
* Basic request validation
* Providing an anti-corruption layer between the API and the service layer. This allows to make changes to the business
logic while maintaining the same interface
* Converting service responses into HTTP responses
  * Marshalling all successful responses into JSON (or other formats)
  * Handling exceptions and returning HTTP 5xx errors with JSON (or other formats) payload
  * Other small things such as transforming a `None` response into an HTTP 404
* Verifying the request is authenticated and authorised (not in this example)

## Cake pattern
This pattern is used to do dependency injection.

Each component defines its dependencies through Scala's self-type annotations. For example:
```scala
trait DefaultAccountServiceComponent extends AccountServiceComponent {
  self: AccountRepoComponent with ExecutionContextComponent with LoggingComponent =>
  // ...
}
```

This code specifies that `DefaultAccountServiceComponent` will need an implementation of `AccountRepoComponent`,
`ExecutionContextComponent` and `LoggingComponent`. Note that these are `trait`s so that the actual implementation can
be injected at a later point, making testing much easier.

All components are then put together at the top level:
```scala
class MoneyTransferService extends Actor
  with AccountResource
  with DefaultAccountServiceComponent
  with InMemoryAccountRepoComponent
  with ExecutionContextComponent
  with LoggingComponent
  with ActorRefFactoryComponent
  with CommonExceptionHandler {
    // ...
  }
```

When unit testing a specific component, its dependencies can easily be defined/mocked:
```scala
trait MockEnvironment extends DefaultAccountServiceComponent with AccountRepoComponent with ExecutionContextComponent
with LoggingComponent with Mockito {

    override val accountRepo = mock[AccountRepo]
    override val executionContext = concurrentExecutionContext
    override implicit val log = LoggingContext.NoLogging

    // ...
}
```


## Money transfers and transactionality
Money is moved between accounts by decreasing the sender balance and increasing the recipient balance. This implies two
separate queries to the account repository and any of them could be failing. Given that the updates are idempotent, the
rollback strategy is simply to update both accounts to their initial value. The only case in which this logic will end
up in an inconsistent state (i.e. money disappearing) is when the rollback (or part of it) fails too:

For example:
  1. Decrease sender balance ✓
  2. Increase recipient balance ✗
  3. Rollback and restore sender balance ✗
  4. Rollback and restore recipient balance ✓

In this case the sender will see his balance decreased without the recipient seeing the balance increased.

Given that the requirements said that the money is always transferred between **internal** accounts, the likelihood of
some queries succeeding and some failing is very low. Anyway, to prevent this from happening there are various strategies:
  1. Use a transactional database to store data which can execute both queries in one go and guarantee consistency
  2. Implement a transaction manager, a separate entity that can handle rollbacks and take actions in case of failures
  3. Implement a 2-phase commit system
