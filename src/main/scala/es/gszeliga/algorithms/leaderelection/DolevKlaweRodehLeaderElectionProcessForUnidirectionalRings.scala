package es.gszeliga.algorithms.leaderelection

import akka.actor.{ActorLogging, Actor, Props, ActorRef}
import DolevKlaweRodehLeaderElectionProcessForUnidirectionalRings._

/**
  * Created by guillermo on 12/04/16.
  */

//Dolev, Klawe, and Rodeh’s election algorithm
//Cost: O(n log n)
object DolevKlaweRodehLeaderElectionProcessForUnidirectionalRings {

  type ID[V] = Ordered[V]

  case class Config(next: ActorRef)
  case class Start()
  case class Election[V](round: Int, id: ID[V])
  case class Elected[V](whoIsElected: ID[V], whoEmitted: ID[V])

  def props[V](pid: ID[V])(implicit ordering: Ordering[ID[V]]) = Props(new DolevKlaweRodehLeaderElectionProcessForUnidirectionalRings[V](pid))

}

class DolevKlaweRodehLeaderElectionProcessForUnidirectionalRings[V](val myself: ID[V])(implicit val ordering: Ordering[ID[V]]) extends Actor with ActorLogging{

  private var next = Option.empty[ActorRef]

  /*
  * Indicates if p i is currently competing on behalf of some process identity or is only relaying messages.
  * The two other local variables are meaningful only when competitor Pi is equal to true.
  * */
  private var competitor = false

  /*
  * Greatest identity known by Pi
  * */
  private var maxid = myself

  /*
  * Identity of the process for which Pi is competing
  * */
  private var proxy_for = Option.empty[ID[V]]

  private var leader = Option.empty[ID[V]]
  private var done = false
  private var elected = false

  def receive = {
    case Config(ref) => next = Option(ref)

    case Start() => {
      competitor=true
      next foreach (_ ! Election(1, myself))
    }

    case m @ Election(1,id) => {

      //If it's no longer a competitor then just forwards the message to the next outgoing channel
      if(!competitor) next foreach(_ ! m)
      else if(id != maxid)
      {
        //Forward the message to the next outgoing channel
        next foreach(_ ! Election(2,id))
        //Grab identity of the process for which we're competing
        proxy_for = Option(id.asInstanceOf[ID[V]])
      }
      else
      {
        //Incoming message has made a full turn on the ring, and consequently 'maxid' is the greatest identity
        next foreach (_ ! Elected(id, myself))
      }
    }

    case m @ Election(2,id) => {

      def max(v1: ID[V], v2: ID[V]): ID[V] = {

        if(v1 > v2) v1
        else if(v1 < v2) v2
        else v1
      }

      //Forward the message if we're no longer a competitor
      if(!competitor) next foreach (_ ! m)
      else if(proxy_for exists (_ > max(id.asInstanceOf[ID[V]], maxid))) {

        //Update 'maxid' and start a new round
        proxy_for foreach (proxy_ref => {
          maxid = proxy_ref
          next foreach (_ ! Election(1, proxy_ref))
        })

      }
        //Since 'proxy_for' is not the highest identity, we stop competing
      else competitor = false

    }

    case m @ Elected(whoIsElected,whoEmitted) => {

      leader = Option(whoIsElected.asInstanceOf[ID[V]])
      done = true
      elected = whoIsElected == myself

      //If not the one who emitted the elected message (meaning it didn't make a full turn)
      if(whoEmitted != myself) next foreach(_ ! m)

    }
  }
}
