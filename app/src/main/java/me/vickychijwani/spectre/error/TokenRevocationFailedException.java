package me.vickychijwani.spectre.error;

import me.vickychijwani.spectre.network.entity.RevokeReqBody;

public final class TokenRevocationFailedException extends RuntimeException {

    /**
     * @param tokenType the type of token (access token, refresh token, etc) that could not be revoked
     * @param error a custom String message
     */
    public TokenRevocationFailedException(@RevokeReqBody.TokenType String tokenType, String error) {
        super("REVOCATION OF " + tokenType + " FAILED: " + error);
    }

    /**
     * @param tokenType the type of token (access token, refresh token, etc) that could not be revoked
     * @param throwable the cause of this token revocation failure
     */
    public TokenRevocationFailedException(@RevokeReqBody.TokenType String tokenType,
                                          Throwable throwable) {
        super("REVOCATION OF " + tokenType + " FAILED: see previous exception for details", throwable);
    }

}
