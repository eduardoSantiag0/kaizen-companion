package io.github.eduardosantiag0.kaizan_companion.miniapp;

import org.springframework.stereotype.Service;

@Service
public class TelegramInitDataService {

    private final HmacService hmacService;

    public TelegramInitDataService(HmacService hmacService) {
        this.hmacService = hmacService;
    }

    public Long validateAndGetUserId(String initData) {

        // https://docs.telegram-mini-apps.com/platform/init-data?utm_source=chatgpt.com



        https://core.telegram.org/bots/webapps?utm_source=chatgpt.com#validating-data-received-via-the-mini-app


//        Data-check-string is a chain of all received fields, sorted alphabetically, in the format key=<value> with a line feed character ('
//        ', 0x0A) used as separator – e.g., 'auth_date=<auth_date> query_id=<query_id> user=<user>'.
        return null;
    }
}
//
//Validating data received via the Mini App
//To validate data received via the Mini App, one should send the data from the Telegram.WebApp.initData field to the bot's backend. The data is a query string, which is composed of a series of field-value pairs.
//
//You can verify the integrity of the data received by comparing the received hash parameter with the hexadecimal representation of the HMAC-SHA-256 signature of the data-check-string with the secret key, which is the HMAC-SHA-256 signature of the bot's token with the constant string WebAppData used as a key.
//
//Data-check-string is a chain of all received fields, sorted alphabetically, in the format key=<value> with a line feed character ('
//                                                                                                                                          ', 0x0A) used as separator – e.g., 'auth_date=<auth_date> query_id=<query_id> user=<user>'.
//
//                                                                                                                                          The full check might look like:
//
//                                                                                                                                          data_check_string = ...
//                                                                                                                                          secret_key = HMAC_SHA256(<bot_token>, "WebAppData")
//if (hex(HMAC_SHA256(data_check_string, secret_key)) == hash) {
//        // data is from Telegram
//        }
//To prevent the use of outdated data, you can additionally check the auth_date field, which contains a Unix timestamp of when it was received by the Mini App.
//
//        Once validated, the data may be used on your server. Complex data types are represented as JSON-serialized objects.
