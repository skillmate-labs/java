package com.skillmate.skillmate.config;

import java.util.Locale;

import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LocaleContextInterceptor implements HandlerInterceptor {

  private final CustomLocaleResolver localeResolver;

  public LocaleContextInterceptor(CustomLocaleResolver localeResolver) {
    this.localeResolver = localeResolver;
  }

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
      Object handler) {
    // Define o locale no contexto antes da validação
    Locale locale = localeResolver.resolveLocale(request);
    LocaleContextHolder.setLocale(locale, true);
    log.debug("Locale definido no contexto: {}", locale);
    return true;
  }

  @Override
  public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
      Object handler, Exception ex) {
    // Limpa o contexto após a requisição
    LocaleContextHolder.resetLocaleContext();
  }
}
