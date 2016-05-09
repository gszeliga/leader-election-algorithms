package es.gszeliga.algorithms.leaderelection

import akka.actor.{Props, Actor, ActorRef, ActorSystem}
import akka.testkit.TestKit
import org.scalatest.{FlatSpecLike, Matchers}

import scala.util.Random

/**
  * Created by guillermo on 9/05/16.
  */
class TestRing2 extends TestKit(ActorSystem("test-actor-system")) with FlatSpecLike with Matchers {

  private val integers: () => Int = Random.nextInt

  case class Config(ref: ActorRef)

  class ShallowActor[ID](val id: ID) extends Actor {
    def receive = {
      case _ =>
    }
  }

  "Ring" should "have an specific number of nodes" in {

    val ring = Ring2(10)(integers) { id => new MemberProps[Int, Unidirectional] {
      def props = Props(new ShallowActor(id))
      def configMsg(ctx: BinderContext[Int, Unidirectional]) = Config(ctx.members(0).ref)
    }
    }

    ring.size shouldBe 10
  }

  it should "create as many nodes as requested" in {

    val ring = Ring2(10)(integers) { id => new MemberProps[Int, Unidirectional] {
      def props = Props(new ShallowActor(id))
      def configMsg(ctx: BinderContext[Int, Unidirectional]) = Config(ctx.members(0).ref)
    }
    }

    ring.members should have length 10
  }

  it should "properly configure all members when requested" in {

    val references = scala.collection.mutable.ListBuffer.empty[ActorRef]

    class ConfigCollector[ID](val id: ID, refs: Seq[ActorRef]) extends Actor {
      def receive = {
        case Config(ref) => refs :+ ref
        case _ =>
      }
    }

    val ring = Ring2(10)(integers) { id => new MemberProps[Int, Unidirectional] {
      def props = Props(new ConfigCollector(id, references))
      def configMsg(ctx: BinderContext[Int, Unidirectional]) = Config(ctx.members(0).ref)
    }
    }

    ring.configure()(members => members.map(m => (m, UniBinderContext(m))))

    references.toList should have length 10
  }


}