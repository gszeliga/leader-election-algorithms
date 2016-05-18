package es.gszeliga.algorithms.leaderelection.rings

import akka.actor.ActorSystem
import es.gszeliga.algorithms.leaderelection.HirschbergSinclairLeaderElectionProcessForBidirectionalRings
import es.gszeliga.algorithms.leaderelection.HirschbergSinclairLeaderElectionProcessForBidirectionalRings.{Config, Start}
import es.gszeliga.algorithms.leaderelection.rings.Ring.Designations._

import scala.util.Random

/**
  * Created by guillermo on 18/05/16.
  */
object BidirectionalRings {

  private val integers: () => Int = Random.nextInt

  def main(args: Array[String]) {

    implicit val system = ActorSystem()

    val ring = Ring(25)(integers)(id => new BMemberProps[Int] {
      def props = HirschbergSinclairLeaderElectionProcessForBidirectionalRings.props(id)
    })

    ring.configure(designation => Config(designation.left.ref, designation.right.ref))
    ring.begin(_ => Start())

  }

}
