package com.moneytransfer.resources

import com.moneytransfer.entities._
import com.moneytransfer.exceptions.{BalanceTooLowException, UnknownAccountException, CommonExceptionHandler}
import com.moneytransfer.models.Account
import com.moneytransfer.services.AccountServiceComponent
import com.moneytransfer.utils.{LoggingComponent, ExecutionContextComponent}
import org.mockito.Matchers
import org.specs2.execute.AsResult
import org.specs2.mock.Mockito
import org.specs2.mutable.{Around, SpecificationFeatures, Specification}
import spray.http.StatusCodes._
import spray.routing.{ExceptionHandler, RejectionHandler}
import spray.testkit.Specs2RouteTest
import spray.util.LoggingContext

import scala.concurrent.Future

class AccountResourceSpec extends Specification {

  trait MockEnvironment extends AccountResource with AccountServiceComponent with ExecutionContextComponent
  with LoggingComponent with CommonExceptionHandler with Specs2RouteTest with SpecificationFeatures
  with Around with Mockito {

    override implicit val actorRefFactory = system
    override implicit val executionContext = system.dispatcher
    override implicit val log = LoggingContext.NoLogging

    implicit def exceptionHandler(implicit log: LoggingContext): ExceptionHandler = commonExceptionHandler
    implicit def rejectionHandler(implicit log: LoggingContext): RejectionHandler = commonRejectionHandler

    override def around[T: AsResult](t: => T) = AsResult(t)
    override def is = specFragments

    override val accountService = mock[AccountService]
    def sealedRoutes = sealRoute(accountRoute)
  }
  
  "AccountResource" in {

    "Create" should {

      "return HTTP 201 with the ID on success" in new MockEnvironment {
        val account = Account(firstName = "firstName", lastName = "lastName", balance = 10.0f)
        val accountCreationRequest = AccountCreationRequest(firstName = account.firstName, lastName = account.lastName, balance = account.balance)

        // Stub method calls
        val accountCreationCapture = capture[Account]
        accountService.create(accountCreationCapture) returns Future.successful(account)

        // Hit endpoint under test and verify expectations
        Post("/accounts", accountCreationRequest) ~> sealedRoutes ~> check {
          status must beEqualTo(Created)
          responseAs[AccountCreationResponse] must beEqualTo(AccountCreationResponse(id = account.id))

          // Verify stubs have been hit correctly
          accountCreationCapture.value must beEqualTo(account.copy(id = accountCreationCapture.value.id))
        }
      }

      "return HTTP 400 when names are too short" in new MockEnvironment {
        val accountCreationRequest = AccountCreationRequest(firstName = "f", lastName = "lastName", balance = 10.0f)

        // Hit endpoint under test and verify expectations
        Post("/accounts", accountCreationRequest) ~> sealedRoutes ~> check {
          status must beEqualTo(BadRequest)
          responseAs[ErrorResponse] must beEqualTo(ErrorResponse("Sorry, bot first name and last name must be longer than 1 character"))

          // Verify stubs have been hit correctly
          there were noCallsTo(accountService)
        }
      }

      "return HTTP 500 on failure" in new MockEnvironment {
        val account = Account(firstName = "firstName", lastName = "lastName", balance = 10.0f)
        val accountCreationRequest = AccountCreationRequest(firstName = account.firstName, lastName = account.lastName, balance = account.balance)
        // Stub method calls
        val accountCreationCapture = capture[Account]
        accountService.create(accountCreationCapture) returns Future.failed(new RuntimeException("Something went wrong"))

        // Hit endpoint under test and verify expectations
        Post("/accounts", accountCreationRequest) ~> sealedRoutes ~> check {
          status must beEqualTo(InternalServerError)
          responseAs[ErrorResponse] must beEqualTo(ErrorResponse("Sorry, unexpected error creating account", Some("AccountCreationError")))

          // Verify stubs have been hit correctly
          accountCreationCapture.value must beEqualTo(account.copy(id = accountCreationCapture.value.id))
        }
      }
    }

    "Update" should {

      "return HTTP 204 on success" in new MockEnvironment {
        val account = Account(firstName = "firstName", lastName = "lastName", balance = 10.0f)
        val accountUpdateRequest = AccountUpdateRequest(firstName = account.firstName, lastName = account.lastName, balance = account.balance)

        // Stub method calls
        val accountUpdateCapture = capture[Account]
        accountService.update(Matchers.eq(account.id), accountUpdateCapture) returns Future.successful(())

        // Hit endpoint under test and verify expectations
        Put(s"/accounts/${account.id}", accountUpdateRequest) ~> sealedRoutes ~> check {
          status must beEqualTo(NoContent)

          // Verify stubs have been hit correctly
          accountUpdateCapture.value must beEqualTo(account.copy(id = accountUpdateCapture.value.id))
        }
      }

      "return HTTP 400 when names are too short" in new MockEnvironment {
        val id = "myId"
        val accountUpdateRequest = AccountUpdateRequest(firstName = "f", lastName = "lastName", balance = 10.0f)

        // Hit endpoint under test and verify expectations
        Put(s"/accounts/$id", accountUpdateRequest) ~> sealedRoutes ~> check {
          status must beEqualTo(BadRequest)
          responseAs[ErrorResponse] must beEqualTo(ErrorResponse("Sorry, bot first name and last name must be longer than 1 character"))

          // Verify stubs have been hit correctly
          there were noCallsTo(accountService)
        }
      }

      "return HTTP 500 on failure" in new MockEnvironment {
        val account = Account(firstName = "firstName", lastName = "lastName", balance = 10.0f)
        val accountUpdateRequest = AccountUpdateRequest(firstName = account.firstName, lastName = account.lastName, balance = account.balance)
        // Stub method calls
        val accountUpdateCapture = capture[Account]
        accountService.update(Matchers.eq(account.id), accountUpdateCapture) returns Future.failed(new RuntimeException("Something went wrong"))

        // Hit endpoint under test and verify expectations
        Put(s"/accounts/${account.id}", accountUpdateRequest) ~> sealedRoutes ~> check {
          status must beEqualTo(InternalServerError)
          responseAs[ErrorResponse] must beEqualTo(ErrorResponse("Sorry, unexpected error updating account", Some("AccountUpdateError")))

          // Verify stubs have been hit correctly
          accountUpdateCapture.value must beEqualTo(account.copy(id = accountUpdateCapture.value.id))
        }
      }
    }

    "Get by ID" should {

      "return HTTP 200 with the account success" in new MockEnvironment {
        val account = Account(firstName = "firstName", lastName = "lastName", balance = 10.0f)

        // Stub method calls
        accountService.findOne(account.id) returns Future.successful(Some(account))

        // Hit endpoint under test and verify expectations
        Get(s"/accounts/${account.id}") ~> sealedRoutes ~> check {
          status must beEqualTo(OK)
          responseAs[AccountFetchResponse] must beEqualTo(AccountFetchResponse(id = account.id,
            firstName = account.firstName, lastName = account.lastName, balance = account.balance))

          // Verify stubs have been hit correctly
          there was one(accountService).findOne(account.id)
        }
      }

      "return HTTP 404 when the account is not found" in new MockEnvironment {
        val unknownId = "unknownId"

        // Stub method calls
        accountService.findOne(unknownId) returns Future.successful(None)

        // Hit endpoint under test and verify expectations
        Get(s"/accounts/$unknownId") ~> sealedRoutes ~> check {
          status must beEqualTo(NotFound)
          responseAs[ErrorResponse] must beEqualTo(ErrorResponse("Unable to find account with the specified ID"))

          // Verify stubs have been hit correctly
          there was one(accountService).findOne(unknownId)
        }
      }

      "return HTTP 500 on failure" in new MockEnvironment {
        val account = Account(firstName = "firstName", lastName = "lastName", balance = 10.0f)
        // Stub method calls
        accountService.findOne(account.id) returns Future.failed(new RuntimeException("Something went wrong"))

        // Hit endpoint under test and verify expectations
        Get(s"/accounts/${account.id}") ~> sealedRoutes ~> check {
          status must beEqualTo(InternalServerError)
          responseAs[ErrorResponse] must beEqualTo(ErrorResponse("Sorry, unexpected error looking up account", Some("AccountLookupError")))

          // Verify stubs have been hit correctly
          there was one(accountService).findOne(account.id)
        }
      }
    }

    "Delete" should {

      "return HTTP 204 on success" in new MockEnvironment {
        val id = "myId"

        // Stub method calls
        accountService.delete(id) returns Future.successful(())

        // Hit endpoint under test and verify expectations
        Delete(s"/accounts/$id") ~> sealedRoutes ~> check {
          status must beEqualTo(NoContent)

          // Verify stubs have been hit correctly
          there was one(accountService).delete(id)
        }
      }

      "return HTTP 500 on failure" in new MockEnvironment {
        val id = "myId"
        // Stub method calls
        accountService.delete(id) returns Future.failed(new RuntimeException("Something went wrong"))

        // Hit endpoint under test and verify expectations
        Delete(s"/accounts/$id") ~> sealedRoutes ~> check {
          status must beEqualTo(InternalServerError)
          responseAs[ErrorResponse] must beEqualTo(ErrorResponse("Sorry, unexpected error deleting account", Some("AccountDeletionError")))

          // Verify stubs have been hit correctly
          there was one(accountService).delete(id)
        }
      }
    }

    "Transfer money" should {

      "return HTTP 204 on success" in new MockEnvironment {
        val fromAccountId = "fromAccountId"
        val toAccountId = "fromAccountId"
        val amount = 10.0f
        val moneyTransferRequest = MoneyTransferRequest(fromAccountId, toAccountId, amount)

        // Stub method calls
        accountService.transferMoney(fromAccountId, toAccountId, amount) returns Future.successful(())

        // Hit endpoint under test and verify expectations
        Post(s"/accounts/money-transfer", moneyTransferRequest) ~> sealedRoutes ~> check {
          status must beEqualTo(NoContent)

          // Verify stubs have been hit correctly
          there was one(accountService).transferMoney(fromAccountId, toAccountId, amount)
        }
      }

      "return HTTP 404 when transferring money from an unknown account" in new MockEnvironment {
        val fromAccountId = "fromAccountId"
        val toAccountId = "toAccountId"
        val amount = 10.0f
        val moneyTransferRequest = MoneyTransferRequest(fromAccountId, toAccountId, amount)

        // Stub method calls
        accountService.transferMoney(fromAccountId, toAccountId, amount) returns Future.failed(new UnknownAccountException(fromAccountId))

        // Hit endpoint under test and verify expectations
        Post(s"/accounts/money-transfer", moneyTransferRequest) ~> sealedRoutes ~> check {
          status must beEqualTo(NotFound)
          responseAs[ErrorResponse] must beEqualTo(ErrorResponse(s"Can't find account with ID $fromAccountId", Some("UnknownAccount")))

          // Verify stubs have been hit correctly
          there was one(accountService).transferMoney(fromAccountId, toAccountId, amount)
        }
      }

      "return HTTP 404 when transferring money to an unknown account" in new MockEnvironment {
        val fromAccountId = "fromAccountId"
        val toAccountId = "toAccountId"
        val amount = 10.0f
        val moneyTransferRequest = MoneyTransferRequest(fromAccountId, toAccountId, amount)

        // Stub method calls
        accountService.transferMoney(fromAccountId, toAccountId, amount) returns Future.failed(new UnknownAccountException(toAccountId))

        // Hit endpoint under test and verify expectations
        Post(s"/accounts/money-transfer", moneyTransferRequest) ~> sealedRoutes ~> check {
          status must beEqualTo(NotFound)
          responseAs[ErrorResponse] must beEqualTo(ErrorResponse(s"Can't find account with ID $toAccountId", Some("UnknownAccount")))

          // Verify stubs have been hit correctly
          there was one(accountService).transferMoney(fromAccountId, toAccountId, amount)
        }
      }

      "return HTTP 400 when transferring money from an account with balance too low" in new MockEnvironment {
        val fromAccountId = "fromAccountId"
        val toAccountId = "toAccountId"
        val amount = 10.0f
        val moneyTransferRequest = MoneyTransferRequest(fromAccountId, toAccountId, amount)

        // Stub method calls
        accountService.transferMoney(fromAccountId, toAccountId, amount) returns Future.failed(new BalanceTooLowException(fromAccountId, amount))

        // Hit endpoint under test and verify expectations
        Post(s"/accounts/money-transfer", moneyTransferRequest) ~> sealedRoutes ~> check {
          status must beEqualTo(BadRequest)
          responseAs[ErrorResponse] must beEqualTo(ErrorResponse(s"Account $fromAccountId can't afford to transfer $amount", Some("BalanceTooLow")))

          // Verify stubs have been hit correctly
          there was one(accountService).transferMoney(fromAccountId, toAccountId, amount)
        }
      }

      "return HTTP 500 on failure" in new MockEnvironment {
        val fromAccountId = "fromAccountId"
        val toAccountId = "toAccountId"
        val amount = 10.0f
        val moneyTransferRequest = MoneyTransferRequest(fromAccountId, toAccountId, amount)
        // Stub method calls
        accountService.transferMoney(fromAccountId, toAccountId, amount) returns Future.failed(new RuntimeException("Something went wrong"))

        // Hit endpoint under test and verify expectations
        Post(s"/accounts/money-transfer", moneyTransferRequest) ~> sealedRoutes ~> check {
          status must beEqualTo(InternalServerError)
          responseAs[ErrorResponse] must beEqualTo(ErrorResponse("Sorry, unexpected error transferring money", Some("MoneyTransferError")))

          // Verify stubs have been hit correctly
          there was one(accountService).transferMoney(fromAccountId, toAccountId, amount)
        }
      }
    }
  }
}
