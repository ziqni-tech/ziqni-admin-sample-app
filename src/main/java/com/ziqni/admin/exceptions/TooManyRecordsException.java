package com.ziqni.admin.exceptions;

public class TooManyRecordsException extends Exception {

    public TooManyRecordsException(int maxLimit, int skip, int limit) {
        super(String.format("You have exceeded the maximum permitted record size of %o, [skip: %o, limit: %o]",maxLimit,skip,limit));
    }

    public static void Validate(int maxLimit, int skip, int limit) throws TooManyRecordsException {
        if(limit>maxLimit)
            throw new TooManyRecordsException(maxLimit, skip, limit);
    }
}
