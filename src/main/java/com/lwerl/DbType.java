package com.lwerl;

import static com.lwerl.Solution.*;

public enum DbType {
    PG(
            "org.postgresql.Driver",
            "jdbc:postgresql://",
            "SELECT " + FIELD_NAME + " FROM " + TABLE_NAME + " ORDER BY " + FIELD_NAME + " LIMIT " + PAGE_SIZE + " OFFSET (?)"
    );

    private String driverName;
    private String urlPrefix;
    private String pagingPreparedQuery;

    DbType(String driverName, String urlPrefix, String pagingPreparedQuery) {
        this.driverName = driverName;
        this.urlPrefix = urlPrefix;
        this.pagingPreparedQuery = pagingPreparedQuery;
    }

    public String getDriverName() {
        return driverName;
    }

    public String getUrlPrefix() {
        return urlPrefix;
    }

    public String getPagingPreparedQuery() {
        return pagingPreparedQuery;
    }
}
