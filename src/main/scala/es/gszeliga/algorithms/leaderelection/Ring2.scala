package es.gszeliga.algorithms.leaderelection

import akka.actor.{ActorRef, ActorSystem, Props}
import es.gszeliga.algorithms.leaderelection.Ring2.Binder

/**
  * Created by guillermo on 9/05/16.
  */

trait RingNature
trait Unidirectional extends RingNature
trait Bidirectional extends RingNature

trait Member[ID, RN <: RingNature]{
  def id:ID
  def ref: ActorRef
  def config(ctx: BinderContext[ID, RN])
}

trait MemberProps[ID, RN <: RingNature]{
  def props: Props
  def configMsg(ctx: BinderContext[ID, RN]): Any
}

sealed class BinderContext[ID, RN <: RingNature](val members: Member[ID, RN]*)
sealed case class UniBinderContext[ID](val member: Member[ID, Unidirectional]) extends BinderContext[ID, Unidirectional](member)
sealed case class BiBinderContext[ID](val m1: Member[ID, Bidirectional],val m2: Member[ID, Bidirectional]) extends BinderContext[ID, Bidirectional](m1,m2)

trait Ring2[ID, RN <: RingNature]{
  def size: Int
  def members: Vector[Member[ID, RN]]
  def configure()(implicit binder: Binder[ID,RN])
}

object Ring2 {

  type Binder[ID, RN <: RingNature] = Vector[Member[ID,RN]] => Seq[(Member[ID,RN],BinderContext[ID,RN])]

  def apply[ID, RN <: RingNature](numberOfMembers: Int)(f: () => ID)(g: ID => MemberProps[ID,RN])(implicit system: ActorSystem) = new Ring2[ID,RN] {
    val size = numberOfMembers
    val members = Range(0,numberOfMembers).map(_ => {

      val currentId = f()
      val memberProps = g(currentId)

      new Member[ID,RN] {
        val id = currentId
        val ref = system.actorOf(memberProps.props)
        def config(ctx: BinderContext[ID,RN]) = ref ! memberProps.configMsg(ctx)
      }

    }).toVector

    def configure()(implicit binder: Binder[ID,RN]) = {
      binder(members).foreach{
        case (member, ctx) => member.config(ctx)
      }
    }
  }

}
