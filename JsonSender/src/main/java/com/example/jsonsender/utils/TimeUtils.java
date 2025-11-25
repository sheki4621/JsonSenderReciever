package com.example.jsonsender.utils;

import java.time.ZoneId;
import java.time.ZonedDateTime;

public class TimeUtils {
    public static ZonedDateTime getNow(String timezone) {
        return ZonedDateTime.now(ZoneId.of(timezone));
    }
}
