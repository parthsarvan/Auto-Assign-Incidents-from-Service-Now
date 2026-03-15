package com.example.backend.service;

public interface SmsService {
    boolean sendSms(String toPhoneNumber, String message);
}
