package es.gszeliga.algorithms.leaderelection.rings

import akka.actor.{ActorRef, ActorSystem, Props}

/**
  * Created by guillermo on 9/05/16.
  */

trait RingNature

trait Unidirectional extends RingNature

trait Bidirectional extends RingNature

trait Member[ID, RN <: RingNature] {
  def id: ID
  def ref: ActorRef
}

trait MemberProps[ID, RN <: RingNature] {
  def props: Props
}

trait UMemberProps[ID] extends MemberProps[ID, Unidirectional]
trait BMemberProps[ID] extends MemberProps[ID, Bidirectional]

abstract class AssignmentContext[ID, RN <: RingNature]
case class UAssignment[ID](val member: Member[ID, Unidirectional]) extends AssignmentContext[ID, Unidirectional]
case class BAssignment[ID](val left: Member[ID, Bidirectional], val right: Member[ID, Bidirectional]) extends AssignmentContext[ID, Bidirectional]

trait Ring[ID, RN <: RingNature, CTX <: AssignmentContext[ID, RN]] {
  def size: Int

  def members: Vector[Member[ID, RN]]

  def beginElectionWith[M](f: Member[ID, RN] => M) = members.foreach(m => m.ref ! f(m))
}

object Ring {

  type Designation[ID, RN <: RingNature, CTX <: AssignmentContext[ID, RN]] = Vector[Member[ID, RN]] => Seq[(Member[ID, RN], CTX)]

  def apply[ID, RN <: RingNature, CTX <: AssignmentContext[ID, RN]](numberOfMembers: Int)(f: () => ID)(g: ID => MemberProps[ID, RN])(implicit system: ActorSystem, designation: Designation[ID, RN, CTX]) =

    new Ring[ID, RN, CTX] {

      val size = numberOfMembers
      val members = Range(0, numberOfMembers).map(_ => {

        val currentId = f()
        val memberProps = g(currentId)

        new Member[ID, RN] {
          val id = currentId
          val ref = system.actorOf(memberProps.props)
        }

      }).toVector

      def configure[M](f: CTX => M) = {
        designation(members).foreach {
          case (member, ctx) => member.ref ! f(ctx)
        }
      }
    }

  object Designations{

    implicit def unidirectional[ID]: Designation[ID, Unidirectional, UAssignment[ID]] = {
      members => members.zip(members.tail :+ members.head).map{case ((m1,m2)) => (m1, UAssignment(m2))}
    }

    implicit def bidirectional[ID]: Designation[ID, Bidirectional, BAssignment[ID]] = members => Seq.empty

  }

}
