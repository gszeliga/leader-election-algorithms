package es.gszeliga.algorithms.leaderelection.rings

import akka.actor.ActorSystem
import es.gszeliga.algorithms.leaderelection.DolevKlaweRodehLeaderElectionProcessForUnidirectionalRings
import es.gszeliga.algorithms.leaderelection.DolevKlaweRodehLeaderElectionProcessForUnidirectionalRings.{Start, Config}
import es.gszeliga.algorithms.leaderelection.rings.Ring.Assignments._
import es.gszeliga.algorithms.leaderelection.rings.Ring.Props._

import scala.util.Random

/**
  * Created by guillermo on 11/05/16.
  */
object UnidirectionalRings {

  private val integers: () => Int = Random.nextInt

  def main(args: Array[String]) {

    implicit val system = ActorSystem()

    val ring = Ring(3)(integers)(id => unidirectional(DolevKlaweRodehLeaderElectionProcessForUnidirectionalRings.props(id)))

    ring.configure(assignment => Config(assignment.member.ref))
    ring.begin(_ => Start())

  }

}
