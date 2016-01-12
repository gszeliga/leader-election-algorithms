package es.gszeliga.algorithms.leaderelection

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import es.gszeliga.algorithms.leaderelection.SimpleElectionProcess._

/**
  * Created by guillermo on 11/01/16.
  */

object SimpleElectionProcess{

  type ID[V] = Comparable[V]

  case class Config(next: ActorRef)
  case class Start()
  case class Election[V](id: ID[V])
  case class Elected[V](id: ID[V])

  def props[V](pid: ID[V]) = Props(new SimpleElectionProcess[V](pid))

}

class SimpleElectionProcess[V](val pid: ID[V]) extends Actor with ActorLogging{

  var participates = false
  var elected = false
  var leader: Option[ID[V]] = None
  var done = false
  var next:Option[ActorRef] = None

  def receive = {
    case Config(ref) => {
      log.debug(s"[CONFIG]> 'next' process reference provided => $ref")
      next = Option(ref)
    }
    case Start() if !participates => {
      log.debug(s"[START]> Starting election process")
      promoteElectionOf(pid)
    }
    case Election(id) => {

      val comparison = id.compareTo(pid)

      if(comparison == 1) promoteElectionOf(id)
      else if(comparison == -1 && !participates) promoteElectionOf(pid)
      else {
        elected=true
        notifyElectionOf(id)
      }
    }
    case Elected(id) => {
      leader=Some(id.asInstanceOf)
      done=true

      if(pid != id) {
        elected=false
        notifyElectionOf(id)
      }
    }
  }

  def promoteElectionOf[V](id: ID[V]) = {
    next.map{ref =>
      log.info(s"\t> Promoting election of '$id'")
      participates=true
      ref ! Election(id)
    }.getOrElse(log.warning("\t> No 'next' process is available. Process election cannot be promoted"))
  }

  def notifyElectionOf[V](id: ID[V]) = {
    next.map{ref =>
      log.info(s"\t> Election done. New leader is: '$id'")
      ref ! Elected(id)
    }.getOrElse(log.warning("\t> No 'next' process is available. Leader election cannot be notified"))
  }

}
