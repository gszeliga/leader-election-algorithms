package es.gszeliga.algorithms.leaderelection

import akka.actor.ActorSystem
import akka.testkit.{TestActorRef, TestKit}
import es.gszeliga.algorithms.leaderelection.LeLannChangRobertsLeaderElectionProcessForUnidirectionalRings.{Elected, Config, Election, Start}
import org.scalatest.{FlatSpecLike, Matchers}

/**
  * Created by guillermo on 12/01/16.
  */
class TestLeLannChangRobertsLeaderElectionProcessForUnidirectionalRings extends TestKit(ActorSystem("test")) with FlatSpecLike with Matchers {

  val process = TestActorRef(new LeLannChangRobertsLeaderElectionProcessForUnidirectionalRings(20))

  "A simple-election process" should "properly be configured" in {
    process ! Config(testActor)
    process.underlyingActor.next shouldBe Some(testActor)
  }

  it should "start election by promoting himself" in {
    process ! Config(testActor)
    process ! Start()

    expectMsg(Election(20))
  }

  it should "not promote itself again after receiving an ID less than the one assigned with a 'Start' already received" in {
    process ! Config(testActor)
    process ! Start()

    expectMsg(Election(20))

    process ! Election(16)

    expectNoMsg()
  }

  it should "stop promoting itself after receiving an ID greater than the one assigned" in {
    process ! Config(testActor)
    process ! Start()

    expectMsg(Election(20))

    process ! Election(53)

    expectMsg(Election(53))
  }

  it should "not promote itself after receiving an ID greater than the one assigned despite 'Start' not being sent beforehand" in {
    process ! Config(testActor)
    process ! Election(53)

    expectMsg(Election(53))
  }

  it should "not promote itself after receiving an ID less than the one assigned despite 'Start' not being sent beforehand" in {
    process ! Config(testActor)
    process ! Election(17)

    expectMsg(Election(20))
  }

  it should "get elected when own ID is received back" in {
    process ! Config(testActor)
    process ! Election(20)

    expectMsg(Elected(20))
    process.underlyingActor.elected shouldBe true
  }

  it should "finalize election when own ID is elected" in {
    process ! Config(testActor)
    process ! Elected(20)

    expectNoMsg()

    process.underlyingActor.leader shouldBe Some(20)
    process.underlyingActor.done shouldBe true
  }

  it should "register new leader and keep promoting it after election is done" in {

    process ! Config(testActor)
    process ! Elected(52)

    expectMsg(Elected(52))

    process.underlyingActor.leader shouldBe Some(52)
    process.underlyingActor.done shouldBe true

  }

}
