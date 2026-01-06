package com.legalconnect.lawyerbooking.dto;

public class BookingResponse {
    
    private boolean success;
    private String message;
    private AppointmentDTO appointment;

    public BookingResponse() {
    }

    public BookingResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    public BookingResponse(boolean success, String message, AppointmentDTO appointment) {
        this.success = success;
        this.message = message;
        this.appointment = appointment;
    }

    // Getters and Setters
    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public AppointmentDTO getAppointment() {
        return appointment;
    }

    public void setAppointment(AppointmentDTO appointment) {
        this.appointment = appointment;
    }
}

