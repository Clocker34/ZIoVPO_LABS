package ru.rkjrth.adboard.dto.license;

/**
 * Ответ клиенту: тикет и ЭЦП (RSA-SHA256) в Base64 по каноническому JSON тикета.
 */
public class TicketResponse {

    private Ticket ticket;

    /** Подпись в Base64 (SHA256withRSA). */
    private String signatureBase64;

    public TicketResponse() {
    }

    public TicketResponse(Ticket ticket, String signatureBase64) {
        this.ticket = ticket;
        this.signatureBase64 = signatureBase64;
    }

    public Ticket getTicket() {
        return ticket;
    }

    public void setTicket(Ticket ticket) {
        this.ticket = ticket;
    }

    public String getSignatureBase64() {
        return signatureBase64;
    }

    public void setSignatureBase64(String signatureBase64) {
        this.signatureBase64 = signatureBase64;
    }
}
