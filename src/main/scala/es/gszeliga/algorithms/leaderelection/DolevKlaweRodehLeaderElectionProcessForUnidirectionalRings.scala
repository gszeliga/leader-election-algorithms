package es.gszeliga.algorithms.leaderelection

import akka.actor.{ActorLogging, Actor, Props, ActorRef}
import DolevKlaweRodehLeaderElectionProcessForUnidirectionalRings._

/**
  * Created by guillermo on 12/04/16.
  */

//Dolev, Klawe, and Rodeh’s election algorithm
//Cost: O(n log n)
object DolevKlaweRodehLeaderElectionProcessForUnidirectionalRings {

  case class Config(next: ActorRef)
  case class Start()
  case class Election[ID](round: Int, id: ID)
  case class Elected[ID](whoIsElected: ID, whoEmitted: ID)

  def props[ID](pid: ID)(implicit ordering: Ordering[ID]) = Props(new DolevKlaweRodehLeaderElectionProcessForUnidirectionalRings[ID](pid))

}

class DolevKlaweRodehLeaderElectionProcessForUnidirectionalRings[ID](val myself: ID)(implicit val ordering: Ordering[ID]) extends Actor with ActorLogging{

  protected[leaderelection] var next = Option.empty[ActorRef]

  /*
  * Indicates if Pi is currently competing on behalf of some process identity or is only relaying messages.
  * The two other local variables are meaningful only when competitor Pi is equal to true.
  * */
  protected[leaderelection] var competitor = false

  /*
  * Greatest identity known by Pi
  * */
  protected[leaderelection] var maxid = myself

  /*
  * Identity of the process for which Pi is competing
  * */
  private[leaderelection] var proxy_for = Option.empty[ID]

  private[leaderelection] var leader = Option.empty[ID]
  private[leaderelection] var done = false
  private[leaderelection] var elected = false

  def receive = {
    case Config(ref) => {

      log.debug(s"> [id: $myself] Configuration received [next] >> [${ref.path}]")

      next = Option(ref)
    }

    case Start() => {
      competitor=true
      next foreach (_ ! Election(1, myself))

      log.debug(s"> [id: $myself] An election got started!")

    }

    case m @ Election(1,id) => {

      //If it's no longer a competitor then just forwards the message to the next outgoing channel
      if(!competitor) {

        log.debug(s"[id: $myself] No longer a competitor. Forwarding [$m] >> [${next map (_.path)}")

        next foreach(_ ! m)
      }
      else if(id != maxid)
      {
        log.debug(s"> [id: $myself] Becoming proxy of process [$id] from now on. ELECTION(2,$id) message will be emitted")

        //Forward the message to the next outgoing channel
        next foreach(_ ! Election(2,id))

        //Grab identity of the process for which we're competing
        proxy_for = Option(id.asInstanceOf[ID])
      }
      else
      {
        log.info(s"> [id: $myself] Process [$id] got elected!!. Forwarding newly elected process to the remaining members of the ring")

        //Incoming message has made a full turn on the ring, and consequently 'maxid' is the greatest identity
        next foreach (_ ! Elected(id, myself))
      }
    }

    case m @ Election(2,id) => {

      def max(v1: ID, v2: ID): ID = {

        if(ordering.gt(v1,v2)) v1
        else if(ordering.lt(v1,v2)) v2
        else v1
      }

      //Forward the message if we're no longer a competitor
      if(!competitor) {

        log.debug(s"> [id: $myself] No longer a competitor. Forwarding [$m] >> [${next map (_.path)}")

        next foreach (_ ! m)
      }
      else if(proxy_for exists(proxy_id => ordering.gt(proxy_id,max(id.asInstanceOf[ID], maxid)))) {

        log.debug(s"> [id: $myself] Proxy[$proxy_for] is greater than max(incoming=$id, max_id=$maxid). Updating 'max_id' and initiating new election round")

        //Update 'maxid' and start a new round
        proxy_for foreach (proxy_ref => {
          maxid = proxy_ref
          next foreach (_ ! Election(1, proxy_ref))
        })

      }
        //Since 'proxy_for' is not the highest identity, we stop competing
      else {

        log.debug(s"> [id: $myself] No longer a competitor since Proxy[$proxy_for] smaller than max(incoming=$id, max_id=$maxid)")

        competitor = false
      }

    }

    case m @ Elected(whoIsElected,whoEmitted) => {

      leader = Option(whoIsElected.asInstanceOf[ID])
      elected = ordering.equiv(whoIsElected.asInstanceOf[ID],myself)
      done = true

      log.debug(s"> [id: $myself] Election finished [leader: $leader, elected: $elected]")

      //If not the one who emitted the elected message (meaning it didn't make a full ring turn yet) then
      // we forward the elected identifier to the remaining members
      if(whoEmitted != myself) {

        log.debug(s"> [id: $myself] Forwarding $m message to other fellows in the ring")

        next foreach(_ ! m)
      }

    }
  }
}
