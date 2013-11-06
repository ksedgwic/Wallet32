package com.bonsai.androidelectrum;

public class Balance {
    public final int accountId;
    public final String accountName;
    public final double balance;
    public Balance(int id, String name, double balance) {
        this.accountId = id;
        this.accountName = name;
        this.balance = balance;
    }
}
