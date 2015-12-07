package com.moneytransfer.services

import com.moneytransfer.models.Account

import scala.concurrent.Future

trait AccountServiceComponent {

  def accountService: AccountService

  trait AccountService {

    def create(entity: Account): Future[Account]

    def update(id: String, entity: Account): Future[Unit]

    def findOne(id: String): Future[Option[Account]]

    def delete(id: String): Future[Unit]

    def transferMoney(fromAccountId: String, toAccountId: String, amount: Float): Future[Unit]
  }
}





