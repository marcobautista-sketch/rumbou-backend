package com.rumbou.backend.service;

import jakarta.mail.internet.MimeMessage;
import jakarta.mail.Session;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSender;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Map;
import java.util.Properties;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EmailServiceTest {

    private JavaMailSender mailSender;
    private TemplateEngine templateEngine;
    private EmailService emailService;

    @BeforeEach
    void setUp() {
        mailSender = mock(JavaMailSender.class);
        templateEngine = mock(TemplateEngine.class);
        emailService = new EmailService(mailSender, templateEngine);
    }

    @Test
    void enviaElCorreoConElHtmlGeneradoPorLaPlantilla() throws Exception {
        when(mailSender.createMimeMessage())
                .thenReturn(new MimeMessage(Session.getDefaultInstance(new Properties())));
        when(templateEngine.process(eq("registro-confirmacion"), any(Context.class)))
                .thenReturn("<h1>Bienvenido Ana</h1>");

        emailService.enviarPlantilla("ana@rumbou.com", "Bienvenido a RumboU",
                "registro-confirmacion", Map.of("nombre", "Ana"));

        verify(mailSender).send(any(MimeMessage.class));
    }
}