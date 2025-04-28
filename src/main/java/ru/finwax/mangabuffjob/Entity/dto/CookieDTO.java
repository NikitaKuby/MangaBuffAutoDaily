package ru.finwax.mangabuffjob.Entity.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.openqa.selenium.Cookie;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CookieDTO {
    private String name;
    private String value;
    private String domain;
    private String path;
    private Date expiry;
    private boolean isSecure;
    private boolean isHttpOnly;
    private String sameSite;

    public static CookieDTO fromSeleniumCookie(Cookie cookie) {
        return new CookieDTO(
            cookie.getName(),
            cookie.getValue(),
            cookie.getDomain(),
            cookie.getPath(),
            cookie.getExpiry(),
            cookie.isSecure(),
            cookie.isHttpOnly(),
            cookie.getSameSite()
        );
    }

    public Cookie toSeleniumCookie() {
        return new Cookie.Builder(name, value)
            .domain(domain)
            .path(path)
            .expiresOn(expiry)
            .isSecure(isSecure)
            .isHttpOnly(isHttpOnly)
            .sameSite(sameSite)
            .build();
    }
}