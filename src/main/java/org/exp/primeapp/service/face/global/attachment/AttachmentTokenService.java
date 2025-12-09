package org.exp.primeapp.service.face.global.attachment;

public interface AttachmentTokenService {
    
    String generateToken();
    
    boolean validateToken(String token);
    
    String refreshToken(String oldToken);
}

