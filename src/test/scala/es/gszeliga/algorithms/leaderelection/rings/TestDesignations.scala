package es.gszeliga.algorithms.leaderelection.rings

import es.gszeliga.algorithms.leaderelection.rings.Ring.Assignments
import org.scalatest.{FlatSpecLike, Matchers}

/**
  * Created by guillermo on 11/05/16.
  */
class TestDesignations extends FlatSpecLike with Matchers {

  case class UTestMember[ID](myid: ID) extends Member[ID, Unidirectional]{
    def id = myid
    def ref = null
  }

  case class BTestMember[ID](myid: ID) extends Member[ID, Bidirectional]{
    def id = myid
    def ref = null
  }

  def umember[ID](id: ID) =  UTestMember(id)
  def bmember[ID](id: ID) = BTestMember(id)

  "Unidirectional designation" should "generate (M1,M1) pair with with 1-member list" in {
    val m1 = umember(1)
    Assignments.forUnidirectional[Int](Vector(m1)) should contain (m1, UAssignment(m1))
  }

  "Unidirectional designation" should "generate [(M1,M2), (M2,M1)] pairs using with 2-members list" in {
    val m1 = umember(1)
    val m2  = umember(2)

    val designations = Assignments.forUnidirectional[Int](Vector(m1, m2))

    designations should have size 2
    designations should contain allOf ((m1, UAssignment(m2)),(m2, UAssignment(m1)))
  }

  "Bidirectional designation" should "generate (M1,M1) pair with a 1-member list" in {
    val m1 = bmember(1)
    Assignments.forBidirectional[Int](Vector(m1)) should contain (m1, BAssignment(m1,m1))
  }

  "Bidirectional designation" should "generate [(M1,(M2,M2)), (M2,(M1,M1))] pairs with a 2-member list" in {
    val m1 = bmember(1)
    val m2 = bmember(2)
    Assignments.forBidirectional[Int](Vector(m1,m2)) should contain allOf ((m1, BAssignment(m2,m2)), (m2, BAssignment(m1,m1)))
  }

  "Bidirectional designation" should "generate accurate designations with a 3-member list" in {
    val m1 = bmember(1)
    val m2 = bmember(2)
    val m3 = bmember(3)

    Assignments.forBidirectional[Int](Vector(m1,m2,m3)) should contain allOf ((m1, BAssignment(m3,m2)), (m2, BAssignment(m1,m3)),(m3, BAssignment(m2,m1)))
  }

}
