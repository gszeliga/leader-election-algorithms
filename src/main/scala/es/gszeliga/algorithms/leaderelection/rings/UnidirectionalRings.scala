package es.gszeliga.algorithms.leaderelection.rings

import akka.actor.ActorSystem
import es.gszeliga.algorithms.leaderelection.DolevKlaweRodehLeaderElectionProcessForUnidirectionalRings
import es.gszeliga.algorithms.leaderelection.DolevKlaweRodehLeaderElectionProcessForUnidirectionalRings.{Start, Config}
import es.gszeliga.algorithms.leaderelection.rings.Ring.Designations._

import scala.util.Random

/**
  * Created by guillermo on 11/05/16.
  */
class UnidirectionalRings {

  private val integers: () => Int = Random.nextInt

  def main(args: Array[String]) {

    implicit val system = ActorSystem()

    val ring = Ring(5)(integers)(id => new UMemberProps[Int] {
      def props = DolevKlaweRodehLeaderElectionProcessForUnidirectionalRings.props(id)
    })

    ring.configure(assignment => Config(assignment.member.ref))
    ring.beginElectionWith(_ => Start())

  }

}
