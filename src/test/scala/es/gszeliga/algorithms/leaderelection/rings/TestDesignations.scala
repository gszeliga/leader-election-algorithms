package es.gszeliga.algorithms.leaderelection.rings

import es.gszeliga.algorithms.leaderelection.rings.Ring.Designations
import org.scalatest.{FlatSpecLike, Matchers}

/**
  * Created by guillermo on 11/05/16.
  */
class TestDesignations extends FlatSpecLike with Matchers {

  case class UTestMember[ID](myid: ID) extends Member[ID, Unidirectional]{
    def id = myid
    def ref = null
  }

  def member[ID](id: ID) =  UTestMember(id)

  "Unidirectional designation" should "generate (M1,M1) pair with with 1-member list" in {
    val m1 = member(1)
    Designations.unidirectional[Int](Vector(m1)) should contain (m1, UAssignment(m1))
  }

  "Unidirectional designation" should "generate [(M1,M2), (M2,M1)] pairs using with 2-members list" in {
    val m1 = member(1)
    val m2  = member(2)

    val designations = Designations.unidirectional[Int](Vector(m1, m2))

    designations should have size 2
    designations should contain allOf ((m1, UAssignment(m2)),(m2, UAssignment(m1)))
  }
}
