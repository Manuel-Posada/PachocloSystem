package com.pachoclosystem.demo.dto;

import java.util.List;

public record ErrorResponse(int status, String error, List<String> mensajes) {
}
