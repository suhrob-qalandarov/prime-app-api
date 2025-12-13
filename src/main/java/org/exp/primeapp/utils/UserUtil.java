package org.exp.primeapp.utils;

import org.springframework.stereotype.Component;

@Component
public class UserUtil {

    private static final int MAX_NAME_LENGTH = 15;

    public String truncateName(String name) {
        if (name == null) {
            return null;
        }
        return name.length() > MAX_NAME_LENGTH ? name.substring(0, MAX_NAME_LENGTH) : name;
    }
}

