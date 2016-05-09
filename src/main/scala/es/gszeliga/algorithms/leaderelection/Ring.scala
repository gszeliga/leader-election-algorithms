package es.gszeliga.algorithms.leaderelection
/*

import akka.actor.{Actor, ActorRef, ActorSystem, Props}

/**
  * Created by guillermo on 27/04/16.
  */

trait Ring[ID,R, C <: MemberProps[ID,_,R]]{
  def size(): Int
  def members(): Vector[Member[ID,R]]
  def config()(implicit binder: Binder[ID,R]): Unit ={

    members().foldLeft(0)({
      case (position,member) => {

        val (pos,neightbours) = binder.bind(position,members())

        member.configure(neightbours)

        pos
      }
    })

  }
}

trait MemberProps[ID,A,N]{
  def props: Props
  def config(references: N): Any
}

trait UnidirectionalMemberProps[ID,A,N] extends MemberProps[ID,A, Member[ID,N]]
trait BidirectionalMemberProps[ID,A,N] extends MemberProps[ID,A, (Member[ID,N],Member[ID,N])]

trait Member[ID,N]{

  def ref(): ActorRef
  def configure(neighbours: N)
  def id(): ID
}

/*trait Unidirectional[ID] extends Member[ID]{
  def configure(next: Unidirectional[ID])
}

trait Bidirectional[ID] extends Member[ID]{
  def configure(left: Bidirectional[ID], right: Bidirectional[ID])
}*/

trait Binder[ID, N]{
  def bind(state: Int, ctx: Vector[Member[ID,N]]): (Int,N)
}

trait UBinder[ID,N] extends Binder[ID, Member[ID,N]]
trait BBinder[ID,N] extends Binder[ID, (Member[ID,N],Member[ID,N])]

object Ring {

  type Tagged[U] = { type Tag = U }
  type @@[T, U] = T with Tagged[U]

  class Tagger[U] {
    def apply[T](t : T) : T @@ U = t.asInstanceOf[T @@ U]
  }

  def tag[U] = new Tagger[U]

  def unidirectional[ID, A[_] <: Actor,N](numberOfMembers: Int)(f: () => ID)(g: ID => UnidirectionalMemberProps[ID,A[ID],N])(implicit system: ActorSystem) = {
    apply(numberOfMembers)(f)(g)
  }

  private def apply[ID, A[_] <: Actor, N](numberOfMembers: Int)(f: () => ID)(g: ID => MemberProps[ID,A[ID],N])(implicit system: ActorSystem) = new Ring[ID,N,MemberProps[ID,A[ID],N]] {
    def size() = numberOfMembers
    def members():Vector[Member[ID,N]] = Range(0,numberOfMembers).map(_ => {

      val currentId = f()
      val member = g(currentId)

      new Member[ID,N] {
        val id = currentId
        val ref = system.actorOf(member.props)
        def configure(neighbours: N) = ref ! member.config(neighbours)
      }

    }).toVector

  }

}
*/
