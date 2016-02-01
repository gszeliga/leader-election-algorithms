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

  protected[leaderelection] var left = Option.empty[ActorRef]
  protected[leaderelection] var right = Option.empty[ActorRef]

  private var replies = BitSet(2)
  private var leader = Option.empty[ActorRef]

  private var elected = false
  private var done = false

  private val LEFT_SIDE = 1
  private val RIGHT_SIDE = 1

  def receive = {

    case Config(leftRef,rightRef) => {

      log.debug(s"> Configuration received  [${leftRef.path}] <= [$myself] => [${rightRef.path}]")

      left = Option(leftRef)
      right = Option(rightRef)
    }

    case Start() =>{

      log.info("> Starting election ...")

      left.foreach(_ ! Election(myself,0,1))
      right.foreach(_ ! Election(myself,0,1))
    }

    case Election(id,round,distance) => {
      val comparison = id.compareTo(myself)

      //Greater than
      if(comparison == 1){
        //If still did not cover all of our neighbours
        if(distance < (2^round)){
          //Continue in the same direction as the message came in broadcasting the election of incoming id
          oppositeSideTo(sender()) foreach(_ ! Election(id,round,distance+1))
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
        //Since the Election message visited all processes within the ring,
        // we've got elected! (notice how we start our elected cycle from the left)
        left foreach (_ ! Elected(id))
        elected = true
      }
    }

    case Reply(id, round) => {

      if(id != myself)
      {
        //Forward incoming message in the same direction
        oppositeSideTo(sender()) foreach (_ ! Reply(id,round))
      }
      else{
        //Did we already get a Reply message from the opposite side?
        // If so, we can start a new election round (we've got the
        // highest identity in both neighbourhoods of size 2^round
        if(gotReplyFrom(oppositeSideTo(sender()))) {
          left.foreach(_ ! Election(myself,round+1,1))
          right.foreach(_ ! Election(myself,round+1,1))
        }
        else
        {
          //Otherwise, we just flag the Reply message and wait for the opposite side
          markReplyFrom(sender())
        }
      }
    }

    //We only process election messages only if it comes from our right neighbour
    case Elected(id) if right.contains(sender())=> {

      leader = Option(id.asInstanceOf)
      done = true

      if(id != myself){
        elected = false
        //Forward elected leader message following the same direction
        left foreach(_ ! Elected(id))
      }

    }
  }

  private def oppositeSideTo(ref: ActorRef) = {
    if(left.contains(ref)) right
    else left
  }

  private def gotReplyFrom(ref: Option[ActorRef]) = {

    ref.exists(reference => {
      //If passed reference equals our left neighbour
      if (left.contains(reference))
        replies(LEFT_SIDE)
      else
        //Then check on our right neighbour
        replies(RIGHT_SIDE)
    })

  }

  private def markReplyFrom(ref: ActorRef) = {

    if(left.contains(ref))
      replies = replies + LEFT_SIDE
    else
      replies = replies + RIGHT_SIDE
  }

}
