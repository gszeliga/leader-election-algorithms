package es.gszeliga.algorithms.leaderelection

import akka.actor.{Actor, ActorRef, ActorSystem, Props}

/**
  * Created by guillermo on 27/04/16.
  */

trait Ring[ID, C <: Configurable[ID,_]]{
  def size(): Int
  def members: Vector[Member[ID]]
/*  def config(): Unit ={

    /*
    * - Dado el tipo de configurable, necesito algo (Binder) que gestine:
    *  ++ Si es uni -> MEMBERS -> CM -> (M)
    *  ++ Si es bi -> MEMBERS -> CM -> (M1, M2)
    *
    *  - Luego, cada miembro M ser configurado, tiene que esperar (M) o (M1,M2):
     *
     *   val b = Binder.using(members)
     *
     *   member.config(b.next())
    *
    * */

  }*/
}

trait Configurable[ID,A]{
  def props: Props
}

trait Unidirectional[ID,A] extends Configurable[ID,A]
trait Bidirectional[ID,A] extends Configurable[ID,A]

trait Member[ID]{
  def ref(): ActorRef
  def id(): ID
}

/*trait Unidirectional[ID] extends Member[ID]{
  def configure(next: Unidirectional[ID])
}

trait Bidirectional[ID] extends Member[ID]{
  def configure(left: Bidirectional[ID], right: Bidirectional[ID])
}*/

trait Binder[C <: Configurable[_, _]]{
  def bind()
}

trait UBinder[Unidirectional]
trait BBinder[Bidirectional]

object Ring {

  type Tagged[U] = { type Tag = U }
  type @@[T, U] = T with Tagged[U]

  class Tagger[U] {
    def apply[T](t : T) : T @@ U = t.asInstanceOf[T @@ U]
  }

  def tag[U] = new Tagger[U]

  def unidirectional[ID, A[_] <: Actor](numberOfMembers: Int)(f: () => ID)(g: ID => Unidirectional[ID,A[ID]])(implicit system: ActorSystem) = {
    apply(numberOfMembers)(f)(g)
  }

  private def apply[ID, A[_] <: Actor, C[_,_] <: Configurable[_,_]](numberOfMembers: Int)(f: () => ID)(g: ID => C[ID,A[ID]])(implicit system: ActorSystem) = new Ring[ID,C] {
    def size() = numberOfMembers
    def members:Vector[Member[ID]] = Range(0,numberOfMembers).map(_ => {

      //Given C, I might need an instance of MemberCreator[C] with specifics (config, start) or pass an implicit Configurator[A[ID]]
      new Member[ID] {
        val id = f()
        val ref = system.actorOf(g(id).props)
      }

    }).toVector

  }

}
