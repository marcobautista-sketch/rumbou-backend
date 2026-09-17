package com.rumbou.backend.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Map;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);
    private static final String REMITENTE = "RumboU <noresponder@rumbou.edu.pe>";

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    public EmailService(JavaMailSender mailSender, TemplateEngine templateEngine) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
    }

    // Generico para las 3 cartas: cada listener elige la plantilla y las variables.
    public void enviarPlantilla(String destinatario, String asunto, String plantilla,
                                Map<String, Object> variables) {
        try {
            MimeMessage mime = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mime, true, "UTF-8");
            helper.setFrom(REMITENTE);
            helper.setTo(destinatario);
            helper.setSubject(asunto);

            Context contexto = new Context();
            contexto.setVariables(variables);
            String html = templateEngine.process(plantilla, contexto);
            helper.setText(html, true);

            mailSender.send(mime);
            log.info("Correo '{}' enviado a {}", asunto, destinatario);
        } catch (MessagingException e) {
            log.warn("No se pudo enviar el correo '{}' a {}: {}", asunto, destinatario, e.getMessage());
        }
    }
}