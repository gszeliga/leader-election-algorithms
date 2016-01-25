package es.gszeliga.algorithms.leaderelection

import akka.actor.{Props, ActorRef, ActorLogging, Actor}
import es.gszeliga.algorithms.leaderelection.SimpleLeaderElectionProcessForBidirectionalRings._

import scala.collection.BitSet

/**
  * Created by guillermo on 25/01/16.
  */


object SimpleLeaderElectionProcessForBidirectionalRings{

  type ID[V] = Comparable[V]

  sealed case class Config(left: ActorRef, right: ActorRef)
  sealed case class Start()
  sealed case class Election[V](id: ID[V], round: Int, distance: Int)
  sealed case class Reply[V](id: ID[V], round: Int)
  sealed case class Elected[V](id: ID[V])

  def props[V](pid: ID[V]) = Props(new SimpleLeaderElectionProcessForBidirectionalRings[V](pid))

}

//Hirschberg and Sinclair’s election algorithm
class SimpleLeaderElectionProcessForBidirectionalRings[V](val myself: ID[V]) extends Actor with ActorLogging{

  private var left = Option.empty[ActorRef]
  private var right = Option.empty[ActorRef]
  private var elected = false
  private var replies = BitSet(2)

  def receive = {

    case Config(leftRef,rightRef) => {
      left = Option(leftRef)
      right = Option(rightRef)
    }

    case Start() =>{
      left.foreach(_ ! Election(myself,0,1))
      right.foreach(_ ! Election(myself,0,1))
    }

    case Election(id,round,distance) => {
      val comparison = id.compareTo(myself)

      //Greater than
      if(comparison == 1){
        //If still did not cover all of our neighbours
        if(distance < (2^round)){
          //Continue in the same direction as the message came broadcasting the election of incoming id
          oppositeTo(sender()) foreach(ref => ref ! Election(id,round,distance+1))
        }
        else
        {
          //It already visited all neighbours on this side, so we need to stop propagating the message by replying back!
          sender() ! Reply(id, round)
        }
      }
      //
      else if(comparison == -1){
        //Process with identifier 'id' cannot be elected, so we stop propagating its election
      }
      else
      {
        //We've got elected!
        sender() ! Elected(id)
        elected = true
      }
    }

    case Reply(id, round) => {

      if(id != myself)
      {
        //Forward incoming message in the same direction
        oppositeTo(sender()) foreach (_ ! Reply(id,round))
      }
      else{
        if(gotReplyFrom(oppositeTo(sender()))) {
          left.foreach(ref => ref ! Election(myself,round+1,1))
          right.foreach(ref => ref ! Election(myself,round+1,1))
        }
        else
        {
          markReplyFrom(sender())
        }

      }
    }
  }

  private def oppositeTo(ref: ActorRef) = {
    if(ref == left.get) right
    else left
  }

  private def gotReplyFrom(ref: Option[ActorRef]) = {

    ref.exists(r => {
      if (r == left.get)
        replies(1)
      else
        replies(0)
    })

  }

  private def markReplyFrom(ref: ActorRef) = {

    if(ref == left.get)
      replies = replies + 1
    else
      replies = replies + 0
  }

}
