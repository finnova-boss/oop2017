package bank.accounts;

import bank.Customer;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Currency;
import java.util.Date;
import java.util.List;

public abstract class Account {
    private Customer owner = null;
    private Currency currency = null;
    private double interest = 0;
    private BigDecimal balance = new BigDecimal(0);
    private List<Statement> statements = new ArrayList<>();

    Account(Customer owner, Currency currency, double interest, BigDecimal balance) {
        this.owner = owner;
        this.currency = currency;
        this.interest = interest;
        this.balance = balance;
    }

    public void deposit(BigDecimal amount) {
        statements.add(new Statement(new Date(), amount));
        balance = balance.add(amount);
    }

    public void withdraw(BigDecimal amount) {
        statements.add(new Statement(new Date(), amount));
        balance = balance.subtract(amount);
    }

    public Customer getOwner() {
        return owner;
    }

    public Currency getCurrency() {
        return currency;
    }

    public double getInterest() {
        return interest;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public List<Statement> getStatements() {
        return statements;
    }
}
