package es.gszeliga.algorithms.leaderelection

import akka.actor.ActorSystem
import akka.testkit.{TestActorRef, TestProbe, TestKit}
import es.gszeliga.algorithms.leaderelection.DolevKlaweRodehLeaderElectionProcessForUnidirectionalRings.{Elected, Election, Start, Config}
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

  it should "send an ELECTION(1, x) message right after receiving a START message" in {

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

  it should "emit and ELECTED(maxid,id) message after ELECTION(1,id) is such that id == maxid" in {

    process ! Config(next.ref)
    process ! Start()

    //Disregard initial ELECTION(1,x) message
    next.receiveOne(2 seconds)

    //Set the maximum identifer seen so far
    process.underlyingActor.maxid=25

    //Send the ELECTED message simulating a full turn on the ring
    process ! Election(1,25)

    next.expectMsg(1 second, "An ELECTED message was expected to be emited after a full turn to the ring", Elected(25,5))

  }

  it should "emit an ELECTION(2,x) message and update 'proxy' reference after ELECTION(1,id) is such that id != maxid" in {

    process ! Config(next.ref)
    process ! Start()

    //Disregard initial ELECTION(1,x) message
    next.receiveOne(2 seconds)

    //Send a different max identifier different that the currently assigned one
    process ! Election(1,7)

    next.expectMsg(1 second, "An ELECTION(2,x) message was not emitted as expected", Election(2, 7))
    process.underlyingActor.proxy_for shouldBe Some(7)

  }

  it should "forward an ELECTION(2,x) message when process is no longer a competitor" in {

    process ! Config(next.ref)
    process ! Start()

    //Disregard initial ELECTION(1,x) message
    next.receiveOne(2 seconds)

    //Make process no longer a competitor
    process.underlyingActor.competitor=false

    val messageToBeForwarded = Election(2, 10)

    //Send message to be forwarded
    process ! messageToBeForwarded

    next.expectMsg(1 second, "Incoming message was not forwarded despite not being no longer a competitor", messageToBeForwarded)

  }

  it should "update 'max_id' with 'proxy_id' after receiving an ELECTION(2,id) message where proxy_id > max(max_id,id) " in {

    process ! Config(next.ref)
    process ! Start() // This will cause 'max_id' to be initialized with process identifier

    //Disregard initial ELECTION(1,x) message
    next.receiveOne(2 seconds)

    //Force a proxy reference to be updated
    process ! Election(1,10)

    //Disregard initial ELECTION(2,10) message
    next.receiveOne(2 seconds)

    //Let's see if 'max_id' gets updated
    process ! Election(2,9)

    process.underlyingActor.maxid shouldBe 10

  }

  it should "initiate a new election round after receiving an ELECTION(2,id) message where proxy_id > max(max_id,id) " in {

    process ! Config(next.ref)
    process ! Start() // This will cause 'max_id' to be initialized with process identifier

    //Disregard initial ELECTION(1,x) message
    next.receiveOne(2 seconds)

    //Force a proxy reference to be updated
    process ! Election(1,10)

    //Disregard initial ELECTION(2,10) message
    next.receiveOne(2 seconds)

    //Let's see if 'max_id' gets updated
    process ! Election(2,9)

    next.expectMsg(1 second, "A new round should've been started", Election(1, 10))

  }

  it should "stop competing after receiving an ELECTION(2,id) message where proxy_id <= max(max_id,id)" in {

    process ! Config(next.ref)
    process ! Start() // This will cause 'max_id' to be initialized with process identifier

    //Disregard initial ELECTION(1,x) message
    next.receiveOne(2 seconds)

    //Force a proxy reference to be updated
    process ! Election(1,10)

    //Disregard initial ELECTION(2,10) message
    next.receiveOne(2 seconds)

    //It should make it stop competing since its proxy is smaller and cannot keep competing for being elected
    process ! Election(2,25)

    process.underlyingActor.competitor shouldBe false

  }

  it should "register newly elected leader after receiving ELECTED(id,x) message " in {

    process ! Config(next.ref)
    process ! Start() // This will cause 'max_id' to be initialized with process identifier

    //Disregard initial ELECTION(1,x) message
    next.receiveOne(2 seconds)

    process ! Elected(5,10)

    process.underlyingActor.leader shouldBe Some(5)

  }

  it should "register current process as elected after receiving ELECTED(id,x) message such that id == process identifier" in {

    process ! Config(next.ref)
    process ! Start() // This will cause 'max_id' to be initialized with process identifier

    //Disregard initial ELECTION(1,x) message
    next.receiveOne(2 seconds)

    process ! Elected(5,10)

    process.underlyingActor.elected shouldBe true
    process.underlyingActor.done shouldBe true

  }

  it should "not forward ELECTED(id,id2) message when id2 == current process identifier" in {

    process ! Config(next.ref)
    process ! Start() // This will cause 'max_id' to be initialized with process identifier

    //Disregard initial ELECTION(1,x) message
    next.receiveOne(2 seconds)

    process ! Elected(5,5)

    next.expectNoMsg(2 seconds)
  }

  it should "forward ELECTED(id,id2) message when id2 != current process identifier" in {

    process ! Config(next.ref)
    process ! Start() // This will cause 'max_id' to be initialized with process identifier

    //Disregard initial ELECTION(1,x) message
    next.receiveOne(2 seconds)

    process ! Elected(5,10)

    next.expectMsg(1 second, "Incoming ELECTED() message was expected to be forwarded since receiver is not the elected leader", Elected(5,10))
  }

}
