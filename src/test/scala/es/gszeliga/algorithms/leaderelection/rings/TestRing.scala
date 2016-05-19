package es.gszeliga.algorithms.leaderelection.rings

import java.util.concurrent.TimeUnit.SECONDS
import java.util.concurrent.{CopyOnWriteArraySet, CountDownLatch}

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.testkit.TestKit
import es.gszeliga.algorithms.leaderelection.rings.Ring.Assignment
import es.gszeliga.algorithms.leaderelection.rings.Ring.Props.unidirectional
import org.scalatest.{FlatSpecLike, Matchers}

import scala.collection.JavaConversions._
import scala.util.Random

/**
  * Created by guillermo on 9/05/16.
  */
class TestRing extends TestKit(ActorSystem("test-actor-system")) with FlatSpecLike with Matchers {

  private val integers: () => Int = Random.nextInt

  case class Config(ref: ActorRef)

  case class Start()

  implicit val unidirectionalDesignation: Assignment[Int, Unidirectional, UAssignment[Int]] = members => members.map(m => (m, UAssignment(m)))

  class ShallowActor[ID](val id: ID) extends Actor {
    def receive = {
      case _ =>
    }
  }

  "Ring" should "have an specific number of nodes" in {

    val ring = Ring(10)(integers) { id => unidirectional(Props(new ShallowActor(id)))}

    ring.size shouldBe 10
  }

  it should "create as many nodes as requested" in {

    val ring = Ring(10)(integers) { id => unidirectional(Props(new ShallowActor(id)))}

    ring.members should have length 10
  }

  it should "properly configure all unidirectional members when requested" in {

    val references = new CopyOnWriteArraySet[ActorRef]()
    val latch = new CountDownLatch(10)

    class ConfigCollector[ID](val id: ID, var refs: java.util.Set[ActorRef], val latch: CountDownLatch) extends Actor {
      def receive = {
        case Config(ref) => {
          refs.add(ref)
          latch.countDown()
        }
        case _ =>
      }
    }

    val ring = Ring(10)(integers) { id => unidirectional(Props(new ConfigCollector(id, references, latch)))}

    ring.configure(ctx => Config(ctx.member.ref))

    latch.await(1, SECONDS) shouldBe true
    references.toList should have length 10
  }

  it should "properly start leader election among unidirectional members when requested" in {

    val latch = new CountDownLatch(10)

    class StartCollector[ID](val id: ID, val latch: CountDownLatch) extends Actor {
      def receive = {
        case Start() => {
          latch.countDown()
        }
        case _ =>
      }
    }

    val ring = Ring(10)(integers) { id => unidirectional(Props(new StartCollector(id, latch)))}

    ring.begin(_ => Start())

    latch.await(1, SECONDS) shouldBe true
  }


}