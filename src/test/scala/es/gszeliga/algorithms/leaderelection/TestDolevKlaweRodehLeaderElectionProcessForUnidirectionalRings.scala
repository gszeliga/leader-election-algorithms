package es.gszeliga.algorithms.leaderelection

import akka.actor.ActorSystem
import akka.testkit.{TestActorRef, TestProbe, TestKit}
import es.gszeliga.algorithms.leaderelection.DolevKlaweRodehLeaderElectionProcessForUnidirectionalRings.{Election, Start, Config}
import org.scalatest.{FlatSpecLike, Matchers}

import scala.concurrent.duration._

/**
  * Created by guillermo on 21/04/16.
  */
class TestDolevKlaweRodehLeaderElectionProcessForUnidirectionalRings extends TestKit(ActorSystem("test-actor-system")) with FlatSpecLike with Matchers {

  val next = new TestProbe(system,"probe")

  val process = TestActorRef(new DolevKlaweRodehLeaderElectionProcessForUnidirectionalRings[Int](5))

  "A Dolev, Klawe, and Rodeh’s election actor" should "properly get configured" in {

    process ! Config(next.ref)

    process.underlyingActor.next shouldBe Some(next.ref)

  }

  it should "initialize 'competitor' and 'maxid' right after receiving a START message" in {

    process ! Config(next.ref)
    process ! Start()

    process.underlyingActor.competitor shouldBe true
    process.underlyingActor.maxid shouldBe 5

  }

  it should "send an ELECTION message right after receiving a START message" in {

    process ! Config(next.ref)
    process ! Start()

    next.expectMsg(2 second, "No ELECTION(1, x) message was emitted after a START message", Election(1, 5))
  }

  it should "forward incoming ELECTION(1,x) message when no longer a competitor" in {

    process ! Config(next.ref)
    process ! Start()

    //Disregard initial ELECTION(1,x) message
    next.receiveOne(2 seconds)

    //Get the actor out of competition
    process.underlyingActor.competitor=false

    val msgToBeForwarded = Election(1,15)

    process ! msgToBeForwarded

    next.expectMsg(2 seconds, "Incoming ELECTION(1,x) message was not forwarded as expected", msgToBeForwarded)

  }

}
