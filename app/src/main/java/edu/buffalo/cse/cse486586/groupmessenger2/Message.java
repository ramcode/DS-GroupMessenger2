package edu.buffalo.cse.cse486586.groupmessenger2;

import java.io.Serializable;

/**
 * Created by ramesh on 2/19/16.
 */
public class Message implements Serializable {

    public String message;
    public String avd;
    public String fromPort;
    public int[] vectorTimeStamp;
    public Double sequencer;

    public boolean isSendProposal() {
        return sendProposal;
    }

    public void setSendProposal(boolean sendProposal) {
        this.sendProposal = sendProposal;
    }

    public boolean sendProposal;

    public boolean isAgreementReceived() {
        return agreementReceived;
    }

    public void setAgreementReceived(boolean agreementReceived) {
        this.agreementReceived = agreementReceived;
    }

    public String messageId;
    public String messageType;
    public boolean agreementReceived;

    public boolean isDeliverable() {
        return isDeliverable;
    }

    public void setIsDeliverable(boolean isDeliverable) {
        this.isDeliverable = isDeliverable;
    }

    public String originalSender;
    public boolean isDeliverable;

    public String getOriginalSender() {
        return originalSender;
    }

    public void setOriginalSender(String originalSender) {
        this.originalSender = originalSender;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Message message1 = (Message) o;

        if (!message.equals(message1.message)) return false;
        return messageId.equals(message1.messageId);

    }

    @Override
    public int hashCode() {
        int result = message.hashCode();
        result = 31 * result + messageId.hashCode();
        return result;
    }

    public String getMessageType() {
        return messageType;
    }

    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }

    public int[] getVectorTimeStamp() {
        return vectorTimeStamp;
    }

    public void setSequencer(Double sequencer) {
        this.sequencer = sequencer;
    }

    public Double getSequencer() {

        return sequencer;
    }

    public void setVectorTimeStamp(int[] vectorTimeStamp) {
        this.vectorTimeStamp = vectorTimeStamp;
    }

    public Message(String message){
        this.message = message;
    }
    public Message(String message, String avd){
        this.message = message;
        this.avd = avd;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getAvd() {
        return avd;
    }

    public void setAvd(String avd) {
        this.avd = avd;
    }

    public String getFromPort() {
        return fromPort;
    }

    public void setFromPort(String fromPort) {
        this.fromPort = fromPort;
    }
}
