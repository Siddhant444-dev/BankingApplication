package com.siddhant.BankingApplication.service.impl;

import com.siddhant.BankingApplication.dto.EmailDetails;

public interface EmailService {
    void sendEmailAlert(EmailDetails emailDetails);
    void sendEmailWithAttachment(EmailDetails emailDetails) throws Exception;


}
