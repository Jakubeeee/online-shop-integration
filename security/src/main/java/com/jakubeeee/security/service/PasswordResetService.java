package com.jakubeeee.security.service;

import com.jakubeeee.common.service.EmailService;
import com.jakubeeee.common.service.LocaleService;
import com.jakubeeee.security.exceptions.ValidationException;
import com.jakubeeee.security.model.PasswordResetToken;
import com.jakubeeee.security.model.User;
import com.jakubeeee.security.repositories.PasswordResetTokenRepository;
import com.jakubeeee.security.validation.forms.ChangePasswordForm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class PasswordResetService {

    @Autowired
    UserService userService;

    @Autowired
    EmailService emailService;

    @Autowired
    LocaleService localeService;

    @Autowired
    PasswordResetTokenRepository passwordResetTokenRepository;

    @Value("${reset.token.lifetime.in.minutes}")
    private int TOKEN_LIFETIME_IN_MINUTES;

    @Transactional
    public void handleForgotMyPasswordProcess(String email, String origin) {
        PasswordResetToken resetToken = createPasswordResetToken(email);
        User tokenOwner = resetToken.getUser();
        String resetPasswordUrl = createResetPasswordUrl(origin, tokenOwner.getId(), resetToken.getValue());
        sendEmailWithResetToken(tokenOwner, resetPasswordUrl);
    }

    @Transactional
    public PasswordResetToken createPasswordResetToken(String email) {
        User user = userService.findByEmail(email);
        String resetTokenValue = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now(Clock.systemUTC());
        var passwordResetToken =
                new PasswordResetToken(resetTokenValue, user, now, TOKEN_LIFETIME_IN_MINUTES);
        passwordResetTokenRepository.save(passwordResetToken);
        return passwordResetToken;
    }

    private String createResetPasswordUrl(String origin, long userId, String token) {
        return origin
                + "/#/changepassword"
                + "?id="
                + userId
                + "&token="
                + token;
    }

    private void sendEmailWithResetToken(User user, String resetPasswordUrl) {
        String emailContent = localeService.getMessage("passwordResetEmailContent")
                + "\r\n" + resetPasswordUrl;
        String emailSubject = localeService.getMessage("passwordResetEmailSubject");
        SimpleMailMessage mailMessage =
                emailService.createMailMessage(user.getEmail(), emailContent, emailSubject);
        emailService.sendMailMessage(mailMessage);
    }

    public void changePassword(ChangePasswordForm form) throws ValidationException {
        try {
            if (StringUtils.isEmpty(form.getResetToken()) || form.getUserId() <= 0) {
                throw new ValidationException("Provided url parameters are invalid");
            }
            PasswordResetToken passwordResetToken = findByValue(form.getResetToken());
            User tokenOwner = passwordResetToken.getUser();
            if (tokenOwner.getId() != form.getUserId()) {
                throw new ValidationException("Unauthorized attempt to change password");
            }
            if (LocalDateTime.now(Clock.systemUTC()).isAfter(passwordResetToken.getExpiryDate())) {
                throw new ValidationException("Reset token has expired");
            }
            userService.updateUserPassword(tokenOwner.getId(), form.getPassword());
        } catch (NoSuchElementException ignored) {
            throw new ValidationException("Reset owner not found");
        }
    }

    public PasswordResetToken findByValue(String value) {
        Optional<PasswordResetToken> tokenO = passwordResetTokenRepository.findByValue(value);
        return tokenO.orElseThrow(() -> new NoSuchElementException("Token with value " + value + " not found in the database"));
    }
}
