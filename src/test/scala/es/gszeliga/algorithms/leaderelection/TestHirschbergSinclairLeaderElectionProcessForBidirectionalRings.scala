package es.gszeliga.algorithms.leaderelection

import akka.actor.ActorSystem
import akka.testkit.{TestActorRef, TestProbe, TestKit}
import es.gszeliga.algorithms.leaderelection.HirschbergSinclairLeaderElectionProcessForBidirectionalRings._
import org.scalatest.{FlatSpecLike, Matchers}

import scala.concurrent.duration._

/**
  * Created by guillermo on 1/02/16.
  */
class TestHirschbergSinclairLeaderElectionProcessForBidirectionalRings extends TestKit(ActorSystem("test-actor-system")) with FlatSpecLike with Matchers {

  val leftNeighbour = new TestProbe(system,"left-neighbour")
  val rightNeighbour = new TestProbe(system, "right-neighbour")

  val process = TestActorRef(new HirschbergSinclairLeaderElectionProcessForBidirectionalRings(20))

  "A Hirschberg/Sinclair’s election actor" should "properly be configured" in {

    process ! Config(leftNeighbour.ref,rightNeighbour.ref)

    process.underlyingActor.left shouldBe Some(leftNeighbour.ref)
    process.underlyingActor.right shouldBe Some(rightNeighbour.ref)

  }

  it should "properly start a new election by notifying both neighbours with round=0 and distance=1" in {

    process ! Config(leftNeighbour.ref,rightNeighbour.ref)
    process ! Start()

    leftNeighbour.expectMsg(Election(20,0,1))
    rightNeighbour.expectMsg(Election(20,0,1))
  }

  it should "properly forward an Election message from LEFT to RIGHT if incoming ID is greater than current one by increasing distance in one" in {

    implicit val sender = leftNeighbour.ref

    process ! Config(leftNeighbour.ref,rightNeighbour.ref)
    process ! Election(50,1,1)

    rightNeighbour.expectMsg(1 seconds,"No Election message was forwarded to the RIGHT side including greater ID and increased distance", Election(50,1,2))
    leftNeighbour.expectNoMsg()

  }

  it should "properly forward an Election message from RIGHT to LEFT if incoming ID is greater than current one by increasing distance in one" in {

    implicit val sender = rightNeighbour.ref

    process ! Config(leftNeighbour.ref,rightNeighbour.ref)
    process ! Election(50,1,1)

    leftNeighbour.expectMsg(1 seconds,"No Election message was forwarded to the LEFT side including greater ID and increased distance",Election(50,1,2))
    rightNeighbour.expectNoMsg()

  }

  it should "properly back to left sender REPLY(id, ROUND) when 2^ROUND number of neighbours have been visited by an Election message" in {

    implicit val sender = leftNeighbour.ref

    process ! Config(leftNeighbour.ref, rightNeighbour.ref)
    process ! Election(50,0,1)

    leftNeighbour.expectMsg(1 seconds, "No REPLY message was received after round is done", Reply(50,0))
    rightNeighbour.expectNoMsg()

  }

  it should "properly back to right sender REPLY(id, ROUND) when 2^ROUND number of neighbours have been visited by an Election message" in {

    implicit val sender = rightNeighbour.ref

    process ! Config(leftNeighbour.ref, rightNeighbour.ref)
    process ! Election(50,0,1)

    rightNeighbour.expectMsg(1 seconds, "No REPLY message was received after round is done", Reply(50,0))
    leftNeighbour.expectNoMsg()

  }

  it should "properly stop progress of a lower ID election when received" in {

    implicit val sender = rightNeighbour.ref

    process ! Config(leftNeighbour.ref, rightNeighbour.ref)
    process ! Election(5,0,1)

    rightNeighbour.expectNoMsg()
    leftNeighbour.expectNoMsg()

  }

  it should "send ELECTED message to the LEFT side neighbour when incoming ID from the RIGHT equals process identifier" in {

    implicit val sender = rightNeighbour.ref

    process ! Config(leftNeighbour.ref, rightNeighbour.ref)
    process ! Election(20,0,1)

    leftNeighbour.expectMsg(1 second, "An ELECTED message was expected to be sent to the LEFT neighbour", Elected(20))
    rightNeighbour.expectNoMsg()

  }

  it should "send ELECTED message to the LEFT side neighbour when incoming ID from the LEFT equals process identifier" in {

    implicit val sender = leftNeighbour.ref

    process ! Config(leftNeighbour.ref, rightNeighbour.ref)
    process ! Election(20,0,1)

    leftNeighbour.expectMsg(1 second, "An ELECTED message was expected to be sent to the LEFT neighbour", Elected(20))
    rightNeighbour.expectNoMsg()

  }

  it should "properly forward REPLY message to RIGHT side when current process it's not the final destination (meaning has not the same ID value)" in {

    implicit val sender = leftNeighbour.ref

    process ! Config(leftNeighbour.ref, rightNeighbour.ref)
    process ! Reply(45,0)

    rightNeighbour.expectMsg(1 second, "REPLY message was expected to be forward to the RIGHT side" , Reply(45,0))
    leftNeighbour.expectNoMsg()

  }

  it should "properly forward REPLY message to LEFT side when current process it's not the final destination (meaning has not the same ID value)" in {

    implicit val sender = rightNeighbour.ref

    process ! Config(leftNeighbour.ref, rightNeighbour.ref)
    process ! Reply(45,0)

    leftNeighbour.expectMsg(1 second, "REPLY message was expected to be forward to the RIGHT side" , Reply(45,0))
    rightNeighbour.expectNoMsg()

  }

  it should "properly mark REPLY reception from LEFT but still wait for a second one from the opposite side to initiate an ELECTION" in {

    implicit val sender = leftNeighbour.ref

    process ! Config(leftNeighbour.ref, rightNeighbour.ref)
    process ! Reply(20,0)
    process ! Reply(20,0)
    process ! Reply(20,0)

    rightNeighbour.expectNoMsg()
    leftNeighbour.expectNoMsg()
  }

  it should "properly mark REPLY reception from RIGHT but still wait for a second one from the opposite side to initiate an ELECTION" in {

    implicit val sender = rightNeighbour.ref

    process ! Config(leftNeighbour.ref, rightNeighbour.ref)
    process ! Reply(20,0)
    process ! Reply(20,0)
    process ! Reply(20,0)

    rightNeighbour.expectNoMsg()
    leftNeighbour.expectNoMsg()
  }

  it should "properly initiate a new ELECTION round after both neighbours on both sides reply back with a REPLY message" in {

    process ! Config(leftNeighbour.ref, rightNeighbour.ref)

    process.!(Reply(20,0))(rightNeighbour.ref)
    process.!(Reply(20,0))(leftNeighbour.ref)

    rightNeighbour.expectMsg(1 second, "An ELECTION round was expected to be initiated on the RIGHT but it didn't", Election(20,1,1))
    leftNeighbour.expectMsg(1 second, "An ELECTION round was expected to be initiated on the LEFT but it didn't", Election(20,1,1))
  }

  it must "assign himself as LEADER after receiving an ELECTION message from right neighbour with its own identifier" in {

    implicit val sender = rightNeighbour.ref

    process ! Config(leftNeighbour.ref, rightNeighbour.ref)
    process ! Elected(20)

    process.underlyingActor.leader shouldBe Some(20)
    process.underlyingActor.done shouldBe true

    leftNeighbour.expectNoMsg()

  }

  it must "should forward an ELECTED message to the LEFT when incoming process identifier does not match its own" in {

    implicit val sender = rightNeighbour.ref

    process ! Config(leftNeighbour.ref, rightNeighbour.ref)
    process ! Elected(5)

    process.underlyingActor.leader shouldBe Some(5)
    process.underlyingActor.done shouldBe true

    leftNeighbour.expectMsg(1 second, "The ELECTED message with new leader was not forwarded to the LEFT as expected", Elected(5))

  }

}
