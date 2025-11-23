package com.skillmate.skillmate.exception;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestControllerAdvice
@Slf4j
@RequiredArgsConstructor
public class GlobalExceptionHandler {

  private final MessageSource messageSource;

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<Map<String, Object>> handleValidationExceptions(
      MethodArgumentNotValidException ex, WebRequest request) {
    // Usa o locale do LocaleContextHolder que foi definido pelo filtro
    Locale locale = LocaleContextHolder.getLocale() != null
        ? LocaleContextHolder.getLocale()
        : request.getLocale();

    // Normaliza o locale para garantir que seja reconhecido pelo MessageSource
    // en_US ou en-US -> Locale.ENGLISH (inglês - usa messages.properties)
    // pt_BR ou pt-BR -> Locale.forLanguageTag("pt-BR") (português - usa
    // messages_pt_BR.properties)
    if (locale != null) {
      String language = locale.getLanguage().toLowerCase();
      if (language.equals("en")) {
        locale = Locale.ENGLISH; // Locale.ENGLISH corresponde a messages.properties (inglês padrão)
      } else if (language.equals("pt")) {
        locale = Locale.forLanguageTag("pt-BR");
      }
    }

    log.debug("Locale usado no handler de validação: {} (normalizado)", locale);

    final Locale finalLocale = locale;
    Map<String, Object> errors = new HashMap<>();
    Map<String, String> fieldErrors = new HashMap<>();

    ex.getBindingResult().getAllErrors().forEach((error) -> {
      String fieldName = ((FieldError) error).getField();
      String errorMessage = error.getDefaultMessage();

      log.debug("Mensagem original do erro para campo {}: {}", fieldName, errorMessage);
      log.debug("Códigos de erro para campo {}: {}", fieldName, java.util.Arrays.toString(error.getCodes()));

      // O Spring Validation pode já ter traduzido a mensagem. Precisamos pegar a
      // chave original
      // se ainda for uma chave (formato {key}), ou tentar retraduzir usando os
      // códigos do erro
      String messageKey = null;

      if (errorMessage != null && errorMessage.startsWith("{") && errorMessage.endsWith("}")) {
        // Ainda é uma chave, traduz usando o locale correto
        messageKey = errorMessage.substring(1, errorMessage.length() - 1);
        errorMessage = messageSource.getMessage(messageKey, null, errorMessage, finalLocale);
      } else if (error.getCodes() != null && error.getCodes().length > 0) {
        // Tenta construir a chave a partir dos códigos de erro
        // Padrão dos códigos: NotBlank.userDTO.password, NotBlank.password, NotBlank
        // Padrão das chaves: validation.user.password.notblank

        // Extrai o tipo de validação do primeiro código
        String validationType = null;
        String module = null;

        for (String code : error.getCodes()) {
          String[] parts = code.split("\\.");
          if (parts.length > 0) {
            String firstPart = parts[0].toLowerCase();
            // Mapeia tipos de validação
            if (firstPart.equals("notblank") || firstPart.equals("notempty")) {
              validationType = "notblank";
            } else if (firstPart.equals("notnull")) {
              validationType = "notnull";
            } else if (firstPart.equals("size")) {
              validationType = "size";
            } else if (firstPart.equals("min")) {
              validationType = "min";
            } else if (firstPart.equals("max")) {
              validationType = "max";
            } else if (firstPart.equals("email")) {
              validationType = "invalid";
            }

            // Extrai o módulo do nome do DTO (ex: userDTO -> user)
            if (parts.length >= 3 && parts[1].endsWith("DTO")) {
              module = parts[1].replace("DTO", "").toLowerCase();
            }
          }
        }

        // Constrói as chaves possíveis e tenta traduzir
        if (validationType != null) {
          java.util.List<String> possibleKeys = new java.util.ArrayList<>();

          // Adiciona módulo se encontrou
          if (module != null) {
            possibleKeys.add("validation." + module + "." + fieldName + "." + validationType);
          }
          // Sem módulo
          possibleKeys.add("validation." + fieldName + "." + validationType);

          // Tenta cada chave
          for (String key : possibleKeys) {
            try {
              log.debug("Tentando chave: {} com locale: {} (language: {}, country: {})",
                  key, finalLocale, finalLocale.getLanguage(), finalLocale.getCountry());
              log.debug("LocaleContextHolder atual: {}", LocaleContextHolder.getLocale());

              // Usa null como default para forçar busca no locale correto
              // Se usar errorMessage como default, pode retornar a mensagem em português
              String translated = messageSource.getMessage(key, error.getArguments(), null, finalLocale);
              log.debug("Tradução encontrada: {} (original: {})", translated, errorMessage);
              log.debug("Tradução é diferente da original? {}", !translated.equals(errorMessage));
              // Se encontrou uma tradução diferente da original, usa ela
              if (translated != null && !translated.equals(key) && !translated.equals(errorMessage)) {
                errorMessage = translated;
                messageKey = key;
                break;
              }
            } catch (org.springframework.context.NoSuchMessageException e) {
              log.debug("Chave não encontrada: {} no locale: {}", key, finalLocale);
              // Chave não existe neste locale, tenta próxima
            } catch (Exception e) {
              log.debug("Erro ao buscar chave {}: {}", key, e.getMessage());
            }
          }
        }

        // Se ainda não encontrou, tenta pegar o último código como chave
        if (messageKey == null && error.getCodes().length > 0) {
          String lastCode = error.getCodes()[error.getCodes().length - 1].toLowerCase();
          try {
            String translated = messageSource.getMessage(lastCode, error.getArguments(), errorMessage, finalLocale);
            if (translated != null && !translated.equals(lastCode) && !translated.equals(errorMessage)) {
              errorMessage = translated;
              messageKey = lastCode;
            }
          } catch (Exception e) {
            // Ignora
          }
        }
      }

      log.debug("Mensagem traduzida para campo {}: {} (locale: {}, key: {})",
          fieldName, errorMessage, finalLocale, messageKey);
      fieldErrors.put(fieldName, errorMessage);
    });

    String validationFailedMessage = messageSource.getMessage(
        "error.validation.failed", null, "Validation failed", finalLocale);

    errors.put("status", HttpStatus.BAD_REQUEST.value());
    errors.put("message", validationFailedMessage);
    errors.put("errors", fieldErrors);

    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errors);
  }

  @ExceptionHandler(RuntimeException.class)
  public ResponseEntity<Map<String, Object>> handleRuntimeException(RuntimeException ex) {
    log.error("Runtime exception occurred", ex);

    Map<String, Object> error = new HashMap<>();
    error.put("status", HttpStatus.BAD_REQUEST.value());
    error.put("message", ex.getMessage());

    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex, WebRequest request) {
    log.error("Unexpected exception occurred", ex);

    // Usa o locale do LocaleContextHolder que foi definido pelo filtro
    Locale locale = LocaleContextHolder.getLocale() != null
        ? LocaleContextHolder.getLocale()
        : request.getLocale();

    // Normaliza o locale
    if (locale != null) {
      String language = locale.getLanguage().toLowerCase();
      if (language.equals("en")) {
        locale = Locale.ENGLISH;
      } else if (language.equals("pt")) {
        locale = Locale.forLanguageTag("pt-BR");
      }
    }

    String errorMessage = messageSource.getMessage(
        "error.unexpected", null, "An unexpected error occurred", locale);

    Map<String, Object> error = new HashMap<>();
    error.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
    error.put("message", errorMessage);

    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
  }
}
