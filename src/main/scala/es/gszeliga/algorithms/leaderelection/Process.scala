package es.gszeliga.algorithms.leaderelection

import akka.actor.{ActorRef, ActorLogging, Actor}

/**
  * Created by guillermo on 11/01/16.
  */

object Process{

  case class Start(next: ActorRef)
  case class Election(id: String)
  case class Elected(id:String)

}

class Process extends Actor with ActorLogging{

  var part = false
  var elected = false
  var leader = None
  var done = false

  def receive = ???
}
