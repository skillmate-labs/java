package com.skillmate.skillmate.config;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.springframework.util.StringUtils;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

import jakarta.servlet.http.HttpServletRequest;

public class CustomLocaleResolver extends AcceptHeaderLocaleResolver {

  private static final String LANG_PARAMETER = "lang";
  private static final Locale DEFAULT_LOCALE = Locale.forLanguageTag("pt-BR");
  private static final Locale LOCALE_PT_BR = Locale.forLanguageTag("pt-BR");
  private static final Locale LOCALE_EN_US = Locale.ENGLISH; // Usa Locale.ENGLISH para messages.properties
  private static final List<Locale> SUPPORTED_LOCALES = Arrays.asList(
      LOCALE_PT_BR,
      LOCALE_EN_US);

  public CustomLocaleResolver() {
    setDefaultLocale(DEFAULT_LOCALE);
    setSupportedLocales(SUPPORTED_LOCALES);
  }

  @Override
  public Locale resolveLocale(HttpServletRequest request) {
    // Primeiro, verifica se há parâmetro 'lang' na query string
    String langParam = request.getParameter(LANG_PARAMETER);
    if (StringUtils.hasText(langParam)) {
      Locale locale = parseLocale(langParam);
      if (locale != null) {
        return locale;
      }
    }

    // Se não houver parâmetro, usa o header Accept-Language (comportamento padrão)
    Locale headerLocale = super.resolveLocale(request);
    Locale normalized = normalizeLocale(headerLocale);
    return normalized;
  }

  private Locale parseLocale(String langParam) {
    // Normaliza o formato: en_US -> en, en -> en, pt_BR -> pt-BR
    String normalized = langParam.replace("_", "-").trim().toLowerCase();

    if (normalized.startsWith("pt")) {
      return LOCALE_PT_BR;
    } else if (normalized.startsWith("en")) {
      // Retorna Locale.ENGLISH que corresponde ao messages.properties (inglês padrão)
      return Locale.ENGLISH;
    }

    return null;
  }

  private Locale normalizeLocale(Locale locale) {
    if (locale == null) {
      return DEFAULT_LOCALE;
    }

    String language = locale.getLanguage().toLowerCase();

    // Normaliza para os locales suportados
    // Usa Locale.US para inglês (o Spring MessageSource reconhece isso)
    // e Locale.forLanguageTag para pt-BR
    if (language.equals("pt")) {
      // Retorna sempre pt-BR, independente do country
      return Locale.forLanguageTag("pt-BR");
    } else if (language.equals("en")) {
      // Retorna sempre Locale.ENGLISH para inglês
      // Locale.ENGLISH corresponde a messages.properties (inglês padrão)
      return Locale.ENGLISH;
    }

    // Se não for suportado, retorna o default
    return DEFAULT_LOCALE;
  }
}
