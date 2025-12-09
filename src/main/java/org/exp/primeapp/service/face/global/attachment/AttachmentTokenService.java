package org.exp.primeapp.service.face.global.attachment;

import org.exp.primeapp.models.entities.User;

public interface AttachmentTokenService {
    
    String generateToken(User user);
    
    boolean validateToken(String token, User user);
    
    String refreshToken(String oldToken, User user);
}

