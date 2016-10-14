package eightyDays.scala211.bank

import eightyDays.scala211.bank.account.Amount
import eightyDays.scala211.bank.partner.Partner

package partner {

  case class Identification(number: String = java.util.UUID.randomUUID.toString)

  abstract class Partner(val name: String) {

    import eightyDays.scala211.bank.account.Account

    def asset(bank: Bank): Amount = bank.filterAccounts(Account.byOwner(this)).foldLeft(BigDecimal.valueOf(0)) { case (balance, account) => balance + account.balance }
  }

  object Partner {
    def byName(name: String): Partner => Boolean = partner => partner.name == name
  }

  case class Person(firstName: String, override val name: String) extends Partner(name)

  object Person {
    def byFirstname(firstName: String): Partner => Boolean = {
      case Person(`firstName`, _) => true
      case _ => false
    }
  }

  case class LegalEntity(override val name: String) extends Partner(name)

}

package object account {
  type Amount = BigDecimal
}

package account {

  import java.time.LocalDateTime

  import eightyDays.scala211.bank.account.fee.PerBooking
  import eightyDays.scala211.bank.account.withdrawal.{Limited, NoWithdraw}
  import eightyDays.scala211.bank.partner.Identification

  import scala.language.implicitConversions

  object Account {
    def byOwner(owner: Partner): Account => Boolean = account => account.owner == owner
  }

  case class Booking(value: Amount, valuta: java.time.LocalDateTime, text: String)

  case class Portfolio(accounts: Set[Account])

  abstract class Account(val owner: Partner, val bookings: Seq[Booking], makeNew: ((Partner, Seq[Booking]) => Account)) {
    implicit def booking2Amount(booking: Booking): Amount = booking.value

    val number = Identification()

    def balance: Amount = bookings.foldLeft(BigDecimal.valueOf(0))((balance, booking) => balance + booking)

    def post(value: Amount, valuta: LocalDateTime = LocalDateTime.now(), text: String = "Booking") = makeNew(owner, Booking(value, valuta, text) +: bookings)

    override def toString: String = s"${getClass.getSimpleName} number:${number.number} balance:$balance"
  }


  abstract class Mortgage(override val owner: Partner, bookings: Seq[Booking], makeNew: ((Partner, Seq[Booking]) => Account)) extends Account(owner, bookings, makeNew)

  case class FixedRateMortgage(override val owner: Partner, override val bookings: Seq[Booking] = Seq()) extends Mortgage(owner, bookings, FixedRateMortgage) with NoWithdraw

  case class VariableRateMortgage(override val owner: Partner, override val bookings: Seq[Booking] = Seq()) extends Mortgage(owner, bookings, VariableRateMortgage)

  case class Current(override val owner: Partner, override val bookings: Seq[Booking] = Seq()) extends Account(owner, bookings, Current)

  case class CreditCard(override val owner: Partner, override val bookings: Seq[Booking] = Seq()) extends Account(owner, bookings, CreditCard)

  case class Saving(override val limit: Amount)(override val owner: Partner, override val bookings: Seq[Booking] = Seq()) extends Account(owner, bookings, Saving(limit)) with Limited {
    override def timeframeInMonths: Int = 6
  }

  case class SavingWithFee(override val limit: Amount, override val fee: Amount)(override val owner: Partner, override val bookings: Seq[Booking] = Seq()) extends Account(owner, bookings, SavingWithFee(limit, fee)) with Limited with PerBooking {
    override def timeframeInMonths: Int = 6
  }

  package fee {

    trait BalanceBased

    trait PerBooking extends Account {
      def fee: Amount

      override def post(value: Amount, valuta: java.time.LocalDateTime, text: String): Account = {
        if (value < 0) super.post(value - fee, text = "Including fee on withdrawal")
        else super.post(value)
      }
    }
  }

  package overdrawn {

    trait NoOverdraw

    trait Limited {
      val limit: Amount
    }

  }

  package withdrawal {

    trait Limited extends Account {
      def limit: Amount

      def timeframeInMonths: Int

      override def post(value: Amount, valuta: java.time.LocalDateTime, text: String): Account = if (value >= -limit) super.post(value, valuta) else throw new RuntimeException("Withdraw within timeframe not allowed")

    }

    trait NoWithdraw extends Account {
      override def post(value: Amount, valuta: java.time.LocalDateTime, text: String): Account = if (value > 0.0) super.post(value, valuta) else throw new RuntimeException("Withdraw not allowed")
    }

  }

}

import eightyDays.scala211.bank.account.Account
import eightyDays.scala211.bank.partner.{Identification, Partner}

case class Bank(name: String, partners: Map[Identification, Partner] = Map[Identification, Partner](), accounts: Set[Account] = Set[Account]()) {
  def post(account: Account, value: Amount): (Account, Bank) = {
    val updatedAccount = account.post(value)
    (updatedAccount, copy(accounts = accounts - account + updatedAccount))
  }

  def posts(posts: (Account, Amount)*) = posts.foldLeft((List[Account](), this)) { (accounts, post) =>
    val (updatedAccount, updatedBank) = accounts._2.post(post._1, post._2)
    (accounts._1 :+ updatedAccount, updatedBank)
  }

  def add(partnerId: Identification, accountFactory: (Partner => Account)): (Account, Bank) = {
    val newAccount = accountFactory(partners(partnerId))
    (newAccount, copy(accounts = accounts + newAccount))
  }

  def add(pPartner: Partner): (Identification, Bank) =
    partners
      .find(_._2 == pPartner)
      .map { entry => (entry._1, this) }
      .getOrElse {
        val result = Identification()
        (result, copy(partners = partners + (result -> pPartner)))
      }

  def find(predicate: Partner => Boolean): Option[Partner] = partners
    .find(p => predicate(p._2))
    .map(_._2)

  def filterPartners(predicate: Partner => Boolean): Set[Partner] =
    partners
      .filter(p => predicate(p._2))
      .values
      .toSet

  def filterAccounts(predicate: Account => Boolean): Set[Account] =
    accounts
      .filter(predicate)
}