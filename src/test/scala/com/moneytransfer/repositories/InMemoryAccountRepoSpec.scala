package com.moneytransfer.repositories

import com.moneytransfer.models.Account
import com.moneytransfer.utils.{LoggingComponent, ExecutionContextComponent}
import org.specs2.execute.AsResult
import org.specs2.mutable.{Around, Specification}
import spray.util.LoggingContext

class InMemoryAccountRepoSpec extends Specification {

  trait MockEnvironment extends InMemoryAccountRepoComponent with ExecutionContextComponent with LoggingComponent
  with Around {

    override val executionContext = concurrentExecutionContext
    override implicit val log = LoggingContext.NoLogging

    override def around[T: AsResult](t: => T) = AsResult(t)

    override val accountRepo = new InMemoryAccountRepo
  }

  "InMemoryAccountRepo" should {

    "create and then find correctly" in new MockEnvironment {
      val account = Account(firstName = "firstName", lastName = "lastName", balance = 10.0f)

      val createAndFind = for {
        createdAccount <- accountRepo.create(account)
        foundAccount <- accountRepo.findOne(createdAccount.id)
      } yield (createdAccount, foundAccount)

      createAndFind.map(_._1) must beLike[Account] {
        case Account(account.id, account.firstName, account.lastName, account.balance) => ok
      }.await

      createAndFind.map(_._2) must beLike[Option[Account]] {
        case Some(Account(account.id, account.firstName, account.lastName, account.balance)) => ok
      }.await
    }

    "update correctly" in new MockEnvironment {
      val account = Account(firstName = "firstName", lastName = "lastName", balance = 10.0f)
      val updatedAccount = Account(id = "myIdThatWillBeIgnored", firstName = "firstName2", lastName = "lastName2", balance = 20.0f)

      val createAndFind = for {
        createdAccount <- accountRepo.create(account)
        foundAccountBeforeUpdate <- accountRepo.findOne(createdAccount.id)
        _ <- accountRepo.update(createdAccount.id, updatedAccount)
        foundAccountAfterUpdate <- accountRepo.findOne(createdAccount.id)
      } yield (createdAccount, foundAccountBeforeUpdate, foundAccountAfterUpdate)

      createAndFind.map(_._1) must beLike[Account] {
        case Account(account.id, account.firstName, account.lastName, account.balance) => ok
      }.await

      createAndFind.map(_._2) must beLike[Option[Account]] {
        case Some(Account(account.id, account.firstName, account.lastName, account.balance)) => ok
      }.await

      createAndFind.map(_._3) must beLike[Option[Account]] {
        case Some(Account(account.id, updatedAccount.firstName, updatedAccount.lastName, updatedAccount.balance)) => ok
      }.await
    }

    "delete correctly"  in new MockEnvironment {
      val account = Account(firstName = "firstName", lastName = "lastName", balance = 10.0f)

      val createAndFind = for {
        createdAccount <- accountRepo.create(account)
        foundAccountBeforeDeletion <- accountRepo.findOne(createdAccount.id)
        _ <- accountRepo.delete(createdAccount.id)
        foundAccountAfterDeletion <- accountRepo.findOne(createdAccount.id)
      } yield (createdAccount, foundAccountBeforeDeletion, foundAccountAfterDeletion)

      createAndFind.map(_._1) must beLike[Account] {
        case Account(account.id, account.firstName, account.lastName, account.balance) => ok
      }.await

      createAndFind.map(_._2) must beLike[Option[Account]] {
        case Some(Account(account.id, account.firstName, account.lastName, account.balance)) => ok
      }.await

      createAndFind.map(_._3) must beNone.await
    }

  }
}
