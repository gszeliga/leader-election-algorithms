package es.gszeliga.algorithms.leaderelection

import akka.actor.ActorSystem
import akka.testkit.{TestActorRef, TestKit}
import es.gszeliga.algorithms.leaderelection.SimpleElectionProcess.{Config, Election, Start}
import org.scalatest.{FlatSpecLike, Matchers}

/**
  * Created by guillermo on 12/01/16.
  */
class TestSimpleElectionProcess extends TestKit(ActorSystem("test")) with FlatSpecLike with Matchers {

  val process = TestActorRef(new SimpleElectionProcess(20))

  "A simple-election process" should "properly be configured" in {
    process ! Config(testActor)
    process.underlyingActor.next shouldBe Some(testActor)
  }

  it should "start election when requested" in {
    process ! Config(testActor)
    process ! Start()

    expectMsg(Election(20))
  }

}
