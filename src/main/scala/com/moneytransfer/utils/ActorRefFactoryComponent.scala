package com.moneytransfer.utils

import akka.actor.ActorRefFactory

trait ActorRefFactoryComponent {

  implicit def actorRefFactory: ActorRefFactory
}