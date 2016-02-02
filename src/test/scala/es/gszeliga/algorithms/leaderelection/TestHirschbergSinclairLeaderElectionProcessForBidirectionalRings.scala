package es.gszeliga.algorithms.leaderelection

import akka.actor.ActorSystem
import akka.testkit.{TestActorRef, TestProbe, TestKit}
import es.gszeliga.algorithms.leaderelection.HirschbergSinclairLeaderElectionProcessForBidirectionalRings.{Election, Start, Config}
import org.scalatest.{FlatSpecLike, Matchers}

import scala.concurrent.duration._

/**
  * Created by guillermo on 1/02/16.
  */
class TestHirschbergSinclairLeaderElectionProcessForBidirectionalRings extends TestKit(ActorSystem("bidirectional-ring")) with FlatSpecLike with Matchers {

  val leftNeighbour = new TestProbe(system,"left-neighbour")
  val rightNeighbour = new TestProbe(system, "right-neighbour")

  val process = TestActorRef(new HirschbergSinclairLeaderElectionProcessForBidirectionalRings(20))

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

  it should "properly forward an Election message from left to right if incoming ID is greater than current one" in {

    implicit val sender = leftNeighbour.ref

    process ! Config(leftNeighbour.ref,rightNeighbour.ref)
    process ! Election(50,0,1)

    rightNeighbour.expectMsg(1 seconds,"No Election message was forwarded to the RIGHT side including greater ID and increased distance", Election(50,0,2))
    leftNeighbour.expectNoMsg()

  }

  it should "properly forward an Election message from right to left increasing distance" in {

    implicit val sender = rightNeighbour.ref

    process ! Config(leftNeighbour.ref,rightNeighbour.ref)
    process ! Election(50,0,1)

    leftNeighbour.expectMsg(1 seconds,"No Election message was forwarded to the LEFT side including greater ID and increased distance",Election(50,0,2))
    rightNeighbour.expectNoMsg()

  }

}
