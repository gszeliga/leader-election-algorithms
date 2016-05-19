package es.gszeliga.algorithms.leaderelection.rings

import akka.actor.ActorSystem
import es.gszeliga.algorithms.leaderelection.HirschbergSinclairLeaderElectionProcessForBidirectionalRings
import es.gszeliga.algorithms.leaderelection.HirschbergSinclairLeaderElectionProcessForBidirectionalRings.{Config, Start}
import es.gszeliga.algorithms.leaderelection.rings.Ring.Assignments._
import es.gszeliga.algorithms.leaderelection.rings.Ring.Props.bidirectional

import scala.util.Random

/**
  * Created by guillermo on 18/05/16.
  */
object BidirectionalRings {

  private val integers: () => Int = Random.nextInt

  def main(args: Array[String]) {

    implicit val system = ActorSystem()

    val ring = Ring(25)(integers)(id => bidirectional(HirschbergSinclairLeaderElectionProcessForBidirectionalRings.props(id)))

    ring.configure(assignment => Config(assignment.left.ref, assignment.right.ref))
    ring.begin(_ => Start())

  }

}
