package com.moneytransfer.services

import com.moneytransfer.exceptions.{BalanceTooLowException, UnknownAccountException}
import com.moneytransfer.models.Account
import com.moneytransfer.repositories.AccountRepoComponent
import com.moneytransfer.utils.{LoggingComponent, ExecutionContextComponent}
import org.mockito.Matchers
import org.specs2.execute.AsResult
import org.specs2.mock.Mockito
import org.specs2.mutable.{Around, Specification}
import spray.util.LoggingContext

import scala.concurrent.Future

class DefaultAccountServiceSpec extends Specification {
  trait MockEnvironment extends DefaultAccountServiceComponent with AccountRepoComponent with ExecutionContextComponent with LoggingComponent
  with Around with Mockito {

    override val accountRepo = mock[AccountRepo]
    override val executionContext = concurrentExecutionContext
    override implicit val log = LoggingContext.NoLogging

    override def around[T: AsResult](t: => T) = AsResult(t)

    override val accountService = new DefaultAccountService()
  }

  "DefaultAccountService" should {

    "create correctly" in new MockEnvironment {
      val account = Account(firstName = "firstName", lastName = "lastName", balance = 10.0f)

      // Stub method calls
      val accountRepoCapture = capture[Account]
      accountRepo.create(accountRepoCapture) returns Future.successful(account)

      // Call method under test and verify expectations
      accountService.create(account) must beEqualTo(account).await

      // Verify stubs have been hit correctly
      accountRepoCapture.value must beEqualTo(account)
    }

    "update correctly" in new MockEnvironment {
      val newAccount = Account(id = "myIdThatWillBeIgnored", firstName = "firstName", lastName = "lastName", balance = 10.0f)
      val accountId = "myId"

      // Stub method calls
      val accountRepoCapture = capture[Account]
      accountRepo.update(Matchers.eq(accountId), accountRepoCapture) returns Future.successful(())

      // Call method under test and verify expectations
      accountService.update(accountId, newAccount) must beEqualTo(()).await

      // Verify stubs have been hit correctly
      accountRepoCapture.value must beEqualTo(newAccount)
    }

    "findOne correctly" in new MockEnvironment {
      val accountId = "myId"
      val account = Account(id = accountId, firstName = "firstName", lastName = "lastName", balance = 10.0f)

      // Stub method calls
      accountRepo.findOne(accountId) returns Future.successful(Some(account))

      // Call method under test and verify expectations
      accountService.findOne(accountId) must beEqualTo(Some(account)).await

      // Verify stubs have been hit correctly
      there was one(accountRepo).findOne(accountId)
    }

    "delete correctly"  in new MockEnvironment {
      val accountId = "myId"

      // Stub method calls
      accountRepo.delete(accountId) returns Future.successful(())

      // Call method under test and verify expectations
      accountService.delete(accountId) must beEqualTo(()).await

      // Verify stubs have been hit correctly
      there was one(accountRepo).delete(accountId)
    }

    "transfer money correctly" in new MockEnvironment {
      val amount = 5.0f
      val fromAccountId = "myId1"
      val fromAccount = Account(id = fromAccountId, firstName = "firstName1", lastName = "lastName1", balance = 10.0f)
      val toAccountId = "myId2"
      val toAccount = Account(id = toAccountId, firstName = "firstName2", lastName = "lastName2", balance = 10.0f)

      // Stub method calls
      val newFromAccountCapture = capture[Account]
      val newToAccountCapture = capture[Account]
      accountRepo.findOne(fromAccountId) returns Future.successful(Some(fromAccount))
      accountRepo.findOne(toAccountId) returns Future.successful(Some(toAccount))
      accountRepo.update(Matchers.eq(fromAccountId), newFromAccountCapture) returns Future.successful(())
      accountRepo.update(Matchers.eq(toAccountId), newToAccountCapture) returns Future.successful(())

      // Call method under test and verify expectations
      accountService.transferMoney(fromAccountId, toAccountId, amount) must beEqualTo(()).await

      // Verify stubs have been hit correctly
      there was one(accountRepo).findOne(fromAccountId)
      there was one(accountRepo).findOne(toAccountId)
      newFromAccountCapture.value must beEqualTo(fromAccount.copy(balance = fromAccount.balance - amount))
      newToAccountCapture.value must beEqualTo(toAccount.copy(balance = toAccount.balance + amount))
    }

    "rollback when transferring money fails" in new MockEnvironment {
      val amount = 5.0f
      val fromAccountId = "myId1"
      val fromAccount = Account(id = fromAccountId, firstName = "firstName1", lastName = "lastName1", balance = 10.0f)
      val toAccountId = "myId2"
      val toAccount = Account(id = toAccountId, firstName = "firstName2", lastName = "lastName2", balance = 10.0f)
      val exception = new RuntimeException("Something unexpected")

      // Stub method calls
      val newFromAccountCapture = capture[Account]
      val newToAccountCapture = capture[Account]
      accountRepo.findOne(fromAccountId) returns Future.successful(Some(fromAccount))
      accountRepo.findOne(toAccountId) returns Future.successful(Some(toAccount))
      accountRepo.update(Matchers.eq(fromAccountId), newFromAccountCapture) returns Future.successful(())
      accountRepo.update(Matchers.eq(toAccountId), newToAccountCapture) throws exception thenReturns Future.successful(())

      accountService.transferMoney(fromAccountId, toAccountId, amount) must throwA(exception).await

      // Verify stubs have been hit correctly
      there was one(accountRepo).findOne(fromAccountId)
      there was one(accountRepo).findOne(toAccountId)
      newFromAccountCapture.values.get(0) must beEqualTo(fromAccount.copy(balance = fromAccount.balance - amount))
      newToAccountCapture.values.get(0) must beEqualTo(toAccount.copy(balance = toAccount.balance + amount))
      newFromAccountCapture.values.get(1) must beEqualTo(fromAccount)
      newToAccountCapture.values.get(1) must beEqualTo(toAccount)
    }

    "throw an exception when transferring money from an unknown account" in new MockEnvironment {
      val fromAccountId = "unknownId"
      val toAccountId = "myId2"
      val toAccount = Account(id = toAccountId, firstName = "firstName2", lastName = "lastName2", balance = 10.0f)

      // Stub method calls
      accountRepo.findOne(fromAccountId) returns Future.successful(None)
      accountRepo.findOne(toAccountId) returns Future.successful(Some(toAccount))

      // Call method under test and verify expectations
      accountService.transferMoney("unknownId", toAccountId, 5.0f) must throwA(new UnknownAccountException(fromAccountId)).await

      // Verify stubs have been hit correctly
      there was one(accountRepo).findOne(fromAccountId)
      there was one(accountRepo).findOne(toAccountId)
    }

    "throw an exception when transferring money to an unknown account" in new MockEnvironment {
      val fromAccountId = "myId2"
      val fromAccount = Account(id = fromAccountId, firstName = "firstName1", lastName = "lastName1", balance = 10.0f)
      val toAccountId = "unknownId"

      // Stub method calls
      accountRepo.findOne(fromAccountId) returns Future.successful(Some(fromAccount))
      accountRepo.findOne(toAccountId) returns Future.successful(None)

      // Call method under test and verify expectations
      accountService.transferMoney(fromAccountId, "unknownId", 5.0f) must throwA(new UnknownAccountException(toAccountId)).await

      // Verify stubs have been hit correctly
      there was one(accountRepo).findOne(fromAccountId)
      there was one(accountRepo).findOne(toAccountId)
    }

    "throw an exception when transferring money from an account with balance too low" in new MockEnvironment {
      val fromAccountId = "myId1"
      val fromAccount = Account(id = fromAccountId, firstName = "firstName1", lastName = "lastName1", balance = 10.0f)
      val toAccountId = "myId2"
      val toAccount = Account(id = toAccountId, firstName = "firstName2", lastName = "lastName2", balance = 10.0f)

      // Stub method calls
      accountRepo.findOne(fromAccountId) returns Future.successful(Some(fromAccount))
      accountRepo.findOne(toAccountId) returns Future.successful(Some(toAccount))

      // Call method under test and verify expectations
      accountService.transferMoney(fromAccountId, toAccountId, 20.0f) must throwA(new BalanceTooLowException(fromAccountId, 20.0f)).await

      // Verify stubs have been hit correctly
      there was one(accountRepo).findOne(fromAccountId)
      there was one(accountRepo).findOne(toAccountId)
    }
  }
}
