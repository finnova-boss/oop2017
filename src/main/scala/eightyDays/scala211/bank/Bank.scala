package eightyDays.scala211.bank

import eightyDays.scala211.bank.account.{Account, Amount}

import scala.util.Try


case class Bank(name: String, var partners: Map[Identification, Partner] = Map[Identification, Partner](), var accounts: Set[Account] = Set[Account]()) {
  def post(account: Account, action: => Try[Account]) = action.map{ updatedAccount =>
    accounts = accounts - account
    accounts = accounts + updatedAccount
    updatedAccount
  }

  def deposit(account: Account, value: Amount) = post(account, account.deposit(value))

  def withdraw(account: Account, value: Amount) = post(account, account.withdraw(value))

  def add(partnerId: Identification, accountFactory: (Partner => Account)): Account = {
    val newAccount = accountFactory(partners(partnerId))
    accounts = accounts + newAccount
    newAccount
  }

  def add(pPartner: Partner): Identification =
  {
    val partner = partners.find(_._2)

    if (partner.isDefined) return partner.get._1
    else {
      val result = Identification()
      partners = partners + (result -> pPartner)
      result
    }
  }
    partners
      .find(_._2 == pPartner)
      .map { entry => entry._1 }
      .getOrElse {
        val result = Identification()
        partners = partners + (result -> pPartner)
        result
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