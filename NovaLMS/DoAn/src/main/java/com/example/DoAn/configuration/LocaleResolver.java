package com.example.DoAn.configuration;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

@Configuration
public class LocaleResolver extends AcceptHeaderLocaleResolver implements WebMvcConfigurer {

    // Vietnamese is primary; English and French as fallbacks
    List<Locale> LOCALES = List.of(
            new Locale("vi", "VN"),
            new Locale("vi"),
            new Locale("en"),
            new Locale("en", "US"),
            new Locale("fr")
    );

    @Override
    public Locale resolveLocale(HttpServletRequest request) {
        String languageHeader = request.getHeader("Accept-Language");
        if (!StringUtils.hasLength(languageHeader)) {
            return new Locale("vi", "VN");   // default: Vietnamese (Vietnam)
        }
        List<Locale.LanguageRange> ranges = Locale.LanguageRange.parse(languageHeader);
        Locale resolved = Locale.lookup(ranges, LOCALES);
        return resolved != null ? resolved : new Locale("vi", "VN"); // fallback to Vietnamese
    }

    @Bean
    public ResourceBundleMessageSource messageSource() {
        ResourceBundleMessageSource rs = new ResourceBundleMessageSource();
        rs.setBasename("messages");
        rs.setDefaultEncoding(StandardCharsets.UTF_8.name());
        rs.setUseCodeAsDefaultMessage(true);
        rs.setCacheSeconds(3600);
        return rs;
    }
}
