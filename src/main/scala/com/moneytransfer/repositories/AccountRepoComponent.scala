package com.moneytransfer.repositories

import com.moneytransfer.models.Account

import scala.concurrent.Future

trait AccountRepoComponent {

  def accountRepo: AccountRepo

  trait AccountRepo {

    def create(entity: Account): Future[Account]

    def update(id: String, entity: Account): Future[Unit]

    def findOne(id: String): Future[Option[Account]]

    def delete(id: String): Future[Unit]
  }
}