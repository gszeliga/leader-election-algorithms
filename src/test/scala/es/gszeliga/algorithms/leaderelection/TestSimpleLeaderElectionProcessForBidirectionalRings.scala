package es.gszeliga.algorithms.leaderelection

import akka.actor.ActorSystem
import akka.testkit.{TestActorRef, TestProbe, TestKit}
import es.gszeliga.algorithms.leaderelection.SimpleLeaderElectionProcessForBidirectionalRings.{Election, Start, Config}
import org.scalatest.{FlatSpecLike, Matchers}

/**
  * Created by guillermo on 1/02/16.
  */
class TestSimpleLeaderElectionProcessForBidirectionalRings extends TestKit(ActorSystem("bidirectional-ring")) with FlatSpecLike with Matchers {

  val leftNeighbour = new TestProbe(system,"left-neighbour")
  val rightNeighbour = new TestProbe(system, "right-neighbour")

  val process = TestActorRef(new SimpleLeaderElectionProcessForBidirectionalRings(20))

  "A Hirschberg/Sinclair’s election algorithm" should "properly be configured" in {

    process ! Config(leftNeighbour.ref,rightNeighbour.ref)

    process.underlyingActor.left shouldBe Some(leftNeighbour.ref)
    process.underlyingActor.right shouldBe Some(rightNeighbour.ref)

  }

  it should "properly start a new election by notifying both neighbours" in {

    process ! Config(leftNeighbour.ref,rightNeighbour.ref)
    process ! Start()

    leftNeighbour.expectMsg(Election(20,0,1))
    rightNeighbour.expectMsg(Election(20,0,1))
  }

}
