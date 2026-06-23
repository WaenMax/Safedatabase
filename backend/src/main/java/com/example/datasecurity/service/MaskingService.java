package com.example.datasecurity.service;

import org.springframework.stereotype.Service;

@Service
public class MaskingService {
    public String mask(String value, String fieldName) {
        if (value == null) return "";
        String name = fieldName == null ? "" : fieldName.toLowerCase();
        if (name.contains("phone") || name.contains("mobile") || name.contains("tel") || name.contains("手机号")) {
            return value.replaceAll("(\\d{3})\\d{4}(\\d+)", "$1****$2");
        }
        if (name.contains("email")) {
            return value.replaceAll("(^.{3}).*(@.*$)", "$1***$2");
        }
        if (name.contains("id_card") || name.contains("身份证")) {
            return value.replaceAll("(^.{3}).*(.{4}$)", "$1***********$2");
        }
        if (name.contains("bank") || name.contains("card") || name.contains("account")) {
            return value.replaceAll("(^\\d{4}).*(\\d{4}$)", "$1 **** **** $2");
        }
        if (name.contains("password") || name.contains("token") || name.contains("secret")) {
            return "******";
        }
        return value.length() <= 4 ? "****" : value.substring(0, 2) + "***" + value.substring(value.length() - 2);
    }
}
