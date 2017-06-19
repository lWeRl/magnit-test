package com.lwerl;

/**
 * Created by lWeRl on 19.06.2017.
 */
public class Main {
    public static void main(String[] args) {
        new Solution(DbType.PG, "localhost", "5432", "magnit", "postgres", "password", 1_000_000).execute();
    }
}
