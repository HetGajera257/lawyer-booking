package com.legalconnect.lawyerbooking.service;

import org.springframework.stereotype.Service;

@Service
public class TextMaskingService {

    // LOOSE version (8–10 digits)
    private static final String PHONE =
            "(?<!\\d)(?:\\+91[- ]?)?[6-9]\\d{7,9}(?!\\d)";

    private static final String AADHAR =
            "\\b\\d{4}\\s?\\d{4}\\s?\\d{4}\\b";

    private static final String EMAIL =
            "[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}";

    private static final String NAME =
            "(?i)(my name is|i am|this is)\\s+[a-z ]+";

    private static final String LOCATION =
            "(?i)(from|live in|village is|residing in|sarnamu|surname|adress is)\\s+[a-z ]+";

    public String maskEnglishPersonalInfo(String text) {

        text = text.replaceAll(PHONE, "**********");
        text = text.replaceAll(AADHAR, "************");
        text = text.replaceAll(EMAIL, "*****@*****");
        text = text.replaceAll(NAME, "$1 *****");
        text = text.replaceAll(LOCATION, "$1 *****");

        return text;
    }
}

