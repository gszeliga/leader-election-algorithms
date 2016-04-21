package es.gszeliga.algorithms.leaderelection

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import es.gszeliga.algorithms.leaderelection.LeLannChangRobertsLeaderElectionProcessForUnidirectionalRings._

/**
  * Created by guillermo on 11/01/16.
  */

object LeLannChangRobertsLeaderElectionProcessForUnidirectionalRings{

  type ID[V] = Comparable[V]

  case class Config(next: ActorRef)
  case class Start()
  case class Election[V](id: ID[V])
  case class Elected[V](id: ID[V])

  def props[V](pid: ID[V]) = Props(new LeLannChangRobertsLeaderElectionProcessForUnidirectionalRings[V](pid))

}

//Elects the process with the highest identity among the processes whose participation in the algorithm is
//due to the reception of a START() message
class LeLannChangRobertsLeaderElectionProcessForUnidirectionalRingsVariant[V](val myself: ID[V]) extends Actor with ActorLogging{

  protected[leaderelection] var next:Option[ActorRef] = None
  protected[leaderelection] var idmax:Option[ID[V]] = None
  protected[leaderelection] var elected = false
  protected[leaderelection] var leader: Option[ID[V]] = None
  protected[leaderelection] var done = false

  def receive = {
    case Config(ref) => {
      log.debug(s"[CONFIG]> 'next' process reference provided => $ref")
      next = Option(ref)
    }
    case Start() if idmax.isEmpty => {
      log.info(s"\t> Start message has been received and yet no max identifier available")
      idmax=Some(myself)
      promoteElectionOf(myself)
    }
    case Start() => {
      log.info(s"\t> Election already started. Current max ID @> $idmax")
    }
    case Election(id) if idmax.isEmpty=> {
      idmax = Option(id.asInstanceOf[ID[V]])
      promoteElectionOf(id)
    }
    case Election(id) => {
      idmax.foreach(max => {
        val comparison = id.compareTo(max)

        if(comparison == 1){
          idmax=Option(id.asInstanceOf[ID[V]])
          promoteElectionOf(id)
        }
        else if(comparison == -1){
          //skip
        }
        else{
          elected=true
          notifyElectionOf(id)
        }

      })
    }
    case Elected(id) => {
      leader=Some(id.asInstanceOf[ID[V]])
      done=true

      if(myself != id) {
        elected=false
        notifyElectionOf(id)
      }
    }
  }

  def promoteElectionOf[V](id: ID[V]) = {
    next.map{ref =>
      log.info(s"\t> Promoting election of '$id'")
      ref ! Election(id)
    }.getOrElse(log.warning("\t> No 'next' process is available. Process election cannot be promoted"))
  }

  private def notifyElectionOf[V](id: ID[V]) = {
    next.map{ref =>
      log.info(s"\t> Election done. New leader is: '$id'")
      ref ! Elected(id)
    }.getOrElse(log.warning("\t> No 'next' process is available. Leader election cannot be notified"))
  }

}
//LeLann-Chang-Roberts algorithm
//Cost: O(n)^2
class LeLannChangRobertsLeaderElectionProcessForUnidirectionalRings[V](val myself: ID[V]) extends Actor with ActorLogging{

  protected[leaderelection] var participates = false
  protected[leaderelection] var elected = false
  protected[leaderelection] var leader: Option[ID[V]] = None
  protected[leaderelection] var done = false
  protected[leaderelection] var next:Option[ActorRef] = None

  def receive = {
    case Config(ref) => {
      log.debug(s"[CONFIG]> 'next' process reference provided => $ref")
      next = Option(ref)
    }
    case Start() if !participates => {
      log.debug(s"[START]> Starting election process")
      promoteElectionOf(myself)
    }
    case Election(id) => {

      val comparison = id.compareTo(myself)

      if(comparison == 1) promoteElectionOf(id)
      else if(comparison == -1){
        //We only send our election promotion if we don't participate in the current election yet
        if(!participates) promoteElectionOf(myself)
      }
      else {
        elected=true
        notifyElectionOf(id)
      }
    }
    case Elected(id) => {
      leader=Some(id.asInstanceOf[ID[V]])
      done=true

      if(myself != id) {
        elected=false
        notifyElectionOf(id)
      }
    }
  }

  private def promoteElectionOf[V](id: ID[V]) = {
    next.map{ref =>
      log.info(s"\t> Promoting election of '$id'")
      participates=true
      ref ! Election(id)
    }.getOrElse(log.warning("\t> No 'next' process is available. Process election cannot be promoted"))
  }

  private def notifyElectionOf[V](id: ID[V]) = {
    next.map{ref =>
      log.info(s"\t> Election done. New leader is: '$id'")
      ref ! Elected(id)
    }.getOrElse(log.warning("\t> No 'next' process is available. Leader election cannot be notified"))
  }

}
